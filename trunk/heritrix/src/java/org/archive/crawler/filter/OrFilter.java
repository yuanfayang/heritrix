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
 * OrFilter.java
 * Created on Nov 13, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.Filter;

/**
 * OrFilter allows any number of other filters to be set up
 * inside it, as <filter> child elements. If any of those
 * children accept a presented object, the OrFilter will
 * also accept it.
 *
 * @author gojomo
 *
 */
public class OrFilter extends Filter {
    public static final String ATTR_INVERTED = "make-filter-XOR";

    private MapType filters;

    public OrFilter(String name, String description) {
        this(name);
        setDescription(description);
    }

    /**
     * @param name
     */
    public OrFilter(String name) {
        super(
            name,
            "OR Filter. \nA filter that serves as a placeholder for other filters who's functionality should be logically OR'ed together.");
        filters =
            new MapType(
                "filters",
                "This is a list of filters who's functionality should be logically or'ed together by the OrFilter.",
                Filter.class);
        addElementToDefinition(
            new SimpleType(
                ATTR_INVERTED,
                "Turn the filter into an XOR filter. \nIf true, instead of "
                    + "filtering out anything that any of the filters added to it "
                    + "matches, it will only filter out URIs that none of them "
                    + "matches.",
                new Boolean(false)));
        addElementToDefinition(filters);
    }

    protected boolean innerAccepts(Object o) {
        if (isEmpty(o)) {
            return true;
        }
        Iterator iter = iterator(o);
        while(iter.hasNext()) {
            Filter f = (Filter)iter.next();
            if( f.accepts(o) ) {
                return true;
            }
        }
        return false;
    }

    public void addFilter(CrawlerSettings settings, Filter f) {
        try {
            filters.addElement(settings, f);
        } catch (InvalidAttributeValueException e) {
            logger.severe(e.getMessage());
        }
    }

    public boolean isEmpty(Object o) {
        return filters.isEmpty(getSettingsFromObject(o));
    }

    public Iterator iterator(Object o) {
        return filters.iterator(getSettingsFromObject(o));
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
