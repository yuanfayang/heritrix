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
 * DataContainer.java
 * Created on Dec 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel.settings;

import java.util.HashMap;

import javax.management.MBeanAttributeInfo;

/**
 * 
 * @author John Erik Halse
 *
 */
public class DataContainer extends HashMap {
	private ComplexType definition;

	/**
	 * 
	 */
	public DataContainer(ComplexType module) {
		super();
		this.definition = module;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		MBeanAttributeInfo attrInfo = definition.getAttributeInfo((String) key);
		if (attrInfo == null) {
			throw new IllegalArgumentException("Name not defined: " + key);
		}
		if (value.getClass().getName().equals(attrInfo.getType())) {
			return super.put(key, value);
		} else {
			throw new IllegalArgumentException("Value of illegal type: '" + value.getClass().getName() + "', '" + attrInfo.getType() + "' was expected");
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		if (definition.getAttributeInfo((String) key) == null) {
			throw new IllegalArgumentException("Name not defined: " + key);
		}
		return super.get(key);
	}
}
