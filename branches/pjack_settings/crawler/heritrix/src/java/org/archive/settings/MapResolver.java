/* Copyright (C) 2007 Internet Archive.
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
 * MapResolver.java
 * Created on January 17, 2007
 *
 * $Header$
 */
package org.archive.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.archive.state.Key;

class MapResolver<T> {
    
    final private SheetBundle start;
    
    final private Object module;
    
    final private Key<T> key;
    
    private Map<String,Object> merged;
    
    private Map<String,List<Sheet>> sheetMap;
    
    private List<Sheet> mergedSheets;

    
    public MapResolver(SheetBundle bundle, Object module, Key<T> key) {
        this.start = bundle;
        this.module = module;
        this.key = key;
        this.sheetMap = new HashMap<String,List<Sheet>>();
    }
    
    
    public Resolved<T> resolve() {
        // If the default sheet defines a map for this module and key,
        // then pre-populate the merged map.  Any map defined in a later
        // sheet will be considered an override of the default map.
        Sheet def = start.getSheetManager().getDefault();
        @SuppressWarnings("unchecked")
        Map<String,Object> defMap = (Map)def.check(module, key);
        if (defMap != null) {
            List<Sheet> ms = Collections.singletonList(def);
            merged = makeMergeMap(defMap, sheetMap, ms);
            mergedSheets = ms;
        }

        List<Sheet> bundles = new ArrayList<Sheet>();
        bundles.add(start);
        resolveMap(bundles);
        return Resolved.makeMap(module, key, merged, mergedSheets, sheetMap);
    }
    
    
    private void resolveMap(
            List<Sheet> bundles
            ) {
        SheetBundle bundle = (SheetBundle)bundles.get(bundles.size() - 1);
        for (Sheet sh: bundle.getSheets()) {
            List<Sheet> newSheetList = new ArrayList<Sheet>(bundles);
            newSheetList.add(sh);
            if (sh instanceof SheetBundle) {
                resolveMap(newSheetList);
            } else {
                @SuppressWarnings("unchecked")
                Map<String,Object> map = (Map<String,Object>)sh.check(module, key);
                if (merged == null) {
                    merged = makeMergeMap(map, sheetMap, newSheetList);
                    mergedSheets = newSheetList;
                } else {
                    merge(merged, sheetMap, map, newSheetList);
                }
            }
        }
    }
    

    static Map<String,Object> makeMergeMap(
            Map<String,Object> newData,
            Map<String,List<Sheet>> sheets,
            List<Sheet> newSheet) {
        if (newData == null) {
            return null;
        }
        Map<String,Object> result;
        if (newData instanceof SortedMap) {
            result = new TreeMap<String,Object>();
        } else {
            result = new LinkedHashMap<String,Object>();
        }
        
        merge(result, sheets, newData, newSheet);
        return result;
    }
    
    
    static void merge(
            Map<String,Object> merged,
            Map<String,List<Sheet>> sheets,
            Map<String,Object> newData,
            List<Sheet> newSheet) {
        if (newData == null) {
            return;
        }
        Set<Map.Entry<String,Object>> entrySet = newData.entrySet();
        Iterator<Map.Entry<String,Object>> iter = entrySet.iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Object> me = iter.next();
            merged.put(me.getKey(), me.getValue());
            sheets.put(me.getKey(), newSheet);
        }        
    }


}
