/**
 *
 */
package au.org.ala.sds.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.util.StringUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityZonesXmlDao {

    protected static final Logger logger = Logger.getLogger(SensitivityZonesXmlDao.class);

    /**
     * @throws IOException
     * @throws Exception
     * @see au.org.ala.sds.dao.SensitiveSpeciesDao#getAll()
     */
    @SuppressWarnings("unchecked")
    public Map<String, SensitivityZone> getMap(InputStream stream) throws Exception {
        Map<String, SensitivityZone> zones = new HashMap<String, SensitivityZone>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(stream);

        Element root = doc.getRootElement();
        List zonesList = root.getChildren();

        for (Iterator sli = zonesList.iterator(); sli.hasNext(); ) {
            Element sze = (Element) sli.next();
            SensitivityZone sz = createSensitiveZone(sze);
            zones.put(sz.getId(), sz);
        }
        return zones;
    }

    private static SensitivityZone createSensitiveZone(Element sze){
        String id = sze.getAttributeValue("id");
        String name = sze.getAttributeValue("name");
        String layerId = sze.getAttributeValue("name");
        String pidsAsString = sze.getAttributeValue("pids", "");
        String[] pids = new String[0];
        if(StringUtils.isNotBlank(pidsAsString)){
            pids = pidsAsString.split(",");
            for(int i=0; i<pids.length; i++){
                pids[i] = pids[i].trim();
            }
        }

        SensitivityZone.ZoneType type = SensitivityZone.ZoneType.valueOf(sze.getAttributeValue("type").toUpperCase());
        return new SensitivityZone(id, name, layerId, type);
    }
}
