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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.hcc.Config;

public class MBeanServerConnectionFactory {
  
   // private static Logger log = Logger.getLogger(MBeanServerConnectionFactory.class.getName());
	private static Log log = LogFactory.getLog(MBeanServerConnectionFactory.class);
	
    /**
     * Creates a new MBeanServerConnection on the specified port.
     * 
     * @param address
     * @return
     * @throws IOException
     */
    public static MBeanServerConnection createConnection(
            InetSocketAddress address) throws IOException {
        JMXConnector jmxc = JMXConnectorFactory.connect(
                createJMXServiceUrl(address),
                formatCredentials(
                	Config.instance().getHeritrixJmxUsername(), 
            		Config.instance().getHeritrixJmxPassword()));

        MBeanServerConnection mbeanServerConnection = jmxc
                .getMBeanServerConnection();

            log.info("successfully created mbeanServerConnection on "
                    + address);
        return mbeanServerConnection;
    }

    protected static JMXServiceURL createJMXServiceUrl(InetSocketAddress address) {
        try {
            String hostport = address.getHostName() + ":" + address.getPort();

            String serviceUrl = "service:jmx:rmi://" + hostport
                    + "/jndi/rmi://" + hostport + "/jmxrmi";

            log.info("service url: " + serviceUrl);

            return new JMXServiceURL(serviceUrl);
        } catch (MalformedURLException e) {
            log.warn(e.toString(), e);
            return null;
        }

    }

    protected static Map<String, Object> formatCredentials(
            String username,
            String password) {
        String[] creds = new String[] { username, password };
        Map<String, Object> env = new HashMap<String, Object>(1);
        env.put(JMXConnector.CREDENTIALS, creds);
        return env;
    }
}
