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
package org.archive.crawler.datamodel.settings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    /** Name of this collection of settings */
    private String name = "";

    /** Description of this collection of settings */
    private String description = "";

    /** Constructs a new CrawlerSettings object.
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
        if (scope == null || scope.equals("")) {
            return null;
        }
        return settingsHandler.getSettings(settingsHandler.getParentScope(scope));
    }

    /** Get the SettingHandler this CrawlerSettings object belongs to.
     *
     * @return the SettingHandler this CrawlerSettings object belongs to.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }
}
