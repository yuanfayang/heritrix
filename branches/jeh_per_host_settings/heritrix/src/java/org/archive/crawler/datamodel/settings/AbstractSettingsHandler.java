/* AbstractSettingsHandler
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
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 
 * @author John Erik Halse
 *
 */
public abstract class AbstractSettingsHandler {
    /** Registry of CrawlerModules in order file indexed on module name */
	private final Map moduleRegistry = new HashMap();
    /** Registry of ComplexTypes in order file indexed on absolute name */
	private final Map complexTypesRegistry = new HashMap();
    /** Settings object referencing order file */
	private final CrawlerSettings globalSettings = new CrawlerSettings(this, null, null);
    /** Cached CrawlerSettings objects */
	private final Map settingsCache = new WeakHashMap();
    private final CrawlerModule controller; // TODO: Change this to real controller
	
	final static String INTEGER = "integer";
	final static String LONG = "long";
	final static String FLOAT = "float";
	final static String DOUBLE = "double";
	final static String BOOLEAN = "boolean";
	final static String STRING = "string";
	final static String OBJECT = "object";
	final static String TIMESTAMP = "timestamp";
	final static String MAP = "map";
	final static String INTEGER_LIST = "integerList";
	final static String LONG_LIST = "longList";
	final static String FLOAT_LIST = "floatList";
	final static String DOUBLE_LIST = "doubleList";
	final static String BOOLEAN_LIST = "booleanList";
	final static String STRING_LIST = "stringList";
	private final static String names[][] = new String[][] { 
		{INTEGER, "java.lang.Integer"},
		{LONG, "java.lang.Long"},
		{FLOAT, "java.lang.Float"},
		{DOUBLE, "java.lang.Double"},
		{BOOLEAN, "java.lang.Boolean"},
		{STRING, "java.lang.String"},
		{OBJECT, "org.archive.crawler.datamodel.settings.CrawlerModule"},
		{TIMESTAMP, "java.util.Date"},
		{MAP, "org.archive.crawler.datamodel.settings.ComplexType"},
		{INTEGER_LIST, "org.archive.crawler.datamodel.settings.IntegerList"},
		{LONG_LIST, "org.archive.crawler.datamodel.settings.LongList"},
		{FLOAT_LIST, "org.archive.crawler.datamodel.settings.FloatList"},
		{DOUBLE_LIST, "org.archive.crawler.datamodel.settings.DoubleList"},
		{BOOLEAN_LIST, "org.archive.crawler.datamodel.settings.BooleanList"},
		{STRING_LIST, "org.archive.crawler.datamodel.settings.StringList"}
	};
	private final static Map name2class = new HashMap();
	private final static Map class2name = new HashMap();
	static {
		for(int i=0; i<names.length; i++) {
			name2class.put(names[i][0], names[i][1]);
			class2name.put(names[i][1], names[i][0]);
		}
	}

	/**
	 * 
	 */
	public AbstractSettingsHandler() {
        controller = new Controller(); // TODO: Change this to real controller
        controller.setAsController(this);
        controller.setDefaults();
	}

	/**
	 * @param controller
	 */
	public void initialize() {
		readSettingsObject(globalSettings);
	}
	
	private String getParentScope(String scope) {
		int split = scope.indexOf('.');
		if(split == -1) {
			return null;
		} else {
			return scope.substring(split+1);
		}
	}

	private CrawlerSettings getParentObject(String scope) {
		CrawlerSettings parent;
		if(scope == null || scope.equals("")) {
			parent = null;
		} else {
			parent = getSettingsObject(getParentScope(scope));
			while(parent == null && scope != null) {
				scope = getParentScope(scope);
				parent = getSettings(scope);
			}
		}
		return parent;
	}

	/**
	 * @param name
	 * @return
	 */
	protected CrawlerModule getModuleFromRegistry(String name) {
		return (CrawlerModule) moduleRegistry.get(name);
	}
	
	protected void addToModuleRegistry(CrawlerModule module) {
		if(moduleRegistry.containsKey(module.getName())) {
			throw new IllegalArgumentException("Duplicate module name: " + module.getName());
		} else {
			moduleRegistry.put(module.getName(), module);
		}
	}

	protected void addToComplexTypeRegistry(ComplexType type) {
		if(complexTypesRegistry.containsKey(type.getAbsoluteName())) {
			throw new IllegalArgumentException("Duplicate module name: " + type.getAbsoluteName());
		} else {
            complexTypesRegistry.put(type.getAbsoluteName(), type);
		}
	}

    /**
     * @param absoluteName
     * @return
     */
    protected ComplexType getComplexTypeFromRegistry(String absoluteName) {
        return (ComplexType) complexTypesRegistry.get(absoluteName);
    }
    
	protected static String getTypeName(String className) {
		return (String) class2name.get(className);
	}
	
	protected static String getClassName(String typeName) {
		return (String) name2class.get(typeName);
	}
	
	/**
	 * Get configuration object for a host.
	 * 
	 * @param host the host to get the configuration for
	 * @return CrawlConfiguration object for the host
	 */
	public CrawlerSettings getSettings(String host) {
		CrawlerSettings settings = getSettingsObject(host);
		while(settings == null && host != null) {
			host = getParentScope(host);
			settings = getSettingsObject(host);
		}
		settingsCache.put(host, settings);
		return settings;
	}
	
	/**
	 * @param scope
	 * @return
	 */
	public CrawlerSettings getSettingsObject(String scope) {
		CrawlerSettings settings;
		if(scope == null || scope.equals("")) {
			settings = globalSettings;
		} else if(settingsCache.containsKey(scope)) {
			settings = (CrawlerSettings) settingsCache.get(scope);
		} else {
			settings = new CrawlerSettings(this, getParentObject(scope), scope);
			readSettingsObject(settings);
            settingsCache.put(scope, settings);
		}
		return settings;
	}
	
	/**
	 * @param scope
	 * @return
	 */
/*	public CrawlerSettings createSettingsObject(String scope) {
		CrawlerSettings settings = new CrawlerSettings(this, getParentObject(scope), scope);
		if(readSettingsObject(settings) == null) {// TODO: Check
			settingsCache.put(scope, settings);
		}
		return settings;
	}
*/
	
	/**
	 * @param configuration
	 */
	public abstract void writeSettingsObject(CrawlerSettings settings);

	/**
	 * @param parent
	 * @param scope
	 * @return
	 */
	protected abstract CrawlerSettings readSettingsObject(CrawlerSettings settings);

    /**
     * TODO: Change this to real controller
     * @return
     */
    public CrawlerModule getController() {
        return controller;
    }
}
