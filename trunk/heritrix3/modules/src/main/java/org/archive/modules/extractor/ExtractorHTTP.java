/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.modules.extractor;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.CrawlURI.FetchType;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;


/**
 * Extracts URIs from HTTP response headers.
 * @author gojomo
 */
public class ExtractorHTTP extends Extractor {

    private static final long serialVersionUID = 3L;

//    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    public ExtractorHTTP() {
    }

    
    
    @Override
    protected boolean shouldProcess(CrawlURI uri) {
        if (uri.getFetchStatus() <= 0) {
            return false;
        }
        FetchType ft = uri.getFetchType();
        return (ft == FetchType.HTTP_GET) || (ft == FetchType.HTTP_POST);
    }
    
    
    @Override
    protected void extract(CrawlURI curi) {
        HttpMethod method = curi.getHttpMethod();
        addHeaderLink(curi, method.getResponseHeader("Location"));
        addHeaderLink(curi, method.getResponseHeader("Content-Location"));
    }

    protected void addHeaderLink(CrawlURI curi, Header loc) {
        if (loc == null) {
            // If null, return without adding anything.
            return;
        }
        // TODO: consider possibility of multiple headers
        try {
            UURI dest = UURIFactory.getInstance(curi.getUURI(), loc.getValue());
            LinkContext lc = new HTMLLinkContext(loc.getName()+":"); 
            Link link = new Link(curi.getUURI(), dest, lc, Hop.REFER);
            curi.getOutLinks().add(link);
            numberOfLinksExtracted++;
        } catch (URIException e) {
            logUriError(e, curi.getUURI(), loc.getValue());
        }

    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append(super.report());
        ret.append("  Function:          " +
            "Extracts URIs from HTTP response headers\n");
        ret.append("  CrawlURIs handled: " + this.getURICount());
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n");
        return ret.toString();
    }
}
