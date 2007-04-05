/* Copyright (C) 2006 Internet Archive.
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
 * SheetBundle.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.archive.state.Key;


/**
 * A bundle of sheets.
 * 
 * @author pjack
 */
public class SheetBundle extends Sheet {


    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;


    /**
     * The sheets contained in this bundle.
     */
    private List<Sheet> sheets;


    /**
     * Constructor.
     * 
     * @param manager
     * @param sheets
     */
    SheetBundle(SheetManager manager, String name, Collection<Sheet> sheets) {
        super(manager, name);
        this.sheets = new CopyOnWriteArrayList<Sheet>(sheets);
    }


    /**
     * Returns the first non-null value returned by this bundle's list of
     * sheets.  The sheets are consulted in order starting at the beginning
     * of the list.  If any particular sheet contains a non-null value for
     * the given processor/key combination, then that value is returned and
     * any remaining sheets in the bundle are ignored.
     */
    public <T> T check(Object processor, Key<T> key) {
        for (Sheet sheet: sheets) {
            T result = sheet.check(processor, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    
    public <T> Offline checkOffline(Offline module, Key<T> key) {
        for (Sheet sheet: sheets) {
            Offline result = sheet.checkOffline(module, key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    
    public <T> Resolved<T> resolve(Object module, Key<T> key) {
        if (Map.class.isAssignableFrom(key.getType())) {
            return new MapResolver<T>(this, module, key).resolve();
        }
        
        
        if (List.class.isAssignableFrom(key.getType())) {
            return new ListResolver<T>(this, module, key).resolve();
        }
        
        return resolveNormal(module, key);
    }

    
    private <T> Resolved<T> resolveNormal (Object module, Key<T> key) {
        List<Sheet> sheets = new ArrayList<Sheet>();
        sheets.add(this);
        Resolved<T> r = resolveNormal(sheets, module, key);
        if (r == null) {
            return resolveDefault(module, key);
        } else {
            return r;
        }
    }
    
    
    private static <T> Resolved<T> resolveNormal(
            List<Sheet> sheets, 
            Object module, 
            Key<T> key) {
        SheetBundle bundle = (SheetBundle)sheets.get(sheets.size() - 1);
        for (Sheet sheet: bundle.getSheets()) {
            if (sheet instanceof SheetBundle) {
                SheetBundle sb = (SheetBundle)sheet;
                sheets.add(sb);
                Resolved<T> r = resolveNormal(sheets, module, key);
                if (r != null) {
                    return r;
                }
                sheets.remove(sheets.size() - 1);
            } else {
                SingleSheet ss = (SingleSheet)sheet;
                T value = ss.check(module, key);
                if (value != null) {
                    sheets.add(ss);
                    return Resolved.makeOnline(module, key, value, sheets);
                }
            }
        }
        return null;
    }
    
    


    /**
     * Returns the list of sheets contained in this bundle.
     * 
     * @return  the list of sheets
     */
    public List<Sheet> getSheets() {
        return sheets;
    }


}
