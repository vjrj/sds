/**
 *
 */
package au.org.ala.sds.dao;

import au.org.ala.names.model.RankType;
import au.org.ala.sds.model.*;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesXmlDao implements SensitiveSpeciesDao {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesXmlDao.class);

    private final InputStream stream;

    public SensitiveSpeciesXmlDao(InputStream stream) throws Exception {
        this.stream = stream;
    }

    /**
     * @throws IOException
     * @throws JDOMException
     * @see au.org.ala.sds.dao.SensitiveSpeciesDao#getAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<SensitiveTaxon> getAll() throws Exception {

        List<SensitiveTaxon> species = new ArrayList<SensitiveTaxon>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(this.stream);

        Element root = doc.getRootElement();
        List speciesList = root.getChildren();

        for (Iterator sli = speciesList.iterator(); sli.hasNext(); ) {
            Element sse = (Element) sli.next();
            String name = sse.getAttributeValue("name");
            String family = sse.getAttributeValue("family");
            RankType rank = RankType.getForStrRank(sse.getAttributeValue("rank"));
            String commonName = sse.getAttributeValue("commonName");

            SensitiveTaxon ss = new SensitiveTaxon(name, rank);
            ss.setFamily(family);
            ss.setCommonName(commonName);
            if(sse.getAttributeValue("guid") != null){
                ss.setLsid(sse.getAttributeValue("guid"));
            } else if(sse.getAttributeValue("lsid") != null){
                ss.setLsid(sse.getAttributeValue("lsid"));
            }

            Element instances = sse.getChild("instances");
            List instanceList = instances.getChildren();

            for (Iterator ili = instanceList.iterator(); ili.hasNext(); ) {
                Element ie = (Element) ili.next();
                SensitivityInstance instance = null;
                String zone  = ie.getAttributeValue("zone");

                if (ie.getName().equalsIgnoreCase("conservationInstance")) {
                    instance = new ConservationInstance(
                        SensitivityCategoryFactory.getCategory(ie.getAttributeValue("category")),
                        ie.getAttributeValue("authority"),
                        ie.getAttributeValue("dataResourceId"),
                        SensitivityZoneFactory.findZone(ie.getAttributeValue("zone")),
                        ie.getAttributeValue("reason"),
                        ie.getAttributeValue("remarks"),
                        ie.getAttributeValue("generalisation")
                    );
                } else if (ie.getName().equalsIgnoreCase("plantPestInstance")) {
                    instance = new PlantPestInstance(
                        SensitivityCategoryFactory.getCategory(ie.getAttributeValue("category")),
                        ie.getAttributeValue("authority"),
                        ie.getAttributeValue("dataResourceId"),
                        SensitivityZoneFactory.findZone(ie.getAttributeValue("zone")),
                        ie.getAttributeValue("reason"),
                        ie.getAttributeValue("remarks"),
                        ie.getAttributeValue("fromDate"),
                        ie.getAttributeValue("toDate")
                    );

                    //check if there are any children transientEvents
                    List<Element> transChildren = ie.getChildren();
                    if(transChildren.size()>0){
                        for(Element te : transChildren){
                            ((PlantPestInstance)instance).addTransientEvent(
                                    te.getAttributeValue("eventDate"),
                                    SensitivityZoneFactory.getZone(te.getAttributeValue("zone")));
                        }
                    }
                }
                ss.getInstances().add(instance);
            }
            species.add(ss);
        }

        // Sort list since MySQL sort order is not the same as Java's
        Collections.sort(species);

        return species;
    }

}
