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


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.archive.settings.file.PrefixFinder;
import org.archive.settings.path.PathChangeException;
import org.archive.state.DefaultPathContext;
import org.archive.state.KeyManager;
import org.archive.state.PathContext;


/**
 * A sheet manager that stores all sheets and their associations in memory.
 * 
 * @author pjack
 */
public class MemorySheetManager extends SheetManager {

    
    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;

    static {
        KeyManager.addKeys(MemorySheetManager.class);
    }


    /**
     * Maps a sheet's name to the actual sheet.  All access to this field 
     * must be manually synchronized.
     */
    final private Map<String,Sheet> sheets;


    /**
     * Associations; maps context string to its sheet.  Requires no 
     * synchronization.
     */
    final private SortedMap<String,Set<Sheet>> associations;

    
    final private PathContext pathContext;


    public MemorySheetManager() {
        this(true);
    }

    
    /**
     * Constructor.
     */
    public MemorySheetManager(boolean live) {
        super("unkownn", live);
        this.pathContext = new DefaultPathContext(new File("."));
        sheets = new HashMap<String,Sheet>();
        sheets.put(DEFAULT_SHEET_NAME, getUnspecifiedSheet());
        associations = new TreeMap<String,Set<Sheet>>();
        SingleSheet globals = addSingleSheet(GLOBAL_SHEET_NAME);
        if (live) {
            globals.set(this, MANAGER, this);
        } else {
            Stub stub = (Stub)getManagerModule();
            globals.setStub(stub, MANAGER, stub);
        }
        globals.set(getManagerModule(), ROOT, 
                new SettingsMap<Object>(globals, Object.class));
//        this.root = new SettingsMap<Object>(globals, Object.class);
    }


//    @Override
//    public Map<String,Object> getRoot() {
//        return root;
//    }



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
            SingleSheet r;
            if (name.equals(GLOBAL_SHEET_NAME)) {
                r = createSingleSheet(null, name);
            } else {
                r = createSingleSheet(getGlobalSheet(), name);
            }
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
    public Collection<String> getAssociations(String context) {
        Set<String> result = new HashSet<String>();
        Set<Sheet> sheets = associations.get(context);
        for (Sheet sheet: sheets) {
            result.add(sheet.getName());
        }
        return result;
    }
    
    @Override
    public Set<String> getContexts() {
        return associations.keySet();
    }


    @Override
    public void associate(Sheet sheet, Collection<String> surts) {
        for (String surt: surts) {
            Set<Sheet> sheets = associations.get(surt);
            if (sheets == null) {
                sheets = new HashSet<Sheet>();
                associations.put(surt, sheets);
            }
            sheets.add(sheet);
        }
    }


    @Override
    public void disassociate(Sheet sheet, Collection<String> surts) {
        for (String surt: surts) {
            Set<Sheet> sheets = associations.get(surt);
            if (sheets != null) {
                sheets.remove(sheet);
                if (sheets.isEmpty()) {
                    associations.remove(surt);
                }
            }
        }
    }


    public File getDirectory() {
        return new File(".");
    }


    public void commit(Sheet sheet) {
        if (sheet.getSheetManager() != this) {
            throw new IllegalArgumentException();
        }
        if (sheet.isClone() == false) {
            throw new IllegalArgumentException();
        }
        clearCloneFlag(sheet);
        this.sheets.put(sheet.getName(), sheet);
        announceChanges(sheet);
    }

    
    public Set<String> getProblemSingleSheetNames() {
        return Collections.emptySet();
    }

    
    public List<PathChangeException> getSingleSheetProblems(String sheet) {
        return Collections.emptyList();
    }


    @Override
    public List<Association> findConfigNames(String uri) {
        final List<Association> result = new ArrayList<Association>();
        List<String> prefixes = new ArrayList<String>();
        PrefixFinder.find((SortedSet<String>)associations.keySet(), uri, prefixes);
        for (String prefix: prefixes) {
            Set<Sheet> sheets = associations.get(prefix);
            for (Sheet sheet: sheets) {
                result.add(new Association(prefix, sheet.getName()));
            }
        }
        return result;
    }
    
    
    @Override
    public Sheet findConfig(String context) {
        final List<Sheet> result = new ArrayList<Sheet>();
        List<String> prefixes = new ArrayList<String>();
        TreeSet<String> k = new TreeSet<String>(associations.keySet());
        PrefixFinder.find(k, context, prefixes);
        for (String prefix: prefixes) {
            Set<Sheet> sheets = associations.get(prefix);
            for (Sheet sheet: sheets) {
                result.add(sheet);
            }
        }

        if (result.isEmpty()) {
            return getGlobalSheet();
        }
        
        if (result.size() == 1) {
            return result.get(0);
        }

        return this.createSheetBundle("anonymous", result);
    }

    
    public Collection<String> listContexts(String sheetName, int ofs, int len) {
        int count = 0;
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String,Set<Sheet>> me: associations.entrySet()) {
            for (Sheet sheet: me.getValue()) {
                if (sheet.getName().equals(sheetName)) {
                    if (count >= ofs) {
                        result.add(me.getKey());
                        if (result.size() >= len) {
                            return result;
                        }
                    }
                    count++;
                }
            }
        }
        return result;
    }
    
    
    public void stubCleanup() {
        
    }

    
    public PathContext getPathContext() {
        return pathContext;
    }

}
