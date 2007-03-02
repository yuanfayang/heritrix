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

import java.io.Serializable;
import java.util.ArrayList;
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

    
    private Class<T> type;
    private Map<T,Object> objects = new WeakHashMap<T,Object>();

    

    public ListModuleListener(Class<T> type) {
        this.type = type;
    }
    
    
    public void moduleChanged(Object old, Object newModule) {
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
    
    
    public Class<T> getType() {
        return type;
    }
}
