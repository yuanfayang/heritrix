/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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
package org.archive.hcc.util;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class NotificationDelegator
        extends Delegator implements NotificationListener {
    
	private static Log log = LogFactory.getLog(NotificationDelegator.class);

    public NotificationDelegator() {
        super(DelegatorPolicy.ACCEPT_ALL);
    }

    public NotificationDelegator(DelegatorPolicy p) {
        super(p);
    }

    public void handleNotification(Notification notification, Object handback) {
    	log.debug("notification.type=" + notification.getType());
    	log.debug("notification.message=" + notification.getMessage());
    	log.debug("notification.userData=" + notification.getUserData());

    	if (notification instanceof MBeanServerNotification) {
    		log.debug("notification.mbeanName="
    				+ ((MBeanServerNotification) notification)
    				.getMBeanName());
    	}

        delegate(notification, handback);
    }

    protected boolean delegate(Notification n, Object handback) {
        boolean consumedAtLeastOne = false;
        for (Delegatable h : handlers) {
            boolean accepted = ((NotificationDelegatableBase) h).delegate(
                    n,
                    handback);
            if (accepted) {
                consumedAtLeastOne = true;
            }

            if (accepted && policy == DelegatorPolicy.ACCEPT_FIRST) {
                return true;
            }
        }

        return consumedAtLeastOne;
    }

    public void addDelegatable(Delegatable d) {
        super.addDelegatable((NotificationDelegatableBase) d);
    }
}