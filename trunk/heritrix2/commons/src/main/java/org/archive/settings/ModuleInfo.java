/**
 * 
 */
package org.archive.settings;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Info about a module contained in a sheet.
 * 
 * <p>Contains a shared reference to the module (represented by a 
 * {@link Holder} instance) and a flag indicating whether or not this 
 * ModuleInfo represents the first occurrence of the module in the 
 * configuration.
 * 
 * @author pjack
 *
 */
class ModuleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    Holder holder;
    boolean first;
    
    public String toString() {
        if (holder == null) {
            return "ModuleInfo{no holder," + first + "}";
        }
        if (holder.module == null) {
            return "ModuleInfo{null," + first + "}";
        }
        return "ModuleInfo{" + holder.module + "," + first + "}";
    }
    
}


class Holder implements Serializable {
    

    private static final long serialVersionUID = 1L;

    Object module;
    
    public String toString() {
        if (module == null) {
            return "Holder{null}";
        }
        return "Holder{" + module.toString() + "}";
    }
}


interface Container extends Serializable {
    
    Object get(Object key);
    Object put(Object key, Object value);
    
}


@SuppressWarnings("unchecked")
class MapContainer implements Container {

    private static final long serialVersionUID = 1L;
    
    private Map map;
    
    public MapContainer(Map map) {
        this.map = map;
    }

    public Object get(Object key) {
        return map.get(key);
    }
    
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }
    
}


@SuppressWarnings("unchecked")
class ListContainer implements Container {

    private static final long serialVersionUID = 1L;
    
    private List list;
    
    public ListContainer(List list) {
        this.list = list;
    }
    
    public Object get(Object key) {
        Integer i = (Integer)key;
        if (i == list.size()) {
            return null;
        }
        return list.get(i);
    }
    
    public Object put(Object key, Object value) {
        Integer i = (Integer)key;
        if (i == list.size()) {
            list.add(value);
            return null;
        } else {
            return list.set(i, value);
        }
    }

}