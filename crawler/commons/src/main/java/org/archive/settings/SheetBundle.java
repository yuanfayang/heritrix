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
import org.archive.state.KeyTypes;


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

    
    @Override
    SheetBundle duplicate() {
        return new SheetBundle(getSheetManager(), getName(),
                new CopyOnWriteArrayList<Sheet>(sheets));
        
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
            return resolveMap(module, key);
        }
        
        
        if (List.class.isAssignableFrom(key.getType())) {
            return resolveList(module, key);
        }
        
        return resolveNormal(module, key);
    }
    
    
    @SuppressWarnings("unchecked")
    private <T> Resolved<T> resolveMap(Object module, Key<T> k) {
        Key<Object> key = (Key)k;
        List<TypedMap<Object>> list = new ArrayList<TypedMap<Object>>();
        List<Sheet> definer = null;
        for (Sheet sheet: sheets) {
            TypedMap map;
            if (sheet instanceof SingleSheet) {
                map = (TypedMap)sheet.check(module, key);
                if (map != null) {
                    list.add(map);
                    if (definer == null) {
                        definer = new ArrayList<Sheet>(2);
                        definer.add(this);
                        definer.add(sheet);
                    }
                }
            } else if (sheet instanceof SheetBundle) {
                Resolved r = sheet.resolve(module, key);
                map = (TypedMap)r.getValue();
                if (map != null) {
                    list.add(map);
                    if (definer == null) {
                        definer = new ArrayList<Sheet>();
                        definer.add(this);
                        definer.addAll(r.getSheets());
                    }
                }
            }
        }        
        if (list.isEmpty()) {
            return getGlobalSheet().resolve(module, k);
        }
        
        TypedMap<Object> tm = (TypedMap)getGlobalSheet().check(module, key);
        if (tm != null) {
            list.add(tm);
        }

        TypedMap<Object> result; 
        if (list.size() == 1) {
            result = list.get(0);
        } else {
            result = new MultiTypedMap(list, this);
        }
        return Resolved.makeMap(module, k, result, definer);
    }
    
    
    @SuppressWarnings("unchecked")
    private <T> Resolved<T> resolveList(Object module, Key<T> k) {
        Key<Object> key = (Key)k;
        List<TypedList<Object>> lists = new ArrayList<TypedList<Object>>();
        List<Sheet> definer = null;
        for (Sheet sheet: sheets) {
            TypedList list;
            if (sheet instanceof SingleSheet) {
                list = (TypedList)sheet.check(module, key);
                if (list != null) {
                    lists.add(list);
                    if (definer == null) {
                        definer = new ArrayList<Sheet>(2);
                        definer.add(this);
                        definer.add(sheet);
                    }
                }
            } else if (sheet instanceof SheetBundle) {
                Resolved r = sheet.resolve(module, key);
                list = (TypedList)r.getValue();
                if (list != null) {
                    lists.add(list);
                    if (definer == null) {
                        definer = new ArrayList<Sheet>();
                        definer.add(this);
                        definer.addAll(r.getSheets());
                    }
                }
            }
            
        }

        if (lists.isEmpty()) {
            return getGlobalSheet().resolve(module, k);
        }
        
        TypedList<Object> result; 
        if (lists.size() == 1) {
            result = lists.get(0);
        } else {
            result = new MultiTypedList(lists, this);
        }
        return Resolved.makeList(module, k, result, definer);
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
                if (isOnline(bundle, key.getType())) {
                    T value = ss.check(module, key);
                    if (value != null) {
                        sheets.add(ss);
                        return Resolved.makeOnline(module, key, value, sheets);
                    }
                } else {
                    Offline value = ss.checkOffline((Offline)module, key);
                    if (value != null) {
                        sheets.add(ss);
                        return Resolved.makeOffline(module, key, value, sheets);
                    }
                }
            }
        }
        return null;
    }
    
    
    private static boolean isOnline(Sheet sheet, Class c) {
        if (sheet.getSheetManager().isOnline()) {
            return true;
        }
        if (KeyTypes.isSimple(c)) {
            return true;
        }
        if (Map.class.isAssignableFrom(c)) {
            return true;
        }
        if (List.class.isAssignableFrom(c)) {
            return true;
        }
        return false;
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
