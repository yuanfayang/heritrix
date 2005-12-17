/* BloomUriUniqFilter
*
* $Id$
*
* Created on June 21, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.util;

import java.io.Serializable;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.util.BloomFilter;
import org.archive.util.BloomFilter32bitSplit;
import org.archive.util.BloomFilter32bp2Split;


/**
 * A MG4J BloomFilter-based implementation of an AlreadySeen list.
 *
 * This implementation performs adequately without blowing out
 * the heap. See
 * <a href="http://crawler.archive.org/cgi-bin/wiki.pl?AlreadySeen">AlreadySeen</a>.
 *
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class BloomUriUniqFilter extends SetBasedUriUniqFilter implements Serializable {
    private static Logger LOGGER =
        Logger.getLogger(BloomUriUniqFilter.class.getName());

    BloomFilter bloom; // package access for testing convenience
    protected int expected_n; // remember bloom contruction param

    protected static final String EXPECTED_SIZE_KEY = ".expected-size";
    protected static final String HASH_COUNT_KEY = ".hash-count";

    // these defaults create a bloom filter that is
    // 1.44*125mil*22/8 ~= 495MB in size, and at full
    // capacity will give a false contained indication
    // 1/(2^22) ~= 1 in every 4 million probes
    private static final int DEFAULT_EXPECTED_SIZE = 125000000; // 125 million
    private static final int DEFAULT_HASH_COUNT = 22; // 1 in 4 million false pos

    /**
     * Default constructor
     */
    public BloomUriUniqFilter() {
        super();
        String ns = System.getProperty(this.getClass().getName() + EXPECTED_SIZE_KEY);
        int n = (ns == null) ? DEFAULT_EXPECTED_SIZE : Integer.parseInt(ns);
        String ds = System.getProperty(this.getClass().getName() + HASH_COUNT_KEY);
        int d = (ds == null) ? DEFAULT_HASH_COUNT : Integer.parseInt(ds);
        initialize(n,d);
    }

    /**
     * Constructor.
     *
     * @param n the expected number of elements.
     * @param d the number of hash functions; if the filter adds not more
     * than <code>n</code> elements, false positives will happen with
     * probability 2<sup>-<var>d</var></sup>.
     */
    public BloomUriUniqFilter( final int n, final int d ) {
        super();
        initialize(n, d);
    }

    /**
     * Initializer shared by constructors.
     *
     * @param n the expected number of elements.
     * @param d the number of hash functions; if the filter adds not more
     * than <code>n</code> elements, false positives will happen with
     * probability 2<sup>-<var>d</var></sup>.
     */
    protected void initialize(final int n, final int d) {
        this.expected_n = n;
        bloom = new BloomFilter32bitSplit(n,d);
    }

    public void forget(String canonical, CandidateURI item) {
        // TODO? could use in-memory exception list of currently-forgotten items
        LOGGER.severe("forget(\""+canonical+"\",CandidateURI) not supported");
    }

    
    protected boolean setAdd(CharSequence uri) {
        boolean added = bloom.add(uri);
        // check if bloom is operating past expected range (and thus
        // giving more false indications a string was already contained
        // than was intended); if so, offer a warning on every 10000th
        // increment
        if( added && (count() > expected_n) && (count() % 10000 == 0)) {
            LOGGER.warning(count()+" beyond expected limit "+expected_n);
        }
        return added;
    }

    protected long setCount() {
        return bloom.size();
    }

    protected boolean setRemove(CharSequence uri) {
        throw new UnsupportedOperationException();
    }
}
