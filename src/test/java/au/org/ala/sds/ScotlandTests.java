package au.org.ala.sds;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityZoneFactory;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.util.TestUtils;
import au.org.ala.sds.validation.ValidationOutcome;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by mar759 on 12/02/2016.
 */
public class ScotlandTests {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {

        TestUtils.initConfig();
        SensitivityZoneFactory.reset(); //FIXME this isnt pleasant
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species-scotland.xml").toURI().toString();
        String zonesUrl = nameSearcher.getClass().getClassLoader().getResource("sensitivity-zones-scotland.xml").toURI().toString();

        Configuration.getInstance().setLayersServiceUrl("http://layers.als.scot/layers-service");
        Configuration.getInstance().setSpeciesUrl(uri);
        Configuration.getInstance().setZoneUrl(zonesUrl);
        Configuration.getInstance().setSpatialLayers(new ArrayList<String>() {{
            add("cl2");
        }});

        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);

    }

    @AfterClass
    public static void runOnceAfter() throws Exception {
        Configuration.reset(); //FIXME this isnt pleasant
        SensitivityZoneFactory.reset(); //FIXME this isnt pleasant
    }

    @Ignore("layers.als.scot not available")
    @Test
    public void lookupGavia() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Gavia arctica");
        assertNotNull(ss);

        //test with no location
        SensitiveDataService sds = new SensitiveDataService();

        //test with non-sensitive location
        Map<String, String> properties2 = new HashMap<String, String>(){{
            put("decimalLatitude", "55.132432432");
            put("decimalLongitude", "13.1889");
        }};
        ValidationOutcome outcome2 = sds.testMapDetails(finder, properties2, "Tringa glareola");
        assertFalse(outcome2.isSensitive());

        //test with sensitive location
        Map<String, String> properties3 = new HashMap<String, String>(){{
            put("decimalLatitude", "57.132432432");
            put("decimalLongitude", "-3.1889");
            put("cl2", "Scotland");
        }};
        assertTrue(sds.testMapDetails(finder, properties3, "Gavia arctica").isSensitive());
        assertFalse(sds.testMapDetails(finder, properties3, "Gaviad arctica").isSensitive());
    }
}