/* HashCrawlMapper
 * 
 * Created on Sep 30, 2005
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
package org.archive.crawler.processor;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.settings.SimpleType;

import st.ata.util.FPGenerator;

/**
 * Maps URIs to one of N crawler names by applying a hash to the
 * URI's (possibly-transformed) classKey. 
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class HashCrawlMapper extends CrawlMapper {
    private static final long serialVersionUID = 1L;
    
    /** where to load map from */
    public static final String ATTR_CRAWLER_COUNT = "crawler-count";
    public static final Long DEFAULT_CRAWLER_COUNT = new Long(1);

    long bucketCount = 1;
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public HashCrawlMapper(String name) {
        super(name, "LexicalCrawlMapper. Maps URIs to a named " +
                "crawler by a lexical comparison of the URI's " +
                "classKey to a supplied ranges map.");
        addElementToDefinition(new SimpleType(ATTR_CRAWLER_COUNT,
            "Number of crawlers among which to split up the URIs. " +
            "Their names are assumed to be 0..N-1.",
            DEFAULT_CRAWLER_COUNT));
    }

    /**
     * Look up the crawler node name to which the given CandidateURI 
     * should be mapped. 
     * 
     * @param cauri CandidateURI to consider
     * @return String node name which should handle URI
     */
    protected String map(CandidateURI cauri) {
        // get classKey, via frontier to generate if necessary
        String classKey = getController().getFrontier().getClassKey(cauri);
        // TODO: transform by regex-replace?
        
        long fp = FPGenerator.std32.fp(classKey);
        return Long.toString(fp % bucketCount);
    }

    protected void initialTasks() {
        super.initialTasks();
        bucketCount = (Long) getUncheckedAttribute(null,ATTR_CRAWLER_COUNT);
    }
}