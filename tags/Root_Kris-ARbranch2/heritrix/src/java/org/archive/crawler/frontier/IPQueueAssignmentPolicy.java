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

import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;

/**
 * Uses target IP as basis for queue-assignment, unless it is unavailable,
 * in which case it behaves as HostnameQueueAssignmentPolicy.
 * 
 * @author gojomo
 */
public class IPQueueAssignmentPolicy extends HostnameQueueAssignmentPolicy {

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueueAssignmentPolicy#getClassKey(org.archive.crawler.datamodel.CrawlURI)
     */
    public String getClassKey(CrawlURI curi) {
        CrawlServer server = curi.getServer();
        if (server == null || server.getHost().getIP()==null ) {
            // if no server or no IP, use superclass implementation
            return super.getClassKey(curi);
        }
        // use dotted-decimal IP address
        return server.getHost().getIP().getHostAddress();
    }
}
