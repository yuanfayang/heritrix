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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.archive.settings.Offline;
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
    
    
    private static class PendingInit {
        Initializable module;
        String path;
    }
    

    private LinkedList<PendingInit> pendingInit = new LinkedList<PendingInit>();

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
    
    
    
    public void change(SingleSheet sheet, PathChange pair) {
        String path = pair.getPath();
        String typeTag = pair.getType();
        String value = pair.getValue();
        
        initIfNecessary(sheet, path);
        
        Object v;
        if (typeTag.equals(REFERENCE_TAG)) {
            v = makeReference(sheet, value);
        } else if (typeTag.equals(OBJECT_TAG)) {
            v = makeObject(sheet, pair);
        } else {
            v = makeSimple(typeTag, value);
        }

        finish(sheet, path, v);        
    }
    
    
    public void finish(SingleSheet sheet) {
        initIfNecessary(sheet, ".");
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

    
    private Object makeObject(SingleSheet sheet, PathChange pc) {
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
            return Class.forName(value).newInstance();
        } catch (ClassNotFoundException e) {
            throw new PathChangeException("No such class: " + value);
        } catch (InstantiationException e) {
            throw new PathChangeException(e);
        } catch (IllegalAccessException e) {
            throw new PathChangeException(e);
        }        
    }
    

    private void finish(SingleSheet sheet, String path, Object value) {
        String previousPath;
        String lastToken;
        Object previous;

        int p = path.lastIndexOf('.');
        if (p < 0) {
            previousPath = "";
            lastToken = path;
            previous = sheet.getSheetManager();
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
