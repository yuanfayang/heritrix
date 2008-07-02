/* 
 * Copyright (C) 2007 Internet Archive.
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
 * JmxWaiter.java
 *
 * Created on Feb 21, 2007
 *
 * $Id:$
 */

package org.archive.util;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * @author pjack
 *
 */
public class JmxWaiter {

    
    private boolean notified;
    
    
    public JmxWaiter(MBeanServer server, ObjectName name, final String notif) {
        NotificationListener listener = new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {
                synchronized (JmxWaiter.this) {
                    notified = true;
                    JmxWaiter.this.notify();
                }
            }
        };
        
        NotificationFilter filter = new NotificationFilter() {

            private static final long serialVersionUID = 0L;

            public boolean isNotificationEnabled(Notification n) {
                return n.getType().equals(notif);
            }
        };
        
        try {
            server.addNotificationListener(name, listener, filter, null);
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException("No such object: " + name);
        }
    }
    
    
    public void waitUntilNotification(long timeout) 
    throws InterruptedException {
        synchronized (this) {
            if (!notified) {
                this.wait(timeout);
            }
            if (!notified) {
                throw new IllegalStateException("Timeout expired.");
            }
        }
    }
}
