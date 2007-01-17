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
 * KeyManager.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manager for keys.  All {@link StateProcessor} implementations that define
 * keys must use the {@link #addKeys(Class)} method to register themselves
 * with the KeyManager.  This should be done in a static initializer, so that
 * the Keys are registered as soon as the class is loaded, eg:
 * 
 * <pre>
 * public class Foo {
 * 
 *     final public static Key<String> BAR = new Key("");
 *     
 *     static {
 *         KeyManager.addKeys(Foo.class);
 *     }
 * 
 * 
 * }
 * </pre>
 * 
 * FIXME: The KeyManager keeps strong references to the classes that register
 * themselves.  A future version of this class should allow classes that are
 * no longer in use to be unloaded.
 * 
 * @author pjack
 */
final public class KeyManager {


    /**
     * Maps a class to the keys defined by that class and its superclasses.
     * Anticipates hundreds of keys, but few newly loaded classes.  (It is 
     * assumed that most StateProcessor implementations will be initialized at 
     * application start-up time, and that loading new processors will be
     * a rare event.)
     */
    private static Map<Class,KeyManagerData> keys
     = new ConcurrentHashMap<Class,KeyManagerData>(256, (float)0.75, 1);


    /**
     * Static library.
     */
    private KeyManager() {
    }


    /**
     * Adds the keys defined by the given class to this manager.  Any class
     * that defines keys should invoke this method in a static initializer.
     * 
     * @param c  the class whose keys to add
     * @throws  InvalidKeyException  if the class declares a key that is
     *  non-static, non-public or non-final
     * @throws  DuplicateKeyException   if the class declares a key with
     *  the same name as one of its superclass keys
     */
    public static void addKeys(Class c) {
        if (KeyTypes.isSimple(c)) {
            return;
        }
        KeyManagerData kmd = keys.get(c);
        if (kmd != null) {
            // Class already processed, perhaps by a subclass
            return;
        }

        // Ensure static initializer has been run on c.
        initialize(c);
        
        // Make sure superclass keys exist in the map.
        for (Class s = c.getSuperclass(); s != null; s = s.getSuperclass()) {
            addKeys(s);
        }
        
        // Construct new map of superclass keys.
        Map<String,Key<Object>> discovered = new HashMap<String,Key<Object>>();
        discovered.putAll(getKeys(c.getSuperclass()));
        
        // Remember dependencies for later processing.
        Map<Class,Key<Object>> dependencies = new HashMap<Class,Key<Object>>();
        
        // Add keys declared by given class
        for (Field field: c.getDeclaredFields()) {
            if (isKeyField(field)) {
                Key<Object> k = getKey(field);
                String name = field.getName().toLowerCase().replace('_', '-');
                k.setMetadata(c, field);
                Key old = discovered.put(name, k);
                if (old != null) {
                    throw new DuplicateKeyException("duplicate key: " + name);
                }
                if (k.isDependency()) {
                    old = dependencies.put(k.getType(), k);
                    if (old != null) {
                        throw new DependencyException("duplicate dependency: " 
                                + k.getType());
                    }
                }
            }
        }

        // Verify that there is a constructor that satisfies all the
        // marked dependencies.
        Constructor cons = depCons(c, dependencies.keySet());
        
        // Figure out which Dependency keys match which parameters in the
        // constructor.
        List<Key<Object>> depConsParams = depConsParams(cons, dependencies);
        
        // Store the key-related data for the class for future reference
        discovered = Collections.unmodifiableMap(discovered);
        kmd = new KeyManagerData(discovered, cons, depConsParams);
        keys.put(c, kmd);
    }

    
    private static List<Key<Object>> depConsParams(Constructor cons, 
            Map<Class,Key<Object>> dependencies) {
        if (cons == null) {
            return Collections.emptyList();
        }
        List<Key<Object>> depConsParams;
        Class[] ptypes = cons.getParameterTypes();
        if (ptypes.length == 0) {
            depConsParams = Collections.emptyList();
        } else {
            depConsParams = new ArrayList<Key<Object>>(ptypes.length);
            for (Class pt: ptypes) {
                depConsParams.add(dependencies.get(pt));
            }
            depConsParams = Collections.unmodifiableList(depConsParams);
        }
        return depConsParams;
    }
    

    /**
     * Fetch the Key instance from the given Key field.
     * This method assumes that the given field passes the
     * {@link #isKeyField(Field)} test.
     * 
     * @param keyField  the key field to fetch
     * @return  the Key stored in that field
     */
    private static Key<Object> getKey(Field keyField) {
        try {
            @SuppressWarnings("unchecked")
            Key<Object> r = (Key<Object>)keyField.get(null);
            return r;
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        }
    }


    /**
     * Returns true if the given field defines a Key.
     * The field must be final, public and static.
     * Also, the field's type must be {@link Key}.
     * 
     * @param f  the field to test
     * @return  true if that field is a Key field
     */
    private static boolean isKeyField(Field f) {
        int mods = f.getModifiers();
        if (f.getType() != Key.class) {
            return false;
        }
        if (!Modifier.isStatic(mods)) {
            throw new InvalidKeyException("Non-static Key: " + f);
        }
        if (!Modifier.isPublic(mods)) {
            throw new InvalidKeyException("Non-public Key: " + f);
        }
        if (!Modifier.isFinal(mods)) {
            throw new InvalidKeyException("Non-final Key: " + f);
        }
        return true;
    }

    
    private static KeyManagerData lookup(Class c) {
        if (c == null) {
            return null;
        }
        
        KeyManagerData result = keys.get(c);
        if (result == null) {
            addKeys(c);
            result = keys.get(c);
        }
        
        return result;
    }

    /**
     * Returns the set of keys defined by the given class and its
     * superclasses.  If the given class and its superclasses
     * do not define any keys, then an empty set is returned.
     * 
     * @param c   the class whose keys to return
     * @return   the set of keys defined by that class
     */
    public static Map<String,Key<Object>> getKeys(Class c) {
        if (c == null) {
            return Collections.emptyMap();
        }
        if (KeyTypes.isSimple(c)) {
            return Collections.emptyMap();
        }
        KeyManagerData result = lookup(c);
        if (result == null) {
            return Collections.emptyMap();
        } else {
            return result.keys;
        }
    }


    /**
     * Ensures that the static initializer of the given class has been run.
     * 
     * @param c  the class to initialize
     */
    private static void initialize(Class c) {
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (ClassNotFoundException e) {
            // This is impossible by contract.
            throw new AssertionError();
        }
    }

    

    /**
     * Returns the constructor that satisfies the given class's dependencies.
     * 
     * @param c
     * @return
     */
    public static Constructor getDependencyConstructor(Class c) {
        KeyManagerData kmd = lookup(c);
        if (kmd != null) {
            return kmd.depCons;
        }
        
        try {
            return c.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new DependencyException(c.getName() + " does not define "
                    + " a public, no-argument constructor.");
        }
    }
    
    
    public static List<Key<Object>> getDependencyKeys(Class c) {
        KeyManagerData kmd = lookup(c);
        if (kmd != null) {
            return kmd.depConsParams;
        }
        
        return Collections.emptyList();
    }

    
    private static Constructor depCons(Class c, Set<Class> params) {
        if (Modifier.isAbstract(c.getModifiers())) {
            return null;
        }
        
        if (params.isEmpty()) {
            try {
                return c.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new DependencyException(c.getName() + " requires " +
                    "a no-argument constructor since it has no dependencies.");
            }
        }
                
        List<Constructor> results = new LinkedList<Constructor>();
        for (Constructor cons: c.getConstructors()) {
            if (isDepCons(cons, params)) {
                results.add(cons);
            }
        }
        switch (results.size()) {
            case 0:
                throw new DependencyException(c.getName() + " does not " 
                    + " define a constructor that satisfies its dependencies.");
            case 1:
                return results.get(0);
            default:
                throw new DependencyException(c.getName() + " defines more "
                    + "than one constructor that satisfies its dependencies.");
        }
    }

    
    private static boolean isDepCons(Constructor c, Set<Class> params) {
        HashSet<Class> formal = new HashSet<Class>();
        formal.addAll(Arrays.asList(c.getParameterTypes()));
        return formal.equals(params);
    }


}
