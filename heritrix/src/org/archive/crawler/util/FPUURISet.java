/*
 * MemFPUURISet.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.AbstractSet;
import java.util.Iterator;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
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
public class FPUURISet extends AbstractSet implements UURISet {
	LongFPSet fpset;
	FPGenerator fpgen = FPGenerator.std64;
	
	/**
	 * 
	 */
	public FPUURISet(LongFPSet fpset) {
		this.fpset = fpset;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#count()
	 */
	public long count() {
		return fpset.count();
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.UURI)
	 */
	public boolean contains(UURI u) {
		return fpset.contains(fpgen.fp(u.getUri().toASCIIString()));
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.CandidateURI)
	 */
	public boolean contains(CandidateURI curi) {
		return contains(curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.UURI)
	 */
	public void add(UURI u) {
		fpset.add(fpgen.fp(u.getUri().toASCIIString()));
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.UURI)
	 */
	public void remove(UURI u) {
		fpset.remove(fpgen.fp(u.getUri().toASCIIString()));
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void add(CandidateURI curi) {
		add(curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void remove(CandidateURI curi) {
		remove(curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#size()
	 */
	public int size() {
		return (int)count();
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#iterator()
	 */
	public Iterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
