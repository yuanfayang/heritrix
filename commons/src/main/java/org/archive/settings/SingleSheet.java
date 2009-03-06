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
 * SingleSheet.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.settings.path.PathChangeException;
import org.archive.state.Constraint;
import org.archive.state.Key;
import org.archive.state.KeyTypes;


/**
 * A single sheet of settings.
 * 
 * @author pjack
 */
public class SingleSheet extends Sheet {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    /**
     * Maps a module to the Key/Value settings for that module.  The map only
     * keeps weak references to the module; the existence of settings for a
     * module will not prevent that module from being garbage collected.
     * 
     * <p>It's assumed that modules don't override equals() and hashcode().
     * 
     * <p>This is a map of maps.  The outer Map goes from a module to a 
     * map of Key->values containing that module's settings.
     * 
     * <p>The values contained in the Map<Key<?>,Object> are a little strange.
     * If a particular Key maps to a simple value, then the Map entry will
     * contain that Key and its value, which is straightforward.
     * 
     * <p>However, if a Key maps to a module type, then the Key Map entry 
     * will contain that Key and a {@link ModuleInfo} will will eventually 
     * resolve to the actual module value for the setting.  See 
     * {@link ModuleInfo} for the rationale behind this bizarre behavior.
     */
    private transient WeakHashMap<Object,Map<Key<?>,Object>> settings;

    
    /**
     * Maps module to a holder for that module.
     */
    transient WeakHashMap<Object,Holder> holders;
    
    /**
     * True if we're the global sheet.
     */
    final private boolean global;

    
    private transient WeakHashMap<Object,String> primaries;
    
    
    private List<KeyChangeEvent> pending;

    /**
     * Constructor.
     * 
     * @param manager   the manager who created this sheet
     */
    SingleSheet(SheetManager manager, String parentName, String name, boolean global) {
        super(manager, parentName, name);
        this.global = global;
        this.settings = new WeakHashMap<Object,Map<Key<?>,Object>>();
        this.primaries = new WeakHashMap<Object,String>();
        this.holders = new WeakHashMap<Object,Holder>();
    }

    
    public boolean isGlobal() {
        return global;
    }

    @Override
    SingleSheet duplicate() {
        SingleSheet result = new SingleSheet(
                getSheetManager(), parentName, getName(), global);
        result.primaries.putAll(this.primaries);
        Duplicator d = new Duplicator(this, result);
        
        for (Map.Entry<Object,Holder> me: holders.entrySet()) {
            Holder n = new Holder();
            n.module = me.getValue().module;
            result.holders.put(me.getKey(), n);
        }
        
        for (Map.Entry<Object,Map<Key<?>,Object>> setting: settings.entrySet()) {
            Object module = setting.getKey();
            Map<Key<?>,Object> settings = setting.getValue();
            if ((module != null) && (settings != null)) {
                // Copy the settings.
                Map<Key<?>,Object> settingsClone = 
                    new HashMap<Key<?>,Object>(settings);
                for (Map.Entry<Key<?>,Object> me: settings.entrySet()) {
                    Key<?> key = me.getKey();
                    Object value = me.getValue();
                    value = d.duplicate(me.getValue());
                    if (value != null) {
                        settingsClone.put(key, value);
                    }
                }
                result.settings.put(module, settingsClone);
            }
        }

        return result;
    }


    @Override
    public boolean contains(Object module, Key<?> key) {
        Map<Key<?>,Object> keys = settings.get(module);
        if (keys == null) {
            return false;
        }
        return keys.containsKey(key);
    }
    
    
    private Object fetch(Object module, Key<?> key) {
        Map<Key<?>,Object> keys = settings.get(module);
        if (keys == null) {
            return null;
        }
        return keys.get(key);
    }
    
    
    @Override
    Object check(Object module, Key<?> key) {
        Object r = fetch(module, key);
        if (r instanceof ModuleInfo) {
            r = ((ModuleInfo)r).holder.module;
        }
        return r;
    }


    @Override
    public <T> Resolved<T> resolve(Object module, Key<T> key) {
        // check preconditions
        if (module == null) {
            throw new IllegalArgumentException("Requested key on null module.");
        }
        validateModuleType(module, key);
        
        // Map/List elements may need to be merged with parent sheet(s)'
        if (Map.class.isAssignableFrom(key.getType())) {
            return resolveMap(module, key);
        }
        if (List.class.isAssignableFrom(key.getType())) {
            return resolveList(module, key);
        }

        // If this sheet doesn't override the key, defer to the parent.
        if (!contains(module, key)) {
            return getParent().resolve(module, key);
        }
        
        // This sheet overrides the key.  Get the overridden value...
        Object result = fetch(module, key);

        // Module values are actually stored as a ModuleInfo, from which the 
        // actual module instance can be retrieved.  In stub mode, the instance
        // will be a Stub<T> instead of a T.
        if (result instanceof ModuleInfo) {
            ModuleInfo minfo = (ModuleInfo)result;
            if (isLive(key.getType())) {
                T typesafe = key.getType().cast(minfo.holder.module);
                return Resolved.makeLive(module, key, typesafe, this);
            } else {
                @SuppressWarnings("unchecked")
                Stub<?> stub = (Stub)minfo.holder.module;
                return Resolved.makeStub(module, key, stub, this);
            }
        } else {
            // It was a just simple value (eg, a String).
            T typesafe = key.getType().cast(result);
            return Resolved.makeLive(module, key, typesafe, this);
        }
    }
    
    
    private <T> Resolved<T> resolveMap(Object module, Key<T> key) {
        // Merge all values into one glorious map.
        List<Sheet> sheets = new ArrayList<Sheet>();
        List<TypedMap<Object>> maps = new ArrayList<TypedMap<Object>>();        
        for (Sheet sh = this; !(sh instanceof UnspecifiedSheet); sh = sh.getParent()) {
            if (sh.contains(module, key)) {
                sheets.add(sh);
                Object o = sh.check(module, key);
                TypedMap<Object> map = (TypedMap)o;
                maps.add(map);
            }
        }
        
        // If no sheets actually overrode the map, then just use the default.
        if (sheets.isEmpty()) {
            return getSheetManager().getUnspecifiedSheet().resolve(module, key);
        }
        
        TypedMap<Object> result = new MultiTypedMap<Object>(maps, null);
        return Resolved.makeMap(module, key, result, sheets);
    }


    private <T> Resolved<T> resolveList(Object module, Key<T> key) {
        // Merge all values into one glorious map.
        List<Sheet> sheets = new ArrayList<Sheet>();
        List<TypedList<Object>> lists = new ArrayList<TypedList<Object>>();        
        for (Sheet sh = this; !(sh instanceof UnspecifiedSheet); sh = sh.getParent()) {
            if (sh.contains(module, key)) {
                sheets.add(sh);
                Object o = sh.check(module, key);
                TypedList<Object> list = (TypedList)o;
                lists.add(0, list);
            }
        }

        // If no sheets actually overrode the map, then just use the default.
        if (sheets.isEmpty()) {
            return getSheetManager().getUnspecifiedSheet().resolve(module, key);
        }

        TypedList<Object> result = new MultiTypedList<Object>(lists, null);
        return Resolved.makeList(module, key, result, sheets);
    }
    
    
    /**
     * Removes a value from this sheet.
     * 
     * @param module   the module whose setting to remove
     * @param key      the setting to remove
     */
    public void remove(Object module, Key<?> key) {
        if (module == null) {
            throw new IllegalArgumentException(
                    "Attempt to remove key value on null module.");
        }

        Map<Key<?>,Object> map = settings.get(module);
        if (map == null) {
            return;
        }
        
        map.remove(key);
    }

    /**
     * Changes the value for a setting.
     * 
     * <p>The rules governing this can be quite complex, depending on the the
     * type of the setting (the result of {@link Key#getType()}).
     * 
     * <p>If the Key has a simple type, then the value of the setting 
     * simply changes to the given value.  If the given module implements 
     * {@link KeyChangeListener}, then it will be notified of the changed value.
     * 
     * <p>If the Key has a complex type, then the behavior of this method 
     * depends on three factors.  To describe those factors, let's assume that:
     * 
     * <ul>
     * <li><i>M</i> is the given module.</li>
     * <li><i>K</i> is the given Key.</li>
     * <li><i>P</i> is that module's previous value, if any.</li>
     * <li><i>N</i> is the given new value for the module.</li>
     * </ul>
     * 
     * Given the above, then the three factors are:
     * 
     * <ol>
     * <li>Does <i>M</i> already have a value for <i>K</i> in this sheet?  Put
     * another way, does <i>P</i> exist?</li>
     * <li>If so, does <i>K</i> represent the first time that <i>P</i> was 
     * ever used in this configuration?</li>
     * </ol>
     * 
     * If the answer to either #1 or #2 is no then this 
     * method will behave the same as for simple types: The new setting is added
     * to the sheet, and if <i>M</i> is a {@link KeyChangeListener} then it 
     * will be notified of the change.  Also, all of the {@link ModuleListeners} 
     * defined in the {@link SheetManager} are notified that <i>N</i> has been
     * used in the configuration (perhaps again).
     * 
     * <p>If the answers to both #1 and #2 are yes, then <i>all</i> settings
     * that had a value of <i>P</i> are changed to have a value of <i>N</i>.
     * This is what end users usually expect: If you change the first
     * definition of the ServerCache, then you expect everything that refers
     * to the ServerCache to use the new value; you don't expect to have to 
     * go through every sheet and manually change the value yourself. 
     * 
     * @param <T>         the type of the property to set
     * @param module      the module to set the property on
     * @param key         the property to set
     * @param value       the new value for that property, or null to remove
     *     the property from this sheet
     */
    public <T> void set(Object module, Key<T> key, T value) {
        if (module == null) {
            throw new IllegalArgumentException(
                    "Attempt to set key value on null module.");
        }
        
        Class vtype = (value == null) ? null: value.getClass();
        validateTypes(module, key, vtype);
        T v = key.getType().cast(value);
        for (Constraint<T> c: key.getConstraints()) {
            if (!c.allowed(v)) {
                // if not live, complete change to invalid value anyway
                if(!getSheetManager().isLive()) {
                    set2(module, key, value);
                }
                throw new PathChangeException(
                    "value '" + v + "' disallowed for '" 
                    + key.getFieldName() +"' by constraint: "
                    + c);
            }
        }
        
        if (v instanceof String) {
            String s = (String)v;
            if ((s.indexOf('\n') >= 0) || (s.indexOf('\r') >= 0)) {
                throw new IllegalArgumentException(
                        "String values must not contain new lines.");
            }
        }
        
        // Module and value are of the correct type, and value is valid.
        set2(module, key, value);
    }


    
    public <T> void setStub(Stub module, Key<T> key, Object value) {
        Class vtype;
        if (value == null) {
            vtype = null;
        } else if (value instanceof Stub) {
            vtype = ((Stub)value).getType();
        } else {
            vtype = value.getClass();
        }
        validateTypes(module, key, vtype);
        set2(module, key, value);
    }

    
    /**
     * Performs the actual mutation after preconditions are enforced.
     * When this method is called, it is known that the key belongs to the
     * module, and that the value is appropriate for the key.
     * 
     * @param <T>
     * @param module   the module whose setting to change
     * @param key      the setting's key
     * @param value    the new value for that setting
     */
    private <T> void set2(Object module, Key<T> key, Object value) {
        Map<Key<?>,Object> map = settings.get(module);
        if (map == null) {
            map = new HashMap<Key<?>,Object>();
            settings.put(module, map);
        }
        value = transform(value);
        Object old;
        if (isModuleType(Stub.getType(value))) {
            old = setModuleValue(map, key, value);
            getSheetManager().fireModuleChanged(old, value);
        } else {
            old = map.put(key, value);
        }
        if ((module instanceof KeyChangeListener) && (pending != null)) {
            if (!eq(old, value)) {
                KeyChangeEvent event = new KeyChangeEvent(this, 
                        module, key, old, value);
                pending.add(event);
            }
        }
    }


    private Object setModuleValue(
            Map<Key<?>,Object> map, 
            Key<?> key, 
            Object value) {
        return setModuleValue(new MapContainer(map), key, value);
    }
        
    Object setModuleValue(
            Container map,
            Object key,
            Object value
        ) {    
        ModuleInfo minfo = (ModuleInfo)map.get(key);
        Holder holder = holders.get(value);
        if (holder == null) {
            // The new value was never used before; we have to mark it as "first"
            Object old = (minfo == null) ? null : minfo.holder.module;
            if (old == value) {
                // Don't change anything.
                return old;
            }
            // We're replacing the first reference to the module, which actually
            // means we want to replace ALL of them.
            if (minfo != null && minfo.first) {
                minfo.holder.module = value;
                holders.put(value, minfo.holder);
                primaries.remove(old);
            } else {
                ModuleInfo newInfo = new ModuleInfo();
                holder = new Holder();
                holder.module = value;
                holders.put(value, holder);
                newInfo.holder = holder;
                newInfo.first = true;
                map.put(key, newInfo);
            }
            return old;
        }
        
        if (minfo == null) {
            // Value was used before, so don't mark it as first
            minfo = new ModuleInfo();
            minfo.holder = holder;
            map.put(key, minfo);
            return null;
        } 
        
        // Value was used before, and setting was previously set.
        
        if (minfo.first) {
            Object old = holder.module;
            if (old == value) {
                // Don't change anything.
                return old;
            }
            holder.module = value;
            holders.remove(old);
            holders.put(value, holder);
            // Above has the effect of changing all settings that refer to this
            // setting as well.
            primaries.remove(old);
            return old;
        }

        Object old = minfo.holder.module;
        minfo.holder = holder;
        return old;
    }

    
    private boolean eq(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null) {
            return false;
        }
        if (o2 == null) {
            return false;
        }
        if (KeyTypes.isSimple(Stub.getType(o1))) {
            return o1.equals(o2);
        } else {
            return false;
        }
    }

    private static <T> void validateTypes(Object module, Key<T> key, Class vtype) {
        validateModuleType(module, key);
        if ((vtype != null) && !key.getType().isAssignableFrom(vtype)) {
            throw new IllegalArgumentException("Illegal value type for " +
                    key.getFieldName() + ". Expected " + 
                    key.getType().getName() + " but got " + 
                    vtype.getName());
        }
    }


    @SuppressWarnings("unchecked")
    private Object transform(Object o) {
        if ((o instanceof Map) && (!(o instanceof SettingsMap))) {
            throw new IllegalArgumentException("Maps must be TypedMap.");
        }
        if ((o instanceof List) && (!(o instanceof SettingsList))) {
            throw new IllegalArgumentException("Lists must be TypedList.");
        }
        return o;
    }

    
    public <T> Map<String,T> resolveEditableMap(Object o, Key<Map<String,T>> k) {
        Map result = (Map)check(o, k);
        return result;
    }

    
    public List resolveEditableList(Object o, Key<List> k) {
        List result = (List)check(o, k);
        return result;
    }

    
    @Override
    SingleSheet getGlobalSheet() {
        if (this.global) {
            return this;
        }
        return getSheetManager().getGlobalSheet();
    }


    List<KeyChangeEvent> clearKeyChangeEvents() {
        List<KeyChangeEvent> result = pending;
        pending = null;
        return result;
    }

    
    @Override
    void setClone(boolean clone) {
        super.setClone(clone);
        if (clone) {
            this.pending = new ArrayList<KeyChangeEvent>();
        }
    }

    
    public void addPrimary(Object primary) {
        primaries.put(primary, "");
    }


    public void removePrimary(Object primary) {
        primaries.remove(primary);
    }


    public boolean isPrimary(Object primary) {
        return primaries.containsKey(primary);
    }


    public Object findPrimary(Class<?> type) {
        Object result = null;
        for (Object o: primaries.keySet()) {
            Class<?> otype = Stub.getType(o);
            if (type.isAssignableFrom(otype)) {
                if (result != null) {
                    throw new IllegalStateException(
                            "More than one primary found for " 
                            + type.getName());
                }
                result = o;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream input) 
    throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        Map map = (Map)input.readObject();
        this.primaries = new WeakHashMap(map);
        map = (Map)input.readObject();
        this.holders = new WeakHashMap(map);
        map = (Map)input.readObject();
        this.settings = new WeakHashMap(map);
    }
    
    
    private void writeObject(ObjectOutputStream output) 
    throws IOException {
        output.defaultWriteObject();
        Map<Object,String> map = new HashMap<Object,String>(primaries);
        output.writeObject(map);
        output.writeObject(new HashMap<Object,Holder>(holders));
        output.writeObject(new HashMap<Object,Map<Key<?>,Object>>(settings));
    }

    static boolean isModuleType(Class<?> c) {
        if (KeyTypes.isSimple(c)) {
            return false;
        }
        if (List.class.isAssignableFrom(c)) {
            return false;
        }
        if (Map.class.isAssignableFrom(c)) {
            return false;
        }
        return true;
    }
}
