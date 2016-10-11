/**
 *
 */
package au.org.ala.sds.model;

import au.org.ala.sds.util.Configuration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.sds.dao.SensitivityZonesXmlDao;
import au.org.ala.sds.util.Configuration;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZoneFactory {

    protected static final Logger logger = Logger.getLogger(SensitivityZoneFactory.class);

    private static final String ZONES_RESOURCE = "sensitivity-zones.xml";

    private static Map<String, SensitivityZone> zones;

    private static Map<String, SensitivityZone> pidToZoneMap;

    public static SensitivityZone getZone(String key) {
        if (zones == null) {
            initZones();
        }
        return zones.get(key.toUpperCase());
    }

    public static SensitivityZone getZoneByName(String name) {
        if (zones == null) {
            initZones();
        }
        for (SensitivityZone sz : zones.values()) {
            if (sz.getName().equalsIgnoreCase(name) || sz.getId().equalsIgnoreCase(name)) {
                return sz;
            }
        }
        logger.warn("No sensitivity zone matches " + name);
        return null;
    }

    public static SensitivityZone findZone(String nameOrId){
        SensitivityZone zone = getZoneByName(nameOrId);
        if (zone == null)
            zone = getZone(nameOrId);
        return zone;
    }


    private static void initZones() {

        zones = new HashMap<String, SensitivityZone>();

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(getZonesInputStream());
            Element root = doc.getRootElement();
            List zonesList = root.getChildren();

            for (Iterator sli = zonesList.iterator(); sli.hasNext(); ) {
                Element sze = (Element) sli.next();
                SensitivityZone sz = createSensitiveZone(sze);
                zones.put(sz.getId(), sz);
            }
        } catch (Exception e){
            throw new RuntimeException("Unable to load zone information.", e);
        }
    }

    private static InputStream getZonesInputStream(){

        URL url = null;
        InputStream is = null;

        try {
            url = new URL(Configuration.getInstance().getZoneUrl());
            is = url.openStream();
        } catch (Exception e) {
            logger.warn("Exception occurred getting zones list from " + url, e);
            is = SensitivityZoneFactory.class.getClassLoader().getResourceAsStream(ZONES_RESOURCE);
            if (is == null) {
                logger.error("Unable to read " + ZONES_RESOURCE + " from jar file");
            } else {
                logger.info("Reading bundled resource " + ZONES_RESOURCE + " from jar file");
            }
        }

        return is;
    }

    private static SensitivityZone createSensitiveZone(Element sze){
        String id = sze.getAttributeValue("id");
        String name = sze.getAttributeValue("name");
        String layerId = sze.getAttributeValue("name");

        SensitivityZone.ZoneType type = SensitivityZone.ZoneType.valueOf(sze.getAttributeValue("type").toUpperCase());
        return new SensitivityZone(id, name, layerId, type);
    }

    public static void reset(){
       zones = null;
    }
}
