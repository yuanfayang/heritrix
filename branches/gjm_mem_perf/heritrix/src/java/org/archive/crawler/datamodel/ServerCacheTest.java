/* ServerCacheTest
*
* Created on August 4, 2004
*
* Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel;

import java.lang.ref.SoftReference;
import java.util.LinkedList;

import junit.framework.TestCase;

/**
 * Test the ServerCache
 * 
 * @author gojomo
 */
public class ServerCacheTest extends TestCase {

    public void testHolds() {
        ServerCache servers = new ServerCache(null);
        
        String serverKey = "www.example.com:9090";
        String hostKey = "www.example.com";
        CrawlServer s = servers.getServerFor(serverKey);
        
        forceScarceMemory();
                
        assertTrue("cache lost server", servers.containsServer(serverKey));
        assertTrue("cache lost host", servers.containsHost(hostKey));
    }
    
    public void testDiscards() {
        ServerCache servers = new ServerCache(null);
        
        String serverKey = "www.example.com:9090";
        String hostKey = "www.example.com";
        CrawlServer s = servers.getServerFor(serverKey);
        s = null;// dereference
        
        forceScarceMemory();
        
        assertFalse("cache held server", servers.containsServer(serverKey));
        assertFalse("cache held host", servers.containsHost(hostKey));
    }

    /**
     * 
     */
    private void forceScarceMemory() {
        // force soft references to be broken
        LinkedList hog = new LinkedList();
        long blocks = Runtime.getRuntime().maxMemory() / 1000000;
        for(long l = 0; l <= blocks; l++) {
            try {
                hog.add(new SoftReference(new byte[1000000]));
            } catch (OutOfMemoryError e) {
                hog = null;
                break;
            }
        }
    }
}
