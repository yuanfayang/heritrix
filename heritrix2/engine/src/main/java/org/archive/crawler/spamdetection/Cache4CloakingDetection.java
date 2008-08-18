package org.archive.crawler.spamdetection;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This cache is designed for cloaking detection. It holds information of each
 * HTML document who needs cloaking detection. For each HTML document, there 
 * are four prerequisites which have to be all available before performing 
 * cloaking detection.
 * <p>Four prerequisites:</p>
 * <p>- referrer: used for client side cloaking detection. </p>
 * <p>- content-with-referrer: fetched content with referrer field having being 
 * set to non null in HTTP header. </p>
 * <p>- content-without-referrer: fetched content with referrer field having 
 * being set to null in HTTP header.</p>
 * <p>- js-required-resources: The list of resources required by JavaScript 
 * execution.</p>
 *   
 * @author Ping Wang
 *
 */
public class Cache4CloakingDetection implements Closeable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = 
        Logger.getLogger(Cache4CloakingDetection.class.getName());
    
    /**
     * The hash map to hold all information.
     */
    protected Map<String, Map<String, Object>> prerequisites = null;
    
    public Cache4CloakingDetection() {
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
    
    /**
     * Remove an entry from the hash map.
     * @param urikey
     */
    public synchronized void removeEntry(String urikey) {
        if (prerequisites.containsKey(urikey)) {
            prerequisites.remove(urikey);
        }
    }
    
    /**
     * Check if a uri's all four prerequisites are available.
     * @param urikey
     * @return
     */
    public synchronized boolean isReady4Detection(String urikey) {
        if (prerequisites.containsKey(urikey)) {
            Map<String, Object> data = prerequisites.get(urikey);
            if (data.containsKey("referrer") &&
                    data.containsKey("content-with-referrer") &&
                    data.containsKey("content-without-referrer") &&
                    data.containsKey("resources"))
                return true;
        }
        
        return false;
    }
    
    /**
     * Add one prerequisite of a uri to the hash map. 
     * @param urikey string representation of a uri
     * @param datakey the name of the prerequisite.
     * @param data the value of the prerequisite.
     */
    public synchronized void addPrerequisite(String urikey, 
            String datakey, Object data) {
        getEntry(urikey).put(datakey, data);
    }

    public void close() {
        
    }
}