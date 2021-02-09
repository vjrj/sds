package au.org.ala.sds.util;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.PlantPestEradicatedTest;
import au.org.ala.sds.SensitiveSpeciesFinderFactory;

public class TestUtils {
    /**
     * Set up a common configuration
     *
     * @throws Exception
     */
    public static void initConfig() throws Exception {
        System.setProperty("sds.config.file", "/sds-test.properties");
        Configuration.getInstance().setSpeciesUrl(TestUtils.class.getResource("/sensitive-species.xml").toExternalForm());
        Configuration.getInstance().setZoneUrl(TestUtils.class.getResource("/sensitivity-zones.xml").toExternalForm());
        Configuration.getInstance().setCategoriesUrl(TestUtils.class.getResource("/sensitivity-categories.xml").toExternalForm());
    }

}
