/* SupplementaryLinksScoper
 * 
 * $Id$
 *
 * Created on Oct 2, 2003
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
 *
 */
package org.archive.crawler.postprocessor;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.framework.Scoper;
import org.archive.crawler.settings.MapType;

/**
 * Run CandidateURI links carried in the passed CrawlURI through a filter
 * and 'handle' rejections.
 * Used to do supplementary processing of links after they've been scope
 * processed and ruled 'in-scope' by LinkScoper.  An example of
 * 'supplementary processing' would check that a Link is intended for
 * this host to crawl in a multimachine crawl setting. Configure filters to
 * rule on links.  Default handler writes rejected URLs to disk.  Subclass
 * to handle rejected URLs otherwise.
 * @author stack
 */
public class SupplementaryLinksScoper extends Scoper {
    private static Logger LOGGER =
        Logger.getLogger(SupplementaryLinksScoper.class.getName());
    
    public static final String ATTR_LINK_FILTERS = "link-filters";
    
    /**
     * Instance of filters to run.
     */
    private MapType filters = null;
    
    
    /**
     * @param name Name of this filter.
     */
    public SupplementaryLinksScoper(String name) {
        super(name, "SupplementaryLinksScoper. Use to do supplementary " +
            "processing of in-scope links.  Will run each link through " +
            "configured filters.  Must be run after LinkScoper.  " +
            "Handler logs rejected links. " +
            "Uses java logging. Logs to file named for this class. Change " +
            "java.util.logging.FileHandler.* properties in " +
            "heritrix.properties to change rotation and file size " +
            "characteristics.");
        
        this.filters = (MapType)addElementToDefinition(
            new MapType(ATTR_LINK_FILTERS, "Filters to apply to each " +
            "link carried by the passed CrawlURI.", Filter.class));
        this.filters.setExpertSetting(true);
    }

    protected void innerProcess(final CrawlURI curi) {
    }
}