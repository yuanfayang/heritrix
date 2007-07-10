/* IPQueueAssignmentPolicy
*
* $Id$
*
* Created on Oct 5, 2004
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
package org.archive.crawler.frontier;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;

/**
 * Uses target IP as basis for queue-assignment, unless it is unavailable,
 * in which case it behaves as HostnameQueueAssignmentPolicy.
 * 
 * @author gojomo
 */
public class IPQueueAssignmentPolicy
extends HostnameQueueAssignmentPolicy {
    public String getClassKey(CrawlController controller, CrawlURI cauri) {
        ServerCache cache = controller.getServerCache();
        CrawlHost host = ServerCacheUtil.getHostFor(cache, cauri.getUURI());
        if (host == null || host.getIP() == null) {
            // if no server or no IP, use superclass implementation
            return super.getClassKey(controller, cauri);
        }
        // use dotted-decimal IP address
        return host.getIP().getHostAddress();
    }
}
