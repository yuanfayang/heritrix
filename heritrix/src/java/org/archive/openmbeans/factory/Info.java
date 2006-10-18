/* Info
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

import javax.management.openmbean.OpenMBeanInfoSupport;


/**
 * A mutable object that produces an immutable 
 * {@link OpenMBeanInfoSupport}.
 * 
 * @author pjack
 */
public class Info {

    
    /** The name of the class. */
    public String name;
    
    
    /** The description of the class. */
    public String desc;
    
    
    /** The attributes defined by the class. */
    public List<Attr> attrs = new ArrayList<Attr>();
    
    
    /** The operations defined by the class. */
    public List<Op> ops = new ArrayList<Op>();
    
    
    /** The constructors defined by the class. */
    public List<Cons> cons = new ArrayList<Cons>();
    
    
    /** The notifications defined by the class. */
    public List<Notif> notifs = new ArrayList<Notif>();
    
    
    /**
     * Creates a new OpenMBeanInfoSupport object populated with 
     * this Info's fields.
     * 
     * @return  the new OpenMBeanInfoSupport
     * @throws  IllegalArgumentException  if this object's fields cannot create
     *    a valid OpenMBeanInfoSupport (eg, if the name field is null)
     */
    public OpenMBeanInfoSupport make() {
        return new OpenMBeanInfoSupport(name, desc, Attr.array(attrs),
                Cons.array(cons), Op.array(ops), Notif.array(notifs));
    }
    
    
}
