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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.state.DefaultDirectoryModule;
import org.archive.state.DirectoryModule;
import org.archive.settings.file.Checkpointable;
import org.archive.settings.path.PathChangeException;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.surt.SURTTokenizer;


/**
 * Manager for sheets.  
 * 
 * @author pjack
 */
public abstract class SheetManager 
implements StateProvider, Serializable, DirectoryModule {

    final public static Logger LOGGER = 
        Logger.getLogger(SheetManager.class.getName());
    
    final public static String DEFAULT_SHEET_NAME = "default";
    
    final private UnspecifiedSheet unspecified;
    
    final transient private Offline offlineThis;

    final private List<ModuleListener> moduleListeners = 
        new CopyOnWriteArrayList<ModuleListener>();
    
    final private ListModuleListener<Finishable> finishables = 
        ListModuleListener.make(Finishable.class);
    
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
        moduleListeners.add(finishables);
        KeyManager.addKeys(getClass());
    }
    
    
    public List<Checkpointable> getCheckpointables() {
        return checkpointables.getList();
    }
    
    
    public List<Closeable> getCloseables() {
        return closeables.getList();
    }
    
    public List<Finishable> getFinishables() {
        return finishables.getList();
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
     * FIXME: Rename this to getGlobal()
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
        boolean global = name.equals("default");
        SingleSheet result = new SingleSheet(this, name, global);
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

    
    public boolean isOnline() {
        return true; // FIXME: Make this abstract
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
    

    public String toAbsolutePath(String path) {
        return DefaultDirectoryModule.toAbsolutePath(getDirectory(), path);
    }
    
    
    public String toRelativePath(String path) {
        return DefaultDirectoryModule.toRelativePath(getDirectory(), path);
    }

    
    /**
     * Returns the configuration for the given URI.
     * 
     * @param uri
     * @return
     */
    public StateProvider findConfig(String uri) {
        SURTTokenizer st;
        try {
            st = new SURTTokenizer(uri.toString());
        } catch (URIException e) {
            throw new IllegalArgumentException(e);
        }
        
        SheetList list = null;
        for (String s = st.nextSearch(); s != null; s = st.nextSearch()) {
            Sheet sheet = getAssociation(s);
            if (sheet != null) {
                if (list == null) {
                    list = new SheetList();
                }
                list.add(sheet);
            }
        }
        if (list == null) {
            return getDefault();
        }
        
        list.add(getDefault());
        return list;
    }    

    
    /**
     * Closes any modules that require special cleanup.  First, this method
     * invokes {@link Finishable#finalTasks} on any {@link Finishable} 
     * module being managed by this SheetManager.
     * 
     * <p>After all Finishable modules have run their final tasks, this 
     * method invokes {@link Closeable#close} on any {@link Closeable} 
     * module being managed by this SheetManager.
     * 
     * <p>Any exceptions encountered are logged with a SEVERE level, but
     * otherwise ignored; this method never raises an exception.
     */
    public void closeModules() {
        List<Finishable> finishables = this.getFinishables();
        for (Finishable f: finishables) try {
            f.finalTasks(this);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not finish " + f, e);            
        }
        
        List<Closeable> closeables = this.getCloseables();
        for (Closeable c: closeables) try {
            c.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not close " + c, e);
        }
    }

    
    public Sheet checkout(String name) {
        Sheet s = getSheet(name);
        s = s.duplicate();
        s.setClone(true);
        return s;
    }

    
    protected void clearCloneFlag(Sheet sheet) {
        sheet.setClone(false);
    }

    
    public abstract void commit(Sheet sheet);
    
    
    /**
     * Returns a read-only set of single sheets that generated errors while
     * loading.
     * 
     * @return
     */
    public abstract Set<String> getProblemSingleSheetNames();

    
    /**
     * Returns the list of exceptions that a problem sheet generated, in the
     * order they were encountered.
     * 
     * @param sheet  the name of the sheet whose problems to return
     * @return   the list of problems for that sheet
     */
    public abstract List<PathChangeException> getSingleSheetProblems(
            String sheet);

}
