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
 * PathChanger.java
 * Created on October 24, 2006
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/path/Attic/PathChanger.java,v 1.1.2.4 2007/01/17 01:47:59 paul_jack Exp $
 */
package org.archive.settings.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.archive.settings.Offline;
import org.archive.settings.SettingsList;
import org.archive.settings.SettingsMap;
import org.archive.settings.SingleSheet;
import org.archive.settings.path.PathValidator;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.KeyTypes;


/**
 * Applies one or more changes to a sheet.  The changes to take place are 
 * specified using strings.  Objects to change are identified by their
 * paths, and the new value for those paths are also provided as strings.
 * 
 * @author pjack
 *
 */
public class PathChanger {

    
    final public static String OBJECT_TAG = "object";
    
    final public static String REFERENCE_TAG = "reference";
    
    final public static String MAP_TAG = "map";
    
    final public static String LIST_TAG = "list";
    
    private static class PendingInit {
        Initializable module;
        String path;
    }
    

    private LinkedList<PendingInit> pendingInit = new LinkedList<PendingInit>();

    private List<PathChangeException> problems 
        = new ArrayList<PathChangeException>();
    
    /**
     * Constructs a new PathChanger with the default set of transformers.
     * The default set of transformers consist of every final static field
     * of this class, eg {@link #BOOLEAN_TRANSFORMER}, {@link #INT_TRANSFORMER},
     * and so on.
     */
    public PathChanger() {
    }

    
    public void change(SingleSheet ss, Iterable<PathChange> changes) {
        change(ss, changes.iterator());
    }
    
    public void change(SingleSheet ss, Iterator<PathChange> changes) {
        while (changes.hasNext()) {
            change(ss, changes.next());
        }
        finish(ss);
    }
    
    
    public List<PathChangeException> getProblems() {
        return problems;
    }

    
    public void change(SingleSheet sheet, PathChange pair) {
        System.out.println(pair);
        if (sheet.getSheetManager().isOnline()) {
            changeLoudly(sheet, pair);
        } else {
            changeQuietly(sheet, pair);
        }
    }

    
    private void changeQuietly(SingleSheet sheet, PathChange pair) {
        try {
            changeLoudly(sheet, pair);
        } catch (PathChangeException e) {
            e.printStackTrace();
            e.setPathChange(pair);
            problems.add(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            PathChangeException pce = new PathChangeException(e);
            pce.setPathChange(pair);
            problems.add(pce);
        }
    }

    
    private void changeLoudly(SingleSheet sheet, PathChange pair) {
        String path = pair.getPath();
        String typeTag = pair.getType();
        String value = pair.getValue();
        
        initIfNecessary(sheet, path);
    
        Object v;
        
        if (value == null) {
            v = null;
        } else if (typeTag.equals(REFERENCE_TAG)) {
            v = makeReference(sheet, value);
        } else if (typeTag.equals(OBJECT_TAG)) {
            v = makeObject(sheet, pair);
        } else if (typeTag.equals(MAP_TAG)) {
            v = makeMap(sheet, pair);
        } else if (typeTag.equals(LIST_TAG)) {
            v = makeList(sheet, pair);
        } else {
            v = makeSimple(typeTag, value);
        }

        finish(sheet, path, v);        
    }
    
    
    public void finish(SingleSheet sheet) {
        initIfNecessary(sheet, String.valueOf(PathValidator.DELIMITER));
    }
    
    
    private void initIfNecessary(SingleSheet sheet, String currentPath) {
        while (!pendingInit.isEmpty()) {
            PendingInit pi = pendingInit.getFirst();
            if (currentPath.startsWith(pi.path)) {
                return;
            }
            pendingInit.removeFirst();
            pi.module.initialTasks(sheet);
        }
    }    
    
    
    private Object makeReference(SingleSheet sheet, String value) {
        return PathValidator.validate(sheet, value);
    }
    
    
    private Object makeSimple(String typeTag, String value) {
        Class type = KeyTypes.getSimpleType(typeTag);
        try {
            return KeyTypes.fromString(type, value);
        } catch (Exception e) {
            throw new PathChangeException(e);
        }
    }

    
    private Object getPreexistingObject(SingleSheet sheet, PathChange pc) {
        try {
            Object o = PathValidator.validate(sheet, pc.getPath());
            if (o == null) {
                // There was no preexisting object.
                return null;
            }
            
                Class otype = Offline.getType(o);
                if (otype.getName().equals(pc.getValue())) {
                    return o;
                }                            
            return null;            
        } catch (InvalidPathException e) {
            // This may simply mean that the path doesn't exist yet, which
            // is legal (it what's this method is testing for.)
            //
            // If the path really is invalid, later checks will raise an
            // appropriate error.
            return null;
        }
    }
    
    
    private Object getPreexistingCol(SingleSheet sheet, PathChange pc) {
        try {
            Object o = PathValidator.check(sheet, pc.getPath());

            if (pc.getType().equals(MAP_TAG)) {
                if (!(o instanceof SettingsMap)) {
                    return null;
                }
                SettingsMap sm = (SettingsMap) o;
                if (sm.getElementType().getName().equals(pc.getValue())) {
                    return sm;
                }
            } else if (pc.getType().equals(LIST_TAG)) {
                if (!(o instanceof SettingsList)) {
                    return null;
                }
                SettingsList sm = (SettingsList) o;
                if (sm.getElementType().getName().equals(pc.getValue())) {
                    return sm;
                }
            }
            return null;
        } catch (RuntimeException e) {
            // This may simply mean that the path doesn't exist yet, which
            // is legal (it what's this method is testing for.)
            //
            // If the path really is invalid, later checks will raise an
            // appropriate error.
            return null;
        }
    }
    
    
    private Object makeObject(SingleSheet sheet, PathChange pc) {
        // If an object already exists at the given path, AND that object
        // has the type we want to change to, do NOT destroy the old object.
        Object pre = getPreexistingObject(sheet, pc);
        if (pre != null) {
            return pre;
        }
        
        Object result = makeObject2(sheet, pc);
        if (result instanceof Initializable) {
            PendingInit pi = new PendingInit();
            pi.path = pc.getPath();
            pi.module = (Initializable)result;
            pendingInit.addFirst(pi);
        }
        return result;
    }
    
    
    private Object makeObject2(SingleSheet sheet, PathChange pc) {
        String value = pc.getValue();

        try {
            Class<?> c = Class.forName(value);
            if (!online(sheet, c)) {
                Offline offline = Offline.make(c);
                return offline;
            }
            return c.newInstance();
        } catch (ClassNotFoundException e) {
            throw new PathChangeException("No such class: " + value);
        } catch (InstantiationException e) {
            throw new PathChangeException(e);
        } catch (IllegalAccessException e) {
            throw new PathChangeException(e);
        }        
    }
    
    
    private Object makeList(SingleSheet sheet, PathChange pc) {
        Object pre = getPreexistingCol(sheet, pc);
        if (pre != null) {
            return pre;
        }

        String value = pc.getValue();
        try {
            Class c = Class.forName(value);
            
            @SuppressWarnings("unchecked")
            Object r = new SettingsList(sheet, c);
            return r;
        } catch (ClassNotFoundException e) {
            throw new PathChangeException("No such class: " + value);
        }
    }
    
    
    private Object makeMap(SingleSheet sheet, PathChange pc) {
        Object pre = getPreexistingCol(sheet, pc);
        if (pre != null) {
            return pre;
        }
        
        String value = pc.getValue();
        try {
            Class c = Class.forName(value);
            
            @SuppressWarnings("unchecked")
            Object r = new SettingsMap(sheet, c);
            return r;
        } catch (ClassNotFoundException e) {
            throw new PathChangeException("No such class: " + value);
        }
    }

    
    
    private boolean online(SingleSheet sheet, Class c) {
        if (Map.class.isAssignableFrom(c)) {
            return true;
        }
        if (List.class.isAssignableFrom(c)) {
            return true;
        }
        return sheet.getSheetManager().isOnline();
    }
    

    private void finish(SingleSheet sheet, String path, Object value) {
        String previousPath;
        String lastToken;
        Object previous;

        int p = path.lastIndexOf(PathValidator.DELIMITER);
        if (p < 0) {
            previousPath = "";
            lastToken = path;
            previous = sheet.getSheetManager().getManagerModule();
        } else {
            previousPath = path.substring(0, p);
            lastToken = path.substring(p + 1);
            previous = PathValidator.check(sheet, previousPath);
        }
        
        if (previous instanceof List) {
            int index;
            try {
                index = Integer.parseInt(lastToken);
            } catch (NumberFormatException e) {
                throw new PathChangeException(e);
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)previous;
            if (index < list.size()) {
                list.set(index, value);
            } else if (index == list.size()) {
                list.add(value);
            } else {
                throw new PathChangeException("Incorrect index: " 
                        + path + " (expected " + list.size() + ")");
            }
            return;
        }
        
        if (previous instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>)previous;
            map.put(lastToken, value);
            return;
        }

        if (previous == null) {
            throw new PathChangeException("Can't change '" + path + 
                    "' because '"
                    + previousPath + "' resolves to null.");
        }
        
        Class prevType = Offline.getType(previous);
        Map<String,Key<Object>> keys = KeyManager.getKeys(prevType);
        Key<Object> key = keys.get(lastToken);
        if (key == null) {
            throw new PathChangeException("No such key: " + path);
        }
        if (value instanceof Offline) {
            sheet.setOffline((Offline)previous, key, (Offline)value);
        } else {
            sheet.set(previous, key, value);
        }
    }

}
