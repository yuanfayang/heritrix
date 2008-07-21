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
 * 
 * @author pjack
 */
public abstract class Sheet implements StateProvider, Serializable {

    
    /**
     * The manager that created this sheet.
     */
    final private SheetManager manager;

    
    /**
     * The parent of this sheet.  May be null if this is the UnspecifiedSheet.
     */
    final String parentName;

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
    Sheet(SheetManager manager, String parentName, String name) {
        if (manager == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.parentName = parentName;
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
     * Returns the parent of this SingleSheet.  The parent will either be
     * another SingleSheet, or the default sheet if this sheet is the global
     * sheet.
     * 
     * @return   the parent of this sheet
     */
    public Sheet getParent() {
        if (parentName == null) {
            return null;
        }
        // Look up by name in case actual sheet instance was replaced by
        // a checkout/commit
        return manager.getSheet(parentName);
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
     * Returns true if this sheet contains a value for the given key and
     * module.
     * 
     * @param module   the module to check
     * @param key      a key of that module to check
     * @return      true if this sheet contains a value for that module/key
     */
    public abstract boolean contains(Object module, Key<?> key);

    abstract Object check(Object module, Key<?> key);


    /**
     * Resolves a the given setting on the given module.  The returned 
     * {@link Resolved} object will contain the value of the setting, as well
     * as the list of sheets that were consulted to provide the value.
     * 
     * @param <T>     the type of the setting
     * @param module  the module whose setting to resolve
     * @param key     the setting to resolve
     * @return        the resolution of that setting
     */
    public abstract <T> Resolved<T> resolve(Object module, Key<T> key);

    
    /**
     * Returns this sheet's value for the given setting on the given module.
     * If this sheet does not contain an override for the setting, then 
     * the parent sheet (and its parents) will be consulted until a value 
     * is found.  Ultimately the default sheet must provide a value.
     * 
     * <p>For list settings, the returned value will actually be a merged list
     * of all the elements provided by this sheet's list (if any) and its parent 
     * sheets' lists (if any).
     * 
     * <p>For map settings, the returned value will be a merged map containing
     * this sheet's mappings (if any) and its parent sheets' mappings (if any).
     * 
     * @param <T>      the type of the setting 
     * @param module   the module whose setting to resolve
     * @param key      the setting whose value to return
     * @return         the value of that setting
     */
    public <T> T get(Object module, Key<T> key) {
        return resolve(module, key).getLiveValue();
    }

    
    /**
     * Returns true if the "live" method of lookup and storage should be
     * used for the given key.  For stub sheets, only some kinds of 
     * objects are actually proxied with Stub objects.  For instance,
     * Strings and other essentially primitive types are stored using the
     * the actual object values instead of Stub proxies.
     * 
     * <p>This will return true if:
     * 
     * <ol>
     * <li>The SheetManager's isLive() returns true.
     * <li>The given key's type is java.util.List.
     * <li>The given key's type is java.util.Map.
     * <li>The given key's isLeaf() method returns true.
     * </ol>
     * 
     * @param key
     * @return
     */
    boolean isLive(Key key) {
        return isLive(key.getType());
    }
    
    
    boolean isLive(Class type) {
        if (getSheetManager().isLive()) {
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
        Class<?> mtype = Stub.getType(module);
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
