package au.org.ala.sds.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;

public class GeoLocationHelper {

    final static Logger logger = Logger.getLogger(GeoLocationHelper.class);

    /**
     * Retrieves zones
     *
     * @param latitude
     * @param longitude
     * @return
     * @throws Exception
     */
    public static Set<SensitivityZone> getZonesContainingPoint(String latitude, String longitude) throws Exception {

        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();

        // Call spatial web service for polygon intersections
        URL url = new URL(Configuration.getInstance().getLayersServiceUrl() + "/intersect/" + getLayersForUri() + "/" + latitude + "/" + longitude);
        URLConnection connection = url.openConnection();
        logger.debug("Looking up location using " + url);
        InputStream inStream = connection.getInputStream();

        // Parse JSON result
        logger.debug("Parsing location results");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(inStream, JsonNode.class);
        for (JsonNode node : rootNode) {

            JsonNode valueNode = node.get("value");
            if(valueNode != null){
                String field = node.get("field").getTextValue();
                String value = valueNode.getTextValue();
                if(StringUtils.isNotBlank(value) && StringUtils.isNotBlank(field)){
                    Set<SensitivityZone> auZones = AUWorkarounds.getZones(field, value, latitude, longitude);
                    zones.addAll(auZones);

                    SensitivityZone sensitivityZone = SensitivityZoneFactory.getZoneByName(value);
                    if(sensitivityZone != null){
                        zones.add(sensitivityZone);
                    }
                }
            }
        }

        if (zones.isEmpty()) {
            logger.debug("Zone could not be determined from location: Lat " + latitude + ", Long " + longitude);
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOT_ATLAS_COUNTRY_CODE));
        }
        return zones;
    }

    public static List<SensitivityZone> filterForZoneType(List<SensitivityZone> zones, SensitivityZone.ZoneType type){
        List<SensitivityZone> newZones = new ArrayList<SensitivityZone>();
        for(SensitivityZone zone : zones){
            if(zone.getType() == type)
                newZones.add(zone);
        }
        return newZones;
    }

    private static String getLayersForUri() throws Exception {
        return StringUtils.join(Configuration.getInstance().getGeospatialLayers(), ",");
    }

}
