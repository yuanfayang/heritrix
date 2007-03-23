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
 * SimplePreselector.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.prefetch;


import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Scoper;
import org.archive.processors.ProcessResult;
import org.archive.processors.ProcessorURI;
import org.archive.state.Expert;
import org.archive.state.Key;
import org.archive.util.TextUtils;


/**
 * If set to recheck the crawl's scope, gives a yes/no on whether
 * a CrawlURI should be processed at all. If not, its status
 * will be marked OUT_OF_SCOPE and the URI will skip directly
 * to the first "postprocessor".
 *
 *
 * @author gojomo
 *
 */
public class Preselector extends Scoper
implements FetchStatusCodes {

    private static final long serialVersionUID = 3L;


    /**
     * Recheck if uri is in scope. This is meaningful if the scope is altered
     * during a crawl. URIs are checked against the scope when they are added to
     * queues. Setting this value to true forces the URI to be checked against
     * the scope when it is comming out of the queue, possibly after the scope
     * is altered.
     */
    @Expert
    final public static Key<Boolean> RECHECK_SCOPE = Key.make(false);


    /**
     * Block all URIs from being processed. This is most likely to be used in
     * overrides to easily reject certain hosts from being processed.
     */
    @Expert
    final public static Key<Boolean> BLOCK_ALL = Key.make(false);

    
    /**
     * Block all URIs matching the regular expression from being processed.
     */
    @Expert
    final public static Key<String> BLOCK_BY_REGEXP = Key.make("");


    /**
     * Allow only URIs matching the regular expression to be processed.
     */
    @Expert
    final public static Key<String> ALLOW_BY_REGEXP = Key.make("");


    /**
     * Constructor.
     */
    public Preselector() {
        super();
    }

    
    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }

    
    @Override
    protected void innerProcess(ProcessorURI puri) {
        throw new AssertionError();
    }
    

    @Override
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        
        // Check if uris should be blocked
        if (curi.get(this, BLOCK_ALL)) {
            curi.setFetchStatus(S_BLOCKED_BY_USER);
            return ProcessResult.FINISH;
        }

        // Check if allowed by regular expression
        String regexp = curi.get(this, ALLOW_BY_REGEXP);
        if (regexp != null && !regexp.equals("")) {
            if (!TextUtils.matches(regexp, curi.toString())) {
                curi.setFetchStatus(S_BLOCKED_BY_USER);
                return ProcessResult.FINISH;
            }
        }

        // Check if blocked by regular expression
        regexp = curi.get(this, BLOCK_BY_REGEXP);
        if (regexp != null && !regexp.equals("")) {
            if (TextUtils.matches(regexp, curi.toString())) {
                curi.setFetchStatus(S_BLOCKED_BY_USER);
                return ProcessResult.FINISH;
            }
        }

        // Possibly recheck scope
        if (curi.get(this, RECHECK_SCOPE)) {
            if (!isInScope(curi)) {
                // Scope rejected
                curi.setFetchStatus(S_OUT_OF_SCOPE);
                return ProcessResult.FINISH;
            }
        }
        
        return ProcessResult.PROCEED;
    }
}
