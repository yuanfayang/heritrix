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
 * HopsFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.Filter;

/**
 * Accepts all urls passed in with a path depth
 * less or equal than the max-path-depth
 * value.
 *
 * @author Igor Ranitovic
 *
 */
public class PathDepthFilter extends Filter {
    public static final String ATTR_INVERTED = "path-deeper-than";
    public static final String ATTR_MAX_PATH_DEPTH = "max-path-depth";
    Integer maxPathDepth = new Integer(Integer.MAX_VALUE);
    char slash = '/';
    String path;

    /**
     * @param name
     */
    public PathDepthFilter(String name) {
        super(name, "Path depth filter");
        addElementToDefinition(new SimpleType(ATTR_MAX_PATH_DEPTH, "Max path depth", maxPathDepth));
        addElementToDefinition(
            new SimpleType(
                ATTR_INVERTED,
                "Allow only paths deeper then max path depth. \nNormally max path" +
                "depth means that only URIs with shorter paths are accepted," +
                "setting this to true means that max path depth becomes (in" +
                "effect) minimum path depth.",
                new Boolean(false)));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        if(o instanceof CandidateURI) {
            path = ((CandidateURI)o).getUURI().getPath();
        } else if (o instanceof UURI ){
            path = ((UURI)o).getPath();
        }else{
            path = null;
        }

        if (path == null){
            return true;
        }

        int count = 0;
        for (int i = path.indexOf(slash);
            i != -1;
            i = path.indexOf(slash, i + 1)) {
            count++;
        }
        if (o instanceof CrawlURI) {
            try {
                maxPathDepth = (Integer) getAttribute(ATTR_MAX_PATH_DEPTH, (CrawlURI) o);
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
        }
        return (count <= maxPathDepth.intValue());
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#applyInversion()
     */
    protected boolean applyInversion(CrawlURI curi) {
       boolean inverter = false;
       try {
           inverter = ((Boolean) getAttribute(ATTR_INVERTED, curi)).booleanValue();
       } catch (AttributeNotFoundException e) {
           logger.severe(e.getMessage());
       }
       return inverter;
    }
}
