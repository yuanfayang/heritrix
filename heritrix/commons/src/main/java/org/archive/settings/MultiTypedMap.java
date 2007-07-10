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
 * MultiTypedMap.java
 *
 * Created on Jun 4, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pjack
 *
 */
class MultiTypedMap<T> extends AbstractMap<String,T> implements TypedMap<T> {


    private List<TypedMap<T>> maps;
    private Set<Map.Entry<String,T>> entrySet;
    private Sheet start;
    
    public MultiTypedMap(List<TypedMap<T>> maps, Sheet start) {
        this.maps = maps;
        this.start = start;
    }

    

    public T get(Object object) {
        for (TypedMap<T> map: maps) {
            T result = map.get(object);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
   
    
    public List<Sheet> getSheets(String key) {
        List<Sheet> result = new ArrayList<Sheet>();
        if (start != null) {
            result.add(start);
        }
        for (TypedMap<T> map: maps) {
            if (map.get(key) != null) {
                List<Sheet> s = map.getSheets(key);
                result.addAll(s);
                return result;
            }
        }
        return result;
    }
    
    
    public Set<Map.Entry<String,T>> entrySet() {
        if (maps.size() == 1) {
            return maps.get(0).entrySet();
        }
        if (this.entrySet == null) {
            this.entrySet = new LinkedHashSet<Map.Entry<String,T>>();
            for (TypedMap<T> map: maps) {
                entrySet.addAll(map.entrySet());
            }
        }
        return entrySet;
    }

    
    public Class<T> getElementType() {
        return maps.get(0).getElementType();
    }


}
