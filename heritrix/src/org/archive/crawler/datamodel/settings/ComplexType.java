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
 * ComplexType.java
 * Created on Dec 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

/**
 * 
 * @author John Erik Halse
 *
 */
public class ComplexType implements DynamicMBean {
	private ComplexType parent;
	private String name;
	private String description;
	private String absoluteName;
	private List attributes;
	private Map attributeNames;
	private DataContainer defaults;
	private AbstractSettingsHandler settingsHandler;

	/**
	 * 
	 */
	private ComplexType() {
	}

	public ComplexType(AbstractSettingsHandler settingsHandler, CrawlerModule parent, String name, String description) {
		this.settingsHandler = settingsHandler;
		this.name = name;
		this.description = description;
		attributes = new ArrayList();
		attributeNames = new HashMap();
		defaults = new DataContainer(this);
		if(parent != null) {
			parent.addElement(name, description, true, null, this);
			absoluteName = parent.getAbsoluteName() + '/' + name;
		} else {
			// This is the top level crawl controller
			absoluteName = name;
		}
		setDefaults();
	}
	
	protected void setDefaults() {
		// Standar null implementation
	}
	
	public void addElement(
		String name,
		String description,
		boolean overrideable,
		Object[] legalValues,
		Object defaultValue) {
		if (attributeNames.containsKey(name)) {
			throw new IllegalArgumentException("Duplucate field: " + name);
		}
		MBeanAttributeInfo attribute =
			new ModuleAttributeInfo(
				name,
				defaultValue,
				description,
				overrideable,
				legalValues);
		attributes.add(attribute);
		attributeNames.put(name, attribute);
		defaults.put(name, defaultValue);
	}

	public void addElement(
		String name,
		String description,
		Object defaultValue) {
		addElement(name, description, true, null, defaultValue);
	}

/*
	protected void addComplexType(
		String description,
		ComplexType defaultValue) {
		addElement(defaultValue.getName(), description, true, null, defaultValue);
	}
*/


	public AbstractSettingsHandler getSettingsHandler() {
		return settingsHandler;
	}

	public String getName() {
		return name;
	}
	
	/**
	 * @return
	 */
	protected String getAbsoluteName() {
		return absoluteName;
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name)
		throws AttributeNotFoundException, MBeanException, ReflectionException {
		return defaults.get(name);
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
	 */
	public void setAttribute(Attribute attribute)
		throws
			AttributeNotFoundException,
			InvalidAttributeValueException,
			MBeanException,
			ReflectionException {
		defaults.put(attribute.getName(), attribute.getValue());
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
	 */
	public AttributeList getAttributes(String[] name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
	 */
	public AttributeList setAttributes(AttributeList attributes) {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(String arg0, Object[] arg1, String[] arg2)
		throws MBeanException, ReflectionException {
		throw new ReflectionException(
			new NoSuchMethodException("No methods to invoke."));
	}

	/* (non-Javadoc)
	 * @see javax.management.DynamicMBean#getMBeanInfo()
	 */
	public MBeanInfo getMBeanInfo() {
		MBeanAttributeInfo attrs[] =
			(MBeanAttributeInfo[]) attributes.toArray(
				new MBeanAttributeInfo[0]);
		MBeanInfo info =
			new MBeanInfo(
				this.getClass().getName(),
				description,
				attrs,
				null,
				null,
				null);
		return info;
	}

	protected MBeanAttributeInfo getAttributeInfo(String name) {
		return (MBeanAttributeInfo) attributeNames.get(name);
	}
}
