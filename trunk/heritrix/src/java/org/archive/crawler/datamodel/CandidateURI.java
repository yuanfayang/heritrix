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
 * CandidateURI.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;
import java.io.Serializable;

import org.archive.util.Lineable;

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
public class CandidateURI implements Serializable, Lineable {
	private static final long serialVersionUID = -7152937921526560388L;
	
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
	// X speculative embed (as from javascript, some alternate-format extractors
	// L link
	// for example LLLE (an embedded image on a page 3 links from seed)
	String pathFromSeed; 
	/** Where this URI was (presently) discovered */
	// mostly for debugging; will be a CrawlURI when memory is no object
	// just a string or null when memory is an object (configurable)
	Object via;
	
	// Should a fetch be forced even though the URI is in the already included map
	private boolean forceFetch = false;
	
	
	/**
	 * @param u
	 */
	public CandidateURI(UURI u) {
		uuri = u;
	}
	
	/**
	 * @param uriString
	 */
//	public CandidateURI(String s){
//		try{
//			setUURI(UURI.createUURI(s));
//		}catch(Exception e){
//			setUURI(null);
//		}
//	}

	
	/**
	 * @param b
	 */
	public void setIsSeed(boolean b) {
		isSeed=b;
		setPathFromSeed("");
		setVia("");
	}

	/**
	 * @return UURI
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
	 * @return Whether seeded.
	 */
	public boolean getIsSeed() {
		return isSeed;
	}
	
	/**
	 * @return Scope version.
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

	public String getPathFromSeed() {
		return pathFromSeed;
	}

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
		 via = flattenVia();
		 out.defaultWriteObject();
	}

	/**
	 * 
	 */
	private String flattenVia() {
		if (via instanceof String) {
			// already OK
			return (String) via;
		}
		if (via instanceof UURI) {
			return ((UURI)via).getUriString();
		}
		if (via instanceof CandidateURI) {
			return ((CandidateURI)via).getUURI().getUriString();
		}
		return via.toString();
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Lineable#getLine()
	 */
	public String getLine() {
		return this.getClass().getName()
		        +" "+getUURI().getUriString()
		        +" "+pathFromSeed
		        +" "+flattenVia();
	}

	/**
     * @return URI String
     */
	public String getURIString() {
		return getUURI().getUriString();
	}

	/**
	 * Compares the domain of this CandidateURI with that of another CandidateURI
	 * 
	 * @param other The other CandidateURI
	 * 
	 * @return True if both are in the same domain, false otherwise.
	 */
	public boolean sameDomainAs(CandidateURI other) {
		String domain = getUURI().getHost();
		if (domain==null) return false;
		while(domain.lastIndexOf('.')>domain.indexOf('.')) {
			// while has more than one dot, lop off first segment
			domain = domain.substring(domain.indexOf('.')+1);
		}
		if(other.getUURI().getHost()==null ) {
			return false;
		}
		return other.getUURI().getHost().endsWith(domain);
	}
	
	/**
	 * If this method returns true, this URI should be fetched even though
	 * it already has been crawled. This also implies
	 * that this URI will be scheduled for crawl before any other waiting
	 * URIs for the same host.
	 * 
	 * This value is used to refetch any expired robots.txt or dns-lookups.
	 * 
	 * @return true if crawling of this URI should be forced
	 */
	public boolean forceFetch() {
		return forceFetch;
	}

	/**
	 * Method to signal that this URI should be fetched even though
	 * it already has been crawled. Setting this to true also implies
	 * that this URI will be scheduled for crawl before any other waiting
	 * URIs for the same host.
	 * 
	 * This value is used to refetch any expired robots.txt or dns-lookups.
	 * 
	 * @param b set to true to enforce the crawling of this URI
	 */
	public void setForceFetch(boolean b) {
		forceFetch = b;
	}
}
