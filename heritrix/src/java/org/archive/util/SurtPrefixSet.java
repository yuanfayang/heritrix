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
import java.io.IOException;
import java.io.Reader;
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
     * @return
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
     * Read a set of SURT prefixes from a file; return sorted and 
     * with redundant entries removed.
     * 
     * @param attribute
     * @return
     * @throws IOException
     */
    public void importFrom(Reader r) throws IOException {
        BufferedReader reader = new BufferedReader(r);
        String read;
        while ((read = reader.readLine()) != null) {
            read = read.trim();
            if (read.length() == 0 || read.startsWith("#")) {
                continue;
            }
            // TODO: handle other cruft or end-of-line comments?
            add(read);
        }
    }
}
