/* SURTPrefixSet
*
* $Id$
*
* Created on Jul 23, 2004
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
package org.archive.util;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Specialized TreeSet for keeping a set of String prefixes. 
 * 
 * Redundant prefixes (those that are themselves prefixed
 * by other set entries) are eliminated.
 * 
 * @author gojomo
 */
public class SurtPrefixSet extends TreeSet {

    /**
     * Test whether the given String is prefixed by one
     * of this set's entries. 
     * 
     * @param s
     * @return True if contains prefix.
     */
    public boolean containsPrefixOf(String s) {
        SortedSet sub = headSet(s+"\0");
        // because redundant prefixes have been eliminated,
        // only a single test is necessary
        return !sub.isEmpty() && s.startsWith((String)sub.last());
    }
    
    /** 
     * Maintains additional invariant: if one entry is a 
     * prefix of another, keep only the prefix. 
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o) {
        String s = (String) o;
        SortedSet sub = headSet(s);
        if (!sub.isEmpty() && s.startsWith((String)sub.last())) {
            // no need to add; prefix is already present
            return false;
        }
        boolean retVal = super.add(s);
        sub = tailSet(s+"\0");
        while(!sub.isEmpty() && ((String)sub.first()).startsWith(s)) {
            // remove redundant entries
            sub.remove(sub.first());
        }
        return retVal;
    }
    
    
    /**
     * Read a set of SURT prefixes from a reader source; keep sorted and 
     * with redundant entries removed.
     * 
     * @param r reader over file of SURT_format strings
     * @throws IOException
     */
    public void importFrom(Reader r) {
        BufferedReader reader = new BufferedReader(r);
        String s;
        
        LineReadingIterator iter = 
            new LineReadingIterator(
                    reader,
                    LineReadingIterator.COMMENT_LINE,
                    LineReadingIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                    LineReadingIterator.ENTRY);

        while (iter.hasNext()) {
            s = (String) iter.next();
            add(s);
        }
    }

    /**
     * @param r Where to read from.
     */
    public void importFromUris(Reader r) {
        BufferedReader reader = new BufferedReader(r);
        String s;
        
        LineReadingIterator iter = 
            new LineReadingIterator(
                    reader,
                    LineReadingIterator.COMMENT_LINE,
                    LineReadingIterator.NONWHITESPACE_ENTRY_TRAILING_COMMENT,
                    LineReadingIterator.ENTRY);

        while (iter.hasNext()) {
            s = (String) iter.next();
            // s is a URI (or even fragmentary hostname), not a SURT
            if(s.indexOf(':')==-1 || s.indexOf('.')<s.indexOf(':')) {
                // no scheme present; prepend "http://"
                s = "http://" + s;
            }
            // convert to full SURT
            s = SURT.fromURI(s);
            // truncate to implied prefix
            s = SurtPrefixSet.asPrefix(s);
            add(s);
        }
        
    }

    /**
     * Utility method for truncating a SURT that came from a 
     * full URI (as a seed, for example) into a prefix
     * for determining inclusion.
     * 
     * This involves: 
     * <pre>
     *    (1) removing the last path component, if any
     *        (anything after the last '/', if there are
     *        at least 3 '/'s)
     *    (2) removing a trailing ',)', if present, opening
     *        the possibility of proper subdomains and 
     *        numbered sibling domains like www3. (This
     *        means that the presence or absense of a
     *        trailing '/' after a hostname is significant
     *        for the how the SURT prefix is created, even
     *        though it is not signficant for the URI's
     *        treatment as a seed.)
     * </pre>
     *
     * @param s String to work on.
     * @return As prefix.
     */
    private static String asPrefix(String s) {
        // strip last path-segment, if more than 3 slashes
        s = s.replaceAll("^(.*//.*/)[^/]*","$1");
        // strip trailing ",)", if present
        s = s.replaceAll("^(.*),\\)","$1");       
        return s;
    }

    /**
     * @param fw
     * @throws IOException
     */
    public void exportTo(FileWriter fw) throws IOException {
        Iterator iter = this.iterator();
        while(iter.hasNext()) {
            fw.write((String)iter.next() + "\n");
        }
    }
}
