/* BucketQueueAssignmentPolicy
 * 
 * $Header$
 * 
 * Created on May 06, 2005
 *
 *  Copyright (C) 2005 Christian Kohlschuetter
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
 */
package org.archive.crawler.frontier;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * Uses the target IPs as basis for queue-assignment,
 * distributing them over a fixed number of sub-queues.
 * 
 * @author Christian Kohlschuetter
 */
public class BucketQueueAssignmentPolicy extends QueueAssignmentPolicy 
implements Initializable {

    private static final long serialVersionUID = 3L;

    private static final int DEFAULT_NOIP_BITMASK = 1023;
    private static final int DEFAULT_QUEUES_HOSTS_MODULO = 1021;

    @Immutable
    final public static Key<ServerCache> SERVER_CACHE =
        Key.makeAuto(ServerCache.class);
    
    private ServerCache serverCache;

    
    public void initialTasks(StateProvider global) {
        this.serverCache = global.get(this, SERVER_CACHE);
    }
    
    public String getClassKey(final CrawlURI curi) {
        
        CrawlHost host;
        try {
            host = serverCache.getHostFor(curi.getUURI().getReferencedHost());
        } catch (URIException e) {
            // FIXME error handling
            e.printStackTrace();
            host = null;
        }
        if(host == null) {
            return "NO-HOST";
        } else if(host.getIP() == null) {
            return "NO-IP-".concat(Integer.toString(Math.abs(host.getHostName()
                .hashCode())
                & DEFAULT_NOIP_BITMASK));
        } else {
            return Integer.toString(Math.abs(host.getIP().hashCode())
                % DEFAULT_QUEUES_HOSTS_MODULO);
        }
    }

    public int maximumNumberOfKeys() {
        return DEFAULT_NOIP_BITMASK + DEFAULT_QUEUES_HOSTS_MODULO + 2;
    }
    
    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(BucketQueueAssignmentPolicy.class);
    }
}
