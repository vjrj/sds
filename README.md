# sds   [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/sds.svg?branch#master)](https://travis-ci.org/AtlasOfLivingAustralia/sds)


The sensitive data service manages sensitivity concerns in the conservation and biosecurity areas.

The sensitive species are supplied via external agencies and are currently stored and maintained in the list tool:
http://lists.ala.org.au/public/speciesLists?isSDS#eq:true

The sds api relies on the sds-webapp2 to generate and supply XML configuration files as listed:
http://sds.ala.org.au

## LIST TOOL REQUIREMENTS

When a user is modifying or adding an SDS list the following information is required as list metadata (potentially
this metadata should be moved to the collectory we put it in the list tool to prevent modifications to collectory):

Region - the region of Australia in which this is applied.  This needs to be the id of one of the regions defined here:

http://sds.ala.org.au/sensitivity-zones.xml

Authority - an acronym for the organisation under whose authority it is listed

Category (optional) - The category for the sensitivity. When this is provided at the list level it applies to all items
on the list (Example http://lists.ala.org.au/speciesListItem/listAuth/dr878).  If it is not supplied the list needs to have
a Category property that is populated for each item (Example
http://lists.ala.org.au/speciesListItem/listAuth/dr493).  The value of the category must be an id of one of the categories
defined here:

http://sds.ala.org.au/sensitivity-categories.xml

Generalisation (optional) - how to generalise the coordinates, mainly used for conservation species. Supported values include
WITHHOLD, 10km, 1km, 100m.  If this is supplied at the list level it will apply to all species. If it has not been supplied
 at the list level it must be supplied as a property for each record.

SDS Type - either PLANT_PEST or CONSERVATION.

### SENSITIVE SPECIES XML GENERATION
This is performed automatically by the sds-webapp2, it checks the list tool regularly to see if it needs to update the list.
It uses au.org.ala.sds.util.SensitiveSpeciesXmlBuilder#generateFromWebservices
located in this project.

### SDS
The SDS can be configured using an external properties file that is located in the /data/sds/sds-config.properties, this in contrary
to where we put the configuration files for other apps and should probably be changed. Configuration options are:

 * species-data - a url (either file:/// or http://) to the xml file that contains all the sensitive species. This defaults
               to the http://sds.ala.org.au/sensitive-species-data.xml which is the correct value to use in most situations.
 * category-data - the url to the category xml file, by default http://sds.ala.org.au/sensitivity-categories.xml

 * zone-data - thw url to the zone xml file , by default http://sds.ala.org.au/sensitivity-zones.xml

 * species-cache - a boolean value to indicate whether or not to read the sensitive species from the cache file. You can set
this to true to prevent all the sensitive species needing to be rematched. This will improve startup time for the sensitive
data service. The disadvantage to this is you will not automatically get the new updates to the sensitive list flowing through.

 * cache-data - the location in which to cache the sensitive species data, by default /data/sds/species-cache.ser

 * spatial-layer-ws - the URL to test for intersection of spatial layers. This is used by the SDS if the required layer values
are not provided in the data.  The biocache will always provide the layer information to prevent WS bottleneck. The default
value for the property is http://spatial.ala.org.au/layers-service/intersect/

 * namematching-index - The location to the name matching index. The SDS uses the name matching index to ensure that synonyms
to sensitive species are correctly determined. This property is only used by the Tests and sds-webapp2.  When you construct
your sensitive data service you will need to an ALASearcher to be used:
au.org.ala.sds.SensitiveSpeciesFinderFactory#getSensitiveSpeciesFinder(String dataUrl, ALANameSearcher nameSearcher).  The
default value for this is /data/lucene/namematching_v13 which should be overridden with the correct value.

 * list-url - the URL to the list tool that is used to generate the species xml file, by default http://lists.ala.org.au

 * flag-rules - a CSV value of source fields that are used as a flag rule for the plant pest rules. This is very half baked
because there was never any indication of what the flag would be called etc.  As there are no flag rules currently provided
this option can be safely ignored.

There are multiple entry points into the SDS to test for sensitivity. You can either use the SensitiveDataService to automatcially
handle the determination of sensitivity and application of the rules.  OR you can generate your own SensitiveSpeciesFinder
and handle the validation yourself via the ValidationService.

It is probably easiest to use the SensitiveDataService#testMapDetails methods. They take a map of darwinCoreTerms and return a
ValidationOutcome. The validation Outcome contains whether or not the details were considered sensitive and a map of values
to use instead of the supplied values. It will also contain error reports and emails that need to be sent as notifications
in a plant pest situation.

### CONSERVATION RULES ###
There are 2 different classes of conservation rules, state provided and data resource provided. State provided rules need
to be applied to all records that fall within the state that supplied the rules.  Whereas data resource provided rules
are only applied to records that are supplied by the same data resource that supplied the rule.  At the moment we only
have one data resource provided list and this is http://lists.ala.org.au/speciesListItem/listAuth/dr494 this is applied to
all the Bird Life data resource; dr359, dr570 and dr571.

When are species is identified as a conservation the SDS:
1. Generalises the coordinates based on the configured amount. Populating the [dataGeneralizations](http://rs.tdwg.org/dwc/terms/dataGeneralizations) 
and/or [informationWithheld](http://rs.tdwg.org/dwc/terms/informationWithheld) properties in the return map.

2. It removes values in the locationRemarks, verbatimLatitude,verbatimLongitude, locality, verbatimCoordinates and footprintWKT
fields.

3. It supplies the original values in an "originalSensitiveValues" map - this is used by the biocache so that the original
values can be provided under the correct authority.

### PLANT_PEST RULES THEORY ###
There are 10 categories of plant pest rules that have been defined in <TO DO LINK TO PDF doc>.

* **Category 1** - Not known to occur in Australia - supplied by:
  * http://lists.ala.org.au/speciesListItem/listAuth/dr945
  * http://lists.ala.org.au/speciesListItem/listAuth/dr946
  * http://lists.ala.org.au/speciesListItem/listAuth/dr873
  * http://lists.ala.org.au/speciesListItem/listAuth/dr872

According to the original documentation this needs to be applied first. But subsequent talks with APPD indicate that this
may be a catch all rule that should be applied only if no other rules are matched. There is a test case in
PlantPestNotKnownInAustraliaTest to test this, it currently fails. We were waiting to confirm that this is the case before
making the changes in the SDS.

* **Category 2** - Pest absent due to eradication - Currently no lists available. This is an action item for Ian from the APPD
meeting on the 30/01/2014

* **Category 3** - Pest present under eradication - Currently no lists available. This is an action item for Ian from the APPD
meeting on the 30/01/2014

* **Category 4** - Pest present subject to official control - Currently no lists available. This is an action item for Ian from the APPD
meeting on the 30/01/2014

* **Category 5** - In quarantine or other plant health zones - Currently only implemented for Bactrocera tryoni.

* **Category 6** - Notifiable pests according to State or Territory legislation - supplied by
  * [ACT](http://lists.ala.org.au/speciesListItem/listAuth/dr947)
  * [NSW](http://lists.ala.org.au/speciesListItem/listAuth/dr877)
  * [NT](http://lists.ala.org.au/speciesListItem/listAuth/dr878)
  * [QLD](http://lists.ala.org.au/speciesListItem/listAuth/dr879)
  * [SA](http://lists.ala.org.au/speciesListItem/listAuth/dr880)
  * [TAS](http://lists.ala.org.au/speciesListItem/listAuth/dr881)
  * [VIC](http://lists.ala.org.au/speciesListItem/listAuth/dr882)
  * [WA](http://lists.ala.org.au/speciesListItem/listAuth/dr883)

* **Category 7** - Absent interception only - partially implemented based on flag in record but will not be fully supported because
it is too difficult for APPD to generate lists.

* **Category 8** - Transient non actionable - it is implemented as per the specification but there is no list available. There
is no plan for APPD to generate a list in the near future. It would require a specific project to generate the list.

* **Category 9** - Exotic Biological Control Agent - it is implemented as per the rule. Currently no lists available. This is an
action item for Ian from the APPD action item for Ian from the APPD

* **Category 10** - Identification to Genus or higher taxon - it is implemented. Currently no lists available.

These categories has specific notification requirements as per <TO DO link to XLSX document>.  These are not implemented
within the SDS. The SDS will provide the necessary messages it will be up to the individual components to perform the required
actions. At the moment the biocache DOES NOT handle the notifications correctly.  We were waiting for the SDS to have complete
lists and updated contacts.

### PLANT PEST RULES IMPLEMENTATION ###

The Plant Pest rules are implemented using Drools, a rules based engine. The Rules are defined in drl files that are
located in the src/resources directory.  The category to which a sensitive species belongs dictates which rule is applied.

The rules will determine whether or not a record can be loaded, the email alerts that need to be sent and the warning
messages that should be displayed.
