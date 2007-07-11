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

import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;

/**
 * 
 * @author John Erik Halse
 */
public class ModuleAttributeInfo extends MBeanAttributeInfo {
    private String type;
    private final boolean overrideable;
    private final Object defaultValue;
    private final Object legalValueLists[];
    private boolean complexType = false;

    /**
     * @param name
     * @param type
     * @param description
     * @param isOverrideable
     * @param legalValues
     * @param defaultValue
     * @throws InvalidAttributeValueException
     * @throws java.lang.IllegalArgumentException
     */
    public ModuleAttributeInfo(
        String name,
        Object type,
        String description,
        boolean isOverrideable,
        Object[] legalValues,
        Object defaultValue)
        throws InvalidAttributeValueException {
        super(name, null, description, true, true, false);
        setType(type);
        overrideable = isOverrideable;
        legalValueLists = legalValues;
        this.defaultValue = checkValue(defaultValue);
        if (type instanceof ComplexType) {
            complexType = true;
        }
    }

    public Object[] getLegalValues() {
        return legalValueLists;
    }

    public boolean isComplexType() {
        return complexType;
    }
    
    /**
     * @return True if overrideable.
     */
    public boolean isOverrideable() {
        return overrideable;
    }

    /**
     * @return Default value.
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object checkValue(Object value) throws InvalidAttributeValueException {
        // Check if value is of correct type. If not, see if it is
        // a string and try to turn it into right type
        Class typeClass;
        try {
            typeClass = defaultValue == null ? Class.forName(type) : defaultValue.getClass();
            if (!(typeClass.isInstance(value)) && value instanceof String) {
                value = SettingsHandler.StringToType(
                   (String) value, SettingsHandler.getTypeName(getType()));
            }
        } catch (Exception e) {
            throw new InvalidAttributeValueException(
                "Unable to decode string '" + value
                    + "' into type '" + getType() + "'");
        }

        // If it still isn't a legal type throw an error
        try {
            if (!typeClass.isInstance(value)) {
                throw new InvalidAttributeValueException();
            }
        } catch (Exception e) {
            throw new InvalidAttributeValueException(
                "Value of illegal type: '"
                    + value.getClass().getName()
                    + "', '"
                    + getType()
                    + "' was expected");
        }

        // If this attribute is constrained by a list of legal values,
        // check that the value is in that list
        Object legalValues[] = getLegalValues();
        if (legalValues != null) {
            boolean found = false;
            for (int i = 0; i < legalValues.length; i++) {
                if (legalValues[i].equals(value)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new InvalidAttributeValueException(
                    "Value '" + value + "' not in legal values list");
            }
        }

        return value;
    }
    
    /* (non-Javadoc)
     * @see javax.management.MBeanAttributeInfo#getType()
     */
    public String getType() {
        return type;
    }

    protected void setType(Object type) {
        this.type = type.getClass().getName();
    }
}