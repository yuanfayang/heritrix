/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * DefaultCheckpointRecovery.java
 *
 * Created on Mar 8, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.net.URI;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.archive.state.Key;

/**
 * Default implementation.
 * 
 * @author pjack
 *
 */
public class DefaultCheckpointRecovery implements CheckpointRecovery {

    
    final private Map<Object,Map<Key,Object>> newSettings =
        new IdentityHashMap<Object,Map<Key,Object>>();

    final private Map<URI,URI> uriTranslations = new HashMap<URI,URI>();

    final private Map<String,String> fileTranslations = 
        new HashMap<String,String>();

    
    public DefaultCheckpointRecovery() {
    }

    
    public Map<String,String> getFileTranslations() {
        return fileTranslations;
    }
    
    
    public Map<URI,URI> getURITranslations() {
        return uriTranslations;
    }


    public <T> void setState(Object module, Key<T> key, T value) {
        Map<Key,Object> map = newSettings.get(module);
        if (map == null) {
            map = new HashMap<Key,Object>();
            newSettings.put(module, map);
        }
        
        map.put(key, value);
    }


    public String translatePath(String path) {
        Map.Entry<String,String> match = null;
        for (Map.Entry<String,String> me: fileTranslations.entrySet()) {
            if (path.startsWith(me.getKey())) {
                if ((match == null) 
                || (match.getKey().length() < me.getKey().length())) {
                    match = me;
                }
            }
        }
        
        if (match == null) {
            return path;
        }
        
        int size = match.getKey().length();
        return match.getValue() + path.substring(size);
    }


    public URI translateURI(URI uri) {
        URI r = uriTranslations.get(uri);
        return r == null ? uri : r;
    }
    
    
    public void apply(SingleSheet global) {
        for (Map.Entry<Object,Map<Key,Object>> mod: newSettings.entrySet()) {
            Object module = mod.getKey();
            for (Map.Entry<Key,Object> me: mod.getValue().entrySet()) {
                @SuppressWarnings("unchecked")
                Key<Object> k = me.getKey();
                global.set(module, k, me.getValue());
            }
        }
    }
}
