/* Copyright (C) 2003 Internet Archive.
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
 * CrawlerSettings.java
 * Created on Dec 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author John Erik Halse
 *
 */
public class CrawlerSettings {
	private final Map localComplexTypes = new HashMap();
	private final Map modules = new HashMap();
	private String scope;
	private String name = "";
	private String description = "";
	private CrawlerSettings parent;

	/**
	 * 
	 */
	public CrawlerSettings(CrawlerSettings parent, String scope) {
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
	
	public void addComplexType(ComplexType type) {
		if(localComplexTypes.containsKey(type.getAbsoluteName())) {
			throw new IllegalArgumentException("Duplicate complex type: " + type.getAbsoluteName());
		} else {
			localComplexTypes.put(type.getAbsoluteName(), new Object[] {type, new DataContainer(type)});
		}
	}

	public Object[] getComplexType(String absoluteName) {
		return (Object[]) localComplexTypes.get(absoluteName);
	}

	public DataContainer getData(String absoluteName) {
		return (DataContainer) ((Object[]) localComplexTypes.get(absoluteName))[1];
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
