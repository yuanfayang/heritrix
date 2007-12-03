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
 * KeyMaker.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Aids construction of Key instances.  This class is fully mutable and
 * provides a way to legibly construct a new Key:
 * 
 * <pre>
 * KeyMaker<String> km = KeyMaker.make("default value");
 * km.constraints.add(new OneStringConstraint());
 * km.constraints.add(new TwoStringConstraint());
 * km.expert = true;
 * km.overrideable = false;
 * MY_KEY = km.toKey();
 * </pre>
 * 
 * In the absence of KeyMaker, the above code would look something like this:
 * 
 * <pre>
 * MY_KEY = new Key<String>(String.class, "default value", true, false,
 *   new HashSet<Constraint<String>>(Arrays.asList(new Constraint<String>[] {
 *     new OneStringConstraint(), new TwoStringConstraint() 
 *   })));
 * </pre>
 * 
 * It would get even worse as new Key attributes were defined (metadata
 * fields, perhaps).  So this class keeps things clean.
 * 
 * <p>You probably don't have to use this class.  See the 
 * {@link Key#make(Object)} family of methods for shortcuts for common kinds
 * of Keys.
 * 
 * @author pjack
 *
 * @param <T>   the type of the generated key's values
 */
public class KeyMaker<T> {

    /** Type of the property. */
    Class<T> type;

    /** Constraints for the property.  Defaults to new HashSet. */
    Set<Constraint<T>> constraints;

    /** Default value for the property.  Defaults to null. */
    Object def;
    
    /** 
     * The element type of a list or map.  Will be null unless this.type
     * is java.util.List or java.util.Map.
     * 
     * <p>For maps, the element type is the type of the map's values.  Maps
     * in the settings system always have String keys.
     */ 
    Class<?> elementType;

    
    /**
     * True if the settings framework should attempt to autodetect the right 
     * value for this setting.  Only if the autodetect attempt fails will the
     * value specified by {@link def} be used.
     */
    boolean autoDetect;
    
    /** Constructor. */
    public KeyMaker() {
        constraints = new HashSet<Constraint<T>>();
    }


    /**
     * Resets this KeyMaker.  All fields are set to null, and the constraint
     * set is cleared.  There's usually no reason to explicitly invoke this
     * method; it will be called when a key is produced by this maker.
     */
    public void reset() {
        type = null;
        constraints.clear();
        def = null;
    }


    /**
     * Validates that the current contents of this KeyMaker can produce 
     * a valid key.
     * 
     * @throws IllegalArgumentException  if any of this KeyMaker's fields
     *   are invalid
     */
    void validate() {
    }
    
    /**
     * Adds a constraint, returning this KeyMaker (for 
     * chained invocations). 
     * 
     * @return  this KeyMaker
     */
    public KeyMaker<T> addConstraint(Constraint<T> c) {
        constraints.add(c);
        return this;
    }
    
    
    /**
     * Makes a key, then resets this KeyMaker.
     * 
     * @return  the made Key
     */
    public Key<T> toKey() {
        return new Key<T>(this);
    }


    /**
     * Returns a KeyMaker with the given default value.  Type type will be
     * automatically set based on the type of the default.  
     * 
     * <p>The point of this method is that it does most of the work.  You can
     * subsequent add constraints and so on before making the key.
     * 
     * @param <T>   the type of the Key to make
     * @param def   the default value for the Key
     * @return   the KeyMaker that will make that key
     */
    public static <T> KeyMaker<T> make(T def) {
        @SuppressWarnings("unchecked")
        Class<T> c = (Class<T>)def.getClass();
        if (!KeyTypes.isSimple(c)) {
            throw new IllegalArgumentException(
                    "Supplied actual object default value for a non-simple " +
                    "Key.  Supply the Class of an implementation of " 
                    + c.getName() + " instead.");
        }
        KeyMaker<T> result = new KeyMaker<T>();
        result.type = c;
        result.def = def;
        return result;
    }
    
    
    /**
     * Returns a KeyMaker for a Key with a module type.  
     * 
     * @param <T>       the type of the Key to make
     * @param type      the type of the Key to make
     * @param defClass  the class of the default implementation 
     * @return   the KeyMaker that will make the Key
     */
    public static <T> KeyMaker<T> make(Class<T> type, Class<? extends T> defClass) {
        if (KeyTypes.isSimple(type)) {
            throw new IllegalArgumentException(
                    "Supplied implementation class for a simple Key. " +
                    "Supply the actual default object instead.");
        }
        KeyMaker<T> result = new KeyMaker<T>();
        result.type = type;
        result.def = defClass;
        return result;
    }


    /**
     * Returns a KeyMaker for a List with the given element type.  The default
     * value will be set to an empty, unmodifiable list of that type.
     * 
     * @param <T>       the element type
     * @param element   the element type
     * @return  the KeyMaker that will make the Key
     */
    public static <T> KeyMaker<List<T>> makeList(Class<T> element) {
        Class<?> c = List.class;
        @SuppressWarnings("unchecked")
        Class<List<T>> c2 = (Class<List<T>>)c;

        KeyMaker<List<T>> r = new KeyMaker<List<T>>();
        r.type = c2;
        r.elementType = element;
        @SuppressWarnings("unchecked")
        List<T> empty = Collections.EMPTY_LIST;
        
        r.def = empty;
        return r;
    }


    /**
     * Returns a KeyMaker for a List with the given simple element type.  
     * The default value will be set to the given elements.
     * 
     * @param <T>       the element type of the list
     * @param element   the element type of the list
     * @param def       the elements in the default value of the list
     * @return   the KeyMaker that will make the Key
     */
    @SuppressWarnings("unchecked")
    public static <T> KeyMaker<List<T>> makeSimpleList(Class<T> element, T... def) {
        if (!KeyTypes.isSimple(element)) {
            throw new IllegalArgumentException("Can't provide instances for " +
            		"List with module element type.  Use makeModuleList " +
            		"instead.");
        }
        KeyMaker<List<T>> r = new KeyMaker<List<T>>();
        r.type = (Class)List.class;
        r.elementType = element;
        r.def = Arrays.asList(def);
        return r;
    }
    
    
    /**
     * Returns a KeyMaker for a List with the given module element type.
     * The default value will be set to the given list of implementation
     * classes.
     * 
     * @param <T>      the element type of the list
     * @param element  the element type of the list
     * @param def      the implementation classes of the default elements 
     *                    of the list
     * @return   the KeyMaker that will make the Key
     */
    @SuppressWarnings("unchecked")
    public static <T> KeyMaker<List<T>> makeModuleList(Class<T> element, 
            Class<? extends T>... def) {
        if (KeyTypes.isSimple(element)) {
            throw new IllegalArgumentException("Can't provide implementation" +
            		" classes for List with simple element type.  Use " +
            		"makeSimpleList instead.");
        }
        KeyMaker<List<T>> r = new KeyMaker<List<T>>();
        r.type = (Class)List.class;
        r.elementType = element;
        r.def = Arrays.asList(def);
        return r;
    }
    
    

    /**
     * Returns a KeyMaker for a Map with the given value type.  The default
     * value will be set to an empty, unmodifiable map of that type.
     * 
     * @param <T>     the value type
     * @param value   the value type
     * @return   a KeyMaker for a Map with that value type
     */
    public static <T> KeyMaker<Map<String,T>> makeMap(Class<T> value) {
        Class<?> c = Map.class;
        @SuppressWarnings("unchecked")
        Class<Map<String,T>> c2 = (Class<Map<String,T>>)c;

        @SuppressWarnings("unchecked")
        Map<String,T> empty = Collections.EMPTY_MAP;
        
        KeyMaker<Map<String,T>> r = new KeyMaker<Map<String,T>>();
        r.type = c2;
        r.def = empty;
        r.elementType = value;
        
        return r;
    }
    
    
    @SuppressWarnings("unchecked")
    public static<T> KeyMaker<Map<String,T>> makeSimpleMap(Class<T> element, 
            Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        KeyMaker<Map<String,T>> r = new KeyMaker<Map<String,T>>();
        r.type = (Class)Map.class;
        r.elementType = element;
        Map<String,T> def = new LinkedHashMap<String,T>();
        for (int i = 0; i < pairs.length; i += 2) {
            String k = (String)pairs[i];
            T v = element.cast(pairs[i + 1]);
            def.put(k, v);
        }
        r.def = Collections.unmodifiableMap(def);
        return r;
    }
    

    @SuppressWarnings("unchecked")
    public static<T> KeyMaker<Map<String,T>> makeModuleMap(Class<T> element, 
            Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        KeyMaker<Map<String,T>> r = new KeyMaker<Map<String,T>>();
        r.type = (Class)Map.class;
        r.elementType = element;
        Map<String,Class<? extends T>> def = 
            new LinkedHashMap<String,Class<? extends T>>();
        for (int i = 0; i < pairs.length; i += 2) {
            String k = (String)pairs[i];
            Class v = (Class)pairs[i + 1];
            def.put(k, v);
        }
        r.def = Collections.unmodifiableMap(def);
        return r;
    }


    
    public static <T> KeyMaker<T> makeNull(Class<T> type) {
        KeyMaker<T> km = new KeyMaker<T>();
        km.type = type;
        km.def = null;
        return km;
    }

    

    
    public static <T> KeyMaker<T> makeAuto(Class<T> type) {
        KeyMaker<T> km = new KeyMaker<T>();
        km.autoDetect = true;
        km.type = type;
        km.def = null;
        return km;
    }

}
