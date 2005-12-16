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

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
 
/**
 * This class manages singleton instances of the ClusterControllerClient.
 * Within a jvm there will be one instance of ClusterControllerClient for
 * each jmx socket address (assuming that a ClusterControllerBean is running
 * on that local or remote socket).
 * @author Daniel Bernstein (dbernstein@archive.org)
 *
 */
public class ClusterControllerClientManager {
    private static Logger log = Logger.getLogger(ClusterControllerClientManager.class.getName());
    private static ClusterControllerClient defaultClient;
    
    /**
     * Returns this singleton instance of the default cluster controller client.
     * The host and jmx port can be set in the following command line options. 
     * -Dorg.archive.hcc.client.host={your host}
     * -Dorg.archive.hcc.client.jmxPort={your port}
     * 
     * If you do not specify these values on the command line, 
     * the client will connect to the local jmx server on port 8849.
     * @return
     */
    public static ClusterControllerClient getDefaultClient(){
        try {
            if(defaultClient == null){
                String host = System.getProperty("org.archive.hcc.client.host","localhost");
                int jmxPort = Integer.parseInt(System.getProperty("org.archive.hcc.client.jmxPort","8849"));
                defaultClient = new ClusterControllerClientImpl(new InetSocketAddress(host, jmxPort));
            }
            
            return defaultClient;
        } catch (Exception e) {
            e.printStackTrace();
            if (log.isLoggable(Level.SEVERE)) {
                log.severe(": " + e.getMessage());
            }
            
            throw new RuntimeException(e);
        } 
    }
    
    

    
}
