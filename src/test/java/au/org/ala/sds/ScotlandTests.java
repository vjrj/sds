package au.org.ala.sds;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.ValidationOutcome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by mar759 on 12/02/2016.
 */
public class ScotlandTests {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species-scotland.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

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

        //test with no location
        Map<String, String> properties3 = new HashMap<String, String>(){{
            put("decimalLatitude", "57.132432432");
            put("decimalLongitude", "-3.1889");
            put("cl2", "Scotland");
        }};
        assertTrue(sds.testMapDetails(finder, properties3, "Gavia arctica").isSensitive());
        assertFalse(sds.testMapDetails(finder, properties3, "Gaviad arctica").isSensitive());
    }
}