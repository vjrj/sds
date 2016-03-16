package au.org.ala.sds.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;

public class GeoLocationHelper {

    final static Logger logger = Logger.getLogger(GeoLocationHelper.class);

    public final static String COASTAL_WATERS_LAYER = "cl927";
    public final static String LGA_BOUNDARIES_LAYER = "cl23";
    public final static String TSPZ_LAYER = "cl937";
    public final static String TSSQZ_LAYER = "cl941";
    public final static String FFEZ_TRI_STATE_LAYER = "cl938";
    public final static String PCN_VIC_LAYER = "cl939";
    public final static String PIZ_NSW_ALBURY_LAYER = "cl936";
    public final static String PIZ_NSW_SYDNEY_LAYER = "cl940";
    public final static String PIZ_VIC_NORTH_EAST_LAYER = "cl963";
    public final static String PIZ_VIC_MAROONDAH_LAYER = "cl962";
    public final static String PIZ_VIC_NAGAMBIE_LAYER = "cl961";
    public final static String PIZ_VIC_MOOROOPNA_LAYER = "cl960";
    public final static String PIZ_VIC_UPTON_LAYER = "cl964";
    public final static String PIZ_VIC_WHITEBRIDGE_LAYER = "cl965";
    public final static String STATES_TERRITORIES_LAYER ="cl22";

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
        URL url = new URL(Configuration.getInstance().getSpatialUrl() + getLayersForUri() + "/" + latitude + "/" + longitude);
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
                String value = valueNode.getTextValue();
                if(StringUtils.isNotBlank(value)){
                    SensitivityZone sensitivityZone = SensitivityZoneFactory.getZoneByName(value);
                    if(sensitivityZone != null){
                        zones.add(sensitivityZone);
                    }
                }
            }
//
//            if (field.equalsIgnoreCase(COASTAL_WATERS_LAYER)) {
//                String state = value.replace(" (including Coastal Waters)", "");
//                state = state.replace("Captial", "Capital");
//                state = state.replace("Jervis Bay Territory", "Australian Capital Territory");
//                SensitivityZone zone;
//                if ((zone = SensitivityZoneFactory.getZoneByName(state)) != null) {
//                    zones.add(zone);
//                }
//
//                // TODO PFF PQA work around - remove when implemented in Gazetteer
//                if (state.equalsIgnoreCase("Queensland") &&
//                    NumberUtils.toFloat(latitude) >= -19.0 &&
//                    NumberUtils.toFloat(longitude) >= 144.25) {
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PFFPQA1995));
//                }
//
//            }  else if(field.equalsIgnoreCase(STATES_TERRITORIES_LAYER)){
//                //NC 2013-04-29: This is necessary to insert the external territories
//                //TODO find out which layer should actually be used for this purpose
//                SensitivityZone zone;
//                if ((zone = SensitivityZoneFactory.getZoneByName(value)) != null) {
//                    zones.add(zone);
//                }
//
//            } else if (field.equalsIgnoreCase(LGA_BOUNDARIES_LAYER)) {
//                // TODO Special zones work around - remove when implemented in Gazetteer
//                if (value.equalsIgnoreCase("Bauhinia") ||
//                    value.equalsIgnoreCase("Emerald") ||
//                    value.equalsIgnoreCase("Peak Downs")) {
//                    // Emerald Citrus Canker PQA
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004));
//                }
//
//            } else if (field.equalsIgnoreCase(TSPZ_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSPZ));
//
//            } else if (field.equalsIgnoreCase(TSSQZ_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSSQZ));
//
//            } else if (field.equalsIgnoreCase(FFEZ_TRI_STATE_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.FFEZ));
//
//            } else if (field.equalsIgnoreCase(PCN_VIC_LAYER)) {
//                // Potato Cyst Nematode Control Area
//                if (value.equalsIgnoreCase("Thorpedale")) {
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICTHO));
//                } else if (value.equalsIgnoreCase("Koo Wee Rup")) {
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICKWR));
//                } else if (value.equalsIgnoreCase("Gembrook")) {
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICGEM));
//                } else if (value.equalsIgnoreCase("Wandin")) {
//                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICWAN));
//                }
//
//            } else if (field.equalsIgnoreCase(PIZ_NSW_ALBURY_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWAC));
//
//            } else if (field.equalsIgnoreCase(PIZ_NSW_SYDNEY_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWSR));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_NORTH_EAST_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNE));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_MAROONDAH_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMAR));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_NAGAMBIE_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNAG));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_MOOROOPNA_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMOR));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_UPTON_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICUPT));
//
//            } else if (field.equalsIgnoreCase(PIZ_VIC_WHITEBRIDGE_LAYER) && !value.equalsIgnoreCase("n/a")) {
//                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICWHB));
//            } else if (!value.equalsIgnoreCase("n/a")){
//                zones.add(new SensitivityZone("", "", SensitivityZone.ZoneType.valueOf("")));
//            }
        }

        if (zones.isEmpty()) {
            logger.debug("Zone could not be determined from location: Lat " + latitude + ", Long " + longitude);
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOTAUS));
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
        return StringUtils.join(Configuration.getInstance().getSpatialToSample(), ",");
    }
}