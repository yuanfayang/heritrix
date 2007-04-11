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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.StateProvider;

import st.ata.util.FPGenerator;

/**
 * Maps URIs to one of N crawler names by applying a hash to the
 * URI's (possibly-transformed) classKey. 
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class HashCrawlMapper extends CrawlMapper {

    private static final long serialVersionUID = 2L;
    

    /**
     * Number of crawlers among which to split up the URIs. Their names are
     * assumed to be 0..N-1.
     */
    @Immutable
    final public static Key<Long> CRAWLER_COUNT = Key.make(1L);


    /**
     * A regex pattern to apply to the classKey, using the first match as the
     * mapping key. If empty (the default), use the full classKey.
     * 
     */
    final public static Key<Pattern> REDUCE_PREFIX_PATTERN = 
        Key.make(Pattern.compile("."));
    
//    /** replace pattern for reducing classKey */
//    public static final String ATTR_REPLACE_PATTERN = "replace-pattern";
//    public static final String DEFAULT_REPLACE_PATTERN = "";
 
    long bucketCount = 1;
//    String replacePattern = null;



    /**
     * Constructor.
     */
    public HashCrawlMapper() {
        super();
    }

    /**
     * Look up the crawler node name to which the given CrawlURI 
     * should be mapped. 
     * 
     * @param cauri CrawlURI to consider
     * @return String node name which should handle URI
     */
    protected String map(CrawlURI cauri) {
        // get classKey, via frontier to generate if necessary
        String key = getController().getFrontier().getClassKey(cauri);
        Pattern reducePattern = cauri.get(this, REDUCE_PREFIX_PATTERN);
        return mapString(key, reducePattern, bucketCount); 
    }

    public void initialTasks(StateProvider context) {
        super.initialTasks(context);
        bucketCount = context.get(this, CRAWLER_COUNT);
    }


    public static String mapString(String key, Pattern reducePattern,
            long bucketCount) {

        if (reducePattern != null) {
            Matcher matcher = reducePattern.matcher(key);
            if (matcher.find()) {
                key = matcher.group();
            }
        }
        long fp = FPGenerator.std64.fp(key);
        long bucket = fp % bucketCount;
        return Long.toString(bucket >= 0 ? bucket : -bucket);
    }
}