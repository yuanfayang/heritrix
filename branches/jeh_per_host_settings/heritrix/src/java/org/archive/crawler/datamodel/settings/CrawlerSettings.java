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
    private final AbstractSettingsHandler handler;
	private final String scope;
	private String name = "";
	private String description = "";
	private CrawlerSettings parent;

	/**
	 * 
	 */
	public CrawlerSettings(AbstractSettingsHandler handler, CrawlerSettings parent, String scope) {
        this.handler = handler;
		this.scope = scope;
		this.parent = parent;
	}

	/**
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public String getScope() {
		return scope;
	}

	/**
	 * @param string
	 */
	public void setDescription(String string) {
		description = string;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	public void addModule(CrawlerModule module) {
		if(modules.containsKey(module.getName())) {
			throw new IllegalArgumentException("Duplicate module name: " + module.getName());
		} else {
			modules.put(module.getName(), module);
		}
	}
	
	public DataContainer addComplexType(ComplexType type) {
		if(localComplexTypes.containsKey(type.getAbsoluteName())) {
			throw new IllegalArgumentException("Duplicate complex type: " + type.getAbsoluteName());
		} else {
            DataContainer data = new DataContainer(type);
			localComplexTypes.put(type.getAbsoluteName(), new Object[] {type, data});
            return data;
		}
	}

	public Object[] getComplexType(String absoluteName) {
		return (Object[]) localComplexTypes.get(absoluteName);
	}

	public DataContainer getData(String absoluteName) {
        Object[] data = (Object[]) localComplexTypes.get(absoluteName);
        if(data != null) {
            return (DataContainer) data[1];
        } else {
            return null;
        }
        //return (DataContainer) ((Object[]) localComplexTypes.get(absoluteName))[1];
	}
	
	public CrawlerModule getModule(String name) {
		return (CrawlerModule) modules.get(name);
	}
	
	public Iterator modules() {
		return modules.values().iterator();
	}
	
	/**
	 * @return
	 */
	public CrawlerSettings getParent() {
		return parent;
	}
}
