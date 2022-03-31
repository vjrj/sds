package au.org.ala.sds.model;

import java.io.Serializable;
import java.util.*;

import au.org.ala.sds.util.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class SensitivityZone implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ATLAS_COUNTRY_CODE = Configuration.getInstance().getCountryId();
    public static final String NOT_ATLAS_COUNTRY_CODE = Configuration.getInstance().getNotCountryId();
    public static final String ACT = "ACT";
    public static final String NSW = "NSW";
    public static final String QLD = "QLD";
    public static final String VIC = "VIC";
    public static final String TAS = "TAS";
    public static final String SA = "SA";
    public static final String WA = "WA";
    public static final String NT = "NT";
    public static final String CC = "CC";
    public static final String CX = "CX";
    public static final String AC = "AC";
    public static final String CS = "CS";
    public static final String NF = "NF";
    public static final String HM = "HM";
    public static final String AQ = "AQ";
    public static final String TSPZ = "TSPZ";
    public static final String TSSQZ = "TSSQZ";
    public static final String FFEZ = "FFEZ";
    public static final String PFFPQA1995 = "PFFPQA1995";
    public static final String ECCPQA2004 = "ECCPQA2004";
    public static final String RIFARA = "RIFARA";
    public static final String PIZNSWAC = "PIZNSWAC";
    public static final String PIZNSWSR = "PIZNSWSR";
    public static final String PIZVICNE = "PIZVICNE";
    public static final String PIZVICMAR = "PIZVICMAR";
    public static final String PIZVICNAG = "PIZVICNAG";
    public static final String PIZVICMOR = "PIZVICMOR";
    public static final String PIZVICUPT = "PIZVICUPT";
    public static final String PIZVICWHB = "PIZVICWHB";
    public static final String PCNCAVICTHO = "PCNCAVICTHO";
    public static final String PCNCAVICGEM = "PCNCAVICGEM";
    public static final String PCNCAVICKWR = "PCNCAVICKWR";
    public static final String PCNCAVICWAN = "PCNCAVICWAN";

    public enum ZoneType { COUNTRY, STATE, EXTERNAL_TERRITORY, QUARANTINE_ZONE }

    private final String id;
    private final String name;
    private final String layerId;   //the layer used to identify a layer
    private final ZoneType type;

    final static Map<String,String> countryCodes = new HashMap<String,String>();

    static {
        for (Locale locale : Locale.getAvailableLocales()) {
            countryCodes.put(locale.getDisplayCountry(), locale.getCountry());
        }
    }

    public SensitivityZone(String id, String name, String layerId, ZoneType type) {
        this.id = id;
        this.name = name;
        this.layerId = layerId;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLayerId() {
        return layerId;
    }

    public ZoneType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
            append(this.id).
            append(this.name).
            append(this.layerId).
            append(this.type).
            toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SensitivityZone other = (SensitivityZone) obj;
        return new EqualsBuilder()
            .append(this.id, other.id)
            .isEquals();
    }

    @Override
    public String toString() {
        return this.id + ", Name: " + this.getName() + ", Zone: " + getType();
    }

    public String toJson() {
        return "{\"name\":\"" + name + "\", \"type\":\"" + type.toString() + "\"}";
    }

    public static boolean isInAtlasCountry(SensitivityZone zone) {
        return
            zone == SensitivityZoneFactory.getZone(ATLAS_COUNTRY_CODE) ||
            zone.getType() == ZoneType.STATE;
    }

    public static boolean isInAtlasCountry(List<SensitivityZone> zones) {
        for (SensitivityZone zone : zones) {
            if (zone.equals(SensitivityZoneFactory.getZone(ATLAS_COUNTRY_CODE)) || zone.getType() == ZoneType.STATE) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExternalTerritory(SensitivityZone zone) {
        return zone.getType() == ZoneType.EXTERNAL_TERRITORY;
    }

    public static boolean isExternalTerritory(List<SensitivityZone> zones) {
        for (SensitivityZone zone : zones) {
            if (zone.getType() == ZoneType.EXTERNAL_TERRITORY) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInTorresStrait(List<SensitivityZone> zones) {
        for (SensitivityZone zone : zones) {
            if (zone.equals(SensitivityZoneFactory.getZone(TSPZ)) || zone.equals(SensitivityZoneFactory.getZone(TSSQZ))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotInAtlasCountry(SensitivityZone zone) {
        return zone == null || zone.equals(SensitivityZoneFactory.getZone(NOT_ATLAS_COUNTRY_CODE));
    }

    public static boolean isNotInAtlasCountry(List<SensitivityZone> zones) {
        for (SensitivityZone zone : zones) {
            if (zone.equals(SensitivityZoneFactory.getZone(NOT_ATLAS_COUNTRY_CODE))) {
                return true;
            }
        }
        return false;
    }

    public static List<SensitivityZone> getListFromString(String string) {
        List<SensitivityZone> zoneList = new ArrayList<SensitivityZone>();
        String[] zones = StringUtils.split(StringUtils.substringBetween(string, "[", "]"), ',');
        for (String zone : zones) {
            SensitivityZone sz = SensitivityZoneFactory.getZone(StringUtils.strip(zone));
            if(sz != null){
                zoneList.add(sz);
            }
        }
        return zoneList;
    }

    public static String getZoneDescriptions(List<SensitivityZone> zones) {
        StringBuffer buff = new StringBuffer();
        Collections.sort(zones, new Comparator<SensitivityZone>(){

            @Override
            public int compare(SensitivityZone o1, SensitivityZone o2) {
                if(o1.getType() != null && o2.getType() != null
                        && o1.getType().equals(ZoneType.COUNTRY)
                        && o2.getType().equals(ZoneType.STATE)
                        ){
                    return 1;
                }
                if(o1.getType() != null && o2.getType() != null
                        && o1.getType().equals(ZoneType.STATE)
                        && o2.getType().equals(ZoneType.COUNTRY)
                        ){
                    return -1;
                }

                return o1.getName().compareTo(o2.getName());
            }
        });
        for (SensitivityZone sz : zones) {
            if (sz != null) {
                if (buff.length() > 0) {
                    buff.append(", ");
                }
                buff.append(sz.getName());
            }
        }
        return buff.toString();
    }

    public static String getCountryCode(String name) {
        return countryCodes.get(name);
    }

}
