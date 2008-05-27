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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.archive.state.Key;


/**
 * A bundle of sheets.  These are created by the 
 * {@link SheetManager#findConfig(String)} method if multiple overrides are
 * present for a given context string.
 * 
 * <p>This class is immutable and read-only.  It is expected that instances
 * will live a short while (in Heritrix, during the lifetime of one CrawlURI
 * being processed) and then discarded.
 * 
 * @author pjack
 */
class SheetBundle extends Sheet {

    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The sheets contained in this bundle.  This is the list of sheets as
     * it was passed to the constructor.
     */
    final private List<Sheet> sheets;

    /**
     * The sheets, plus their parent sheets, in order of settings list element 
     * resolution.  This list reorders the elements in {@link sheets} -- 
     * however, this list will always be longer than {@link sheets} because
     * it also includes the parent sheets.
     * 
     * <p>The sheet elements in this list are ordered in the way that the
     * sheets' elements should be appended to a {@link MultiTypedList} for 
     * a particular Key<List<?>> value.  There are two rules:
     * 
     * <ol>
     * <li>Setting list elements are added in the order the sheets were 
     * specified in {@link sheets}; but</li>
     * <li>Parent setting list elements always appear before child setting
     * list elements.</li>
     * </ol>
     * 
     * <p>For instance, assume the SheetManager had the following sheets:
     * 
     * <ul>
     * <li>global (of course)
     * <li>D (child of global)
     * <li>A (child of D)
     * <li>K (child of D)
     * <li>S (child of global)
     * </ul>
     * 
     * <p>If you construct a SheetBundle with the sheets (A, K, S) then 
     * the sheetsInListOrder would contain (global, D, A, K, S).  If you 
     * constructed the SheetBundle with the sheets (S, K, A) then the
     * sheetsInListOrder would contain (global, S, D, K, A).
     */
    final private List<Sheet> sheetsInListOrder;

    
    /**
     * The sheets, plus their parent sheets, in order of settings list element 
     * resolution.  This list reorders the elements in {@link sheets} -- 
     * however, this list will always be longer than {@link sheets} because
     * it also includes the parent sheets.
     * 
     * <p>The sheet elements in this list are ordered in the way that the
     * sheets' elements should be consulted in a {@link MultiTypedMap} for 
     * a particular Key<Map<String,?>> value.  There are two rules:
     * 
     * <ol>
     * <li>Setting map elements are consulted in the order the sheets were
     * specified in {@link sheets}; but</li>
     * <li>Parent setting list elements always consulted after child setting
     * list elements.</li>
     * </ol>
     * 
     * <p>For instance, assume the SheetManager had the following sheets:
     * 
     * <ul>
     * <li>global
     * <li>D (child of global)
     * <li>A (child of D)
     * <li>K (child of D)
     * <li>S (child of global)
     * </ul>
     * 
     * <p>If you construct a SheetBundle with the sheets (A, K, S) then 
     * the sheetsInMapOrder would contain (A, K, D, S, global).  If you 
     * constructed the SheetBundle with the sheets (S, K, A) then the
     * sheetsInMapOrder would contain (S, K, A, D, global).
     */
    final private List<Sheet> sheetsInMapOrder;
    

    /**
     * Constructor.
     * 
     * @param manager
     * @param sheets
     */
    SheetBundle(SheetManager manager, String name, Collection<Sheet> sheets) {
        super(manager, null, name);
        this.sheets = Collections.unmodifiableList(new ArrayList<Sheet>(sheets));
        this.sheetsInListOrder = orderSheetsForListElements(this.sheets);
        this.sheetsInMapOrder = orderSheetsForMapElements(this.sheets);
    }
    
    
    private void addParentsForListElements(Map<Sheet,Object> map, Sheet sheet) {
        if (sheet instanceof UnspecifiedSheet) {
            return;
        }
        addParentsForListElements(map, sheet.getParent());
        map.put(sheet, null);
    }
    
    
    private List<Sheet> orderSheetsForListElements(List<Sheet> orig) {
        Map<Sheet,Object> result = new LinkedHashMap<Sheet,Object>();
        for (Sheet sheet: orig) {
            addParentsForListElements(result, sheet);
        }
        return Collections.unmodifiableList(
                new ArrayList<Sheet>(result.keySet()));
    }
    
    
    private void addParentsForMapElements(List<Sheet> list, Sheet sheet) {
        if (sheet.getName().equals(SheetManager.GLOBAL_SHEET_NAME)) {
            if (!list.contains(sheet)) {
                list.add(sheet);
            }
            return;
        }
        Sheet parent = sheet.getParent();
        addParentsForMapElements(list, parent);
        int i = list.indexOf(parent);
        list.add(i, sheet);
    }


    private List<Sheet> orderSheetsForMapElements(List<Sheet> orig) {
        List<Sheet> result = new ArrayList<Sheet>();
        for (Sheet sheet: orig) {
            addParentsForMapElements(result, sheet);
        }
        return Collections.unmodifiableList(result);
    }



    @Override
    SheetBundle duplicate() {
        return new SheetBundle(getSheetManager(), getName(),
                new CopyOnWriteArrayList<Sheet>(sheets));
        
    }


    @Override
    public boolean contains(Object module, Key<?> key) {
        for (Sheet sheet: sheets) {
            if (sheet.contains(module, key)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the first present value returned by this bundle's list of
     * sheets.  The sheets are consulted in order starting at the beginning
     * of the list.  If any particular sheet contains a non-null value for
     * the given processor/key combination, then that value is returned and
     * any remaining sheets in the bundle are ignored.
     */
    @Override
    Object check(Object module, Key<?> key) {
        for (Sheet sheet: sheets) {
            if (sheet.contains(module, key)) {
                return sheet.check(module, key);
            }
            Object result = sheet.check(module, key);
            return result;
        }
        return null;
    }


    @Override
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
        for (Sheet sheet: sheetsInMapOrder) {
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
                // TODO: It's now impossible for a bundle to contain other bundles
                // This can probably be safely deleted.
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
        for (Sheet sheet: sheetsInListOrder) {
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
                // TODO: It's now impossible for a bundle to contain other bundles
                // This can probably be safely deleted.
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
            return getSheetManager().getUnspecifiedSheet().resolve(module, k);
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
        for (Sheet sheet: sheetsInMapOrder) {
            if (sheet.contains(module, key)) {
                return sheet.resolve(module, key);
            }
        }
        return getSheetManager().getUnspecifiedSheet().resolve(module, key);
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
