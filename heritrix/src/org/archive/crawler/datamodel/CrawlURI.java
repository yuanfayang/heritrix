/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.archive.crawler.basic.URIStoreable;
import org.archive.crawler.fetcher.FetcherDNS;
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

////////////////////////////////////////////////////////////////////
	CrawlServer server;

	private int contentSize = -1;
	
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
	 * @return
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

	/** 
	  *   
	  */   
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
	 * Return a token (usually the hostname) which indicates
	 * what "class" this CrawlURI should br grouped with,
	 * for the purposes of ensuring only one item of the
	 * class is processed at once, all items of the class
	 * are held for a politeness period, etc.
	 * 
	 * @return
	 */
	public Object getClassKey() {
		//return host.getHostname();
		
		String scheme = getUURI().getUri().getScheme();
		if (scheme.equals("dns")){
			return FetcherDNS.parseTargetDomain(this);
		}
		String host = getUURI().getUri().getHost();
		if (host == null) {
			return getUURI().getUri().getAuthority();
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

	/**
	 * 
	 */
	public AList getAList() {
		return alist;
	}
	
	
	/**
	 * @return
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
	 * @param string
	 */
	public void setPrerequisiteUri(String string) {
		alist.putString(A_PREREQUISITE_URI,string);
	}

	/**
	 * 
	 */
	public String getPrerequisiteUri() {
		return alist.getString(A_PREREQUISITE_URI);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlURI("+getUURI()+")";
	}
	
	/**
	 * @
	 */
	public String getURIString(){
		return this.getUURI().getUri().toString();
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

	/**
	 * 
	 */
	public int getThreadNumber() {
		return threadNumber;
	}

	/**
	 * 
	 */
	public void incrementDeferrals() {
		deferrals++;
	}

	/**
	 * @return
	 */
	public int getDeferrals() {
		return deferrals;
	}

	/**
	 * 
	 */
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
	 * @param value
	 */
	public void addEmbed(String u) {
		addToNamedList(A_HTML_EMBEDS, u);
	}

	private void addToNamedList(String key, Object o) {
		List l;
		if(!alist.containsKey(key)) {
			l = new ArrayList();
			alist.putObject(key, l);
		} else {
			l = (List)alist.getObject(key);
		}
		l.add(o);
	}

	/**
	 * @param value
	 */
	public void addLink(String u) {
		addToNamedList(A_HTML_LINKS, u);
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
	
	/** Set the size of this URI's content in bytes for reporting */
	public void setContentSize(int size){
		contentSize = size;
	}
	
	/** Get the size in bytes of this URI's content.  This may be set
	 *  at any time by any class and therefor should not be trusted.  Primarily
	 *  it exists to ease the calculation of statistics.
	 * @return contentSize
	 */
	public int getContentSize(){
		return contentSize;
	}
	
	public void setAList(AList a){
		alist = a;
	}

	/**
	 * Make note of a non-fatal error which should be logged
	 * somewhere, but allows processing to continue.
	 * 
	 * @param string
	 * @param e
	 */
	public void addLocalizedError(String processorName, Exception ex, String message) {
		// TODO implement
		System.out.println("CrawlURI.addLocalizedError() says: \"Implement me!\"");
	}

	/**
	 * @param sourceCuri
	 */
	public void setViaLinkFrom(CrawlURI sourceCuri) {
		via = sourceCuri;
		// reset embedCount -- but only back to 1 if >0, so special embed handling still applies
		embedHopCount = (embedHopCount > 0) ? 1 : 0;
		int candidateLinkHopCount = sourceCuri.getLinkHopCount()+1;
		if (linkHopCount == -1) {
			linkHopCount = candidateLinkHopCount;
			return;
		}
		if (linkHopCount > candidateLinkHopCount) {
			linkHopCount = candidateLinkHopCount; 
		}
	}
	
	/**
	 * @param sourceCuri
	 */
	public void setViaEmbedFrom(CrawlURI sourceCuri) {
		via = sourceCuri;
		int candidateLinkHopCount = sourceCuri.getLinkHopCount();
		if (linkHopCount == -1) {
			linkHopCount = candidateLinkHopCount;
		} else if (linkHopCount > candidateLinkHopCount) {
			linkHopCount = candidateLinkHopCount; 
		}
		int candidateEmbedHopCount = sourceCuri.getEmbedHopCount()+1;
		if (embedHopCount == -1) {
			embedHopCount = candidateEmbedHopCount;
		} else if (embedHopCount > candidateEmbedHopCount) {
			embedHopCount = candidateEmbedHopCount; 
		}
	}

	
/*	public boolean isFubared(){
		return ( fetchStatus < 0 && numberOfFetchAttempts >= 3);
	}*/

	
	/**
	 * @return
	 */
	public int getEmbedHopCount() {
		return embedHopCount;
	}

	/**
	 * @return
	 */
	public int getLinkHopCount() {
		return linkHopCount;
	}

	/**
	 * 
	 */
	public void markAsSeed() {
		linkHopCount = 0;
		embedHopCount = 0;
	}

	/**
	 * @param processor
	 */
	public void skipToProcessor(Processor processor) {
		setNextProcessor(processor);
	}
}
