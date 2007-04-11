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
 * Identity.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import org.archive.util.Transformer;

public class Identity {

    
    final public static Transformer<Identity,Object> TO_OBJECT = 
        new Transformer<Identity,Object>() {
        
        public Object transform(Identity id) {
            return id.getObject();
        }
    };
    
    
    private Object object;
    //private int hashCode;
    
    public Identity(Object object) {
        this.object = object;
        //this.hashCode = System.identityHashCode(object);
    }
    
    
    public int hashCode() {
        return System.identityHashCode(object);
    }
    
    
    public boolean equals(Object o) {
        if (!(o instanceof Identity)) {
            return false;
        }
        Identity id = (Identity)o;
        return id.object == object;
    }
    
    
    public Object getObject() {
        return object;
    }
 

    public String toString() {
        return object.toString();
    }
    
    
}
