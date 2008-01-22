/* 
 * Copyright (C) 2007 Internet Archive.
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
 * ListModuleListener.java
 *
 * Created on Mar 1, 2007
 *
 * $Id:$
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

/**
 * @author pjack
 *
 */
public class ListModuleListener<T> implements ModuleListener, Serializable {

    final private static Object PRESENT = "";
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    
    final private Class<T> type;
    private transient Map<T,Object> objects = new WeakHashMap<T,Object>();

    

    public ListModuleListener(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null.");
        }
        this.type = type;
    }
    
    
    public void moduleChanged(Object old, Object newModule) {
        if (newModule == null) {
            // old is being removed
            return;
        }
        if (!type.isAssignableFrom(newModule.getClass())) {
            return;
        }
        
        T t = type.cast(newModule);
        
        synchronized (objects) {
            objects.put(t, PRESENT);
        }
    }


    public List<T> getList() {
        synchronized (objects) {
            return new ArrayList<T>(objects.keySet());
        }
    }

    
    public static <T> ListModuleListener<T> make(Class<T> type) {
        return new ListModuleListener<T>(type);
    }
    
    
    public static <T> List<T> get(SheetManager mgr, Class<T> type) {
        for (ModuleListener l: mgr.getModuleListeners()) {
            if (l instanceof ListModuleListener) {
                ListModuleListener lml = (ListModuleListener)l;
                if (lml.getType().equals(type)) {
                    @SuppressWarnings("unchecked")
                    List<T> result = lml.getList();
                    return result;
                }
            }
        }
        return Collections.emptyList();
    }
    
    public Class<T> getType() {
        return type;
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        Map<T,Object> temp = new HashMap<T,Object>(objects.size());
        synchronized (objects) {
            temp.putAll(objects);
        }
        out.writeObject(temp);
    }
    
    
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        @SuppressWarnings("unchecked")
        Map<T,Object> temp = (Map)in.readObject();
        this.objects = new WeakHashMap<T,Object>(temp);
    }

}
