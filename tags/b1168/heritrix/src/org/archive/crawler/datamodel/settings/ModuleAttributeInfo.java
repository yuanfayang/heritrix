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
 * ModuleAttributeInfo.java
 * Created on Dec 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import javax.management.MBeanAttributeInfo;

/**
 * 
 * @author John Erik Halse
 */
public class ModuleAttributeInfo extends MBeanAttributeInfo {
	private final boolean overridable;
	private final Object legalValueLists[];
	private final ComplexType complexType;

	/**
	 * @param name
	 * @param type
	 * @param description
	 * @param isOverrideable
	 * @throws java.lang.IllegalArgumentException
	 */
	public ModuleAttributeInfo(
		String name,
		Object type,
		String description,
		boolean isOverrideable,
		Object[] legalValues)
		throws IllegalArgumentException {
		super(name, type.getClass().getName(), description, true, true, false);
		overridable = isOverrideable;
		legalValueLists = legalValues;
		if(type instanceof ComplexType) {
			complexType = (ComplexType) type;
		} else {
			complexType = null;
		}
	}

	public Object[] getLegalValues() {
		return legalValueLists;
	}

	public ComplexType getComplexType() {
		return complexType;
	}
	
	/**
	 * @return
	 */
	public boolean isOverridable() {
		return overridable;
	}
}
