/* 
 * CandidateURI.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;
import java.io.Serializable;

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
public class CandidateURI implements Serializable {
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
	// mostly for debugging; will be a CrawlURI when memory is no object
	// just a string or null when memory is an object (configurable)
	Object via;
	
	
	/**
	 * @param u
	 */
	public CandidateURI(UURI u) {
		uuri = u;
	}
	
	/**
	 * @param uriString
	 */
	public CandidateURI(String s){
		try{
			setUURI(UURI.createUURI(s));
		}catch(Exception e){
			setUURI(null);
		}
	}

	
	/**
	 * @param b
	 */
	public void setIsSeed(boolean b) {
		isSeed=b;
		setPathFromSeed("");
		setVia("");
	}

	/**
	 * 
	 */
	public UURI getUURI() {
		return uuri;
	}
	
	/**
	 * @param u
	 */
	private void setUURI(UURI u) {
		uuri=u;
	}

	/**
	 * @return
	 */
	public boolean getIsSeed() {
		return isSeed;
	}
	
	/**
	 * 
	 */
	public int getScopeVersion() {
		return inScopeVersion;
	}
	
	/**
	 * @param i
	 */
	public void setScopeVersion(int i) {
		inScopeVersion = i;
	}
	/**
	 * @return
	 */
	public String getPathFromSeed() {
		return pathFromSeed;
	}

	/**
	 * @return
	 */
	public Object getVia() {
		return via;
	}

	/**
	 * @param string
	 */
	public void setPathFromSeed(String string) {
		pathFromSeed = string;
	}

	/**
	 * @param object
	 */
	public void setVia(Object object) {
		via = object;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CandidateURI("+getUURI()+")";
	}
	
	
	private void writeObject(java.io.ObjectOutputStream out)
		 throws IOException {
		 flattenVia();
		 out.defaultWriteObject();
	}

	/**
	 * 
	 */
	private void flattenVia() {
		if (via instanceof String) {
			// already OK
			return;
		}
		if (via instanceof UURI) {
			via = ((UURI)via).getUri().toString();
			return;
		}
		if (via instanceof CandidateURI) {
			via = ((CandidateURI)via).getUURI().getUri().toString();
			return;
		}
		via = via.toString();
	}
}
