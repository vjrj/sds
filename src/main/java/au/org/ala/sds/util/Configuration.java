package au.org.ala.sds.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {

    private static  Configuration instance = null;

    private final Properties config;

    private static List<String> sampleFields = null;

    private static List<String> spatialLayers = null;

    private Configuration() throws Exception {
        config = new Properties();
        File configFile = new File("/data/sds/config/sds-config.properties");
        config.load(new FileInputStream(configFile));
    }

    public static Configuration getInstance() {
        try {
            if (instance == null)
                instance = new Configuration();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return instance;
    }

    public String get(String field){
        return config.getProperty(field,field);
    }

    public String getFlagRules(){
        return config.getProperty("sds.flag.rules","PBC7,PBC8,PBC9");
    }

    public String getSpeciesUrl() {
        return config.getProperty("sds.species.data", "http://sds.ala.org.au/sensitive-species-data.xml");
    }

    public String getCategoryUrl() {
        return config.getProperty("sds.category.data", "http://sds.ala.org.au/sensitivity-categories.xml");
    }

    public String getZoneUrl() {
        return config.getProperty("sds.zone.data", "http://sds.ala.org.au/sensitivity-zones.xml");
    }

    public boolean isCached() {
        return config.getProperty("sds.species.cache", "false").equalsIgnoreCase("true");
    }

    public String getCacheUrl() {
        return config.getProperty("sds.cache.data", "/data/sds/data/species-cache.ser");
    }

    public String getSpatialUrl() {
        return config.getProperty("layers.service.url", "http://spatial.ala.org.au/layers-service") + "/intersect/";
    }

    public List<String> getSpatialToSample() {
        if(sampleFields == null){
            String fieldsToSample = config.getProperty("sample.fields", "NOT_SUPPLIED");
            if (fieldsToSample.equals("NOT_SUPPLIED")){
                sampleFields = Configuration.getInstance().getGeospatialLayers();
            } else {
                String[] sampleFieldsArray = config.getProperty("sample.fields", "").split(",");
                sampleFields = new ArrayList<String>();
                for(String field : sampleFieldsArray){
                    sampleFields.add(field.trim());
                }
            }
        }
        return sampleFields;
    }

    public String getNameMatchingIndex() {
        return config.getProperty("name.index.dir", "/data/lucene/namematching");
    }

    public String getListToolUrl(){
        return config.getProperty("list.tool.url", "http://lists.ala.org.au");
    }

    public List<String> getGeospatialLayers() {
        if(spatialLayers == null){
            spatialLayers = new ArrayList<String>();
            String configList = config.getProperty("sds.spatial.layers",
                    "cl927,cl23,cl937,cl941,cl938,cl939,cl936,cl940,cl963,cl962,cl961,cl960,cl964,cl965,cl22");
            for(String layerId : configList.split(",")){
                spatialLayers.add(layerId.trim());
            }
        }
        return spatialLayers;
    }
}
