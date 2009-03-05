/* $Id$
 *
 * (Created on Dec 12, 2005
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
package org.archive.hcc.util.jmx;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.ObjectName;

import org.archive.hcc.util.NotificationDelegatableBase;
import org.archive.util.JmxUtils;

public abstract class RegistrationNotificationHandler
        extends
            NotificationDelegatableBase {
	private static final Logger log = Logger.getLogger(RegistrationNotificationHandler.class.getName());
	
	@Override
    protected boolean delegate(Notification n, Object handback) {
        if (n instanceof MBeanServerNotification) {
            MBeanServerNotification msn = (MBeanServerNotification) n;
            ObjectName name = msn.getMBeanName();

            if (name.getKeyProperty(JmxUtils.TYPE).equals(getType())) {
                if (msn.getType().equals(
                        MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                	if(log.isLoggable(Level.FINE)){
                		log.fine("handling registration of " + name);
                	}
                    handleRegistered(name);
                    return true;
                } else if (msn.getType().equals(
                        MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                	if(log.isLoggable(Level.FINE)){
                		log.fine("handling deregistration of " + name);
                	}
                	handleUnregistered(name);
                    return true;
                }
            }

            return false;
        }
        return false;
    }

    protected abstract String getType();

    protected abstract void handleRegistered(ObjectName n);

    protected abstract void handleUnregistered(ObjectName n);
}
