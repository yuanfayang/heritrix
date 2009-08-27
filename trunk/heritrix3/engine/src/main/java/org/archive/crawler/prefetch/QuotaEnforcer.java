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

import static org.archive.modules.fetcher.FetchStatusCodes.S_BLOCKED_BY_QUOTA;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.Frontier;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.fetcher.FetchStats;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A simple quota enforcer. If the host, server, or frontier group
 * associated with the current CrawlURI is already over its quotas, 
 * blocks the current URI's processing with S_BLOCKED_BY_QUOTA.
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class QuotaEnforcer extends Processor {
    private static final long serialVersionUID = 3L;

    private static final Logger LOGGER =
        Logger.getLogger(QuotaEnforcer.class.getName());
    
    // indexed table of reused string categorical names/keys
    protected static final int SERVER = 0;
    protected static final int HOST = 1;
    protected static final int GROUP = 2;
    
    protected static final int SUCCESSES = 0;
    protected static final int SUCCESS_KB = 1;
    protected static final int RESPONSES = 2;
    protected static final int RESPONSE_KB = 3;
    
    private static final String SERVER_MAX_FETCH_SUCCESSES = "serverMaxFetchSuccesses";
    private static final String SERVER_MAX_SUCCESS_KB = "serverMaxSuccessKb";
    private static final String SERVER_MAX_FETCH_RESPONSES = "serverMaxFetchResponses";
    private static final String SERVER_MAX_ALL_KB = "serverMaxAllKb";

    private static final String HOST_MAX_FETCH_SUCCESSES = "hostMaxFetchSuccesses";
    private static final String HOST_MAX_SUCCESS_KB = "hostMaxSuccessKb";
    private static final String HOST_MAX_FETCH_RESPONSES = "hostMaxFetchResponses";
    private static final String HOST_MAX_ALL_KB = "hostMaxAllKb";

    private static final String GROUP_MAX_FETCH_SUCCESSES = "groupMaxFetchSuccesses";
    private static final String GROUP_MAX_SUCCESS_KB = "groupMaxSuccessKb";
    private static final String GROUP_MAX_FETCH_RESPONSES = "groupMaxFetchResponses";
    private static final String GROUP_MAX_ALL_KB = "groupMaxAllKb";
    
    protected static final String[][] keys = new String[][] {
        {
            //"server",
            SERVER_MAX_FETCH_SUCCESSES,
            SERVER_MAX_SUCCESS_KB,
            SERVER_MAX_FETCH_RESPONSES,
            SERVER_MAX_ALL_KB
        },
        {
            //"host"
            HOST_MAX_FETCH_SUCCESSES,
            HOST_MAX_SUCCESS_KB,
            HOST_MAX_FETCH_RESPONSES,
            HOST_MAX_ALL_KB
            ,
        },
        {
            //"group"
            GROUP_MAX_FETCH_SUCCESSES,
            GROUP_MAX_SUCCESS_KB,
            GROUP_MAX_FETCH_RESPONSES,
            GROUP_MAX_ALL_KB
        }
    };

   // server quotas
   // successes

    /**
     * Maximum number of fetch successes (e.g. 200 responses) to collect from
     * one server. Default is -1, meaning no limit.
     */
    {
        setServerMaxFetchSuccesses(-1L); // no limit
    }
    public long getServerMaxFetchSuccesses() {
        return (Long) kp.get(SERVER_MAX_FETCH_SUCCESSES);
    }
    public void setServerMaxFetchSuccesses(long max) {
        kp.put(SERVER_MAX_FETCH_SUCCESSES,max);
    }


    /**
     * Maximum amount of fetch success content (e.g. 200 responses) in KB to
     * collect from one server. Default is -1, meaning no limit.
     */
    {
        setServerMaxSuccessKb(-1L); // no limit
    }
    public long getServerMaxSuccessKb() {
        return (Long) kp.get(SERVER_MAX_SUCCESS_KB);
    }
    public void setServerMaxSuccessKb(long max) {
        kp.put(SERVER_MAX_SUCCESS_KB,max);
    }

    /**
     * Maximum number of fetch responses (incl. error responses) to collect from
     * one server. Default is -1, meaning no limit.
     */
    {
        setServerMaxFetchResponses(-1L); // no limit
    }
    public long getServerMaxFetchResponses() {
        return (Long) kp.get(SERVER_MAX_FETCH_RESPONSES);
    }
    public void setServerMaxFetchResponses(long max) {
        kp.put(SERVER_MAX_FETCH_RESPONSES,max);
    }

    /**
     * Maximum amount of response content (incl. error responses) in KB to
     * collect from one server. Default is -1, meaning no limit.
     */
    {
        setServerMaxAllKb(-1L); // no limit
    }
    public long getServerMaxAllKb() {
        return (Long) kp.get(SERVER_MAX_ALL_KB);
    }
    public void setServerMaxAllKb(long max) {
        kp.put(SERVER_MAX_ALL_KB,max);
    }

    /**
     * Maximum number of fetch successes (e.g. 200 responses) to collect from
     * one host. Default is -1, meaning no limit.
     */
    {
        setHostMaxFetchSuccesses(-1L); // no limit
    }
    public long getHostMaxFetchSuccesses() {
        return (Long) kp.get(HOST_MAX_FETCH_SUCCESSES);
    }
    public void setHostMaxFetchSuccesses(long max) {
        kp.put(HOST_MAX_FETCH_SUCCESSES,max);
    }

    /**
     * Maximum amount of fetch success content (e.g. 200 responses) in KB to
     * collect from one host. Default is -1, meaning no limit.
     */
    {
        setHostMaxSuccessKb(-1L); // no limit
    }
    public long getHostMaxSuccessKb() {
        return (Long) kp.get(HOST_MAX_SUCCESS_KB);
    }
    public void setHostMaxSuccessKb(long max) {
        kp.put(HOST_MAX_SUCCESS_KB,max);
    }

    /**
     * Maximum number of fetch responses (incl. error responses) to collect from
     * one host. Default is -1, meaning no limit.
     */
    {
        setHostMaxFetchResponses(-1L); // no limit
    }
    public long getHostMaxFetchResponses() {
        return (Long) kp.get(HOST_MAX_FETCH_RESPONSES);
    }
    public void setHostMaxFetchResponses(long max) {
        kp.put(HOST_MAX_FETCH_RESPONSES,max);
    }

    /**
     * Maximum amount of response content (incl. error responses) in KB to
     * collect from one host. Default is -1, meaning no limit.
     */
    {
        setHostMaxAllKb(-1L); // no limit
    }
    public long getHostMaxAllKb() {
        return (Long) kp.get(HOST_MAX_ALL_KB);
    }
    public void setHostMaxAllKb(long max) {
        kp.put(HOST_MAX_ALL_KB,max);
    }

    /**
     * Maximum number of fetch successes (e.g. 200 responses) to collect from
     * one group. Default is -1, meaning no limit.
     */
    {
        setGroupMaxFetchSuccesses(-1L); // no limit
    }
    public long getGroupMaxFetchSuccesses() {
        return (Long) kp.get(GROUP_MAX_FETCH_SUCCESSES);
    }
    public void setGroupMaxFetchSuccesses(long max) {
        kp.put(GROUP_MAX_FETCH_SUCCESSES,max);
    }

    /**
     * Maximum amount of fetch success content (e.g. 200 responses) in KB to
     * collect from one group. Default is -1, meaning no limit.
     */
    {
        setGroupMaxSuccessKb(-1L); // no limit
    }
    public long getGroupMaxSuccessKb() {
        return (Long) kp.get(GROUP_MAX_SUCCESS_KB);
    }
    public void setGroupMaxSuccessKb(long max) {
        kp.put(GROUP_MAX_SUCCESS_KB,max);
    }

    /**
     * Maximum number of fetch responses (incl. error responses) to collect from
     * one group. Default is -1, meaning no limit.
     */
    {
        setGroupMaxFetchResponses(-1L); // no limit
    }
    public long getGroupMaxFetchResponses() {
        return (Long) kp.get(GROUP_MAX_FETCH_RESPONSES);
    }
    public void setGroupMaxFetchResponses(long max) {
        kp.put(GROUP_MAX_FETCH_RESPONSES,max);
    }

    /**
     * Maximum amount of response content (incl. error responses) in KB to
     * collect from one group. Default is -1, meaning no limit.
     */
    {
        setGroupMaxAllKb(-1L); // no limit
    }
    public long getGroupMaxAllKb() {
        return (Long) kp.get(GROUP_MAX_ALL_KB);
    }
    public void setGroupMaxAllKb(long max) {
        kp.put(GROUP_MAX_ALL_KB,max);
    }

    /**
     * Whether an over-quota situation should result in the containing queue
     * being force-retired (if the Frontier supports this). Note that if your
     * queues combine URIs that are different with regard to the quota category,
     * the retirement may hold back URIs not in the same quota category. Default
     * is false.
     */
    {
        setForceRetire(true);
    }
    public boolean getForceRetire() {
        return (Boolean) kp.get("forceRetire");
    }
    public void setForceRetire(boolean force) {
        kp.put("forceRetire",force);
    }
    
    protected ServerCache serverCache;
    public ServerCache getServerCache() {
        return this.serverCache;
    }
    @Autowired
    public void setServerCache(ServerCache serverCache) {
        this.serverCache = serverCache;
    }
    
    protected Frontier frontier;
    public Frontier getFrontier() {
        return this.frontier;
    }
    @Autowired
    public void setFrontier(Frontier frontier) {
        this.frontier = frontier;
    }
    
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }

    protected void innerProcess(ProcessorURI puri) {
        throw new AssertionError();
    }
    
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        final CrawlServer server = ServerCacheUtil.getServerFor(serverCache, 
                curi.getUURI());
        final CrawlHost host = ServerCacheUtil.getHostFor(serverCache, 
                curi.getUURI());
        FetchStats.HasFetchStats[] haveStats = 
            new FetchStats.HasFetchStats[] {
                server, 
                host, 
                frontier.getGroup(curi)
            };
        
        for(int cat=SERVER;cat<=GROUP;cat++) {
            if (checkQuotas(curi,haveStats[cat],cat)) {
                return ProcessResult.FINISH;
            }
        }
        
        return ProcessResult.PROCEED;
    }

    /**
     * Check all quotas for the given substats and category (server, host, or
     * group). 
     * 
     * @param curi CrawlURI to mark up with results
     * @param hasStats  holds CrawlSubstats with actual values to test
     * @param CAT category index (SERVER, HOST, GROUP) to quota settings keys
     * @return true if quota precludes fetching of CrawlURI
     */
    protected boolean checkQuotas(final CrawlURI curi,
            final FetchStats.HasFetchStats hasStats,
            final int CAT) {
        if (hasStats == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(curi.toString() + " null stats category: " + CAT);
            }
            return false;
        }
        FetchStats substats = hasStats.getSubstats();
        long[] actuals = new long[] {
                substats.getFetchSuccesses(),
                substats.getSuccessBytes()/1024,
                substats.getFetchResponses(),
                substats.getTotalBytes()/1024,
        };
        for(int q=SUCCESSES; q<=RESPONSE_KB; q++) {
            String key = keys[CAT][q];
            if (applyQuota(curi, key, actuals[q])) {
                return true; 
            }
        }
        return false; 
    }

    /**
     * Apply the quota specified by the given key against the actual 
     * value provided. If the quota and actual values rule out processing the 
     * given CrawlURI,  mark up the CrawlURI appropriately. 
     * 
     * @param curi CrawlURI whose processing is subject to a potential quota
     * limitation
     * @param quotaKey settings key to get applicable quota
     * @param actual current value to compare to quota 
     * @return true is CrawlURI is blocked by a quota, false otherwise
     */
    protected boolean applyQuota(CrawlURI curi, String key, long actual) {
        long quota = (Long)kp.get(key);
        if (quota >= 0 && actual >= quota) {
            curi.setFetchStatus(S_BLOCKED_BY_QUOTA);
            curi.getAnnotations().add("Q:"+key);
            if (getForceRetire()) {
                curi.setForceRetire(true);
            }
            return true;
        }
        return false; 
    }
}