/* CrawlerSettings
 *
 * $Id$
 *
 * Created on Dec 16, 2003
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.settings;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.settings.refinements.Refinement;

/**
 * Class representing a settings file.
 *
 * More precisely it represents a collection of settings valid in a particular
 * scope. The scope is either the global settings, or the settings to be used
 * for a particular domain or host. For scopes other than global, the instance
 * will only contain those settings that are different from the global.
 *
 * In the default implementation this is a one to one mapping from a file to
 * an instance of this class, but in other implementations the information in
 * an instance of this class might be stored in a different way (for example
 * in a RDBMS).
 *
 * @author John Erik Halse
 */
public class CrawlerSettings {
    /** Registry of DataContainers for ComplexTypes in this settings object
     *  indexed on absolute name */
    private final Map localComplexTypes = new HashMap();

    /** Registry of top level ModuleTypes in this settings object indexed on
     * module name. These are modules that doesn't have parents in this
     * settings object
     */
    private final Map topLevelModules = new HashMap();

    /** Registry of all ModuleTypes in this settings object indexed on
     * module name.
     */
    private final Map localModules = new HashMap();

    /** Reference to the settings handler this settings object belongs to */
    private final SettingsHandler settingsHandler;

    /** Scope for this collection of settings (hostname) */
    private final String scope;
    
    /** List of refinements applied to this settings object */
    private List refinements;
    
    /** True if this settings object is a refinement */
    private boolean isRefinement = false;

    /** Name of this collection of settings */
    private String name = "";

    /** Description of this collection of settings */
    private String description = "";
    
    /** Time when this collection was last saved to persistent storage */
    private Date lastSaved = null;

    /**
     * Constructs a new CrawlerSettings object.
     *
     * Application code should not call the constructor directly, but use the
     * methods in SettingsHandler instead.
     *
     * @param handler The SettingsHandler this object belongs to.
     * @param scope The scope of this settings object (ie. host or domain).
     *
     * @see SettingsHandler#getSettings(String)
     * @see SettingsHandler#getSettingsObject(String)
     */
    public CrawlerSettings(SettingsHandler handler, String scope) {
        this.settingsHandler = handler;
        this.scope = scope;
    }

    /**
    * Constructs a new CrawlerSettings object which is a refinement of another
    * settings object.
    *
    * Application code should not call the constructor directly, but use the
    * methods in SettingsHandler instead.
    *
    * @param handler The SettingsHandler this object belongs to.
    * @param scope The scope of this settings object (ie. host or domain).
    * @param refinement the name or reference to the refinement.
    *
    * @see SettingsHandler#getSettings(String)
    * @see SettingsHandler#getSettingsObject(String)
    */
    public CrawlerSettings(SettingsHandler handler, String scope,
            String refinement) {
        this(handler, scope);
        if (refinement != null && !refinement.equals("")) {
            this.isRefinement = true;
            this.name = refinement;
        }
    }

    /** Get the description of this CrawlerSettings object.
     *
     * @return the description of this CrawlerSettings object.
     */
    public String getDescription() {
        return description;
    }

    /** Get the name of this CrawlerSettings object.
     *
     * @return the name of this CrawlerSettings object.
     */
    public String getName() {
        return name;
    }

    /** Get the scope of this CrawlerSettings object.
     *
     * @return the scope of this CrawlerSettings object.
     */
    public String getScope() {
        return scope;
    }

    /** Set the description of this CrawlerSettings object.
     *
     * @param string the description to be set for this CrawlerSettings object.
     */
    public void setDescription(String string) {
        description = string;
    }

    /** Set the name of this CrawlerSettings object.
     *
     * @param string the name to be set for this CrawlerSettings object.
     */
    public void setName(String string) {
        name = string;
    }
    
    /**
     * Get the time when this CrawlerSettings was last saved to persistent
     * storage.
     * 
     * @return the time when this CrawlerSettings was last saved to persistent
     * storage. Null if it has not been saved.
     */
    public Date getLastSavedTime() {
        return lastSaved;
    }

    /**
     * Set the time when this CrawlerSettings was last saved to persistent
     * storage.
     * 
     * @param lastSaved the time when this CrawlerSettings was last saved to
     * persistent storage.
     */
    protected void setLastSavedTime(Date lastSaved) {
        this.lastSaved = lastSaved;
    }

    protected void addTopLevelModule(ModuleType module) {
//        if (topLevelModules.containsKey(module.getName())) {
//            throw new IllegalArgumentException(
//                "Duplicate module name: " + module.getName());
//        } else {
            topLevelModules.put(module.getName(), module);
//        }
    }

    protected DataContainer addComplexType(ComplexType type) {
        DataContainer data = new DataContainer(this, type);
        localComplexTypes.put(type.getAbsoluteName(), data);
        if (type instanceof ModuleType) {
            localModules.put(type.getName(), type);
        }
        return data;
    }

    protected DataContainer getData(ComplexType complex) {
        return getData(complex.getAbsoluteName());
    }

    protected DataContainer getData(String absoluteName) {
        return (DataContainer) localComplexTypes.get(absoluteName);
    }

    protected ModuleType getTopLevelModule(String name) {
        return (ModuleType) topLevelModules.get(name);
    }

    public ModuleType getModule(String name) {
        return (ModuleType) localModules.get(name);
    }

    protected Iterator topLevelModules() {
        return topLevelModules.values().iterator();
    }

    /** Get the parent of this CrawlerSettings object.
     *
     * @return the parent of this CrawlerSettings object.
     */
    public CrawlerSettings getParent() {
        return getParent(null);
    }
    
    /**
     * Get the parent of this CrawlerSettings object.
     * This method passes around a URI so that refinements could be checked.
     *   
     * @param uri The uri for which parents of this object shoul be found.
     * @return the parent of this CrawlerSettings object.
     */
    public CrawlerSettings getParent(UURI uri) {
        if (isRefinement()) {
            return settingsHandler.getSettingsForHost(scope);
        } else {
            if (scope == null || scope.equals("")) {
                return null;
            } else {
                return settingsHandler.getSettings(settingsHandler
                        .getParentScope(scope), uri);
            }
        }
    }

    /** Get the SettingHandler this CrawlerSettings object belongs to.
     *
     * @return the SettingHandler this CrawlerSettings object belongs to.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }
    
    /**
     * Get an <code>ListIterator</code> over the refinements for this
     * settings object.
     * 
     * @return Returns an iterator over the refinements.
     */
    public ListIterator refinementsIterator() {
        if (refinements == null) {
            refinements = new ArrayList();
        }
        return refinements.listIterator();
    }
    
    /**
     * Add a refinement to this settings object.
     * 
     * @param refinements The refinements to set.
     */
    public void addRefinement(Refinement refinement) {
        if (refinements == null) {
            refinements = new ArrayList();
        }
        this.refinements.remove(refinement);
        this.refinements.add(refinement);
    }
    
    /**
     * Remove a refinement from this settings object.
     * 
     * @param reference the reference (name) to the refinement to be removed.
     * @return true if something was removed, false if the refinement was not
     *         found.
     */
    public boolean removeRefinement(String reference) {
        if (hasRefinements()) {
            for(Iterator it = refinements.iterator(); it.hasNext();) {
                if (((Refinement) it.next()).getReference().equals(reference)) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Get a refinement with a given reference. 
     * 
     * @param reference the reference (name) to the refinement to get.
     * @return the refinement having the specified reference or null if no
     *         refinement matches it.
     */
    public Refinement getRefinement(String reference) {
        if (hasRefinements()) {
            for(Iterator it = refinements.iterator(); it.hasNext();) {
                Refinement tmp = (Refinement) it.next();
                if (tmp.getReference().equals(reference)) {
                    return tmp;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns true if this settings object has refinements attached to it.
     * 
     * @return true if this settings object has refinements attached to it.
     */
    public boolean hasRefinements() {
        return refinements != null && !refinements.isEmpty();
    }
    
    /**
     * Returns true if this settings object is a refinement.
     * 
     * @return true if this settings object is a refinement.
     */
    public boolean isRefinement() {
        return isRefinement;
    }
    
    /**
     * Mark this settings object as an refinement.
     * 
     * @param isRefinement Set this to true if this settings object is a
     *            refinement.
     */
    public void setRefinement(boolean isRefinement) {
        this.isRefinement = isRefinement;
    }
}
