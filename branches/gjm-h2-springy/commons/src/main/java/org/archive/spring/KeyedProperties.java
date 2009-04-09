/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
 
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
        for(OverrideContext ocontext: threadOverrides.get()) {
            for(String name: ocontext.getOverrideNames()) {
                Map<String,Object> m = ocontext.getOverrideMap(name);
                for(String ok : getOverrideKeys(key)) {
                    Object val = m.get(ok);
                    if(val!=null) {
                        return val;
                    }
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

    
    //
    // CLASS SERVICES
    //
    
    /**
     * ThreadLocal (contextual) collection of pushed override maps
     */
    static ThreadLocal<LinkedList<OverrideContext>> threadOverrides = 
        new ThreadLocal<LinkedList<OverrideContext>>() {
        protected LinkedList<OverrideContext> initialValue() {
            return new LinkedList<OverrideContext>();
        }
    };
    /**
     * Add an override map to the stack 
     * @param m Map to add
     */
    static public void pushOverrideContext(OverrideContext ocontext) {
        threadOverrides.get().addFirst(ocontext);
    }
    
    /**
     * Remove last-added override map from the stack
     * @return Map removed
     */
    static public OverrideContext popOverridesContext() {
        // TODO maybe check that pop is as expected
        return threadOverrides.get().removeFirst();
    }
    
    static public void clearAllOverrideContexts() {
        threadOverrides.get().clear(); 
    }
    
    static public void loadOverridesFrom(OverrideContext ocontext) {
        assert ocontext.haveOverrideNamesBeenSet();
        pushOverrideContext(ocontext);
    }
    
    static public boolean clearOverridesFrom(OverrideContext ocontext) {
        return threadOverrides.get().remove(ocontext);
    }
    
    static public void withOverridesDo(OverrideContext ocontext, Runnable todo) {
        try {
            loadOverridesFrom(ocontext);
            todo.run();
        } finally {
            clearOverridesFrom(ocontext); 
        }
    }

    public static boolean overridesActiveFrom(OverrideContext ocontext) {
        return threadOverrides.get().contains(ocontext);
    }
}
