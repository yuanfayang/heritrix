/* CrawlSubstats
*
* $Id$
*
* Created on Nov 4, 2005
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
package org.archive.crawler.datamodel;

import java.io.Serializable;

import org.apache.commons.httpclient.HttpStatus;

/**
 * Collector of statististics for a 'subset' of a crawl,
 * such as a server (host:port), host, or frontier group 
 * (eg queue). 
 * 
 * @author gojomo
 */
public class CrawlSubstats implements Serializable, FetchStatusCodes {

    private static final long serialVersionUID = 8624425657056569036L;

    public interface HasCrawlSubstats {
        public CrawlSubstats getSubstats();
    }
    
    long fetchSuccesses;   // 2XX response codes
    long fetchResponses;   // all positive responses (incl. 3XX, 4XX, 5XX)
    long successBytes;     // total size of all success responses
    long totalBytes;       // total size of all responses
    long fetchNonResponses; // processing attempts resulting in no response
                           // (both failures and temp deferrals)
    
    public synchronized void tally(CrawlURI curi) {
        if(curi.getFetchStatus()<=0) {
            fetchNonResponses++;
            return;
        }
        fetchResponses++;
        totalBytes += curi.getContentSize();
        if(curi.getFetchStatus()>=HttpStatus.SC_OK && 
                curi.getFetchStatus()<300) {
            fetchSuccesses++;
            successBytes += curi.getContentSize();
        }
    }
    
    public long getFetchSuccesses() {
        return fetchSuccesses;
    }
    public long getFetchResponses() {
        return fetchResponses;
    }
    public long getSuccessBytes() {
        return successBytes;
    }
    public long getTotalBytes() {
        return totalBytes;
    }
    public long getFetchNonResponses() {
        return fetchNonResponses;
    }
}
