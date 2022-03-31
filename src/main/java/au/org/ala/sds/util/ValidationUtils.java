/**
 *
 */
package au.org.ala.sds.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import au.org.ala.sds.SensitiveDataService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.MessageFactory;
import au.org.ala.sds.validation.ValidationReport;
import org.gbif.dwc.terms.DwcTerm;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationUtils {

    protected static final Logger logger = Logger.getLogger(ValidationUtils.class);

    public static boolean validateLocation(FactCollection facts, ValidationReport report) {

        String country = facts.get(FactCollection.COUNTRY_KEY);
        String stateProvince = facts.get(FactCollection.STATE_PROVINCE_KEY);
        String decimalLatitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String decimalLongitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);

        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();

        SensitivityZone countryZone = null;
        SensitivityZone stateProvinceZone = null;

        //if country supplied add a zone for it
        if (StringUtils.isNotBlank(country)) {
            if (country.equalsIgnoreCase(SensitivityZoneFactory.getZone(SensitivityZone.ATLAS_COUNTRY_CODE).getName())) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.ATLAS_COUNTRY_CODE));
            } else {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOT_ATLAS_COUNTRY_CODE));

                countryZone = SensitivityZoneFactory.getZoneByName(country);
                if(countryZone != null){
                    zones.add(countryZone);
                }
            }
        }

        //if stateProvince supplied add a zone for it
        if (StringUtils.isNotBlank(stateProvince)) {
            stateProvinceZone = SensitivityZoneFactory.getZoneByName(stateProvince.toUpperCase());
            if(stateProvinceZone != null){
                zones.add(stateProvinceZone);
            }
        }

        if (!facts.contains(SensitiveDataService.SAMPLED_VALUES_PROVIDED) /*&& StringUtils.isNotBlank(decimalLatitude) && StringUtils.isNotBlank(decimalLongitude)*/) {
            logger.debug("Performing sampling using web services....");
            if (StringUtils.isNotBlank(decimalLatitude) && StringUtils.isNotBlank(decimalLongitude)) {
                try {
                    zones = GeoLocationHelper.getZonesContainingPoint(decimalLatitude, decimalLongitude);
                } catch (Exception e) {
                    logger.error("Problem getting zone from lat/long", e);
                }
            }
        } else {
            logger.debug("Sampling in SDS avoided. Sample values are provided.");

            //TODO add sensitivity zones for sampled values ?????

//            SensitivityZone zone = SensitivityZoneFactory.getZoneByName(stateProvince);
//            if (zone == null) {
//                if (StringUtils.isNotBlank(country) && !country.equalsIgnoreCase("Australia") && !country.equalsIgnoreCase("AUS")) {
//                    report.addMessage(MessageFactory.createErrorMessage(MessageFactory.NOT_AUSTRALIA, country));
//                } else {
//                    report.addMessage(MessageFactory.createErrorMessage(MessageFactory.STATE_INVALID, stateProvince));
//                }
//                return false;
//            }
//            if(zone != null){
//                zones.add(zone);
//            }
        }

        facts.add(FactCollection.ZONES_KEY, zones.toString());
        return true;
    }

    public static boolean validateLocationCoords(FactCollection facts, ValidationReport report) {

        String decimalLatitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String decimalLongitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);

        if (StringUtils.isBlank(decimalLatitude) || StringUtils.isBlank(decimalLongitude)) {
            if (StringUtils.isBlank(facts.get(FactCollection.STATE_PROVINCE_KEY)) && StringUtils.isBlank(facts.get(FactCollection.COUNTRY_KEY))) {
                report.addMessage(MessageFactory.createInfoMessage(MessageFactory.LOCATION_MISSING));
                return false;
            } else {
                return true;
            }
        }

        return isValidNumber(decimalLatitude) && isValidNumber(decimalLongitude);
    }

    public static boolean isValidNumber(String number) {
        if (StringUtils.isBlank(number)) {
            return false;
        }

        try {
            Float.parseFloat(number);
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }

    /**
     * Restricts the supplied properties to ones that are allowed to be displayed in a PEST.
     *
     * @param properties
     * @return
     */
    public static Map<String,Object> restrictForPests(Map<String,String>properties){
        return restrictToTypes(
                properties,
                new String[]{"dataResourceUid","institutionCode","collectionCode","gridReference"},
                "Taxon");
    }

    /**
     * Restricts the supplied map to the DWC fields in the "types" AND the extraFields.
     * @param properties
     * @param extraFields
     * @param types
     * @return
     */
    public static Map<String,Object> restrictToTypes(Map<String,String>properties,String[] extraFields,String...types){
        List<String> list = new java.util.ArrayList<String>();
        for(String type: types){
            List<DwcTerm> terms = DwcTerm.listByGroup(type);
            for(DwcTerm term:terms)
                list.add(term.simpleName());
        }
        if(extraFields != null){
            for(String f : extraFields)
                list.add(f);
        }
        //System.out.println("LIST: " + list);
        return restrictProperties(properties, list, false);

    }

    /**
     * Modifies the supplied map so that it contains either only the properties in the list OR doesn't
     * contain the properties in the list. The action depends on the isBlacklist setting
     * @param properties
     * @param list
     * @param isBlacklist
     */
    public static Map<String, Object> restrictProperties(Map<String,String> properties, List<String> list, boolean isBlacklist){
        Map<String, String> originalSensitiveValues = new HashMap<String, String>();
        Map<String, Object> results = new HashMap<String, Object>();
        for(String key : properties.keySet()){
            String value = properties.get(key);
            if(StringUtils.isNotBlank(value) && ((list.contains(key) && isBlacklist) || (!list.contains(key) && !isBlacklist))){
                //add it to the original sensitive values
                results.put(key, "");
                originalSensitiveValues.put(key, value);
            } else {
                //we can return the property
                results.put(key, value);
            }
        }
        //at the end add the original sensitive values
        results.put("originalSensitiveValues", originalSensitiveValues);
        return results;
    }
}