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
 * SheetManager.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;


import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.file.Checkpointable;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.KeyTypes;
import org.archive.state.StateProvider;


/**
 * Manager for sheets.  
 * 
 * @author pjack
 */
public abstract class SheetManager implements StateProvider, Serializable {

    final public static Logger LOGGER = 
        Logger.getLogger(SheetManager.class.getName());
    
    final public static String DEFAULT_SHEET_NAME = "default";
    
    final private UnspecifiedSheet unspecified;
    
    final transient private Offline offlineThis;

    final private List<ModuleListener> moduleListeners = 
        new CopyOnWriteArrayList<ModuleListener>();
    
    final private ListModuleListener<Checkpointable> checkpointables =
        ListModuleListener.make(Checkpointable.class);
    
    final private ListModuleListener<Closeable> closeables = 
        ListModuleListener.make(Closeable.class);
    
    @Immutable
    final public static Key<Map<String,Object>> ROOT = 
        Key.makeMap(Object.class);

    @Immutable
    final public static Key<SheetManager> MANAGER = 
        Key.make(SheetManager.class, null);

    /**
     * Constructor.
     */
    public SheetManager() {
        this.unspecified = new UnspecifiedSheet(this, "unspecified");
        offlineThis = Offline.make(getClass());
        moduleListeners.add(checkpointables);
        moduleListeners.add(closeables);
        KeyManager.addKeys(getClass());
    }
    
    
    public List<Checkpointable> getCheckpointables() {
        return checkpointables.getList();
    }
    
    
    public List<Closeable> getCloseables() {
        return closeables.getList();
    }
    
    public SheetManager(Collection<ModuleListener> listeners) {
        this();
        moduleListeners.addAll(listeners);
    }

    
    Sheet getUnspecifiedSheet() {
        return unspecified;
    }
    
    
    public Object getManagerModule() {
        if (isOnline()) {
            return this;
        } else {
            return offlineThis;
        }
    }
    

    /**
     * Returns the default sheet.  This sheet is consulted if there is no
     * association for a given SURT or its predecessors.
     * 
     * @return   the default sheet
     */
    public abstract SingleSheet getDefault();
    
    
    /**
     * Returns the root module configured by this manager.
     * 
     * @return  the root module
     */
    public Map<String,Object> getRoot() {
        return getDefault().get(getManagerModule(), ROOT);
    }


    /**
     * Returns the names of the sheets being managed.  The returned set 
     * is unmodifiable, and represents a snapshot of the names.  Changes
     * to the sheets being managed will not be reflected in the returned set.
     * 
     * @return   the set of sheet names
     */
    public abstract Set<String> getSheetNames();

    
    /**
     * Adds a single sheet with the given name.  Subclasses should use
     * {@link #createSingleSheet(String)} to allocate a new sheet, then 
     * store that sheet in persistent storage, etc.
     * 
     * @param name    the name for the new sheet
     * @return   the newly allocated sheet
     * @throws  IllegalArgumentException  
     *  if a sheet with that name already exists
     */
    public abstract SingleSheet addSingleSheet(String name);

    
    /**
     * Adds a sheet bundle with the given name.  Subclasses should use
     * {@link #createSheetBundle(String)} to allocate a new sheet, then 
     * store that sheet in persistent storage, etc.
     * 
     * @param name    the name for the new sheet
     * @return   the newly allocated sheet
     * @throws  IllegalArgumentException  
     *  if a sheet with that name already exists
     */
    public abstract SheetBundle addSheetBundle(String name, 
            Collection<Sheet> sheets);


    /**
     * Returns the sheet with the give name.
     * 
     * @param sheetName   the name of the sheet to return
     * @return  the sheet with that name
     * @throws  IllegalArgumentException   if no such sheet exists
     */
    public abstract Sheet getSheet(String sheetName)
    throws IllegalArgumentException;
    
    
    /**
     * Creates a single sheet and registers it with this manager.
     * 
     * @param name   the name of the new sheet
     * @return   the newly created and registered sheet
     * @throws IllegalArgumentException   if that name already exists
     */
    final protected SingleSheet createSingleSheet(String name) 
    throws IllegalArgumentException {
        SingleSheet result = new SingleSheet(this, name);
        return result;
    }


    /**
     * Creates a sheet bundle and registers it with this manager.
     * 
     * @param name  the name of the new sheet
     * @param c     the sheets to include in the bundle 
     * @return      the newly registered sheet
     * @throws IllegalArgumentException   if that name already exists
     */
    final protected SheetBundle createSheetBundle(String name, Collection<Sheet> c) 
    throws IllegalArgumentException {
        SheetBundle result = new SheetBundle(this, name, c);
        return result;
    }
    

    /**
     * Removes the given sheet from this manager.  All associations to that
     * sheet will also be removed.
     * 
     * @param sheetName   the name of the sheet to remove
     * @throws  IllegalArgumentException   if no such sheet exists
     */
    public abstract void removeSheet(String sheetName)
    throws IllegalArgumentException;

    
    /**
     * Renames a sheet.
     * 
     * @param oldName   the sheet's old name
     * @param newName   the new name for that sheet
     * @throws  IllegalArgumentException   if a sheet already exists with the
     *    specified new name
     */
    public abstract void renameSheet(String oldName, String newName);

    
    /**
     * Associates a sheet with one or more context strings.  If one or more
     * of the given strings already has an association, then those 
     * associations are replaced.
     * 
     * @param sheet     the sheet to associate
     * @param strings   the context strings to associate with that sheet
     */
    public abstract void associate(Sheet sheet, Iterable<String> strings);
    
    
    /**
     * Disassociates a sheet with one or more context strings.  If one or
     * more of the given strings is not associated with the given sheet,
     * then those strings are silently skipped.
     * 
     * @param sheet   the sheet to disassociate
     * @param strings   the context strings to disassociate from that sheet
     */
    public abstract void disassociate(Sheet sheet, Iterable<String> strings);


    /**
     * Returns the sheet for a given context.  If no association exists for
     * that context, this method returns null.  Otherwise the sheet that is
     * associated with the context is returned.
     * 
     * @param context   the context whose association to look up
     * @return   the sheet associated with that context, or null if no sheet
     * is associated with that context
     */
    public abstract Sheet getAssociation(String context);


    
    public abstract void reload();
    
    
    public abstract void save();

    
    public void cleanup() {}
    
    
    public String getCrawlName() {
        return "unknown"; // FIXME: Make this abstract 
    }
    
    
    public File getWorkingDirectory() {
        return new File("."); // FIXME: Make this abstract
    }

    
    public boolean isOnline() {
        return true; // FIXME: Make this abstract
    }

    
    public List<Object> getDependencies(Object module) {
        Class c;
        if (module instanceof Offline) {
            c = ((Offline)module).getType();
        } else {
            c = module.getClass();
        }
        if (KeyTypes.isSimple(c)) {
            return Collections.emptyList();
        }
        List<Key<Object>> deps = KeyManager.getDependencyKeys(c);
        if (deps.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Object> result = new ArrayList<Object>();
        for (Key<Object> k: deps) {
            result.add(getDefault().resolve(module, k).getValue());
        }
        return result;
    }


    public <T> T get(Object module, Key<T> key) {
        Sheet def = getDefault();
        return def.get(module, key);
    }

    
    public void addModuleListener(ModuleListener listener) {
        this.moduleListeners.add(listener);
    }
    
    
    public void removeModuleListener(ModuleListener listener) {
        moduleListeners.remove(listener);
    }
    
    public List<ModuleListener> getModuleListeners() {
        return new ArrayList<ModuleListener>(moduleListeners);
    }
    
    void fireModuleChanged(Object oldModule, Object newModule) {
        for (ModuleListener ml: moduleListeners) try {
            ml.moduleChanged(oldModule, newModule);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ModuleListener raised exception.", e);
        }
    }
}
