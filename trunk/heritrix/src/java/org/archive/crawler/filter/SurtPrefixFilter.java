/* SurtPrefixFilter
*
* $Id$
*
* Created on Jul 22, 2004
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

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.SURT;

/**
 * A filter which tests a URI against a set of SURT 
 * prefixes, and if the URI's prefix is in the set,
 * returns the chosen true/false accepts value. 
 * 
 * @author gojomo
 */
public class SurtPrefixFilter extends Filter {
    public static final String ATTR_SOURCE_FILE = "source-file";
    public static final String ATTR_MATCH_RETURN_VALUE = "if-match-return";

    SortedSet surtPrefixes = null;
    
    /**
     * @param name
     */
    public SurtPrefixFilter(String name) {
        super(name, "SURT prefix filter");
        addElementToDefinition(
            new SimpleType(ATTR_MATCH_RETURN_VALUE, "What to return when " +
                    "a prefix matches.\n", new Boolean(true)));
        addElementToDefinition(
                new SimpleType(ATTR_SOURCE_FILE, "Source file from which to " +
                        "read SURT prefixes.", ""));
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        if (surtPrefixes == null) {
            readPrefixes();
        }
        String s = asString(o);
        SortedSet subset = surtPrefixes.headSet(s);
        return s.startsWith((String) subset.last());
    }

    /**
     * 
     */
    private void readPrefixes() {
        try {
            surtPrefixes = SURT.importPrefixSetFrom((String)getAttributeOrNull(ATTR_SOURCE_FILE,(CrawlURI)null));
        } catch (IOException e) {
            e.printStackTrace();
            // use empty set
            surtPrefixes = new TreeSet(); 
            throw new RuntimeException(e);
        }
    }
    
    
}
