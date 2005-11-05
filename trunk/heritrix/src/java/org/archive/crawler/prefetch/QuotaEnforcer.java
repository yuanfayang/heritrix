/* QuotaEnforcer
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
package org.archive.crawler.prefetch;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlSubstats;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.Frontier.FrontierGroup;
import org.archive.crawler.settings.SimpleType;

/**
 * A simple quota enforcer. If the host, server, or frontier group
 * associated with the current CrawlURI is already over its quotas, 
 * blocks the current URI's processing with S_BLOCKED_BY_QUOTA.
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class QuotaEnforcer extends Processor implements FetchStatusCodes {
    private static final Logger LOGGER =
        Logger.getLogger(QuotaEnforcer.class.getName());
    
   // server quotas
   /** server max successful fetches */
   protected static final String ATTR_SERVER_MAX_FETCH_SUCCESSES = 
       "server-max-fetch-successes";
   protected static final Long DEFAULT_SERVER_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** server max successful fetch bytes */
   protected static final String ATTR_SERVER_MAX_SUCCESS_KB = 
       "server-max-success-kb";
   protected static final Long DEFAULT_SERVER_MAX_SUCCESS_KB =
       new Long(-1);
   
   // host quotas
   /** host max successful fetches */
   protected static final String ATTR_HOST_MAX_FETCH_SUCCESSES = 
       "host-max-fetch-successes";
   protected static final Long DEFAULT_HOST_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** host max successful fetch bytes */
   protected static final String ATTR_HOST_MAX_SUCCESS_KB = 
       "host-max-success-kb";
   protected static final Long DEFAULT_HOST_MAX_SUCCESS_KB =
       new Long(-1);
   
   // group quotas
   /** group max successful fetches */
   protected static final String ATTR_GROUP_MAX_FETCH_SUCCESSES = 
       "group-max-fetch-successes";
   protected static final Long DEFAULT_GROUP_MAX_FETCH_SUCCESSES =
       new Long(-1);
   /** group max successful fetch bytes */
   protected static final String ATTR_GROUP_MAX_SUCCESS_KB = 
       "group-max-success-kb";
   protected static final Long DEFAULT_GROUP_MAX_SUCCESS_KB =
       new Long(-1);
   
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public QuotaEnforcer(String name) {
        super(name, "QuotaEnforcer.");
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_FETCH_SUCCESSES,
            "Maximum number of successful fetches to collect from a single " +
            "server. Default is -1, meaning no limit.",
            DEFAULT_SERVER_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_SERVER_MAX_SUCCESS_KB,
            "Maximum amount of content in KB to collect from a single " +
            "server. Default is -1, meaning no limit.",
            DEFAULT_SERVER_MAX_SUCCESS_KB));
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_FETCH_SUCCESSES,
            "Maximum number of successful fetches to collect from a single " +
            "host. Default is -1, meaning no limit.",
            DEFAULT_HOST_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_HOST_MAX_SUCCESS_KB,
            "Maximum amount of content in KB to collect from a single " +
            "host. Default is -1, meaning no limit.",
            DEFAULT_HOST_MAX_SUCCESS_KB));
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_FETCH_SUCCESSES,
            "Maximum number of successful fetches to collect from a single " +
            "frontier group (queue). Default is -1, meaning no limit.",
            DEFAULT_GROUP_MAX_FETCH_SUCCESSES));
        addElementToDefinition(new SimpleType(ATTR_GROUP_MAX_SUCCESS_KB,
            "Maximum amount of content in KB to collect from a single " +
            "frontier group (queue). Default is -1, meaning no limit.",
            DEFAULT_GROUP_MAX_SUCCESS_KB));
    }
    
    protected void innerProcess(CrawlURI curi) {
        // check per-server quotas
        CrawlServer server =
            getController().getServerCache().getServerFor(curi);
        long fetchQuota = ((Long)getUncheckedAttribute(curi,ATTR_SERVER_MAX_FETCH_SUCCESSES)).longValue();
        long bytesQuota = 1024*((Long)getUncheckedAttribute(curi,ATTR_SERVER_MAX_SUCCESS_KB)).longValue();
        CrawlSubstats substats = server.getSubstats();
        if (checkQuota(curi, fetchQuota, substats.getFetchSuccesses(), 
                "Q:server-fetchSuccesses")) {
            return;
        }
        if (checkQuota(curi, bytesQuota,  substats.getSuccessBytes(), 
                "Q:server-successBytes")) {
            return;
        }

        // check per-host quotas
        CrawlHost host = 
            getController().getServerCache().getHostFor(curi);
        fetchQuota = ((Long)getUncheckedAttribute(curi,ATTR_HOST_MAX_FETCH_SUCCESSES)).longValue();
        bytesQuota = 1024*((Long)getUncheckedAttribute(curi,ATTR_HOST_MAX_SUCCESS_KB)).longValue();
        substats = host.getSubstats();
        if (checkQuota(curi, fetchQuota, substats.getFetchSuccesses(),
                "Q:host-fetchSuccesses")) {
            return;
        }
        if (checkQuota(curi, bytesQuota, substats.getSuccessBytes(),
                "Q:host-successBytes")) {
            return;
        }
        
        // check per-frontier-group (queue) quotas
        FrontierGroup group = 
            getController().getFrontier().getGroup(curi);
        fetchQuota = ((Long)getUncheckedAttribute(curi,ATTR_GROUP_MAX_FETCH_SUCCESSES)).longValue();
        bytesQuota = 1024*((Long)getUncheckedAttribute(curi,ATTR_GROUP_MAX_SUCCESS_KB)).longValue();
        substats = group.getSubstats();
        if (checkQuota(curi, fetchQuota, substats.getFetchSuccesses(),
                "Q:group-fetchSuccesses")) {
            return;
        }
        if (checkQuota(curi, bytesQuota, substats.getSuccessBytes(),
                "Q:group-successBytes")) {
            return;
        }
    }

    /**
     * @param serverFetchQuota
     * @param fetchSuccesses
     * @return
     */
    protected boolean checkQuota(CrawlURI curi, long quota, long actual, String annotate) {
        if(quota >= 0 && actual >= quota) {
            curi.setFetchStatus(S_BLOCKED_BY_QUOTA);
            curi.addAnnotation(annotate);
            curi.skipToProcessorChain(getController().
                    getPostprocessorChain());
            return true;
        }
        return false; 
    }
}