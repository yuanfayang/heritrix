/*
 * UURISet.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.Set;


/**
 * Represents a collection of URIs, without duplicates.
 * 
 * Implementors may also choose to include various sorts
 * of "fuzzy" contains tests, to determine when another
 * URI which is "close enough" (eg same except with different
 * in-URI session ID) is included.
 * 
 * @author gojomo
 *
 */
public interface UURISet extends Set {
	public long count();
	public boolean contains(UURI u);
	public boolean contains(CandidateURI curi);
	
	/**
	 * Do a contains() check that doesn't require laggy
	 * activity (eg disk IO). If this returns true, 
	 * UURI is definitely contained; if this returns 
	 * false, UURI *MAY* still be contained -- must use
	 * full-cost contains() to be sure. 
	 * 
	 * @param u
	 * @return
	 */
	public boolean quickContains(UURI u);
	public boolean quickContains(CandidateURI curi);

	public void add(UURI u);
	public void remove(UURI u);
	
	public void add(CandidateURI curi); // convenience; only really adds the UURI
	public void remove(CandidateURI curi); // convenience; only really adds the UURI

}
