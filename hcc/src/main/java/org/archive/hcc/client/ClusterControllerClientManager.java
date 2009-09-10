/* ClusterControllerClientManager
 * 
 * $Id$
 * 
 * Created on Dec 15, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

package org.archive.hcc.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class manages singleton instances of the ClusterControllerClient. Within
 * a jvm there will be one instance of ClusterControllerClient for each jmx
 * socket address (assuming that a ClusterControllerBean is running on that
 * local or remote socket).
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 * 
 */
public class ClusterControllerClientManager {
	
	private static Log log = LogFactory.getLog(ClusterControllerClientManager.class);

    private static ClusterControllerClient defaultClient;

    /**
     * Returns this singleton instance of the default cluster controller client.
     * The host and jmx port can be set in the following command line options.
     * -Dorg.archive.hcc.client.host={your host}
     * -Dorg.archive.hcc.client.jmxPort={your port}
     * 
     * If you do not specify these values on the command line, the client will
     * connect to the local jmx server on port 8849.
     * 
     * @return
     */
    public static ClusterControllerClient getDefaultClient() {
    	InetSocketAddress address = null;
        try {
            if (defaultClient == null) {
                String host = System.getProperty(
                        "org.archive.hcc.client.host",
                        InetAddress.getLocalHost().getHostName());

                int jmxPort = Integer.parseInt(System.getProperty(
                        "org.archive.hcc.client.jmxPort",
                        "8849"));
                address = new InetSocketAddress(host, jmxPort);
                
        		// XXX a pedantic implementation would look at com.sun.management.jmxremote.access.file 
            	// to find the role with readWrite access, and fall back to controlRole if 
            	// com.sun.management.jmxremote.access.file is unset
            	String username = "controlRole";
                defaultClient = new ClusterControllerClientImpl(address, username, getJmxPassword(username));
            }

            return defaultClient;
        } catch (Exception e) {
            log.error("unable to connect to [XXX uh what exactly??] at " + address + ": " + e);
            throw new RuntimeException(e);
        }
    }

    /* reads jmxremote.password to find password for supplied username
     * XXX there's no api for this built in is there? */
    private static String getJmxPassword(String username) throws IOException {
    	String passwordFile = System.getProperty("com.sun.management.jmxremote.password.file");
    	if (passwordFile == null)
    		throw new RuntimeException("com.sun.management.jmxremote.password.file must point to a file that defines a password for user " + username);
    	BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(passwordFile)));

    	for (String line = in.readLine(); line != null; line = in.readLine()) {
    		String[] usernamePassword = line.trim().split("\\s+", 2);
    		if (usernamePassword[0].equals(username))
    			return usernamePassword[1];
    	}
    	
    	log.error("unable to find password for user " + username + " in " + passwordFile + ", returning null; this will probably cause problems");
    	return null;
	}


    /**
     * Removes the manager's reference to the default client for use with unit
     * tests.
     */
    public static void resetDefaultClient() {
        defaultClient = null;
    }

}
