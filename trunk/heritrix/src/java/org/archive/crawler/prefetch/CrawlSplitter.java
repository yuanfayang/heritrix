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

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;

/**
 * A dumb crawl splitter.
 * Sits at top of processing chain and looks at CrawlURis as they come off
 * the Frontier.  Does a lexical comparison of CrawlURI#classKey against the
 * supplied range.
 * @author stack
 * @version $Date$, $Revision$
 */
public class CrawlSplitter extends Processor implements FetchStatusCodes {
    private static final Logger LOGGER =
        Logger.getLogger(CrawlSplitter.class.getName());
    private static final String ATTR_RANGE_LOWER = "crawl-range-lower";
    private static final String ATTR_RANGE_UPPER = "crawl-range-upper";
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public CrawlSplitter(String name) {
        super(name, "CrawlSplitter. Compares CrawlURI#classKey " +
            "lexically against configured range. " +
            "Uses String.compareTo. If CrawlURI " +
            "falls outside of the configured range, its marked " +
            CrawlURI.fetchStatusCodesToString(S_BLOCKED_BY_CUSTOM_PROCESSOR) +
            " and processing jumps to postprocessor (The CrawlURI " +
            "will show with a " + S_BLOCKED_BY_CUSTOM_PROCESSOR +
            " in the crawl.log. Put this processor " +
            "first ahead of all processors including PreconditionEnforcer.");
        addElementToDefinition(new SimpleType(ATTR_RANGE_LOWER,
            "String to use as lower-bound on range (Inclusive). Use the " +
            "empty string to signify absolute bottom of the range.",
            ""));
        addElementToDefinition(new SimpleType(ATTR_RANGE_UPPER,
            "String to use as upper-bound on range (Non-inclusive). " +
            "Use '~' to signify maximum upper-bound.", "~"));
    }
    
    protected void innerProcess(CrawlURI curi) {
        // Check if blocked by regular expression.  Run regex against the
        // CrawlURI class key.
        try {
            String lower = (String)getAttribute(ATTR_RANGE_LOWER);
            String upper = (String)getAttribute(ATTR_RANGE_UPPER);
            if (lower.compareTo(upper) > 0) {
                LOGGER.severe("Lower, " + lower + ", is greater than Upper, " +
                    upper);
            }
            if (curi.getClassKey().compareTo(lower) < 0 ||
                    curi.getClassKey().compareTo(upper) > 0) {
                curi.setFetchStatus(S_BLOCKED_BY_CUSTOM_PROCESSOR);
                curi.skipToProcessorChain(getController().
                    getPostprocessorChain());
         
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}