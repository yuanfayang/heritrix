/* CrawlSplitter
 * 
 * Created on Jun 9, 2005
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

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;

/**
 * Crawl splitter.
 * @author stack
 * @version $Date$, $Revision$
 */
public class CrawlSplitter extends Processor implements FetchStatusCodes {
    private static final String ATTR_SPLIT_STR = "split-string";
    private static final String ATTR_BEFORE_SPLIT = "crawl-before-split";
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public CrawlSplitter(String name) {
        super(name, "CrawlSplitter. Compares CrawlURI#classKey " +
            "lexically against a configured string. If CrawlURI is " +
            "of wrong side of the split, its marked " +
            CrawlURI.fetchStatusCodesToString(S_BLOCKED_BY_CUSTOM_PROCESSOR) +
            " and processing jumps to postprocessor. Put this processor " +
            "first ahead of all processors including PreconditionEnforcer.");
        addElementToDefinition(new SimpleType(ATTR_SPLIT_STR,
            "String to compare CrawlURI#classKey against. If empty, the" +
            "crawl is not split.  If exactly equal, CrawlURI goes into " +
            "the 'before' category.",
            ""));
        addElementToDefinition(new SimpleType(ATTR_BEFORE_SPLIT,
            "If true, we crawl the portion 'before' the split, else " +
            "'after' the split.", Boolean.TRUE));
    }
    
    protected void innerProcess(CrawlURI curi) {
        // Check if blocked by regular expression.  Run regex against the
        // CrawlURI class key.
        try {
            String str = (String)getAttribute(ATTR_SPLIT_STR, curi);
            boolean crawlBeforeSplit = ((Boolean)getAttribute(ATTR_BEFORE_SPLIT)).
                booleanValue();
            boolean before = curi.getClassKey().compareTo(str) <= 0;
            if ((before && !crawlBeforeSplit) || (!before && crawlBeforeSplit)) {
                curi.setFetchStatus(S_BLOCKED_BY_CUSTOM_PROCESSOR);
                curi.skipToProcessorChain(getController().
                    getPostprocessorChain());
         
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}