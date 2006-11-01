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
package org.archive.crawler2.settings;


import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * Manager for sheets.  
 * 
 * @author pjack
 */
public abstract class SheetManager {


    /**
     * Constructor.
     */
    public SheetManager() {
    }


    /**
     * Returns the default sheet.  This sheet is consulted if there is no
     * association for a given SURT or its predecessors.
     * 
     * @return   the default sheet
     */
    public abstract SingleSheet getDefault();
    
    
    /**
     * Returns the root processors configured by this manager.  The returned
     * list is unmodifiable.
     * 
     * @return  the list of root processors
     */
    public abstract List<NamedObject> getRoots();

    
    public abstract void addRoot(String name, Object root);
    
    
    public abstract void removeRoot(String name);
    
    
    public abstract void moveRootUp(String rootName);
    
    
    public abstract void moveRootDown(String rootName);

    /**
     * Returns the names of the sheets being managed.  The returned set 
     * is unmodifiable, and represents a snapshot of the names.  Changes
     * to the sheets being managed will not be reflected in the returned set.
     * 
     * @return   the set of sheet names
     */
    public abstract Set<String> getSheetNames();

    
    /**
     * Adds a sheet with the given name.  This method is invoked 
     * by {@link #createSingleSheet(String, int)} and
     * {@link #createSheetBundle(String, int)} after a new sheet instance
     * is allocated.  Subclasses should store update the database of sheets
     * in persistent storage, etc.
     * 
     * @param sheet   the newly allocated sheet
     * @param name    the name of the new sheet
     */
    protected abstract void addSheet(Sheet sheet, String name);

    
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
    public SingleSheet createSingleSheet(String name) 
    throws IllegalArgumentException {
        SingleSheet result = new SingleSheet(this, name);
        addSheet(result, name);
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
    public SheetBundle createSheetBundle(String name, Collection<Sheet> c) 
    throws IllegalArgumentException {
        SheetBundle result = new SheetBundle(this, name, c);
        addSheet(result, name);
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

    
    public Object getRoot(String name) {
        List<NamedObject> roots = getRoots();
        return NamedObject.getByName(roots, name);
    }
    
    
    public abstract void swapRoot(String name, Object newRoot);
}
