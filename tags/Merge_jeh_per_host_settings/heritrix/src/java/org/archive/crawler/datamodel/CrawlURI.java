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
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.basic.URIStoreable;
import org.archive.crawler.fetcher.FetchDNS;
import org.archive.crawler.framework.Processor;

import st.ata.util.AList;
import st.ata.util.HashtableAList;


/**
 * Represents a candidate URI and the associated state it 
 * collects as it is crawled.
 * 
 * Core state is in instance variables, but a flexible 
 * attribute list is also available. 
 * 
 * Should only be instantiated via URIStore.getCrawlURI(...), 
 * which will assure only one CrawlURI can exist per 
 * UURI within a distinct "crawler".
 * 
 * @author Gordon Mohr
 */
public class CrawlURI extends CandidateURI
	implements URIStoreable, CoreAttributeConstants, FetchStatusCodes {
	// INHERITED FROM CANDIDATEURI
	// uuri: core identity: the "usable URI" to be crawled
	// isSeed
	// inScopeVersion
	// pathFromSeed
	// via 
	
	// Scheduler lifecycle info
	private Object state;   // state within scheduling/store/selector
	private long wakeTime; // if "snoozed", when this CrawlURI may awake
	private long dontRetryBefore = -1;

	// Processing progress
	Processor nextProcessor;
	private int fetchStatus = 0;	// default to unattempted
	private int deferrals = 0;     // count of postponements for prerequisites
	private int fetchAttempts = 0; // the number of fetch attempts that have been made	
	private int threadNumber;

	// flexible dynamic attributes
	private AList alist = new HashtableAList();

	// dynamic context
	private int linkHopCount = -1; // from seeds
	private int embedHopCount = -1; // from a sure link; reset upon any link traversal

	// User agent to masquerade as when crawling this URI. If null, globals should be used
	private String userAgent = null;
	
    // Once a link extractor has finished processing this curi this will be set as true
    private boolean linkExtractorFinished = false;
    
////////////////////////////////////////////////////////////////////
	CrawlServer server;

	private long contentSize = -1;
	private long contentLength = -1;
	
	/**
	 * @param uuri
	 */
	public CrawlURI(UURI uuri) {
		super(uuri);
	}

		/**
	 * @param caUri
	 */
	public CrawlURI(CandidateURI caUri) {
		super(caUri.getUURI());
		setIsSeed(caUri.getIsSeed());
		setPathFromSeed(caUri.getPathFromSeed());
		setVia(caUri.getVia());
	}

	/**
	 * Set the time this curi is considered expired (and thus must be refetched)
	 * to 'expires'.  This function will set the time to an arbitrary value.
	 * @param expires
	 */
	public void setDontRetryBefore(long expires){
		dontRetryBefore = expires;
	}
	
	public long getDontRetryBefore(){
		return dontRetryBefore;
	}
	
	/**
	 * Returns a boolean representing the status of the content in terms 
	 * of expiration date.  If the content is considered expired dontFetchYet()
	 * returns false.
	 * @return A boolean representing the status of the content in terms 
	 */
	public boolean dontFetchYet(){
		if(dontRetryBefore > System.currentTimeMillis()){
			return true;
		}
		return false;
	}

	

	
	
	public int getFetchStatus(){
		return fetchStatus;
	}
	
	public void setFetchStatus(int newstatus){
		fetchStatus = newstatus;
	}
	
	public int getFetchAttempts(){
		return fetchAttempts;
	}
	
	public int incrementFetchAttempts(){
		return fetchAttempts++;
	}

	 public Processor nextProcessor() {   
			 return nextProcessor;   
	 }   
	 /**   
	  * @param processor   
	  */   
	 public void setNextProcessor(Processor processor) {   
			 nextProcessor = processor;   
	 } 

	/**
	 * @return Token (usually the hostname) which indicates
	 * what "class" this CrawlURI should br grouped with,
	 * for the purposes of ensuring only one item of the
	 * class is processed at once, all items of the class
	 * are held for a politeness period, etc.
	 */
	public Object getClassKey() {
		//return host.getHostname();
		
		String scheme = getUURI().getUri().getScheme();
		if (scheme.equals("dns")){
			return FetchDNS.parseTargetDomain(this);
		}
		String host = getUURI().getUri().getHost();
		if (host == null) {
			String authority =  getUURI().getUri().getAuthority();
            if(authority == null) {
                // let it be its own key
                return getUURI().getUriString();
            } else {
                return authority;
            }
		} else {
			return host;
		} 
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getStoreState()
	 */
	public Object getStoreState() {
		return state;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setStoreState(java.lang.Object)
	 */
	public void setStoreState(Object s) {
		state = s;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getWakeTime()
	 */
	public long getWakeTime() {
		return wakeTime;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
	 */
	public void setWakeTime(long w) {
		wakeTime = w;
	}

	public AList getAList() {
		return alist;
	}
	
	
	/**
	 * Return the associated CrawlServer
	 * 
	 * @return server
	 */
	public CrawlServer getServer() {
		return server;
	}

	/**
	 * @param host
	 */
	public void setServer(CrawlServer host) {
		this.server = host;
	}

//	/**
//	 * 
//	 */
//	public void cancelFurtherProcessing() {
//		nextProcessor = null;
//	}

	/**
	 * @param stringOrUURI
	 */
	public void setPrerequisiteUri(Object stringOrUURI) {
		alist.putObject(A_PREREQUISITE_URI,stringOrUURI);
	}

	public Object getPrerequisiteUri() {
		return alist.getObject(A_PREREQUISITE_URI);
	}

	/**
	 * Method to set a URI that has to be fetched before fetching this URI 
	 * regardles if it has been fetched before. Setting this also implies
	 * that the prerequisite URI will be scheduled for crawl before any other
	 * waiting URIs for the same host.
	 * 
	 * This method is used to refetch any expired robots.txt or dns-lookups.
	 * 
	 * @param stringOrUURI The URI that is a prerequisite for this URI to be
	 * fetched
	 */
	public void setForcedPrerequisiteUri(Object stringOrUURI) {
		alist.putObject(A_PREREQUISITE_URI,stringOrUURI);
		alist.putObject(A_FORCED_PREREQUISITE_URI, "true");
	}

	/**
	 * Is the prerequisite URI for this URI a forced prerequisite
	 * 
	 * @return true if fetching this URI's prerequisite URI should be done regardless
	 * of if it has been fetched before
	 */
	public boolean hasForcedPrerequisiteUri() {
		String force = (String) alist.getObject(A_FORCED_PREREQUISITE_URI);
		return (force != null && force.equals("true"));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlURI("+getUURI()+")";
	}
	
	public String getContentType(){
		if (getAList().containsKey(A_CONTENT_TYPE)) {
		 	return getAList().getString(A_CONTENT_TYPE);
		} else {
			return null;
		}
	}

	/**
	 * @param i
	 */
	public void setThreadNumber(int i) {
		threadNumber = i;
	}

	public int getThreadNumber() {
		return threadNumber;
	}

	public void incrementDeferrals() {
		deferrals++;
	}

	public int getDeferrals() {
		return deferrals;
	}

	public void stripToMinimal() {
		alist = null;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getSortFallback()
	 */
	public String getSortFallback() {
		return uuri.toString();
	}

	/**
	 * @param u
	 */
	public void addEmbed(String u) {
		addToNamedSet(A_HTML_EMBEDS, u);
	}

	private void addToNamedSet(String key, Object o) {
		Set s;
		if(!alist.containsKey(key)) {
			s = new HashSet();
			alist.putObject(key, s);
		} else {
			s = (Set)alist.getObject(key);
		}
		s.add(o);
	}

	/**
	 * @param u
	 */
	public void addLink(String u) {
		addToNamedSet(A_HTML_LINKS, u);
	}

	/**
	 * 
	 */
	public void reconstitute() {
		if (alist == null) {
			alist = new HashtableAList();
		}
		setDontRetryBefore(-1);
	}
	
	/** Get the size in bytes of this URI's content.  This may be set
	 *  at any time by any class and therefor should not be trusted.  Primarily
	 *  it exists to ease the calculation of statistics.
	 * @return contentSize
	 */
	public long getContentSize(){
		return contentSize;
	}
	
	public void setAList(AList a){
		alist = a;
	}

	/**
	 * Make note of a non-fatal error, local to a particular Processor,
	 * which should be logged somewhere, but allows processing to continue.
	 * 
	 * @param processorName
	 * @param ex
	 * @param message
	 */
	public void addLocalizedError(String processorName, Exception ex, String message) {
		List localizedErrors;
		if(alist.containsKey(A_LOCALIZED_ERRORS)) {
			localizedErrors = (List) alist.getObject(A_LOCALIZED_ERRORS);
		} else {
			localizedErrors = new ArrayList();
			alist.putObject(A_LOCALIZED_ERRORS,localizedErrors);
		}
		
		localizedErrors.add(new LocalizedError(processorName, ex, message));
	}

    public void addAnnotation(String annotation) {
        String annotations;
        if(alist.containsKey(A_ANNOTATIONS)) {
            annotations = alist.getString(A_ANNOTATIONS);
        } else {
            annotations = "";
        }
        
        annotations += ","+annotation;
        alist.putString(A_ANNOTATIONS,annotations);
    }
//	/**
//	 * @param sourceCuri
//	 */
//	public void setViaLinkFrom(CrawlURI sourceCuri) {
//		via = sourceCuri;
//		// reset embedCount -- but only back to 1 if >0, so special embed handling still applies
//		embedHopCount = (embedHopCount > 0) ? 1 : 0;
//		int candidateLinkHopCount = sourceCuri.getLinkHopCount()+1;
//		if (linkHopCount == -1) {
//			linkHopCount = candidateLinkHopCount;
//			return;
//		}
//		if (linkHopCount > candidateLinkHopCount) {
//			linkHopCount = candidateLinkHopCount; 
//		}
//	}
	
//	/**
//	 * @param sourceCuri
//	 */
//	public void setViaEmbedFrom(CrawlURI sourceCuri) {
//		via = sourceCuri;
//		int candidateLinkHopCount = sourceCuri.getLinkHopCount();
//		if (linkHopCount == -1) {
//			linkHopCount = candidateLinkHopCount;
//		} else if (linkHopCount > candidateLinkHopCount) {
//			linkHopCount = candidateLinkHopCount; 
//		}
//		int candidateEmbedHopCount = sourceCuri.getEmbedHopCount()+1;
//		if (embedHopCount == -1) {
//			embedHopCount = candidateEmbedHopCount;
//		} else if (embedHopCount > candidateEmbedHopCount) {
//			embedHopCount = candidateEmbedHopCount; 
//		}
//	}

	
/*	public boolean isFubared(){
		return ( fetchStatus < 0 && numberOfFetchAttempts >= 3);
	}*/

	
	public int getEmbedHopCount() {
		return embedHopCount;
	}

	public int getLinkHopCount() {
		return linkHopCount;
	}

	public void markAsSeed() {
		linkHopCount = 0;
		embedHopCount = 0;
	}

	/**
	 * Get the user agent to use for crawling this URI.
	 * 
	 * If null the global setting should be used.
	 * 
	 * @return user agent or null
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Set the user agent to use when crawling this URI.
	 * 
	 * If not set the global settings should be used.
	 * 
	 * @param string user agent to use
	 */
	public void setUserAgent(String string) {
		userAgent = string;
	}

	/**
	 * @param processor
	 */
	public void skipToProcessor(Processor processor) {
		setNextProcessor(processor);
	}

	/**
	 * For completed HTTP transactions, the length of the content-body
	 * (as given by the header or calculated)
	 * 
	 * @return The length of the content-body (as given by the header or
     * calculated).
	 * 
	 */
	public long getContentLength() {
		if (contentLength<0) {
			GetMethod get = (GetMethod) getAList().getObject(A_HTTP_TRANSACTION);
			//if (get.getResponseHeader("Content-Length")!=null) {
			//	contentLength = Integer.parseInt(get.getResponseHeader("Content-Length").getValue());
			//} else {
				contentLength = get.getHttpRecorder().getResponseContentLength();
			//}
		}
		return contentLength;
	}

	/**
	 * @param l
	 */
	public void setContentSize(long l) {
		contentSize = l;
	}

	/**
	 * @param string
	 */
	public void addSpeculativeEmbed(String string) {
		addToNamedSet(A_HTML_SPECULATIVE_EMBEDS,string);
	}

	/**
	 * @param string
	 */
	public void addCSSLink(String string) {
		addToNamedSet(A_CSS_LINKS, string);		
	}
    
    /**
     * If true then a link extractor has already claimed this CrawlURI and
     * performed link extraction on it. This does not preclude other link
     * extractors that may have an interest in this CrawlURI from also doing
     * link extraction.
     * @return True if a processor has performed link extraction on this CrawlURI
     * 
     * @see #linkExtractorFinished()
     */
    public boolean hasBeenLinkExtracted(){
        return linkExtractorFinished;
    }
    
    /**
     * Note that link extraction has been performed on this CrawlURI. A processor
     * doing link extraction should invoke this method once it has finished it's
     * work. It should invoke it even if no links are extracted. It should only 
     * invoke this method if the link extraction was performed on the document
     * body (not the HTTP headers etc.).
     * 
     * @see #hasBeenLinkExtracted()
     */
    public void linkExtractorFinished(){
        linkExtractorFinished = true;
    }
}
