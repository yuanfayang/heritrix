/* Cons
 *
 * Created on October 18, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
 */
package org.archive.openmbeans.factory;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;


/**
 * A mutable object that produces an immutable 
 * {@link OpenMBeanConstructorInfoSupport}.
 * 
 * @author pjack
 */
public class Cons {

    
    /** The name of the constructor. */
    public String name;
    
    
    /** The description of the constructor. */
    public String desc;
    
    
    /** The type names of the constructor's parameters. */
    public List<Param> sig = new ArrayList<Param>();
    

    /** Constructor. */
    public Cons() {
    }
    
    
    /**
     * Creates a new OpenMBeanConstructorInfoSupport object populated with 
     * this Cons's fields.
     * 
     * @return  the new OpenMBeanConstructorInfoSupport
     * @throws  IllegalArgumentException  if this object's fields cannot create
     *    a valid OpenMBeanConstructorInfoSupport (eg, if the name field is null)
     */
    public OpenMBeanConstructorInfoSupport make() {
        int sz = sig.size();
        OpenMBeanParameterInfoSupport[] arr = new OpenMBeanParameterInfoSupport[sz];
        for (int i = 0; i < sz; i++) {
            arr[i] = sig.get(i).make();
        }
        return new OpenMBeanConstructorInfoSupport(name, desc, arr);
    }


    /**
     * Converts a list of Cons objects into an array of 
     * OpenMBeanConstructorInfoSupport objects.
     * 
     * @param list   the list of Cons objects
     * @return   the array of OpenMBeanConstructorInfoSupport objects
     */
    public static OpenMBeanConstructorInfoSupport[] array(List<Cons> list) {
        OpenMBeanConstructorInfoSupport[] arr 
         = new OpenMBeanConstructorInfoSupport[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).make();
        }
        return arr;
    }

}
