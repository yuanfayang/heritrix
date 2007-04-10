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
 * ListResolver.java
 * Created on January 17, 2007
 *
 * $Header$
 */
package org.archive.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.archive.state.Key;

class ListResolver<T> {

    
    final private SheetBundle start;
    
    final private Object module;
    
    final private Key<T> key;
    
    private List<Object> merged;
    
    private List<List<Sheet>> sheets;
    
    
    private List<Sheet> mergedSheets;
    
    
    public ListResolver(SheetBundle start, Object module, Key<T> key) {
        this.start = start;
        this.module = module;
        this.key = key;
        this.sheets = new ArrayList<List<Sheet>>(4);
    }
    
    
    public Resolved<T> resolve() {
        Sheet def = start.getSheetManager().getDefault();
        @SuppressWarnings("unchecked")
        List<Object> defList = (List)def.check(module, key);
        if (defList != null) {
            merged = new ArrayList<Object>(defList);
            mergedSheets = Collections.singletonList(def);
            sheets.addAll(Collections.nCopies(defList.size(), mergedSheets));
        }
        
        List<Sheet> bundles = new ArrayList<Sheet>();
        bundles.add(start);
        resolveList(bundles);
        
        return Resolved.makeList(module, key, merged, mergedSheets, sheets);
    }


    
    
    private void resolveList(List<Sheet> bundles) {
        SheetBundle bundle = (SheetBundle)bundles.get(bundles.size() - 1);
        for (Sheet sh: bundle.getSheets()) {
            List<Sheet> newSheetList = new ArrayList<Sheet>(bundles);
            newSheetList.add(sh);
            newSheetList = Collections.unmodifiableList(newSheetList);
            if (sh instanceof SheetBundle) {
                resolveList(newSheetList);
            } else {
                resolveList2(newSheetList, sh);
            }
        }
    }

    
    private void resolveList2(List<Sheet> newSheetList, Sheet single) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List)single.check(module, key);
        if (list == null) {
            return;
        }
        if (merged == null) {
            merged = new ArrayList<Object>(list);
            mergedSheets = newSheetList;
        } else {
            merged.addAll(list);
        }
        for (int i = 0; i < list.size(); i++) {
            sheets.add(newSheetList);
        }
    }
    
    
    
}
