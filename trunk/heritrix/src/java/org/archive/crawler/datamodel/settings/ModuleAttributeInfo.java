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
    private boolean isOverrideable;
    private boolean isTransient;
    private final Object defaultValue;
    private final Object legalValueLists[];
    private boolean complexType = false;
    private boolean isExpertSetting;
    
    /** Construct a new instance of ModuleAttributeInfo.
     * 
     * @param name name of attribute.
     * @param type an instance of the attributes type.
     * @param description a description usable for the UI.
     * @param isOverrideable should this attribute be overrideable on per settings.
     * @param isTransient is this attribute visible for UI and serializer.
     * @param legalValues list of legalvalues or null if no restrictions apply.
     * @param defaultValue the default value for this attribute.
     * @param isExpertSetting should this attribute only show up in UIs expert mode.
     * 
     * @throws InvalidAttributeValueException
     * @throws java.lang.IllegalArgumentException
     */
    public ModuleAttributeInfo(String name, Object type, String description,
            boolean isOverrideable, boolean isTransient, Object[] legalValues,
            Object defaultValue, boolean isExpertSetting)
            throws InvalidAttributeValueException {

        super(name, type.getClass().getName(), description, true, true, false);
        setType(type);
        this.isOverrideable = isOverrideable;
        this.isTransient = isTransient;
        legalValueLists = legalValues;
        this.defaultValue = checkValue(defaultValue);
        if (type instanceof ComplexType) {
            complexType = true;
        }
        this.isExpertSetting = isExpertSetting;
    }

    public Object[] getLegalValues() {
        return legalValueLists;
    }

    /** Returns true if this attribute refers to a ComplexType.
     * 
     * @return true if this attribute refers to a ComplexType.
     */
    public boolean isComplexType() {
        return complexType;
    }

    /** Returns true if this attribute could be overridden in per settings.
     * 
     * @return True if overrideable.
     */
    public boolean isOverrideable() {
        return isOverrideable;
    }

    /** Returns true if this attribute should be hidden from UI and not be
     * serialized to persistent storage.
     * 
     * @return True if transient.
     */
    public boolean isTransient() {
        return isTransient;
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
