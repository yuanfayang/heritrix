/* Attr
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

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;


/**
 * A mutable object that produces an immutable 
 * {@link OpenMBeanAttributeInfoSupport}.
 * 
 * @author pjack
 */
public class Attr {

    
    /** The attribute's name. */
    public String name;
    
    
    /** The attribute's description. */
    public String desc;
    
    
    /** True if the attribute can be read. */
    public boolean read;
    
    
    /** True if the attribute can be written. */
    public boolean write;
    
    
    /** 
     * True if the attribute is accessed via a method that starts with "is".
     * False if the attribute's accessor method starts with "get".
     */
    public boolean is;
    
    
    /** The type of the attribute. */
    public OpenType type;
    
    
    /** The default value for the attribute. */
    public Object def;
    
    
    /** The minimum value for the attribute. */
    public Comparable min;
    
    
    /** The maximum value for the attribute. */
    public Comparable max;
    
    
    /** The list of legal values for the attribute. */
    public List<Object> legal = new ArrayList<Object>();


    /** Constructor. */
    public Attr() {
    }
    
    
    /**
     * Creates a new OpenMBeanAttributeInfoSupport object populated with 
     * this Attr's fields.
     * 
     * @return  the new OpenMBeanAttributeInfoSupport
     * @throws  IllegalStateException  if this object's fields cannot create
     *    a valid OpenMBeanAttributeInfoSupport (eg, if the name field is null)
     */
    public OpenMBeanAttributeInfoSupport make() {
        try {
            return innerMakeAttr();
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    private OpenMBeanAttributeInfoSupport innerMakeAttr() 
    throws OpenDataException {
        OpenMBeanAttributeInfoSupport r;
        if (legal.isEmpty()) {
            if ((min != null) && (max != null)) {
                r = new OpenMBeanAttributeInfoSupport(name, desc, type, read, 
                        write, is, def, min, max);
            } else {
                r = new OpenMBeanAttributeInfoSupport(name, desc, type, read, 
                        write, is);
            }
        } else {
            Object[] arr = legal.toArray();
            r = new OpenMBeanAttributeInfoSupport(name, desc, type, read, 
                    write, is, def, arr);
        }
        return r;
    }


    /**
     * Creates an array of OpenMBeanAttributeInfoSupport objects out of a list
     * of Attr objects.
     * 
     * @param list   the list to convert
     * @return   the array of OpenMBeanAttributeInfoSupport objects
     */
    public static OpenMBeanAttributeInfoSupport[] array(List<Attr> list) {
        OpenMBeanAttributeInfoSupport[] arr 
         = new OpenMBeanAttributeInfoSupport[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).make();
        }
        return arr;
    }


}
