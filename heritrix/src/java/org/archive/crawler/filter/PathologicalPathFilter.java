/* PathologicalFilter
 * 
 * $Id$
 * 
 * Created on Feb 20, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.filter;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;

/** Checks if a URI contains a repated pattern.
 * 
 * This filter is checking if a pattern is repeated a specific number of times.
 * The use is to avoid crawler traps where the server adds the same pattern to 
 * the requested URI like:
 * http://host/img/img/img/img....
 *  
 * @author John Erik Halse
 */
public class PathologicalPathFilter extends URIRegExpFilter {

    public static final String ATTR_REPETITIONS = "repetitions";

    public static final Integer DEFAULT_REPETITIONS = new Integer(3);

    /** Constructs a new PathologicalPathFilter.
     * 
     * @param name the name of the filter.
     */
    public PathologicalPathFilter(String name) {
        super(name);
        setDescription("Pathological path filter. The Pathologicalpath filter" +
                " is used to avoid crawler traps by adding a constraint on" +
                " how many times a pattern in the URI could be repeated.");

        Type type = getElementFromDefinition(ATTR_INVERTED);
        type.setTransient(true);

        type = getElementFromDefinition(ATTR_INVERTED);
        type.setTransient(true);

        addElementToDefinition(new SimpleType(ATTR_REPETITIONS,
                "Number of times the pattern should be allowed to occur.",
                DEFAULT_REPETITIONS));
    }

    /** Construct the regexp string to be matched aginst the URI.
     * 
     * @param o an object to extract a URI from.
     * @return the regexp pattern.
     */
    protected String getRegexp(Object o) {
        int rep;
        try {
            rep = ((Integer) getAttribute(getSettingsFromObject(o),
                    ATTR_REPETITIONS)).intValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;
        }

        if (rep == 0) return null;
        
        String regexp = ".*/(.*/)\\1{" + (rep - 1) + ",}.*";
        return regexp;
    }
}
