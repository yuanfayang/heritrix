package org.archive.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Map for storing overridable properties. 
 * 
 * An object wanting to allow its properties to be overridden 
 * contextually will store those properties in this map. Its 
 * accessors (like getProp() and setProp()) will only pass-through
 * to the 'prop' entry in this map.)
 * 
 */
public class KeyedProperties extends HashMap<String,Object> {
    private static final long serialVersionUID = 3403222335436162778L;
    /** the alternate global property-paths leading to this map 
     * TODO: consider if deterministic ordered list is important */
    HashSet<String> externalPaths = new HashSet<String>(); 
    
    /**
     * Add a path by which the outside world can reach this map
     * @param path String path
     */
    public void addExternalPath(String path) {
        externalPaths.add(path);
    }

    /**
     * Get the given value, checking override maps if appropriate.
     * 
     * @param key
     * @return discovered override, or local value
     */
    public Object get(String key) {
        for(Map m: overrides.get()) {
            for(String ok : getOverrideKeys(key)) {
                Object val = m.get(ok);
                if(val!=null) {
                    return val;
                }
            }
        }
        return super.get(key);
    }

    /**
     * Compose the complete keys (externalPath + local key name) to use
     * for checking for contextual overrides. 
     * 
     * @param key local key to compose
     * @return List of full keys to check
     */
    protected List<String> getOverrideKeys(String key) {
        ArrayList<String> keys = new ArrayList<String>(externalPaths.size());
        for(String path : externalPaths) {
            keys.add(path+"."+key);
        }
        return keys;
    }

    /**
     * ThreadLocal (contextual) collection of pushed override maps
     */
    static ThreadLocal<LinkedList<Map>> overrides = new ThreadLocal<LinkedList<Map>>() {
        protected LinkedList<Map> initialValue() {
            return new LinkedList<Map>();
        }
    };
    /**
     * Add an override map to the stack 
     * @param m Map to add
     */
    static public void  pushOverridesMap(Map m) {
        overrides.get().addFirst(m);
    }
    
    /**
     * Remove last-added override map from the stack
     * @return Map removed
     */
    static public Map popOverridesMap() {
        // TODO maybe check that pop is as expected
        return overrides.get().removeFirst();
    }
}
