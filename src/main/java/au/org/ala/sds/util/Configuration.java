package au.org.ala.sds.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {

    protected static final Logger logger = Logger.getLogger(Configuration.class);

    private static  Configuration instance = null;

    private final Properties config;

    private List<String> spatialLayers = null;

    private String speciesUrl;
    private String categoriesUrl;
    private String zoneUrl;
    private String layersServiceUrl;

    private Configuration() throws Exception {
        config = new Properties();

        String configFilePath = System.getProperty("sds.config.file", "/data/sds/config/sds-config.properties");
        logger.info("Loading the SDS using configuration from " + configFilePath);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFilePath);

        if(inputStream == null){
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("." + configFilePath);
        }

        if(inputStream == null){
            inputStream = new FileInputStream(configFilePath);
        }

        try {
            if(inputStream != null){
                logger.info("Loading config for SDS from " + configFilePath);
                config.load(inputStream);
            } else {
                logger.warn("External config for SDS not found. Using defaults.");
            }
            speciesUrl = config.getProperty("sds.species.data", "https://sds.ala.org.au/sensitive-species-data.xml");
            categoriesUrl = config.getProperty("sds.category.data", "https://sds.ala.org.au/sensitivity-categories.xml");
            zoneUrl = config.getProperty("sds.zone.data", "https://sds.ala.org.au/sensitivity-zones.xml");
            layersServiceUrl = config.getProperty("layers.service.url", "https://spatial.ala.org.au/layers-service");

            spatialLayers = new ArrayList<String>();
            String configList = config.getProperty("sds.spatial.layers",
                    "cl932,cl927,cl23,cl937,cl941,cl938,cl939,cl936,cl940,cl963,cl962,cl961,cl960,cl964,cl965,cl22,cl10925");
            for(String layerId : configList.split(",")){
                spatialLayers.add(layerId.trim());
            }

        } catch(IOException e) {
            logger.warn("External config for SDS not found. Using defaults.");
        }
    }

    public static Configuration getInstance() {
        try {
            if (instance == null) {
                instance = new Configuration();
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public void setZoneUrl(String zoneUrl) {
        this.zoneUrl = zoneUrl;
    }

    public void setCategoriesUrl(String categoriesUrl) {
        this.categoriesUrl = categoriesUrl;
    }

    public void setSpeciesUrl(String speciesUrl) {
        this.speciesUrl = speciesUrl;
    }

    public void setLayersServiceUrl(String layersServiceUrl) {
        this.layersServiceUrl = layersServiceUrl;
    }

    public String get(String field){
        return config.getProperty(field,field);
    }

    public String getFlagRules(){
        return config.getProperty("sds.flag.rules","PBC7,PBC8,PBC9");
    }

    public String getSpeciesUrl() {
        return speciesUrl;
    }

    public String getCategoryUrl() {
        return categoriesUrl;
    }

    public String getZoneUrl() {
        return zoneUrl;
    }

    public boolean isCached() {
        return config.getProperty("sds.species.cache", "false").equalsIgnoreCase("true");
    }

    public String getCacheUrl() {
        return config.getProperty("sds.cache.data", "/data/sds/data/species-cache.ser");
    }

    public String getLayersServiceUrl() {
        return layersServiceUrl;
    }

    public String getNameMatchingIndex() {
        return config.getProperty("name.index.dir", "/data/lucene/namematching");
    }

    public String getListToolUrl(){
        return config.getProperty("list.tool.url", "https://lists.ala.org.au");
    }

    public List<String> getGeospatialLayers() {
        return spatialLayers;
    }

    public void setSpatialLayers(List<String> spatialLayers) {
        this.spatialLayers = spatialLayers;
    }
}
