package au.org.ala.util

import org.apache.commons.lang.time.DateFormatUtils
import org.wyki.cassandra.pelops.Pelops
import au.org.ala.checklist.lucene.HomonymException
import collection.mutable.{HashSet, HashMap, ArrayBuffer}

//import au.org.ala.sds.util.GeneralisedLocationFactory
import java.util.GregorianCalendar
import au.org.ala.checklist.lucene.SearchResultException
import org.slf4j.LoggerFactory
import au.org.ala.biocache._
import au.org.ala.data.model.LinnaeanRankClassification

/**
 * 1. Classification matching
 * 	- include a flag to indicate record hasnt been matched to NSLs
 * 
 * 2. Parse locality information
 * 	- "Vic" -> Victoria
 * 
 * 3. Point matching
 * 	- parse latitude/longitude
 * 	- retrieve associated point mapping
 * 	- check state supplied to state point lies in
 * 	- marine/non-marine/limnetic (need a webservice from BIE)
 * 
 * 4. Type status normalization
 * 	- use GBIF's vocabulary
 * 
 * 5. Date parsing
 * 	- date validation
 * 	- support for date ranges
 * 
 * 6. Collectory lookups for attribution chain
 * 
 * Tests to conform to: http://bit.ly/eqSiFs
 */
object ProcessRecords {

  val logger = LoggerFactory.getLogger("ProcessRecords")
  //Regular expression used to parse an image URL - adapted from http://stackoverflow.com/questions/169625/regex-to-check-if-valid-url-that-ends-in-jpg-png-or-gif#169656
  lazy val imageParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#?]+)+\.(?:jpg|gif|png|jpeg))$""".r
  val afdApniIdentifier = """([:afd.|:apni.])""".r

  def main(args: Array[String]): Unit = {

    logger.info("Starting processing records....")
    processAll
    logger.info("Finished. Shutting down.")
    Pelops.shutdown 
  }

  /**
   * Process all records in the store
   */
  def processAll {
    var counter = 0
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    //page over all records and process
    OccurrenceDAO.pageOverAll(Raw, record => {
      counter += 1
      if (!record.isEmpty) {
        val raw = record.get
        processRecord(raw)

        //debug counter
        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          logger.info(counter + " >> Last key : " + raw.uuid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }
      }
      true
    })
  }

  /**
   * Process a record, adding metadata and records quality systemAssertions
   */
  def processRecord(raw:FullRecord){

    val guid = raw.uuid
    //NC: Changed so that a processed record only contains values that have been processed.
    var processed = new FullRecord//raw.clone
    var assertions = new ArrayBuffer[QualityAssertion]

    //find a classification in NSLs
    assertions ++= processClassification(guid, raw, processed)

    //perform gazetteer lookups - just using point hash for now
    assertions ++= processLocation(guid, raw, processed)

    //temporal processing
    assertions ++= processEvent(guid, raw, processed)

    //basis of record parsing
    assertions ++= processBasisOfRecord(guid, raw, processed)

    //type status normalisation
    assertions ++= processTypeStatus(guid, raw, processed)

    //process the attribution - call out to the Collectory...
    assertions ++= processAttribution(guid, raw, processed)

    //process the images
    assertions ++= processImages(guid, raw, processed)

    //perform SDS lookups - retrieve from BIE for now....
    // processImages
    // processLinkRecord
    // processIdentifierRecords 
    // 

    val systemAssertions = Some(assertions.toArray)
  
    //store the occurrence
    OccurrenceDAO.updateOccurrence(guid, processed, systemAssertions, Processed)
  }
  /**
   * validates that the associated media is a valid image url
   */
  def processImages(guid:String, raw:FullRecord, processed:FullRecord) :Array[QualityAssertion] ={
    val urls = raw.getOccurrence.getAssociatedMedia
    // val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(cl.getter(sg.rank)) => sg.name}
    if(urls != null){
      val aurls = urls.split(";").map(url=> url.trim)
      processed.occurrence.setImages(aurls.filter(isValidImageURL(_)))
      if(aurls.length != processed.occurrence.getImages().length)
          return Array(QualityAssertion(AssertionCodes.INVALID_IMAGE_URL, "URL can not be an image"))
    }
    Array()
  }

  private def isValidImageURL(url:String) : Boolean = {
    imageParser.unapplySeq(url.trim).isEmpty == false
  }

  /**
   * select icm.institution_uid, icm.collection_uid,  ic.code, ic.name, ic.lsid, cc.code from inst_coll_mapping icm
   * inner join institution_code ic ON ic.id = icm.institution_code_id
   * inner join collection_code cc ON cc.id = icm.collection_code_id
   * limit 10;
   */
  def processAttribution(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
    if(raw.occurrence.institutionCode!=null && raw.occurrence.collectionCode!=null){
        val attribution = AttributionDAO.getByCodes(raw.occurrence.institutionCode, raw.occurrence.collectionCode)
        if (!attribution.isEmpty) {
          OccurrenceDAO.updateOccurrence(guid, attribution.get, Processed)
          Array()
        } else {
          Array(QualityAssertion(AssertionCodes.UNRECOGNISED_COLLECTIONCODE, "Unrecognised collection code"))
        }
    } else {
      Array()
    }
  }

  /**
   * Validate the supplied number using the supplied function.
   */
  def validateNumber(number:String, f:(Int=>Boolean) ) : (Int, Boolean) = {
    try {
      if(number != null) {
        val parsedNumber = number.toInt
        (parsedNumber, f(parsedNumber))
      } else {
        (-1, false)
      }
    } catch {
      case e: NumberFormatException => {
        (-1, false)
      }
    }
  }

  /**
   * Date parsing - this is pretty much copied from GBIF source code and needs
   * splitting into several methods
   */
  def processEvent(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    var assertions = new ArrayBuffer[QualityAssertion]
    var date: Option[java.util.Date] = None
    val now = new java.util.Date
    val currentYear = DateFormatUtils.format(now, "yyyy").toInt
    var comment = ""

    var (year,invalidYear) = validateNumber(raw.event.year,{year => year < 0 || year > currentYear})
    var (month,invalidMonth) = validateNumber(raw.event.month,{month => month < 1 || month > 12})
    var (day,invalidDay) = validateNumber(raw.event.day,{day => day < 0 || day > 31})
    var invalidDate = invalidYear || invalidDay || invalidMonth

    //check for sensible year value
    if (year > 0) {
      if (year < 100) {
      //parse 89 for 1989
        if (year > currentYear % 100) {
          // Must be in last century
          year += ((currentYear / 100) - 1) * 100;
        } else {
          // Must be in this century
          year += (currentYear / 100) * 100;
        }
      } else if (year >= 100 && year < 1700) {
        year = -1
        invalidDate = true;
        comment = "Year out of range"
      }
    }

    //construct
    if (year != -1 && month != -1 && day != -1) {
      try {
       val calendar = new GregorianCalendar(
          year.toInt ,
          month.toInt - 1,
          day.toInt
       );
       date = Some(calendar.getTime)
      } catch {
        case e: Exception => {
          invalidDate = true
          comment = "Invalid year, day, month"
        }
      }
    }

    //set the processed values
    if (year != -1) processed.event.year = year.toString
    if (month != -1) processed.event.month = String.format("%02d",int2Integer(month)) //NC ensure that a month is 2 characters long
    if (day != -1) processed.event.day = day.toString
    if (!date.isEmpty) processed.event.eventDate = DateFormatUtils.format(date.get, "yyyy-MM-dd")

    //deal with event date
    if (date.isEmpty && raw.event.eventDate != null && !raw.event.eventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.eventDate)
      if(!parsedDate.isEmpty){
        //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear
      }
    }

    //deal with verbatim date
    if (date.isEmpty && raw.event.verbatimEventDate != null && !raw.event.verbatimEventDate.isEmpty) {
      val parsedDate = DateParser.parseDate(raw.event.verbatimEventDate)
      if(!parsedDate.isEmpty){
        //set processed values
          processed.event.eventDate = parsedDate.get.startDate
          processed.event.day = parsedDate.get.startDay
          processed.event.month = parsedDate.get.startMonth
          processed.event.year = parsedDate.get.startYear
      }
    }

    //if invalid date, add assertion
    if (invalidDate) {
      assertions + QualityAssertion(AssertionCodes.INVALID_COLLECTION_DATE,comment)
    }

    assertions.toArray
  }

  /**
   * Process the type status
   */
  def processTypeStatus(guid:String,raw:FullRecord,processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.occurrence.typeStatus != null && !raw.occurrence.typeStatus.isEmpty) {
      val term = TypeStatus.matchTerm(raw.occurrence.typeStatus)
      if (term.isEmpty) {
        //add a quality assertion
        Array(QualityAssertion(AssertionCodes.UNRECOGNISED_TYPESTATUS,"Unrecognised type status"))
      } else {
        processed.occurrence.typeStatus = term.get.canonical
        Array()
      }
    } else {
      //NC I don't think that a missing type status needs to be reported in a QA
      // It should only be reported if we know for sure that an occurrence record is a typification record with a missing type
      //Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,false,"Missing type status"))
      Array()
    }
  }

  /**
   * Process basis of record
   */
  def processBasisOfRecord(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    if (raw.occurrence.basisOfRecord == null || raw.occurrence.basisOfRecord.isEmpty) {
      //add a quality assertion
      Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,"Missing basis of record"))
    } else {
      val term = BasisOfRecord.matchTerm(raw.occurrence.basisOfRecord)
      if (term.isEmpty) {
        //add a quality assertion
        logger.debug("[QualityAssertion] " + guid + ", unrecognised BoR: " + guid + ", BoR:" + raw.occurrence.basisOfRecord)
        Array(QualityAssertion(AssertionCodes.MISSING_BASIS_OF_RECORD,"Unrecognised basis of record"))
      } else {
        processed.occurrence.basisOfRecord = term.get.canonical
        Array[QualityAssertion]()
      }
    }
  }

  /**
   * Process geospatial details
   *
   * TODO: Handle latitude and longitude that is supplied in verbatim format
   * We will need to parse a variety of formats. Bryn was going to find some regular
   * expressions/test cases he has used previously...
   *
   */
  def processLocation(guid:String,raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {
    //retrieve the point
    var assertions = new ArrayBuffer[QualityAssertion]

    if (raw.location.decimalLatitude != null && raw.location.decimalLongitude != null) {
      //retrieve the species profile
      val taxonProfile = TaxonProfileDAO.getByGuid(processed.classification.taxonConceptID)

      //TODO validate decimal degrees and parse degrees, minutes, seconds format
      processed.location.decimalLatitude = raw.location.decimalLatitude
      processed.location.decimalLongitude = raw.location.decimalLongitude

      //validate coordinate accuracy (coordinateUncertaintyInMeters) and coordinatePrecision (precision - A. Chapman)
      if(raw.location.coordinateUncertaintyInMeters!=null && raw.location.coordinateUncertaintyInMeters.length>0){
          //parse it into a numeric number in metres
          val parsedValue = DistanceRangeParser.parse(raw.location.coordinateUncertaintyInMeters)
          if(!parsedValue.isEmpty)
            processed.location.coordinateUncertaintyInMeters = parsedValue.get.toString
      }

      //generate coordinate accuracy if not supplied
      val point = LocationDAO.getByLatLon(raw.location.decimalLatitude, raw.location.decimalLongitude);
      if (!point.isEmpty) {

        //add state information
        processed.location.stateProvince = point.get.stateProvince
        processed.location.ibra = point.get.ibra
        processed.location.imcra = point.get.imcra
        processed.location.lga = point.get.lga
        processed.location.habitat = point.get.habitat
        //add the environmental information
        processed.location.bioclim_bio11 = point.get.bioclim_bio11
        processed.location.bioclim_bio12 = point.get.bioclim_bio12
        processed.location.bioclim_bio34 = point.get.bioclim_bio34
        processed.location.mean_temperature_cars2009a_band1 =point.get.mean_temperature_cars2009a_band1
        processed.location.mean_oxygen_cars2006_band1 = point.get.mean_oxygen_cars2006_band1

        //TODO - replace with country association with points via the gazetteer
        if(processed.location.imcra!=null && !processed.location.imcra.isEmpty
            || processed.location.ibra!=null && !processed.location.ibra.isEmpty){
            processed.location.country = "Australia"
        }

        //check matched stateProvince
        if (processed.location.stateProvince != null && raw.location.stateProvince != null) {
          //quality systemAssertions
          val stateTerm = States.matchTerm(raw.location.stateProvince)

          if (!stateTerm.isEmpty && !processed.location.stateProvince.equalsIgnoreCase(stateTerm.get.canonical)) {
            logger.debug("[QualityAssertion] " + guid + ", processed:" + processed.location.stateProvince 
                + ", raw:" + raw.location.stateProvince)
            //add a quality assertion
            val comment = "Supplied: " + stateTerm.get.canonical + ", calculated: " + processed.location.stateProvince
            assertions + QualityAssertion(AssertionCodes.STATE_COORDINATE_MISMATCH,comment)
            //store the assertion
          }
        }

//        //check to see if the points need to generalised
//        if(!taxonProfile.isEmpty && taxonProfile.get.sensitive!= null && !taxonProfile.get.sensitive.isEmpty && processed.location.country == "Australia"){
//          //Call SDS code to get the revised coordinates
//          val ss =DAO.sensitiveSpeciesFinderFactory.findSensitiveSpeciesByLsid(processed.classification.taxonConceptID)
//          if(ss != null){
//            //get the genralised coordinates
//            val gl = GeneralisedLocationFactory.getGeneralisedLocation(raw.location.decimalLatitude, raw.location.decimalLongitude, ss, processed.location.stateProvince);
//            if(gl != null){
//              //check to see if the coordinates have changed
//              if(gl.isGeneralised){
//                logger.debug("Generalised coordinates for " +guid)
//
//                //store the generalised values as the raw.location.decimalLatitude/Longitude
//                //store the orginal as a hidden value
//                raw.location.originalDecimalLatitude = raw.location.decimalLatitude
//                raw.location.originalDecimlaLongitude = raw.location.decimalLongitude
//                raw.location.decimalLatitude = gl.getGeneralisedLatitude
//                raw.location.decimalLongitude = gl.getGeneralisedLongitude
//                //update the raw values
//                val umap = Map[String, String]("originalDecimalLatitude"-> raw.location.originalDecimalLatitude,
//                                               "originalDecimalLongitude"-> raw.location.originalDecimlaLongitude,
//                                            "decimalLatitude" -> raw.location.decimalLatitude,
//                                            "decimalLongitude" -> raw.location.decimalLongitude)
//
//                DAO.persistentManager.put(guid,"occ",umap)
//                processed.location.decimalLatitude = gl.getGeneralisedLatitude
//                processed.location.decimalLongitude = gl.getGeneralisedLongitude
//                //TODO may need to fix locality information... change ths so that the generalisation is performed before the point matching to gazetteer...
//              }
//
//            }
//          }
//        }

        //check marine/non-marine
        if(processed.location.habitat!=null){

          if(!taxonProfile.isEmpty && taxonProfile.get.habitats!=null && !taxonProfile.get.habitats.isEmpty){
            val habitatsAsString =  taxonProfile.get.habitats.reduceLeft(_+","+_)
            val habitatFromPoint = processed.location.habitat
            val habitatsForSpecies = taxonProfile.get.habitats
            //is "terrestrial" the same as "non-marine" ??
            val validHabitat = HabitatMap.areTermsCompatible(habitatFromPoint, habitatsForSpecies)
            if(!validHabitat.isEmpty){
              if(!validHabitat.get){
                if(habitatsAsString != "???"){ //HACK FOR BAD DATA
                  logger.debug("[QualityAssertion] ******** Habitats incompatible for UUID: " + guid + ", processed:" 
                      + processed.location.habitat + ", retrieved:" + habitatsAsString
                      + ", http://maps.google.com/?ll="+processed.location.decimalLatitude+","
                      + processed.location.decimalLongitude)
                  val comment = "Recognised habitats for species: " + habitatsAsString +
                       ", Value determined from coordinates: " + habitatFromPoint
                  assertions + QualityAssertion(AssertionCodes.COORDINATE_HABITAT_MISMATCH,comment)
                }
              }
            }
          }
        }

        //TODO check centre point of the state
        if(StateCentrePoints.coordinatesMatchCentre(point.get.stateProvince, raw.location.decimalLatitude, raw.location.decimalLongitude)){
          assertions + QualityAssertion(AssertionCodes.COORDINATES_CENTRE_OF_STATEPROVINCE,"Coordinates are centre point of "+point.get.stateProvince)
        }

        if(raw.location.decimalLatitude.toDouble == 0.0d && raw.location.decimalLongitude.toDouble == 0.0d ){
            assertions + QualityAssertion(AssertionCodes.ZERO_COORDINATES,"Coordinates 0,0")
        }
      }
    }
    //Only process the raw state value if no latitude and longitude is provided
    if(processed.location.stateProvince ==null && raw.location.decimalLatitude ==null && raw.location.decimalLongitude ==null){
      //process the supplied state
      val stateTerm = States.matchTerm(raw.location.stateProvince)
      if(!stateTerm.isEmpty){
        processed.location.stateProvince = stateTerm.get.canonical
      }
    }
    assertions.toArray
  }

  /**
   * Parse the hints into a usable map with rank -> Set.
   */
  def parseHints(taxonHints:List[String]) : Map[String,Set[String]] = {
      //println("Taxonhints: "  + taxonHints)
      //parse taxon hints into rank : List of
      val rankSciNames = new HashMap[String,Set[String]]
      val pairs = taxonHints.map(x=> x.split(":"))
      for(pair <- pairs){
          val values = rankSciNames.getOrElse(pair(0),Set())
          rankSciNames.put(pair(0), values + pair(1).trim.toLowerCase)
      }
      rankSciNames.toMap
  }

  /**
   * Returns false if the any of the taxonomic hints conflict with the classification
   */
  def isMatchValid(classification:LinnaeanRankClassification, hintMap:Map[String,Set[String]]) : (Boolean, String) = {
      //println("Classification: "  + classification)
      //are there any conflicts??
      for(rank <- hintMap.keys){
          val (conflict, comment) = {
              rank match {
                  case "kingdom" => (classification.getKingdom()!=null && !hintMap.get(rank).get.contains(classification.getKingdom().toLowerCase) , "Kingdom:"+classification.getKingdom() )
                  case "phylum"  => (classification.getPhylum()!=null  && !hintMap.get(rank).get.contains(classification.getPhylum().toLowerCase) , "Phylum:"+classification.getPhylum() )
                  case "class"   => (classification.getKlass()!=null   && !hintMap.get(rank).get.contains(classification.getKlass().toLowerCase), "Class:"+classification.getKlass() )
                  case "order"   => (classification.getOrder()!=null   && !hintMap.get(rank).get.contains(classification.getOrder().toLowerCase) , "Order:"+classification.getOrder() )
                  case "family"  => (classification.getFamily()!=null  && !hintMap.get(rank).get.contains(classification.getFamily().toLowerCase) , "Family:"+classification.getFamily() )
                  case _ => (false, "")
              }
          }
          if(conflict) return (false, comment)
      }
      (true, "")
  }

  /**
   * Match the classification
   */
  def processClassification(guid:String, raw:FullRecord, processed:FullRecord) : Array[QualityAssertion] = {

    //logger.debug("Record: "+occ.uuid+", classification for Kingdom: "+occ.kingdom+", Family:"+  occ.family +", Genus:"+  occ.genus +", Species: " +occ.species+", Epithet: " +occ.specificEpithet)
    try {
      //val nsr = DAO.nameIndex.searchForRecord(classification, true)
      val nsr = ClassificationDAO.getByHashLRU(raw.classification).getOrElse(null)

      //store the matched classification
      if (nsr != null) {
        val classification = nsr.getRankClassification
        //Check to see if the classification fits in with the supplied taxonomic hints
        //get the Attribution
        if(raw.occurrence.institutionCode!=null && raw.occurrence.collectionCode!=null){

          val attribution = AttributionDAO.getByCodes(raw.occurrence.institutionCode, raw.occurrence.collectionCode)

          if(!attribution.isEmpty){
            logger.debug("Checking taxonomic hints")
            val taxonHints = attribution.get.taxonomicHints

            if(taxonHints != null && !taxonHints.isEmpty){
              //TODO this map should be cacheable
              //val hintMap = parseHints(taxonHints.toList)

              val (isValid, comment) = isMatchValid(classification, attribution.get.retrieveParseHints)
              if(!isValid){
                  logger.info("Conflict in matched classification. Matched: " + guid+ ", Matched: "+comment+", Taxonomic hints in use: " + taxonHints.toList)
                  return Array(QualityAssertion(AssertionCodes.TAXONOMIC_ISSUE, "Conflict in matched classification. Matched: "+ comment))
              }
            }
          }
        }
        //store ".p" values
        processed.classification.kingdom = classification.getKingdom
        processed.classification.kingdomID = classification.getKid
        processed.classification.phylum = classification.getPhylum
        processed.classification.phylumID = classification.getPid
        processed.classification.classs = classification.getKlass
        processed.classification.classID = classification.getCid
        processed.classification.order = classification.getOrder
        processed.classification.orderID = classification.getOid
        processed.classification.family = classification.getFamily
        processed.classification.familyID = classification.getFid
        processed.classification.genus = classification.getGenus
        processed.classification.genusID = classification.getGid
        processed.classification.species = classification.getSpecies
        processed.classification.speciesID = classification.getSid
        processed.classification.specificEpithet = classification.getSpecificEpithet
        processed.classification.scientificName = classification.getScientificName
        processed.classification.taxonConceptID = nsr.getLsid
        processed.classification.left = nsr.getLeft
        processed.classification.right = nsr.getRight
        processed.classification.taxonRank = nsr.getRank.getRank
        processed.classification.taxonRankID = nsr.getRank.getId.toString
        //try to apply the vernacular name
        val taxonProfile = TaxonProfileDAO.getByGuid(nsr.getLsid)
        if(!taxonProfile.isEmpty && taxonProfile.get.commonName!=null){
          processed.classification.vernacularName = taxonProfile.get.commonName
        }

        //Add the species group information - I think that it is better to store this value than calculate it at index time
        val speciesGroups = SpeciesGroups.getSpeciesGroups(processed.classification)
        logger.debug("Species Groups: " + speciesGroups)
        if(!speciesGroups.isEmpty && !speciesGroups.get.isEmpty){
          processed.classification.speciesGroups = speciesGroups.get.toArray[String]
        }

        //is the name in the NSLs ???
        if(afdApniIdentifier.findFirstMatchIn(nsr.getLsid).isEmpty){
           Array(QualityAssertion(AssertionCodes.NAME_NOT_IN_NATIONAL_CHECKLISTS, "Record not attached to concept in national species lists"))
        } else {
           Array()
        }

      } else {
        logger.debug("[QualityAssertion] No match for record, classification for Kingdom: " +
            raw.classification.kingdom + ", Family:" + raw.classification.family + ", Genus:" + raw.classification.genus +
            ", Species: " + raw.classification.species + ", Epithet: " + raw.classification.specificEpithet)
        Array(QualityAssertion(AssertionCodes.NAME_NOTRECOGNISED, "Name not recognised"))
      }
    } catch {
      case he: HomonymException => logger.debug(he.getMessage,he); Array(QualityAssertion(AssertionCodes.HOMONYM_ISSUE, "Homonym issue resolving the classification"))
      case se: SearchResultException => logger.debug(se.getMessage,se); Array()
    }
  }
}