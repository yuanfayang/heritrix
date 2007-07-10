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
 * Sheet.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;


import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.archive.state.Key;
import org.archive.state.KeyTypes;
import org.archive.state.StateProvider;


/**
 * A sheet of settings.  Sheets must be created via a sheet manager.
 * Concrete implementations are safe for use by concurrent threads.
 * 
 * @author pjack
 */
public abstract class Sheet implements StateProvider, Serializable {

    
    /**
     * The manager that created this sheet.
     */
    final private SheetManager manager;


    private String name;
    
    /**
     * True if this sheet is a "clone" sheet used to edit settings.
     */
    private boolean clone;


    /**
     * Constructor.  Package-protected to ensure that only two subclasses
     * exist, SingleSheet and SheetBundle.
     * 
     * @param manager   the manager who created this sheet
     */
    Sheet(SheetManager manager, String name) {
        if (manager == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.manager = manager;
    }
    
    
    /**
     * Returns the sheet manager who created this sheet.
     * 
     * @return   the sheet manager
     */
    public SheetManager getSheetManager() {
        return manager;
    }

    
    /**
     * Returns the unique name of this sheet.  The name may be changed via
     * the SheetManager.
     * 
     * @return  the unique name of this sheet
     */
    public String getName() {
        return name;
    }
    
    
    void setName(String name) {
        this.name = name;
    }

    
    /**
     * True if this is a copy of a sheet checked out for editing.
     * 
     * @return
     */
    public boolean isClone() {
        return clone; 
    }
    
    
    void setClone(boolean clone) {
        this.clone = clone;
    }
    
    
    /**
     * Looks up a value for a property.  
     * 
     * @param <T>
     * @param module   the module who needs the property value
     * @param key         a key (declared by the module) that defines the
     *                    property to return
     * @return  either the value for that module/Key combination, or null
     *    if this sheet does not include such a value
     */
    public abstract <T> T check(Object module, Key<T> key);

    
    public abstract <T> Offline checkOffline(Offline module, Key<T> key);
    
    public abstract <T> Resolved<T> resolve(Object module, Key<T> key);


    <T> Resolved<T> resolveDefault(Object module, Key<T> key) {
        if (isOnline(key)) {
            return resolveDefaultOnline(module, key);
        } else {
            return resolveDefaultOffline(module, key);
        }
    }
    
    
    private <T> Resolved<T> resolveDefaultOnline(Object module, Key<T> key) {
        SingleSheet defaults = getGlobalSheet();
        T result = defaults.check(module, key);
        if (result == null) {
            Sheet un = getSheetManager().getUnspecifiedSheet();
            return un.resolve(module, key);
        }
        return Resolved.makeOnline(module, key, result, defaults);
    }
    
    
    private <T> Resolved<T> resolveDefaultOffline(Object module, Key<T> key) {
        Offline offline = (Offline)module;
        SingleSheet defaults = getGlobalSheet();
        Offline result = defaults.checkOffline(offline, key);
        if (result == null) {
            Sheet un = getSheetManager().getUnspecifiedSheet();
            return un.resolve(module, key);
        }
        return Resolved.makeOffline(module, key, result, defaults);
    }
    
    
    final public <T> T get(Object module, Key<T> key) {
        return resolve(module, key).getOnlineValue();
    }



    
    /**
     * Returns true if the "online" method of lookup and storage should be
     * used for the given key.  For offline sheets, only some kinds of 
     * objects are actually proxied with Offline objects.  For instance,
     * Strings and other essentially primitive types are stored using the
     * the actual object values instead of Offline proxies.
     * 
     * <p>This will return true if:
     * 
     * <ol>
     * <li>The SheetManager's isOnline() returns true.
     * <li>The given key's type is java.util.List.
     * <li>The given key's type is java.util.Map.
     * <li>The given key's isLeaf() method returns true.
     * </ol>
     * 
     * @param key
     * @return
     */
    boolean isOnline(Key key) {
        return isOnline(key.getType());
    }
    
    
    boolean isOnline(Class type) {
        if (getSheetManager().isOnline()) {
            return true;
        }
        if (type == List.class) {
            return true;
        }
        if (type == Map.class) {
            return true;
        }
        if (KeyTypes.isSimple(type)) {
            return true;
        }
        return false;        
    }


    public String toString() {
        return name;
    }
    
    
    abstract Sheet duplicate();

    
    
    
    
    static <T> void validateModuleType(Object module, Key<T> key) {
        Class mtype = Offline.getType(module);
        if (!key.getOwner().isAssignableFrom(mtype)) {
            throw new IllegalArgumentException("Illegal module type.  " +
                    "Key owner is " + key.getOwner().getName() + 
                    " but module is " + mtype.getName()); 
        }
    }


    SingleSheet getGlobalSheet() {
        return getSheetManager().getGlobalSheet();
    }

}
