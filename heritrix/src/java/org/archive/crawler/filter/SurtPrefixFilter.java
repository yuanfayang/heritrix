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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.SurtPrefixSet;
import org.archive.util.SURT;
/**
 * A filter which tests a URI against a set of SURT 
 * prefixes, and if the URI's prefix is in the set,
 * returns the chosen true/false accepts value. 
 * 
 * @author gojomo
 */
public class SurtPrefixFilter extends Filter {
    public static final String ATTR_SURTS_SOURCE_FILE = "surts-source-file";
    public static final String ATTR_MATCH_RETURN_VALUE = "if-match-return";

    SurtPrefixSet surtPrefixes = null;
    
    /**
     * @param name
     */
    public SurtPrefixFilter(String name) {
        super(name, "SURT prefix filter");
        addElementToDefinition(
            new SimpleType(ATTR_MATCH_RETURN_VALUE, "What to return when " +
                    "a prefix matches.\n", new Boolean(true)));
        addElementToDefinition(
                new SimpleType(ATTR_SURTS_SOURCE_FILE, "Source file from which to " +
                        "read SURT prefixes.", ""));
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
     */
    protected synchronized boolean innerAccepts(Object o) {
        if (surtPrefixes == null) {
            readPrefixes();
        }
        String s = SURT.fromURI(asString(o));
        // also want to treat https as http
        if(s.startsWith("https:")) {
            s = "http:"+s.substring(6);
        }
        // TODO: consider other cases of scheme-indifference?
        return surtPrefixes.containsPrefixOf(s);
    }

    private void readPrefixes() {
        surtPrefixes = new SurtPrefixSet(); 
        String sourcePath = (String) getAttributeOrNull(ATTR_SURTS_SOURCE_FILE,
                (CrawlURI) null);
        File source = new File(sourcePath);
        if (!source.isAbsolute()) {
            source = new File(getSettingsHandler().getOrder()
                    .getController().getDisk(), sourcePath);
        }
        FileReader fr = null;
        try {
            fr = new FileReader(source);
            try {
                surtPrefixes.importFrom(fr);
            } finally {
                fr.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    /**
     * Re-read prefixes after a settings update.
     * 
     */
    public synchronized void kickUpdate() {
        super.kickUpdate();
        // TODO: make conditional on file having actually changed,
        // perhaps by remembering mod-time
        readPrefixes();
    }
}
