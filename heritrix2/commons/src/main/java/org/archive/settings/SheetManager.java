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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.file.Checkpointable;
import org.archive.settings.path.PathChangeException;
import org.archive.settings.path.PathListConsumer;
import org.archive.settings.path.PathLister;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.PathContext;
import org.archive.state.StateProvider;

import sun.security.util.PathList;


/**
 * Manager for sheets.  
 * 
 * @author pjack
 */
public abstract class SheetManager implements StateProvider, Serializable {

    final public static Logger LOGGER = 
        Logger.getLogger(SheetManager.class.getName());
    
    final public static String GLOBAL_SHEET_NAME = "global";
    
    final private UnspecifiedSheet unspecified;
    
    final transient private Stub<?> stubThis;

    private List<ModuleListener> moduleListeners = 
        new CopyOnWriteArrayList<ModuleListener>();
    
    private ListModuleListener<Finishable> finishables = 
        ListModuleListener.make(Finishable.class);
    
    private ListModuleListener<Checkpointable> checkpointables =
        ListModuleListener.make(Checkpointable.class);
    
    private ListModuleListener<Closeable> closeables = 
        ListModuleListener.make(Closeable.class);

    private boolean live;
    
    @Immutable
    final public static Key<Map<String,Object>> ROOT = 
        Key.makeMap(Object.class);

    @Immutable
    final public static Key<SheetManager> MANAGER = 
        Key.make(SheetManager.class, null);

    static {
        KeyManager.addKeys(SheetManager.class);
    }
    
    private String jobName;
    
    /**
     * Constructor.
     */
    public SheetManager(String jobName, boolean live) {
        this.jobName = jobName;
        this.live = live;
        this.unspecified = new UnspecifiedSheet(this, "default");
        stubThis = Stub.make(getClass());
        moduleListeners.add(checkpointables);
        moduleListeners.add(closeables);
        moduleListeners.add(finishables);
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
    
    public SheetManager(String jobName, Collection<ModuleListener> listeners, boolean live) {
        this(jobName, live);
        moduleListeners.addAll(listeners);
    }

    
    Sheet getUnspecifiedSheet() {
        return unspecified;
    }
    
    
    public Object getManagerModule() {
        if (isLive()) {
            return this;
        } else {
            return stubThis;
        }
    }
    

    /**
     * Returns the global sheet.  This sheet is consulted if there is no
     * association for a given SURT or its predecessors.
     * 
     * @return   the default sheet
     */
    public SingleSheet getGlobalSheet() {
        return (SingleSheet)getSheet(GLOBAL_SHEET_NAME);
    }
    
    
    /**
     * Returns the root module configured by this manager.
     * 
     * @return  the root module
     */
    public Map<String,Object> getRoot() {
        return getGlobalSheet().resolveEditableMap(
                getManagerModule(),
                ROOT);
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
        boolean global = name.equals(GLOBAL_SHEET_NAME);
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
     * Associates a sheet with one or more context strings.
     * 
     * @param sheet     the sheet to associate
     * @param strings   the context strings to associate with that sheet
     */
    public abstract void associate(Sheet sheet, Collection<String> strings);
    
    
    /**
     * Disassociates a sheet with one or more context strings.  If one or
     * more of the given strings is not associated with the given sheet,
     * then those strings are silently skipped.
     * 
     * @param sheet   the sheet to disassociate
     * @param strings   the context strings to disassociate from that sheet
     */
    public abstract void disassociate(Sheet sheet, Collection<String> strings);


    /**
     * Returns the sheets for a given context.  If no association exists for
     * that context, this method returns an empty collection.  Otherwise the 
     * sheets that are associated with the context are returned.
     * 
     * @param context   the context whose association to look up
     * @return   the sheets associated with that context
     */
    public abstract Collection<String> getAssociations(String context);

    
    public void cleanup() {}
    
    
    public String getCrawlName() {
        return jobName; 
    }

    
    final public boolean isLive() {
        return live;
    }


    public <T> T get(Object module, Key<T> key) {
        Sheet def = getGlobalSheet();
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
    
/*
    public String toAbsolutePath(String path) {
        return DefaultDirectoryModule.toAbsolutePath(getDirectory(), path);
    }
    
    
    public String toRelativePath(String path) {
        return DefaultDirectoryModule.toRelativePath(getDirectory(), path);
    }
    */

    
    /**
     * Returns the configuration for the given URI.
     * 
     * @param uri
     * @return
     */
    public abstract Sheet findConfig(String uri);


    public abstract List<Association> findConfigNames(String uri);
    
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
    
    protected void announceChanges(Sheet sheet) {
        if (sheet instanceof SingleSheet) {
            SingleSheet ss = (SingleSheet)sheet;
            List<KeyChangeEvent> list = ss.clearKeyChangeEvents();
            for (KeyChangeEvent event: list) {
                KeyChangeListener listener = (KeyChangeListener)event.getSource();
                try {
                    listener.keyChanged(event);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, 
                            "Exception during property change " + event, e);
                }
            }
        }
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

    public abstract Collection<String> listContexts(String sheetName, 
            int ofs, int len);

    public abstract void stubCleanup();


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeBoolean(live);
        out.writeObject(moduleListeners);
        out.writeObject(checkpointables);
        out.writeObject(closeables);
        out.writeObject(finishables);
    }

    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        live = in.readBoolean();
        moduleListeners = (List)in.readObject();
        checkpointables = (ListModuleListener)in.readObject();
        closeables = (ListModuleListener)in.readObject();
        finishables = (ListModuleListener)in.readObject();
    }

    
    public abstract PathContext getPathContext();

 
    /**
     * Finds a unique module in the global sheet.  There must be only one 
     * module that is an instance of the given type in the sheet, or this
     * method raises IllegalStateException.  If no module is an instance of
     * the type, then null is returned.
     * 
     * @param <T>   the type of the module to return
     * @param cls   the type of the module to return
     * @return   the one module in the global sheet that is an instance of
     *     that type, or null if no such module exists
     * @exception  IllegalStateException   if more than one module in the 
     *     global sheet is an instance of that type
     */
    public <T> T findUniqueGlobalModule(final Class<T> cls) {
        final Holder result = new Holder();
        PathListConsumer consumer = new PathListConsumer() {
            
            List<String> paths = new ArrayList<String>();
            
            @Override
            public void consume(String path, List<Sheet> sheet, Object value,
                    Class type, String seenPath) {
                if (cls.isInstance(value)) {
                    if (result.module == null) {
                        paths.add(path);
                        result.module = value;
                    } else if (result.module != value) {
                        paths.add(path);
                        throw new IllegalStateException("Not unique: " + paths); 
                    }
                }
            }
            
        };
        
        PathLister.resolveAll(getGlobalSheet(), consumer, false);
        return cls.cast(result.module);
    }
}
