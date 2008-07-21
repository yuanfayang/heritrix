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
 * Proxy.java
 *
 * Created on May 8, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.archive.openmbeans.annotations.BeanProxy;

/**
 * @author pjack
 *
 */
public class Remote<T> {

    private JMXConnector connector;
    private ObjectName oname;
    private T object;
    
    public Remote(JMXConnector connector, ObjectName name, Class<T> type) {
        this.oname = name;
        this.connector = connector;
        try {
            MBeanServerConnection conn = connector.getMBeanServerConnection();
            object = BeanProxy.proxy(conn, oname, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    
    public void close() {
        if (connector != null) try {
            connector.close();
        } catch (IOException e) {
            e.printStackTrace(); // FIXME
        }
    }

    
    public JMXConnector getJMXConnector() {
        return connector;
    }
    
    public T getObject() {
        return object;
    }
    
    public static <T> Remote<T> make(
            JMXConnector connector, 
            ObjectName name, 
            Class<T> cls) {
        return new Remote<T>(connector, name, cls);
    }
}
