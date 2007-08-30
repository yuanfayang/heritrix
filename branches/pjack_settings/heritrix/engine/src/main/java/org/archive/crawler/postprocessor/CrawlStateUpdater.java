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
package org.archive.crawler.postprocessor;


import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;

import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlerProcessor;
import org.archive.crawler.framework.Frontier.FrontierGroup;
import org.archive.modules.ProcessorURI;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.RobotsHonoringPolicy;
import org.archive.state.KeyManager;


/**
 * A step, late in the processing of a CrawlURI, for updating the per-host
 * information that may have been affected by the fetch. This will initially
 * be robots and ip address info; it could include other per-host stats that
 * would affect the crawl (like total pages visited at the site) as well.
 *
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class CrawlStateUpdater extends CrawlerProcessor {

    private static final long serialVersionUID = -1072728147960180091L;

    private static final Logger logger =
        Logger.getLogger(CrawlStateUpdater.class.getName());

    

    public CrawlStateUpdater() {
        super();
    }


    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }
    
    @Override
    protected void innerProcess(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        
        // Tally per-server, per-host, per-frontier-class running totals
        CrawlServer server = getServerFor(curi);
        
        String scheme = curi.getUURI().getScheme().toLowerCase();
        if (scheme.equals("http") || scheme.equals("https") &&
                server != null) {
            // Update connection problems counter
            if(curi.getFetchStatus() == S_CONNECT_FAILED) {
                server.incrementConsecutiveConnectionErrors();
            } else if (curi.getFetchStatus() > 0){
                server.resetConsecutiveConnectionErrors();
            }

            // Update robots info
            try {
                if (curi.getUURI().getPath() != null &&
                        curi.getUURI().getPath().equals("/robots.txt")) {
                    RobotsHonoringPolicy rhp = controller.
                        getOrderSetting(CrawlOrder.ROBOTS_HONORING_POLICY);
                    // Update server with robots info
                    server.updateRobots(rhp,  curi);
                }
            }
            catch (URIException e) {
                logger.severe("Failed get path on " + curi.getUURI());
            }
        }
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(CrawlStateUpdater.class);
    }
}
