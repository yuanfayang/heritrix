/**
 * 
 */
package org.archive.settings;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author pjack
 *
 */
class ModuleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    Holder holder;
    boolean first;
    
    
}


class Holder implements Serializable {
    

    private static final long serialVersionUID = 1L;

    Object module;
    
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