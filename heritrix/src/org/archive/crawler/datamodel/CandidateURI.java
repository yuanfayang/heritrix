/* 
 * CandidateURI.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * A URI, discovered or passed-in, that may be scheduled (and
 * thus become a CrawlURI). Contains just the fields necessary
 * to perform quick in-scope analysis. 
 * 
 * A flexible AttributeList, as in CrawlURI, could be added,
 * possibly even subsuming the existing fields.
 * 
 * @author Gordon Mohr
 */
public class CandidateURI {
	/** Usuable URI under consideration */
	UURI uuri;
	/** Seed status */
	boolean isSeed = false;
	/** Latest version of the inScope definition met; (zero if not)*/
	int inScopeVersion = -1; 
	/** String of letters indicating how this URI was reached from a seed */
	// P precondition
	// R redirection
	// E embedded (as frame, src, link, codebase, etc.)
	// L link
	// for example LLLE (an embedded image on a page 3 links from seed)
	String pathFromSeed; 
	/** Where this URI was (presently) discovered */
	UURI precursorUuri;
	
	
	/**
	 * @param u
	 */
	public CandidateURI(UURI u) {
		uuri = u;
	}
	
	/**
	 * @param b
	 */
	public void setSeed(boolean b) {
		isSeed=b;
	}
	
}
