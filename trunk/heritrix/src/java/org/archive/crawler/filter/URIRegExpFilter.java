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

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.TextUtils;


/**
 * Compares passed object -- a CrawlURI, UURI, or String --
 * against a regular expression, accepting matches.
 *
 * @author Gordon Mohr
 */
public class URIRegExpFilter extends Filter {
    public static final String ATTR_REGEXP = "regexp";
    public static final String ATTR_MATCH_RETURN_VALUE = "if-match-return";

    /**
     * @param name Filter name.
     */
    public URIRegExpFilter(String name) {
        super(name, "URI regexp filter.");
        addElementToDefinition(
            new SimpleType(ATTR_MATCH_RETURN_VALUE, "What to return when" +
                    " regular expression matches. \n", new Boolean(true)));
        addElementToDefinition(
                new SimpleType(ATTR_REGEXP, "Java regular expression.", ""));
    }

	public URIRegExpFilter(String name, String regexp) {
		super(name, "URI regexp filter.");
		addElementToDefinition(new SimpleType(ATTR_MATCH_RETURN_VALUE,
	        "What to return when" + " regular expression matches. \n",
		    new Boolean(true)));
		addElementToDefinition(new SimpleType(ATTR_REGEXP,
		    "Java regular expression.", regexp)); 
	}

    protected boolean innerAccepts(Object o) {
        String input = null;
        input = asString(o);
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
            return (String) getAttribute(o, ATTR_REGEXP);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;  // Basically the filter is inactive if this occurs.
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#applyInversion()
     */
    protected boolean returnTrueIfMatches(CrawlURI curi) {
       try {
           return ((Boolean) getAttribute(ATTR_MATCH_RETURN_VALUE, curi)).booleanValue();
       } catch (AttributeNotFoundException e) {
           logger.severe(e.getMessage());
           return true;
       }
    }
}
