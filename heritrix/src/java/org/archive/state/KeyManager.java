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


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private static Map<Class,Map<String,Key>> keys
     = new ConcurrentHashMap<Class,Map<String,Key>>(256, (float)0.75, 1);


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
        if (c == null) {
            new Exception().printStackTrace();
        }

        Map<String,Key> discovered = keys.get(c);
        if (discovered != null) {
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
        discovered = new HashMap<String,Key>();
        discovered.putAll(getKeys(c.getSuperclass()));
        
        // Add keys declared by given class
        for (Field field: c.getDeclaredFields()) {
            if (isKeyField(field)) {
                Key k = getKey(field);
                String name = field.getName();
                k.setMetadata(c, name);
                Key old = discovered.put(name, k);
                if (old != null) {
                    throw new DuplicateKeyException("duplicate key: " + name);
                }
            }
        }
        
        keys.put(c, Collections.unmodifiableMap(discovered));
    }


    /**
     * Fetch the Key instance from the given Key field.
     * This method assumes that the given field passes the
     * {@link #isKeyField(Field)} test.
     * 
     * @param keyField  the key field to fetch
     * @return  the Key stored in that field
     */
    private static Key getKey(Field keyField) {
        try {
            return (Key)keyField.get(null);
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


    /**
     * Returns the set of keys defined by the given class and its
     * superclasses.  If the given class and its superclasses
     * do not define any keys, then an empty set is returned.
     * 
     * @param c   the class whose keys to return
     * @return   the set of keys defined by that class
     */
    public static Map<String,Key> getKeys(Class c) {
        if (c == null) {
            return empty();
        }
        Map<String,Key> result = keys.get(c);
        if (result == null) {
            addKeys(c);
            result = keys.get(c);
            if (result == null) {
                return empty();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Key> empty() {
        return Collections.EMPTY_MAP;
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

}
