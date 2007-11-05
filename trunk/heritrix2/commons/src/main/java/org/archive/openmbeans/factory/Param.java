/* Param
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
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;


/**
 * A mutable object that produces an immutable 
 * {@link OpenMBeanParameterInfoSupport}.
 * 
 * @author pjack
 */
public class Param {

    
    /** The name of the parameter. */
    public String name;
    
    
    /** The description of the parameter. */
    public String desc;
    
    
    /** The type of the parameter. */
    public OpenType type;
    
    
    /** The default value of the parameter. */
    public Object def;
    
    
    /** The minimum value of the parameter. */
    public Comparable min;
    
    
    /** The maximum value of the parameter. */
    public Comparable max;
    
    
    /** The list of allowed values for the parameter. */
    public List<Object> legal = new ArrayList<Object>();


    /**
     * Creates a new OpenMBeanParameterInfoSupport object populated with 
     * this Param's fields.
     * 
     * @return  the new OpenMBeanParameterInfoSupport
     * @throws  IllegalArgumentException  if this object's fields cannot create
     *    a valid OpenMBeanParameterInfoSupport (eg, if the name field is null)
     */
    public OpenMBeanParameterInfoSupport make() {
        try {
            return innerMakeParam();
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    private OpenMBeanParameterInfoSupport innerMakeParam() 
    throws OpenDataException {
        OpenMBeanParameterInfoSupport r;
        if (legal.isEmpty()) {
            r = new OpenMBeanParameterInfoSupport(name, desc, type, def, min, max);
        } else {
            Object[] arr = legal.toArray();
            r = new OpenMBeanParameterInfoSupport(name, desc, type, def, arr);
        }
        return r;
    }

    
    public static OpenMBeanParameterInfoSupport[] array(List<Param> list) {
        OpenMBeanParameterInfoSupport[] arr 
         = new OpenMBeanParameterInfoSupport[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).make();
        }
        return arr;
    }

}
