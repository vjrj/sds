package au.org.ala.sds.util;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * These are
 */
public class AUWorkarounds {

    public final static String COASTAL_WATERS_LAYER = "cl927";
    public final static String LGA_BOUNDARIES_LAYER = "cl23";
    final static String TSPZ_LAYER = "cl937";
    final static String TSSQZ_LAYER = "cl941";
    final static String FFEZ_TRI_STATE_LAYER = "cl938";
    final static String PCN_VIC_LAYER = "cl939";
    final static String PIZ_NSW_ALBURY_LAYER = "cl936";
    final static String PIZ_NSW_SYDNEY_LAYER = "cl940";
    final static String PIZ_VIC_NORTH_EAST_LAYER = "cl963";
    final static String PIZ_VIC_MAROONDAH_LAYER = "cl962";
    final static String PIZ_VIC_NAGAMBIE_LAYER = "cl961";
    final static String PIZ_VIC_MOOROOPNA_LAYER = "cl960";
    final static String PIZ_VIC_UPTON_LAYER = "cl964";
    final static String PIZ_VIC_WHITEBRIDGE_LAYER = "cl965";
    final static String STATES_TERRITORIES_LAYER ="cl22";
    final static String COUNTRY_LAYER ="cl932";

    public static Set<SensitivityZone> getZones(String field, String value, String latitude, String longitude){

        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();

        if (field.equalsIgnoreCase(COASTAL_WATERS_LAYER)) {
            String state = value.replace(" (including Coastal Waters)", "");
            state = state.replace("Captial", "Capital");
            state = state.replace("Jervis Bay Territory", "Australian Capital Territory");
            SensitivityZone zone;
            if ((zone = SensitivityZoneFactory.getZoneByName(state)) != null) {
                zones.add(zone);
            }

            // TODO PFF PQA work around - remove when implemented in Gazetteer
            if (state.equalsIgnoreCase("Queensland") &&
                    NumberUtils.toFloat(latitude) >= -19.0 &&
                    NumberUtils.toFloat(longitude) >= 144.25) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PFFPQA1995));
            }

        }  else if(field.equalsIgnoreCase(STATES_TERRITORIES_LAYER)){
            //NC 2013-04-29: This is necessary to insert the external territories
            //TODO find out which layer should actually be used for this purpose
            SensitivityZone zone;
            if ((zone = SensitivityZoneFactory.getZoneByName(value)) != null) {
                zones.add(zone);
            }

        } else if (field.equalsIgnoreCase(LGA_BOUNDARIES_LAYER)) {
            // TODO Special zones work around - remove when implemented in Gazetteer
            if (value.equalsIgnoreCase("Bauhinia") ||
                    value.equalsIgnoreCase("Emerald") ||
                    value.equalsIgnoreCase("Peak Downs")) {
                // Emerald Citrus Canker PQA
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.ECCPQA2004));
            }

        } else if (field.equalsIgnoreCase(TSPZ_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSPZ));

        } else if (field.equalsIgnoreCase(TSSQZ_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.TSSQZ));

        } else if (field.equalsIgnoreCase(FFEZ_TRI_STATE_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.FFEZ));

        } else if (field.equalsIgnoreCase(PCN_VIC_LAYER)) {
            // Potato Cyst Nematode Control Area
            if (value.equalsIgnoreCase("Thorpedale")) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICTHO));
            } else if (value.equalsIgnoreCase("Koo Wee Rup")) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICKWR));
            } else if (value.equalsIgnoreCase("Gembrook")) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICGEM));
            } else if (value.equalsIgnoreCase("Wandin")) {
                zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PCNCAVICWAN));
            }

        } else if (field.equalsIgnoreCase(PIZ_NSW_ALBURY_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWAC));

        } else if (field.equalsIgnoreCase(PIZ_NSW_SYDNEY_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZNSWSR));

        } else if (field.equalsIgnoreCase(PIZ_VIC_NORTH_EAST_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNE));

        } else if (field.equalsIgnoreCase(PIZ_VIC_MAROONDAH_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMAR));

        } else if (field.equalsIgnoreCase(PIZ_VIC_NAGAMBIE_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICNAG));

        } else if (field.equalsIgnoreCase(PIZ_VIC_MOOROOPNA_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICMOR));

        } else if (field.equalsIgnoreCase(PIZ_VIC_UPTON_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICUPT));

        } else if (field.equalsIgnoreCase(PIZ_VIC_WHITEBRIDGE_LAYER) && !value.equalsIgnoreCase("n/a")) {
            zones.add(SensitivityZoneFactory.getZone(SensitivityZone.PIZVICWHB));
        } else if (field.equalsIgnoreCase(COUNTRY_LAYER) && !value.equalsIgnoreCase("n/a")) {
            SensitivityZone zone = SensitivityZoneFactory.getZone(SensitivityZone.getCountryCode(value));
            if (zone != null) {
                zones.add(zone);

                if (!"AU".equals(zone.getId())) {
                    zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOT_ATLAS_COUNTRY_CODE));
                }
            }
        }
        return zones;
    }
}
