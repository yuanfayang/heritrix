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
	private final Object defaultValue;
	private final Object legalValueLists[];
	private final ComplexType complexType;

	/**
	 * @param name
	 * @param type
	 * @param description
	 * @param isOverrideable
     * @param legalValues
     * @param defaultValue
	 * @throws java.lang.IllegalArgumentException
	 */
	public ModuleAttributeInfo(
		String name,
		Object type,
		String description,
		boolean isOverrideable,
		Object[] legalValues,
		Object defaultValue)
		throws IllegalArgumentException {
		super(name, type.getClass().getName(), description, true, true, false);
		overridable = isOverrideable;
		legalValueLists = legalValues;
        checkValue(defaultValue);
		this.defaultValue = defaultValue;
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
	
	/**
	 * @return
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

    public boolean checkValue(Object value) {
        if (value.getClass().getName().equals(getType())) {
            Object legalValues[] = getLegalValues();
            if (legalValues != null) {
                for (int i = 0; i < legalValues.length; i++) {
                    if (legalValues[i].equals(value)) {
                        return true;
                    }
                }
                throw new IllegalArgumentException("Illegal value: " + value);
            } else {
                return true; // No defaults defined
            }
        } else {
            throw new IllegalArgumentException(
                "Value of illegal type: '"
                    + value.getClass().getName()
                    + "', '"
                    + getType()
                    + "' was expected");
        }
    }
}
