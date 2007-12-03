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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.archive.i18n.LocaleCache;


/**
 * The key to a configurable module setting.  Modules should declare keys
 * as public, static, final fields.  Those keys should be registered in the
 * class's static initializer.  For instance:
 * 
 * <pre>
 * public static class FileModule implements Module ... {
 * 
 *     &amp;Immutable
 *     final public static Key&lt;FileModule&gt; PARENT = 
 *         Key.make(FileModule.class, null);
 *     
 *     &amp;Immutable
 *     final public static Key&lt;String&gt; PATH = 
 *         Key.make("");
 *         
 *         
 *     static {
 *         KeyManager.addKeys(FileModule.class);
 *     }
 * 
 * }
 * </pre>
 * 
 * <p>All classes that define static Key fields <i>must</i> register those keys 
 * in a static initializer, even abstract classes.
 * 
 * <p>Keys define the type of the setting, as well as a default value for the
 * setting.  The KeyManager also determines the Key's name, the Key's 
 * description and the class that declared the static Key field.
 * 
 * <p>Keys may have three flavors of types:
 * 
 * <ol>
 * <li>Simple types.  The Java primitive types and java.lang.String are examples
 * of simple Key types.  See {@link KeyTypes} for the complete list of simple
 * types.</li>
 * <li>Container types.  There are only two container types understood by 
 * the KeyManager (and by {@link org.archive.settings}).  Those two types are
 * {@link java.util.List} and {@link java.util.Map}.</li>
 * <li>Module types.  Any type that is not a simple type or a container type
 * is considered to be a module type.  See {@link Module} for how to define
 * an appropriate module type.</li>
 * </ol>
 * 
 * <p>The three flavors behave somewhat differently depending on the state of
 * an application.  For instance, in the {@link org.archive.settings} system,
 * an application can either be online or offline.  When an application is 
 * offline, the settings system provides {@link Stub} proxy values for 
 * modules types, instead of actually constructing classes.  However, simple
 * types and container types are not replaced with Offline proxies.
 * 
 * <p>A static Key field can be annotated with additional metadata.  See
 * {@link Global}, {@link Immutable} and {@link Expert} for more information.
 * 
 * <p>Keys may have {@link Constraint} instances applied to them.  
 * Implementations of {@link StateProvider#get} must guarantee that any value
 * returned for a particular key is allowed by that key's constraints.
 * 
 * <p>Note this class is immutable.  You can use the {@link #make(Object)}
 * family of methods to quickly construct unconstrained, basic keys.  More
 * elaborate Keys can be constructed using the {@link KeyMaker} class.
 * 
 * @author pjack
 *
 * @param <Value>  the type of the setting's value
 */
final public class Key<Value> implements Serializable {

    /** For serialization. */
    private static final long serialVersionUID = 1L;

    /** The name of the field.  Set by the KeyManager. */
    private String fieldName;
    
    /** The class who declares the field.  Set by the KeyManager. */
    private Class<?> owner;

    /** The type of the field. */
    transient final private Class<Value> type;

    /**
     * The element type of a List or Map.  Will be null if this.type is 
     * anything other than java.util.List or java.util.Map.
     */
    private Class<?> elementType;
        
    /** 
     * The default value of the field.  If {@link type} is a simple type, or
     * if it's list or map and {@link elementType} is a simple type, then this
     * field contains the actual object that is the default value.
     * 
     * <p>If {@link type} is a module type, then this field contains the 
     * Class that describes the default implementation for the module.
     * 
     * <p>If {@link type} is list or map, and {@link elementType} is a 
     * module type, then this field contains a list/map of Class objects that
     * describe the element implementations.
     */
    transient final private Object def;
    
    /** The constraints that determine valid values for the field. */
    transient private Set<Constraint<Value>> constraints;

    // Note that all flags are false by default, and set to true in the
    // presence of a corresponding annotation.
    
    /** True if the property is considered "expert". */
    transient private boolean expert;

    /** True if the property can be overridden. */
    transient private boolean global;
    
    /** True if the property can be mutated. */
    transient private boolean immutable;
    
    
    /**
     * True if the settings framework should attempt to autodetect the right 
     * value for this setting.  Only if the autodetect attempt fails will the
     * value specified by {@link def} be used.
     */
    final private boolean autoDetect;

    
    private boolean initialized;
    
    
    private String guessedOwner;
    
    
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
        this.guessedOwner = guessDeclaringClass();
        this.type = maker.type;
        this.elementType = maker.elementType;
        this.def = maker.def;
        this.autoDetect = maker.autoDetect;
        Set<Constraint<Value>> s = new HashSet<Constraint<Value>>(maker.constraints);
        this.constraints = Collections.unmodifiableSet(s);
        maker.reset();
    }

    
    private void checkInit() {
        if (initialized) {
            return;
        }
        
        if (guessedOwner == null) {
            throw new UnregisteredKeyException("Unregistered key");
        }
        
        throw new UnregisteredKeyException("Unregistered key in class " + 
                guessedOwner + "; you must invoke KeyManager.addKeys(" + 
                guessedOwner + ".class); in its static initializer.");
    }

    /**
     * Invoked by the KeyManager when it registers a new key.
     * 
     * @param owner  the class that declared this key
     * @param field   the Key's field
     */
    void setMetadata(Class<?> owner, Field field) {
        this.initialized = true;
        this.fieldName = field.getName().toLowerCase().replace('_', '-');
        this.owner = owner;
        if (field.getAnnotation(Nullable.class) == null) {
            addNonNullConstraint();
        } else {
            if (KeyTypes.isSimple(type)) {
                throw new IllegalStateException(owner.getName() + "." +
                        fieldName + " uses @Nullable but has a simple type.");
            } else if (type == List.class) {
                throw new IllegalStateException(owner.getName() + "." +
                        fieldName + " uses @Nullable but has a list type.");
            } else if (type == Map.class) {
                throw new IllegalStateException(owner.getName() + "." +
                        fieldName + " uses @Nullable but has a map type.");                
            }
        }
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


    @SuppressWarnings("unchecked")
    private void addNonNullConstraint() {
        Set s = new HashSet(constraints);
        s.add(NonNullConstraint.INSTANCE);
        this.constraints = Collections.unmodifiableSet(s);        
    }
    

    /**
     * The element type of a List or Map.  Will be null if {@link #getType()}
     * is anything other than java.util.List or java.util.Map.
     * 
     * @return  the element type of a List or Map key, or null if 
     */
    public Class<?> getElementType() {
        checkInit();
        return elementType;
    }
    

    /**
     * Returns true if the property is considered "expert".  User interfaces
     * may decide to hide expert properties from end users.
     * 
     * @return  true if this property is expert.
     */
    public boolean isExpert() {
        checkInit();
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
        checkInit();
        return global;
    }
    

    /**
     * Returns true if the setting is immutable.  Immutable settings cannot
     * be changed in a running application, and may be safely cached in 
     * instance fields.  
     * 
     * @return  true if the setting is immutable, or false if the setting can
     *            be changed in a running application
     * @see Initializable 
     */
    public boolean isImmutable() {
        checkInit();
        return immutable;
    }


    public boolean isAutoDetected() {
        checkInit();
        return autoDetect;
    }
    
    
    /**
     * Returns the name of the Java field that declared this key.
     * 
     * @return  the field name of this key
     */
    public String getFieldName() {
        checkInit();
        return fieldName;
    }


    /**
     * Returns the name of this key in the given locale.
     * 
     * @param locale   the locale
     * @return  the name of this key in that locale
     */
    public String getName(Locale locale) {
        checkInit();
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
        checkInit();
        return LocaleCache.load(owner, locale).get(fieldName + "-description");
    }


    /**
     * Returns the class who declared this key.
     * 
     * @return  the class who declared this key
     */
    public Class<?> getOwner() {
        checkInit();
        return owner;
    }


    /**
     * Returns the type of this key's values.
     * 
     * @return  the type of this key's values
     */
    public Class<Value> getType() {
        checkInit();
        return type;
    }


    /**
     * Returns the constraints that determine valid values for this key.
     * 
     * @return  the constraints for this key
     */
    public Set<Constraint<Value>> getConstraints() {
        checkInit();
        return constraints;
    }


    /**
     * Returns the default value if this Key has a simple type.  Or, if this
     * Key has a Map/List type with a simple element type.  If this Key has
     * a module type, or a module element type, then this method returns null.
     * 
     * @return  the default value for this key
     */
    public Value getDefaultValue() {
        checkInit();
        if (KeyTypes.isSimple(type)) {
            return type.cast(def);
        }
        if (elementType == null) {
            return null;
        }
        if (KeyTypes.isSimple(elementType)) {
            return type.cast(def);
        }
        return null;
    }
    

    /**
     * Returns the default implementation class for a Key with a module type.
     * Or, null if this Key has a simple type, or a Map/List type.
     * 
     * @return  the default implementation class
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Value> getDefaultImplementation() {
        if (KeyTypes.isSimple(type)) {
            return null;
        }
        if (type == Map.class) {
            return null;
        }
        if (type == List.class) {
            return null;
        }
        return (Class)def;
    }
    
    
    @SuppressWarnings("unchecked")
    public List<Class<?>> getDefaultListElementImplementations() {
        if (type != List.class) {
            return null;
        }
        return (List)def;
    }
    
    
    @SuppressWarnings("unchecked")
    public Map<String,Class<?>> getDefaultMapElementImplementations() {
        if (type != Map.class) {
            return null;
        }
        return (Map)def;
    }


    @Override
    public String toString() {
        return fieldName;
    }



    /**
     * Creates a basic string Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<String> make(String def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic boolean Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Boolean> make(boolean def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic byte Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Byte> make(byte def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic character Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Character> make(char def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic double Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Double> make(double def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic float Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Float> make(float def) {
        return KeyMaker.make(def).toKey();
    }

    
    
    /**
     * Creates a basic int Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Integer> make(int def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic long Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Long> make(long def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic short Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Short> make(short def) {
        return KeyMaker.make(def).toKey();
    }


    /**
     * Creates a basic path Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Path> make(Path def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic BigInteger Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<BigInteger> make(BigInteger def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic BigDecimal Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<BigDecimal> make(BigDecimal def) {
        return KeyMaker.make(def).toKey();
    }

    
    /**
     * Creates a basic Pattern Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static Key<Pattern> make(Pattern def) {
        return KeyMaker.make(def).toKey();
    }
    
    
    /**
     * Creates a basic Enum Key.  The returned Key has the given default value,
     * is non-expert, can be overridden, and has no constraints.
     * 
     * @param def   the default value for the key
     * @return   the new Key
     */
    public static <T extends Enum> Key<T> make(T def) {
        return KeyMaker.make(def).toKey();
    }


    /**
     * Creates a basic Key with the specified type and default implementation
     * class.
     * 
     * @param <X>   the type of the key
     * @param type  the type of the key
     * @param def   the default value of the key
     * @return   the new Key
     */
    public static <X> Key<X> make(Class<X> type, Class<? extends X> def) {
        KeyMaker<X> result = KeyMaker.make(type, def);
        return result.toKey();
    }

    
    public static <X> Key<X> makeAuto(Class<X> type) {
        KeyMaker<X> result = KeyMaker.makeAuto(type);
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
    
    
    public static <X> Key<List<X>> makeSimpleList(Class<X> element, X... def) {
        return KeyMaker.makeSimpleList(element, def).toKey();
    }
    
    
    public static <X> Key<List<X>> makeModuleList(Class<X> element, Class<? extends X>... def) {
        return KeyMaker.makeModuleList(element, def).toKey();
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


    /**
     * Returns a Key with a <code>null</code> default value.  
     * 
     * @param <X>  the type of the key to create
     * @param cls  the type of the key to create
     * @return   the new Key
     */
    public static <X> Key<X> makeNull(Class<X> cls) {
        return KeyMaker.makeNull(cls).toKey();
    }


    /**
     * Custom serialization to preserve Key field singleton status.
     * 
     * @return  the Key with this Key's owner and fieldName that was 
     *    actually registered with the KeyManager
     */
    private Object readResolve() {
        return KeyManager.getKeys(owner).get(fieldName);
    }


    /**
     * Casts this Key to a key with the specified type.
     * 
     * @param <T>   the type to cast to
     * @param cls   the type to cast to
     * @return   this key, cast to that type
     * @throws   ClassCastException  if cls is not actually the type of this Key
     */
    @SuppressWarnings("unchecked")
    public <T> Key<T> cast(Class<T> cls) {
        if (cls != type) {
            throw new ClassCastException("Can't cast Key<" 
                    + type.getClass().getName() 
                    + "> to Key<" + cls.getClass().getName() + ">");
        }
        Key r = this;
        return r;
    }


    /**
     * Guess the name of the class that's creating a key.  This method assumes
     * that the Key was declared as a final public static field.  If the key
     * wasn't declared as such, or if the current JVM doesn't supply detailed
     * stack traces, then this method may not report the correct class name.
     * The intent of this method is to provide a useful error message for a
     * common mistake: A developer declared Key fields correctly, but forgot
     * to invoke KeyManager.addKeys in a class's static initializer.
     * 
     * This method tries to return null if the class name can't be guessed,
     * but again, depending on circumstances it might report something 
     * incorrect.
     * 
     * @return  the guessed declaring class name
     */
    private static String guessDeclaringClass() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element: stack) {
                if (element.getMethodName().equals("<clinit>")) {
                    return element.getClassName();
                }
//                String cname = element.getClassName();
//                if (!cname.equals(Key.class.getName()) 
//                        && !cname.equals(KeyMaker.class.getName())) {
//                    return cname;
//                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
