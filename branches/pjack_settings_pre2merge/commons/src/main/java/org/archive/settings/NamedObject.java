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
 * NamedObject.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import java.util.List;


/**
 * An object with a name.  
 * 
 * @author pjack
 */
public class NamedObject {


    /** The name. */
    private String name;
    
    /** The object. */
    private Object object;


    /**
     * Constructor.
     * 
     * @param name    the name for the object
     * @param object  the object being named
     */
    public NamedObject(String name, Object object) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        if (object == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.object = object;
    }
    
    
    /**
     * Returns the name.
     * 
     * @return  the name
     */
    public String getName() {
        return name;
    }
    
    
    /**
     * Returns the object.
     * 
     * @return  the object
     */
    public Object getObject() {
        return object;
    }


    public static Object getByName(List<NamedObject> list, String name) {
        for (NamedObject no: list) {
            if (no.getName().equals(name)) {
                return no.getObject();
            }
        }
        return null;
    }
    
    
    public static int getIndex(List<NamedObject> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            NamedObject no = list.get(i);
            if (no.getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
