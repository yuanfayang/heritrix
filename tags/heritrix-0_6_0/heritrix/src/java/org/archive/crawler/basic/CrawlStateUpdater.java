/* CrawlStateUpdater
 * 
 * Created on Jun 5, 2003
 * 
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.basic;

import java.io.IOException;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;


/**
 * A step, late in the processing of a CrawlURI, for updating the per-host
 * information that may have been affected by the fetch. This will initially
 * be robots and ip address info; it could include other per-host stats that
 * would affect the crawl (like total pages visited at the site) as well.
 *
 * @author gojomo
 * @version $Id$
 */
public class CrawlStateUpdater extends Processor implements
        CoreAttributeConstants, FetchStatusCodes {

    public static int MAX_DNS_FETCH_ATTEMPTS = 3;

    public CrawlStateUpdater(String name) {
        super(name, "Crawl state updater");
    }

    protected void innerProcess(CrawlURI curi) {
        String scheme = curi.getUURI().getScheme().toLowerCase();

        // if it's a dns entry set the expire time
        if (scheme.equals("dns")) {
            // if we've looked up the host update the expire time
            if (!curi.getServer().getHost().hasBeenLookedUp()) {
                // TODO: resolve several issues here:
                //   (1) i don't think this if clause is ever reached;
                //       won't every DNS uri that gets this far imply
                //       hasBeenLookedUp will have been set?
                //   (2) we don't want repeated successful attempts to
                //       refetch a domain name, each time it expires,
                //       to eventually exhaust the retries... so in
                //       fact the retry count needs to be reset somewhere,
                //       maybe at each success

                // if we've tried too many times give up
                if (curi.getFetchAttempts() >= MAX_DNS_FETCH_ATTEMPTS) {
                    curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
                }
            }
            // If not dns make sure it's http, 'cause we don't know nuthin'
            // else
        } else if (scheme.equals("http") || scheme.equals("https")) {
            if (curi.getFetchStatus() > 0 && (curi.getUURI().getPath() != null)
                    && curi.getUURI().getPath().equals("/robots.txt")) {
                // Update host with robots info
                if (curi.isHttpTransaction()) {
                    try {
                        curi.getServer().updateRobots(curi);
                    } catch (IOException e) {
                        curi.addLocalizedError(getName(), e,
                                "robots.txt parsing IOException");
                    }
                }
            }
        }
    }
}