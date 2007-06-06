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


import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.archive.i18n.LocaleCache;
import org.archive.settings.Offline;


/**
 * The key to a processor property.  Note this class is immutable.
 * 
 * @author pjack
 *
 * @param <Value>
 */
final public class Key<Value> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The name of the field.  Set by the KeyManager. */
    private String fieldName;
    
    /** The class who declares the field.  Set by the KeyManager. */
    private Class owner;
    
    
    
    /** The type of the field. */
    transient final private Class<Value> type;


    /**
     * The element type of a List or Map.  Will be null if this.type is 
     * anything other than java.util.List or java.util.Map.
     */
    private Class elementType;
    
    
    /** The default value of the field. */
    transient final private Value def;
    
    /** The default offline value of the field. */
    transient final private Object offlineDef;
    
    /** The constraints that determine valid values for the field. */
    transient final private Set<Constraint<Value>> constraints;

    // Note that all flags are false by default, and set to true in the
    // presence of a corresponding annotation.
    
    /** True if the property is considered "expert". */
    transient private boolean expert;

    /** True if the property can be overridden. */
    transient private boolean global;
    
    /** True if the property can be mutated. */
    transient private boolean immutable;
    
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
        this.elementType = maker.elementType;
        this.def = maker.def;
        this.offlineDef = makeOfflineDefault(maker.def);
        Set<Constraint<Value>> s = new HashSet<Constraint<Value>>(maker.constraints);
        this.constraints = Collections.unmodifiableSet(s);
        maker.reset();
    }


    /**
     * Invoked by the KeyManager when it registers a new key.
     * 
     * @param owner  the class that declared this key
     * @param field   the Key's field
     */
    void setMetadata(Class owner, Field field) {
        this.fieldName = field.getName().toLowerCase().replace('_', '-');
        this.owner = owner;
        if (field.getAnnotation(Expert.class) != null) {
            this.expert = true;
        }
        if (field.getAnnotation(Global.class) != null) {
            this.global = true;
        }
        if (field.getAnnotation(Immutable.class) != null) {
            this.global = true;
            this.immutable = true;
        }
    }

    
    public Class getElementType() {
        return elementType;
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
     * Returns false if the property can be overridden.  Overrideable properties
     * may have different values depending on context.  Non-overrideable 
     * properties have the same value for all contexts.
     * 
     * @return  false if this propery can be overridden
     */
    public boolean isGlobal() {
        return global;
    }
    
    
    public boolean isImmutable() {
        return immutable;
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
    public Class<?> getOwner() {
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
     * Creates a basic Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
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


    public static <X> Key<X> make(Class<X> type, X def) {
        KeyMaker<X> result = new KeyMaker<X>();
        result.type = type;
        result.def = def;
        return result.toKey();
    }

    
    
    /**
     * Returns a Key for a List with the given element type.  The default
     * value will be an empty, unmodifiable List of that type.  
     * 
     * @param <X>        the element type of the list
     * @param element    the element type of the list
     * @return   a Key for a List with that element type
     */
    public static <X> Key<List<X>> makeList(Class<X> element) {
        KeyMaker<List<X>> km = KeyMaker.makeList(element);
        return new Key<List<X>>(km);
    }
    
    
    /**
     * Returns a Key for a Map with the given value type.  The default value
     * will be an empty, unmodifiable Map with that value type.
     * 
     * @param <X>     the value type of the Map
     * @param value   the value type of the Map
     * @return   a Key for a Map with that value type
     */
    public static <X> Key<Map<String,X>> makeMap(Class<X> value) {
        KeyMaker<Map<String,X>> km = KeyMaker.makeMap(value);
        return new Key<Map<String,X>>(km);
    }



    public static <X> Key<X> makeNull(Class<X> cls) {
        return KeyMaker.makeNull(cls).toKey();
    }

    
    public boolean hasOffline() {
        return hasOffline(type);
    }

    
    private static boolean hasOffline(Class type) {
        if (List.class.isAssignableFrom(type)) {
            return false;
        }
        if (Map.class.isAssignableFrom(type)) {
            return false;
        }
        if (KeyTypes.isSimple(type)) {
            return false;
        }
        return true;
    }

    
    public Object getOfflineDefault() {
        return offlineDef;
    }
    

    private static Object makeOfflineDefault(Object def) {
        if (def == null) {
            return null;
        }
        Class type = def.getClass();
        if (KeyTypes.isSimple(type)) {
            return def;
        }
        if (List.class.isAssignableFrom(type)) {
            return makeOfflineList(def);
        }
        if (Map.class.isAssignableFrom(type)) {
            return makeOfflineMap(def);
        }
        return Offline.make(def.getClass());
    }


    private static List<Object> makeOfflineList(Object def) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List)def;
        ArrayList<Object> result = new ArrayList<Object>(list.size());
        for (Object o: list) {
            result.add(makeOfflineDefault(o));
        }
        return Collections.unmodifiableList(result);
    }

    
    private static Map<String,Object> makeOfflineMap(Object def) {
        @SuppressWarnings("unchecked")
        Map<String,Object> map = (Map)def;
        
        @SuppressWarnings("unchecked")
        Map<String,Object> result = new HashMap(map.size());
        for (Map.Entry<String,Object> me: map.entrySet()) {
            result.put(me.getKey(), makeOfflineDefault(me.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    
    // Preserve singleton status 
    private Object readResolve() {
        return KeyManager.getKeys(owner).get(fieldName);
    }
}
