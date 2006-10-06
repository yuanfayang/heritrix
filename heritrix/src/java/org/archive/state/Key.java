/* Copyright (C) 2006 Internet Archive.
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
 * Key.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.archive.i18n.LocaleCache;


/**
 * The key to a processor property.  Note this class is immutable.
 * 
 * @author pjack
 *
 * @param <Value>
 */
final public class Key<Value> {

    /** The name of the field.  Set by the KeyManager. */
    private String fieldName;
    
    /** The class who declares the field.  Set by the KeyManager. */
    private Class owner;
    
    /** The type of the field. */
    final private Class<Value> type;
    
    /** The default value of the field. */
    final private Value def;
    
    /** The constraints that determine valid values for the field. */
    final private Set<Constraint<Value>> constraints;

    /** True if the property is consider "expert". */
    final private boolean expert;

    /**
     * Constructs a new key.
     * 
     * Note that the given maker will be reset before the constructor returns;
     * the maker can then be used to define another key.
     * 
     * @param maker  the information for the new key
     */
    public Key(KeyMaker<Value> maker) {
        maker.validate();
        this.type = maker.type;
        this.def = maker.def;
        this.expert = maker.expert;
        Set<Constraint<Value>> s = new HashSet<Constraint<Value>>(maker.constraints);
        this.constraints = Collections.unmodifiableSet(s);
        maker.reset();
    }


    /**
     * Invoked by the KeyManager when it registers a new key.
     * 
     * @param owner  the class that declared this key
     * @param name   the field name of this key
     */
    void setMetadata(Class owner, String name) {
        this.fieldName = name;
        this.owner = owner;
    }


    /**
     * Returns true if the property is considered "expert".  User interfaces
     * may decide to hide expert properties from end users.
     * 
     * @return  true if this property is expert.
     */
    public boolean isExpert() {
        return expert;
    }

    /**
     * Returns the name of the Java field that declared this key.
     * 
     * @return  the field name of this key
     */
    public String getFieldName() {
        return fieldName;
    }


    /**
     * Returns the name of this key in the given locale.
     * 
     * @param locale   the locale
     * @return  the name of this key in that locale
     */
    public String getName(Locale locale) {
        String result = LocaleCache.load(owner, locale).get(fieldName + "-name");
        return (result == null) ? fieldName.replace('_', '-') : result;
    }


    /**
     * Returns the description of this key in the given locale.
     * 
     * @param locale  the locale
     * @return  the description of this key in that locale
     */
    public String getDescription(Locale locale) {
        return LocaleCache.load(owner, locale).get(fieldName + "-description");
    }
    
    /**
     * Returns the class who declared this key.
     * 
     * @return  the class who declared this key
     */
    public Class getOwner() {
        return owner;
    }


    /**
     * Returns the type of this key's values.
     * 
     * @return  the type of this key's values
     */
    public Class<Value> getType() {
        return type;
    }


    /**
     * Returns the constraints that determine valid values for this key.
     * 
     * @return  the constraints for this key
     */
    public Set<Constraint<Value>> getConstraints() {
        return constraints;
    }


    /**
     * Returns the default value for this key.
     * 
     * @return  the default value for this key
     */
    public Value getDefaultValue() {
        return def;
    }


    @Override
    public String toString() {
        return fieldName;
    }


    /**
     * Creates a new expert Key with the given default value and no 
     * constraints.
     * 
     * @param <X>   the type of values for the key
     * @param def   the default value for the key
     * @return  the new Key
     */
    public static <X> Key<X> makeExpert(X def) {
        KeyMaker<X> result = new KeyMaker<X>();
        @SuppressWarnings("unchecked")
        Class<X> c = (Class<X>)def.getClass();
        result.type = c;
        result.def = def;
        result.expert = true;
        return new Key<X>(result);
    }
    

    /**
     * Creates a new non-expert Key with the given default value and no
     * constraints.
     * 
     * @param <X>   the type of values for the key
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static <X> Key<X> make(X def) {
        KeyMaker<X> result = new KeyMaker<X>();
        @SuppressWarnings("unchecked")
        Class<X> c = (Class<X>)def.getClass();
        result.type = c;
        result.def = def;
        return new Key<X>(result);        
    }
}
