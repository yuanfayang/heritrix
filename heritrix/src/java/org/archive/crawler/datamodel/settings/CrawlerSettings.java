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
    private final Map localComplexTypes = new HashMap();
    private final Map modules = new HashMap();
    private final SettingsHandler handler;

    /** Scope for this collection of settings (hostname) */
    private final String scope;

    /** Name of this collection of settings */
    private String name = "";

    /** Description of this collection of settings */
    private String description = "";

    private CrawlerSettings parent;

    /** Constructs a new CrawlerSettings object.
     * 
     * Application code should not call the constructor directly, but use the
     * methods in SettingsHandler instead.
     * 
     * @param handler The SettingsHandler this object belongs to.
     * @param parent The parent of this settings object or null if this is the
     *               order.
     * @param scope The scope of this settings object (ie. host or domain).
     * 
     * @see SettingsHandler#getSettings(String)
     * @see SettingsHandler#getSettingsObject(String)
     */
    public CrawlerSettings(
        SettingsHandler handler,
        CrawlerSettings parent,
        String scope) {
        this.handler = handler;
        this.scope = scope;
        this.parent = parent;
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
     * @param the description to be set for this CrawlerSettings object.
     */
    public void setDescription(String string) {
        description = string;
    }

    /** Set the name of this CrawlerSettings object.
     * 
     * @param the name to be set for this CrawlerSettings object.
     */
    public void setName(String string) {
        name = string;
    }

    protected void addModule(CrawlerModule module) {
        if (modules.containsKey(module.getName())) {
            throw new IllegalArgumentException(
                "Duplicate module name: " + module.getName());
        } else {
            modules.put(module.getName(), module);
        }
    }

    protected DataContainer addComplexType(ComplexType type) {
        DataContainer data = new DataContainer(type);
        localComplexTypes.put(type.getAbsoluteName(), data);
        return data;
    }

    protected DataContainer getData(String absoluteName) {
        DataContainer data =
            (DataContainer) localComplexTypes.get(absoluteName);
        if (data != null) {
            return data;
        } else {
            return null;
        }
    }

    protected CrawlerModule getModule(String name) {
        return (CrawlerModule) modules.get(name);
    }

    protected Iterator modules() {
        return modules.values().iterator();
    }

    /** Get the parent of this CrawlerSettings object.
     * 
     * @return the parent of this CrawlerSettings object.
     */
    public CrawlerSettings getParent() {
        return parent;
    }

    /** Get the SettingHandler this CrawlerSettings object belongs to.
     * 
     * @return the SettingHandler this CrawlerSettings object belongs to.
     */
    public SettingsHandler getSettingsHandler() {
        return handler;
    }
}
