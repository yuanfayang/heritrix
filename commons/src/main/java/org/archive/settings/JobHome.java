package org.archive.settings;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.archive.spring.PathSharingContext;
import org.archive.util.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JobHome implements ApplicationContextAware, InitializingBean {
    public static final String PATHS_PROPERTIES_FILE = "paths.properties";
    Properties materializedPaths = new Properties();
    
    String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    String path = ".";
    public String getPath() {
        return path;
    }
    public void setPath(String path) { // ensure gets set
        this.path = path;
    }
    
    String configPath;
    public String getConfigPath() {
        return configPath;
    }
    public void setConfigPath(String config) {
        this.configPath = config; 
    }
    
    ApplicationContext appCtx;
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appCtx = applicationContext;
    }
    
    public void afterPropertiesSet() throws Exception {
        if(configPath == null) {
            if(appCtx instanceof PathSharingContext) {
                configPath = ((PathSharingContext)appCtx).getPrimaryConfigurationPath(); 
            } else {
                configPath = ".";
            }
        }
    }

    public File resolveToFile(String path, String key) {
        File jobHomeDir = new File(new File(configPath).getParent(),this.path);
        File materialized = new File(jobHomeDir, path);
        if(key!=null && !materializedPaths.containsKey(key)) {
            String materializedPath = materialized.getAbsolutePath();
            if(materializedPath.startsWith(this.path)) {
                // trim to relative
                materializedPath = materializedPath.substring(this.path.length());
            }
            materializedPaths.put(key, materializedPath);
            try {
                FileUtils.storeProperties(
                        materializedPaths, resolveToFile(PATHS_PROPERTIES_FILE,null));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return materialized;
    }


    
}
