/* Copyright (C) 2003 Internet Archive.
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
 * SimpleHTTPExtractor.java
 * Created on Jul 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.extractor;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * Extracts URIs from HTTP response headers.
 * @author gojomo
 */
public class ExtractorHTTP extends Processor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.basic.ExtractorHTTP");

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorHTTP(String name) {
        super(name, "HTTP extractor. \nExtracts URIs from HTTP response headers.");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
     */
    public void innerProcess(CrawlURI curi) {

        if(curi.isHttpTransaction())
        {
            numberOfCURIsHandled++;
            GetMethod get =
                (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
            CrawlURI curi1 = curi;
            GetMethod get1 = get;

            ArrayList uris = new ArrayList();
            Header loc = get1.getResponseHeader("Location");
            if ( loc != null ) {
                uris.add(loc.getValue());
            }
            loc = get1.getResponseHeader("Content-Location");
            if ( loc != null ) {
                uris.add(loc.getValue());
            }
            // TODO: consider possibility of multiple headers
            if(uris.size()>0) {
                numberOfLinksExtracted += uris.size();
                curi1.getAList().putObject(A_HTTP_HEADER_URIS, uris);
                logger.fine(curi+" has "+uris.size()+" uris-from-headers.");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTTP\n");
        ret.append("  Function:          Extracts URIs from HTTP response headers\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
