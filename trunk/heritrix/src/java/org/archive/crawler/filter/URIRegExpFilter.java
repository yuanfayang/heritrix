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
 * RegExpFilter.java
 * Created on Apr 16, 2003
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
import org.archive.util.TextUtils;


/**
 * Compares passed object -- a CrawlURI, UURI, or String --
 * against a regular expression, accepting matches.
 *
 * @author Gordon Mohr
 */
public class URIRegExpFilter extends Filter {
    public static final String ATTR_REGEXP = "regexp";
    public static final String ATTR_INVERTED = "accept-matches";

    /**
     * @param name
     */
    public URIRegExpFilter(String name) {
        super(name, "URI regexp filter.");
        addElementToDefinition(
            new SimpleType(
                ATTR_INVERTED,
                "Only allow matches. \nIf set to true all URIs matching the "
                    + "regular expression will be allowed and only those that "
                    + "don't match will be filtered out. If false then URIs "
                    + "matching the regular expression will be filtered out "
                    + "others will be accepted.",
                new Boolean(false)));
        addElementToDefinition(
                new SimpleType(ATTR_REGEXP, "Java regular expression.", ""));
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        String input = null;
        // TODO consider changing this to ask o for its matchString
        if(o instanceof CandidateURI) {
            input = ((CandidateURI)o).getURIString();
        } else if (o instanceof UURI ){
            input = ((UURI)o).getUriString();
        } else {
            //TODO handle other inputs
            input = o.toString();
        }
        String regexp = getRegexp(o);
        if (regexp == null) {
            return false;
        } else {
            return TextUtils.matches(getRegexp(o), input);
        }
    }
    
    /** Get the regular expression string to match the URI against.
     * 
     * @param o the object for which the regular expression should be
     *          matched against.
     * @return the regular expression to match against.
     */
    protected String getRegexp(Object o) {
        try {
            return (String) getAttribute(getSettingsFromObject(o), ATTR_REGEXP);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;  // Basically the filter is inactive if this occurs.
        }
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
