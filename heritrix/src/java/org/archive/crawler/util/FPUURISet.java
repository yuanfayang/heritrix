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
 * MemFPUURISet.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.util.ArchiveUtils;
import org.archive.util.LongFPSet;

import st.ata.util.FPGenerator;

/**
 * UURISet which only stores 64-bit UURI fingerprints, using an
 * internal LongFPSet instance. (This internal instance may be
 * disk or memory based.)
 *
 * @author gojomo
 *
 */
public class FPUURISet extends AbstractSet implements UURISet, Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(FPUURISet.class,1);

    private static Logger logger = Logger.getLogger("org.archive.crawler.util.FPUURISet");

    LongFPSet fpset;
    transient FPGenerator fpgen = FPGenerator.std64;

    /**
     * @param fpset
     */
    public FPUURISet(LongFPSet fpset) {
        this.fpset = fpset;
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#count()
     */
    public long count() {
        return fpset.count();
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.UURI)
     */
    public boolean contains(UURI u) {
        long fp = fpgen.fp(u.toString());
        boolean retVal = fpset.contains(fp);
        if(retVal) {
            logger.finest("Already contains "+fp+" "+u);
        }
        return retVal;
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.CandidateURI)
     */
    public boolean contains(CandidateURI curi) {
        return contains(curi.getUURI());
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.UURI)
     */
    public void add(UURI u) {
        long fp = fpgen.fp(u.toString());
        logger.finest("Adding "+fp+" "+u);
        fpset.add(fp);
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.UURI)
     */
    public void remove(UURI u) {
        fpset.remove(fpgen.fp(u.toString()));
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.CrawlURI)
     */
    public void add(CandidateURI curi) {
        add(curi.getUURI());
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.CrawlURI)
     */
    public void remove(CandidateURI curi) {
        remove(curi.getUURI());
    }

    /**
     * @see java.util.AbstractCollection#size()
     */
    public int size() {
        return (int)count();
    }

    /**
     * @see java.util.AbstractCollection#iterator()
     */
    public Iterator iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#quickContains(org.archive.crawler.datamodel.UURI)
     */
    public boolean quickContains(UURI u) {
        long fp = fpgen.fp(u.toString());
        boolean retVal = fpset.quickContains(fp);
        if(retVal) {
            logger.finest("Already quickContains "+fp+" "+u);
        }
        return retVal;
    }

    /**
     * @see org.archive.crawler.datamodel.UURISet#quickContains(org.archive.crawler.datamodel.CandidateURI)
     */
    public boolean quickContains(CandidateURI curi) {
        return quickContains(curi.getUURI());
    }
    
    // custom serialization
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    	stream.defaultReadObject();
    	fpgen = FPGenerator.std64;
    }

}
