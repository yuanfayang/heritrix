/* Notif
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

import javax.management.MBeanNotificationInfo;


/**
 * A mutable object that produces an immutable 
 * {@link MBeanNotificationInfo}.
 * 
 * @author pjack
 */
public class Notif {


    /** The name of the notification. */
    public String name;
    
    
    /** The description of the notification. */
    public String desc;
    
    
    /** 
     * The array of strings (in dot notation) containing the notification 
     * types that the MBean may emit.
     */
    public List<String> notif = new ArrayList<String>();
 
        
    /**
     * Creates a new MBeanNotificationInfo object populated with 
     * this Notif's fields.
     * 
     * @return  the new MBeanNotificationInfo
     * @throws  IllegalArgumentException  if this object's fields cannot create
     *    a valid MBeanNotificationInfo (eg, if the name field is null)
     */
    public MBeanNotificationInfo make() {
        String[] arr = notif.toArray(new String[notif.size()]);
        return new MBeanNotificationInfo(arr, name, desc);
    }
    
    
    public Class<MBeanNotificationInfo> getType() {
        return MBeanNotificationInfo.class;
    }
    
    
    public static MBeanNotificationInfo[] array(List<Notif> list) {
        MBeanNotificationInfo[] arr 
         = new MBeanNotificationInfo[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).make();
        }
        return arr;
    }
}
