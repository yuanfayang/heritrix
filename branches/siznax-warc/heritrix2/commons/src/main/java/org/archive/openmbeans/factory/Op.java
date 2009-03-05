/* Op
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

import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;


/**
 * A mutable object that produces an immutable 
 * {@link OpenMBeanOperationInfoSupport}.
 * 
 * @author pjack
 */
public class Op {

    
    /** The name of the operation. */
    public String name;
    
    
    /** The description of the operation. */
    public String desc;
    
    
    /** The formal parameters of the operation. */
    public List<Param> sig = new ArrayList<Param>();
    
    
    /** The return type of the operation. */
    public OpenType ret;
    
    
    /** The impact of the operation. */
    public int impact;
    
        
    /**
     * Creates a new OpenMBeanOperationInfoSupport object populated with 
     * this Op's fields.
     * 
     * @return  the new OpenMBeanOperationInfoSupport
     * @throws  IllegalArgumentException  if this object's fields cannot create
     *    a valid OpenMBeanOperationInfoSupport (eg, if the name field is null)
     */
    public OpenMBeanOperationInfoSupport make() {
        int sz = sig.size();
        OpenMBeanParameterInfoSupport[] arr = new OpenMBeanParameterInfoSupport[sz];
        for (int i = 0; i < sz; i++) {
            arr[i] = sig.get(i).make();
        }
        return new OpenMBeanOperationInfoSupport(name, desc, arr, ret, impact);
    }

    
    public static OpenMBeanOperationInfoSupport[] array(List<Op> list) {
        OpenMBeanOperationInfoSupport[] arr 
         = new OpenMBeanOperationInfoSupport[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).make();
        }
        return arr;
    }
}
