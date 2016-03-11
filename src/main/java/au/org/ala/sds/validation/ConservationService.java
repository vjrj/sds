/**
 *
 */
package au.org.ala.sds.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.GeneralisedLocation;
import au.org.ala.sds.util.GeneralisedLocationFactory;
import au.org.ala.sds.util.ValidationUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ConservationService implements ValidationService {

    protected static final Logger logger = Logger.getLogger(ConservationService.class);

    //TODO perhaps a better way would be to populate this from the collectory
    private static final List<String> BIRDS_AUSTRALIA = Arrays.asList(new String[]{"dr359", "dr570", "dr571"});

    private ReportFactory reportFactory;
    private final SensitiveTaxon taxon;

    public ConservationService(SensitiveTaxon taxon, ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
        this.taxon = taxon;
    }

    /**
     * Validate an occurrence return a validation outcome.
     *
     * @param occurrence a map of biocache information
     * @return
     */
    public ValidationOutcome validate(Map<String, String> occurrence) {

        FactCollection facts = new FactCollection(occurrence);
        ValidationReport report = reportFactory.createValidationReport(taxon);

        // Validate location
        if (!ValidationUtils.validateLocationCoords(facts, report)) {
            return new ValidationOutcome(report, false);
        }
        if (!ValidationUtils.validateLocation(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        // Assemble parameters for location generalisation
        String latitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String longitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);
        String zonesString = facts.get(FactCollection.ZONES_KEY);  //this is a serialised list []

        List<SensitivityZone> zones = SensitivityZone.getListFromString(zonesString);
        List<SensitivityInstance> instances = taxon.getInstancesForZones(zones);

        // Check data provider (Birds Australia generalisation only happens for BA occurrences)
        if (facts.get("dataResourceUid") == null || !BIRDS_AUSTRALIA.contains(facts.get("dataResourceUid"))) {
            SensitivityInstance.removeInstance(instances, SensitivityInstance.BIRDS_AUSTRALIA_INSTANCE);
        }

        // Generalise location
        GeneralisedLocation gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, instances, zones);
        ValidationOutcome outcome = new ValidationOutcome(report);

        // Assemble result map
        Map<String, Object> results = new HashMap<String, Object>();
        Map<String, String> originalSensitiveValues = new HashMap<String, String>();

        if (gl.isSensitive()) {
            StringBuilder extra = new StringBuilder();
            for(SensitivityInstance si : gl.getSensitivityInstances()){
                if(extra.length() > 0) {
                    extra.append("\t");
                } else if(si != null) {
                    extra.append("\n");
                    if(si != null) {
                        extra.append("Sensitive in ")
                            .append(si.getZone());
                    }
                    if(si != null && si.getCategory() != null && si.getAuthority() != null){
                        extra.append(" [")
                            .append(si.getCategory().getValue())
                            .append(", ").append(si.getAuthority()).append("]");
                    }
                }
            }
            String extraDesc = extra.toString();
            outcome.setSensitive(true);
            results.put(FactCollection.DECIMAL_LATITUDE_KEY, gl.getGeneralisedLatitude());
            results.put(FactCollection.DECIMAL_LONGITUDE_KEY, gl.getGeneralisedLongitude());
            originalSensitiveValues.put(FactCollection.DECIMAL_LATITUDE_KEY, gl.getOriginalLatitude());
            originalSensitiveValues.put(FactCollection.DECIMAL_LONGITUDE_KEY, gl.getOriginalLongitude());
            results.put("generalisationInMetres", gl.getGeneralisationInMetres());
            results.put("dataGeneralizations", gl.getDescription() + ". " + extraDesc);

            emptyValueIfNecessary("locationRemarks", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("verbatimLatitude", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("verbatimLongitude", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("locality", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("verbatimLocality", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("verbatimCoordinates", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("footprintWKT", occurrence, originalSensitiveValues, results);

            if (gl.getGeneralisationInMetres().equals("") && gl.getGeneralisedLatitude() != null && gl.getGeneralisedLatitude().equals("")) {
                results.put("informationWithheld", "Location co-ordinates have been withheld in accordance with " + facts.get(FactCollection.STATE_PROVINCE_KEY) + " sensitive species policy");
            }
        } else {
            outcome.setSensitive(false);
        }

        // Handle Birds Australia occurrences
        if (facts.get("dataResourceUid") != null && BIRDS_AUSTRALIA.contains(facts.get("dataResourceUid"))) {
            emptyValueIfNecessary("eventID", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("day", occurrence, originalSensitiveValues, results);
            emptyValueIfNecessary("eventDate", occurrence, originalSensitiveValues, results);
            results.put("informationWithheld", "The eventID and day information has been withheld in accordance with Birds Australia data policy");
        }

        results.put("originalSensitiveValues", originalSensitiveValues);
        outcome.setResult(results);
        outcome.setValid(true);
        outcome.setLoadable(true);
        return outcome;
    }

    private void emptyValueIfNecessary(String field, Map<String,String> facts, Map<String,String> originalSensitiveValues, Map<String,Object> results){
        if(facts.containsKey(field)){
            results.put(field, "");
            originalSensitiveValues.put(field, facts.get(field));
        }
    }

    public void setReportFactory(ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }
}
