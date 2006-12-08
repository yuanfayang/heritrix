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
 * MemorySheetManager.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * A sheet manager that stores all sheets and their associations in memory.
 * 
 * @author pjack
 */
public class MemorySheetManager extends SheetManager {


    /**
     * Maps a sheet's name to the actual sheet.  All access to this field 
     * must be manually synchronized.
     */
    final private Map<String,Sheet> sheets;


    /**
     * Associations; maps context string to its sheet.  Requires no 
     * synchronization.
     */
    final private ConcurrentMap<String,Sheet> associations;


    /**
     * The default sheet.
     */
    final private SingleSheet defaults;


    /**
     * The root processors.
     */
    private Object root;


    public MemorySheetManager() {
        this(null);
    }

    
    /**
     * Constructor.
     */
    public MemorySheetManager(Object root) {
        sheets = new HashMap<String,Sheet>();
        associations = new ConcurrentHashMap<String,Sheet>();
        setRoot(root);
        defaults = addSingleSheet("default");
    }


    @Override
    public Object getRoot() {
        return root;
    }
    
    
    public void setRoot(Object root) {
        this.root = root;
    }


    @Override
    public SingleSheet getDefault() {
        return defaults;
    }


    @Override
    public Set<String> getSheetNames() {
        synchronized (sheets) {
            return new HashSet<String>(sheets.keySet());
        }
    }


    @Override
    public SingleSheet addSingleSheet(String name) {
        synchronized (sheets) {
            Sheet old = sheets.get(name);
            if (old != null) {
                throw new IllegalArgumentException("Sheet already exists: " + name);
            }
            SingleSheet r = createSingleSheet(name);
            sheets.put(name, r);
            return r;
        }
    }

    
    @Override
    public SheetBundle addSheetBundle(String name, Collection<Sheet> c) {
        synchronized (sheets) {
            Sheet old = sheets.get(name);
            if (old != null) {
                throw new IllegalArgumentException("Sheet already exists: " + name);
            }
            SheetBundle r = createSheetBundle(name, c);
            sheets.put(name, r);
            return r;
        }
    }


    @Override
    public void renameSheet(String oldName, String newName) {
        synchronized (sheets) {
            Sheet sheet = sheets.remove(oldName);
            if (sheet == null) {
                throw new IllegalArgumentException("No such sheet: " + oldName);
            }
            sheets.put(newName, sheet);
        }
    }


    @Override
    public Sheet getSheet(String name) {
        synchronized (sheets) {
            Sheet sheet = sheets.get(name);
            if (sheet == null) {
                throw new IllegalArgumentException("No such sheet: " + name);
            }
            return sheet;
        }
    }
    

    @Override
    public void removeSheet(String name) {
        Sheet sheet;
        synchronized (sheets) {
            sheet = sheets.remove(name);
            if (sheet == null) {
                throw new IllegalArgumentException("No such sheet: " + name);
            }
        }
        Collection<Sheet> s = Collections.singleton(sheet);
        associations.values().removeAll(s);
    }
    
    
    @Override
    public Sheet getAssociation(String surt) {
        return associations.get(surt);
    }
    
    
    
    @Override
    public void associate(Sheet sheet, Iterable<String> surts) {
        for (String surt: surts) {
            associations.put(surt, sheet);
        }
    }


    @Override
    public void disassociate(Sheet sheet, Iterable<String> surts) {
        for (String surt: surts) {
            associations.remove(surt, sheet);
        }
    }


    public void reload() {}
    
    public void save() {}
}
