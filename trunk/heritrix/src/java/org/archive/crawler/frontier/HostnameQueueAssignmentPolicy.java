/* HostnameQueueAssignmentPolicy
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

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.fetcher.FetchDNS;
import org.archive.util.DevUtils;

/**
 * QueueAssignmentPolicy based on the hostname:port evident in the given
 * CrawlURI.
 * 
 * @author gojomo
 */
public class HostnameQueueAssignmentPolicy extends QueueAssignmentPolicy {
    /**
     * When neat host-based class-key fails us
     */
    private static String DEFAULT_CLASS_KEY = "default...";
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.QueueAssignmentPolicy#getClassKey(org.archive.crawler.datamodel.CrawlURI)
     */
    public String getClassKey(CrawlURI curi) {
        String scheme = curi.getUURI().getScheme();
        String candidate = null;
        try {
            if (scheme.equals("dns")){
                if (curi.getVia() != null) {
                    // Special handling for DNS: treat as being
                    // of the same class as the triggering URI.
                    // When a URI includes a port, this ensures 
                    // the DNS lookup goes atop the host:port
                    // queue that triggered it, rather than 
                    // some other host queue
                    candidate = UURIFactory.getInstance(curi.flattenVia()).
                        getAuthorityMinusUserinfo();
                } else {
                    candidate= FetchDNS.parseTargetDomain(curi);
                }
            } else {
                candidate =  curi.getUURI().getAuthorityMinusUserinfo();
            }
            
            if(candidate == null || candidate.length() == 0) {
                candidate = DEFAULT_CLASS_KEY;
            }
        } catch (URIException e) {
            DevUtils.warnHandle(e, "Failed to get class key: " +
                e.getMessage() + " " + this);
            candidate = DEFAULT_CLASS_KEY;
        }
        // Ensure classKeys are safe as filenames on NTFS
        candidate.replace(':','#');
        return candidate;

    }

}
