/* Copyright (C) 2007 Internet Archive.
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
 * UnspecifiedSheet.java
 * Created on January 17, 2007
 *
 * $Header$
 */
package org.archive.settings;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyTypes;
import org.archive.state.Path;

class UnspecifiedSheet extends Sheet {

    
    /**
     * First version.
     */
    private static final long serialVersionUID = 1L;
    
    
    private static class ModuleKey implements Serializable {

        final private static long serialVersionUID = 1L;
        
        private Object module;
        private Key<?> key;
        
        public ModuleKey(Object module, Key<?> key) {
            this.module = module;
            this.key = key;
        }
        
        public int hashCode() {
            return module.hashCode() ^ key.hashCode();
        }
        
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof ModuleKey)) {
                return false;
            }
            ModuleKey mk = (ModuleKey)other;
            return module == mk.module && key == mk.key;
        }
    }
    
    
    final private List<Sheet> thisList;
    private ConcurrentHashMap<ModuleKey,Object> defaults;
    
    
    public UnspecifiedSheet(SheetManager manager, String name) {
        super(manager, name);
        thisList = Collections.singletonList((Sheet)this);
        this.defaults = new ConcurrentHashMap<ModuleKey,Object>();
    }
    
    @Override
    UnspecifiedSheet duplicate() {
        return this;
    }

    @Override
    public <T> T check(Object module, Key<T> key) {
        return get(module, key);
    }

    @Override
    public <T> Stub checkStub(Stub module, Key<T> key) {
        validateModuleType(module, key);
        SingleSheet global = getSheetManager().getGlobalSheet();
        Object def = getDefault(global, module, key);
        return (Stub)def;
    }

    
    <T> Object getDefault(SingleSheet global, Object module, Key<T> k) {
        Class<T> type = k.getType();
        if (KeyTypes.isSimple(type)) {
            if (Path.class.isAssignableFrom(type)) {
                Path p = (Path)k.getDefaultValue();
                p = new Path(getSheetManager().getPathContext(), p.toString());
                return p;
            } else {
                return k.getDefaultValue();
            }
        }
        
        if (k.isAutoDetected()) {
            return global.findPrimary(type);
        }
        
        ModuleKey mk = new ModuleKey(module, k);
        Object result = defaults.get(mk);
        if (result != null) {
            if (result == SingleSheet.NULL.VALUE) {
                return null;
            } else {
                return result;
            }
        }
        
        if (type == List.class) {
            List<Object> list;
            if (KeyTypes.isSimple(k.getElementType())) {
                list = (List)k.getDefaultValue();
            } else {
                List<Class<?>> orig = k.getDefaultListElementImplementations();
                list = new ArrayList<Object>();
                for (Class<?> c: orig) {
                    list.add(create(c));
                }
            }
            result = new UnmodifiableTypedList(this, list, k.getElementType()); 
        } else if (type == Map.class) {
            Map<String,Object> map;
            if (KeyTypes.isSimple(k.getElementType())) {
                map = (Map)k.getDefaultValue();
            } else {
                Map<String,Class<?>> orig = k.getDefaultMapElementImplementations();
                map = new LinkedHashMap<String,Object>();
                for (Map.Entry<String,Class<?>> me: orig.entrySet()) {
                    map.put(me.getKey(), create(me.getValue()));
                }
            }
            result = new UnmodifiableTypedMap(this, map, k.getElementType());
        } else {
            result = create(k.getDefaultImplementation());
        }

        if (result == null) {
            result = SingleSheet.NULL.VALUE;
        }
        Object r = defaults.putIfAbsent(mk, result);
        if (r != null) {
            result = r;
        }
        if (result == SingleSheet.NULL.VALUE) {
            return null;
        }
        return result;
    }
    
    
    private Object create(Class<?> c) {
        if (c == null) {
            return null;
        }
        if (getSheetManager().isLive()) {
            try {
                Object o = c.newInstance();
                if (o instanceof Initializable) {
                    ((Initializable)o).initialTasks(this);
                }
                return o;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            return Stub.make(c);
        }
    }


    @Override
    public <T> T get(Object module, Key<T> k) {
        validateModuleType(module, k);
        SingleSheet global = getSheetManager().getGlobalSheet();
        Object value = getDefault(global, module, k);
        return k.getType().cast(value);
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> Resolved<T> resolve(Object module, Key<T> k) {
        validateModuleType(module, k);
        SingleSheet global = getSheetManager().getGlobalSheet();
        Object value = getDefault(global, module, k);
        return Resolved.make(module, k, value, thisList);
    }


}
