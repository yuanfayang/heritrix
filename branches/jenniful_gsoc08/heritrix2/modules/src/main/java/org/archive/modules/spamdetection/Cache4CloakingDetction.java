package org.archive.modules.spamdetection;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;


public class Cache4CloakingDetction implements Closeable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = 
        Logger.getLogger(Cache4CloakingDetction.class.getName());
    
    protected Map<String, Map<String, Object>> prerequisites = null;
    
    public Cache4CloakingDetction() {
        prerequisites = new Hashtable<String, Map<String, Object>>();
    }
    
    public synchronized Map<String, Object> getEntry(String uriKey) {
        Map<String, Object> data = prerequisites.get(uriKey);
        if (data == null) {
            data = new Hashtable<String, Object>();
            prerequisites.put(uriKey, data);
        }
        return data;
        
    }
    
    public synchronized void removeEntry(String urikey) {
        if (prerequisites.containsKey(urikey)) {
            prerequisites.remove(urikey);
        }
    }
    
    public synchronized boolean isReady4Detection(String urikey) {
        if (prerequisites.containsKey(urikey)) {
            Map<String, Object> data = prerequisites.get(urikey);
            if (data.containsKey("referrer") &&
                    data.containsKey("wreferrer") &&
                    data.containsKey("woreferrer") &&
                    data.containsKey("resources"))
                return true;
        }
        
        return false;
    }
    
    public synchronized void addPrerequisite(String urikey, 
            String datakey, Object data) {
        getEntry(urikey).put(datakey, data);
    }

    public void close() {
        
    }
}
