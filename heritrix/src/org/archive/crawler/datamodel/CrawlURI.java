/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import org.archive.crawler.basic.FetcherDNS;
import org.archive.crawler.basic.URIStoreable;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;


import st.ata.util.AList;
import st.ata.util.HashtableAList;


/**
 * Represents a URI and the associated state it collects as
 * it is crawled.
 * 
 * Except for a few special components, state is in a flexible
 * attibute list.
 * 
 * Should only be instantiated via URIStore.getCrawlURI(...), 
 * which will assure only one CrawlURI can exist per 
 * UURI within a distinct "crawler".
 * 
 * @author Gordon Mohr
 */
public class CrawlURI
	implements URIStoreable, Comparable, CoreAttributeConstants, FetchStatusCodes {
	private long wakeTime;

	public static final String CONTENT_TYPE_LABEL = "content-type";
	
	private UURI baseUri;
	private AList alist = new HashtableAList();
	private UURI uuri; 
	private Object state;
	CrawlController controller;
	Processor nextProcessor;
	CrawlHost host;

	private int fetchStatus = 0;	// default to unattempted
	private int deferrals = 0;
	private int fetchAttempts = 0;	// the number of fetch attempts that have been made

	private int threadNumber;
	
	private long dontRetryBefore = -1;

	/**
	 * @param uuri
	 */
	public CrawlURI(UURI u) {
		uuri=u;
	}
	
	/**
	 * Set the time this curi is considered expired (and thus must be refetched)
	 * to 'expires'.  This function will set the time to an arbitrary value, if you do not wish
	 * to clobber an earlier expiration time (e.g. setting a robots expiration after a dns
	 * expiration) use setDontRetrySmart().
	 * @param expires
	 */
	public void setDontRetryBefore(long expires){
		dontRetryBefore = expires;
	}
	
	/**
	 * Set the expire time.  This function only allows you to set a 
	 * nearer expire time, not to increase it.  To set an arbitrary
	 * expire time use setDontRetryBefore()
	 * @param expires
	 */
	public void setDontRetryBeforeSmart(long expires){
		if(expires < dontRetryBefore || dontRetryBefore < 0){
			dontRetryBefore = expires;
		}
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

	
	/**
	 * @param uri
	 * @return
	 */
	public CrawlURI(URI u){
		uuri = new UURI(u);
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
	 * @param uriString
	 */
	public CrawlURI(String s){
		try{
			uuri = new UURI(new URI(s));
		}catch(Exception e){
			uuri = null;
		}
	}
	
	/**
	 * @return
	 */
	public UURI getUURI() {
		return uuri;
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
		String scheme = getUURI().getUri().getScheme();
		if (scheme.equals("dns")){
			return FetcherDNS.parseTargetDomain(this);
		}
		String authorityUsuallyHost = getUURI().getUri().getAuthority();
		if (authorityUsuallyHost != null) {
			return authorityUsuallyHost;
		} else {
			return null;
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
		// TODO Auto-generated method stub
		return alist;
	}
	
	
	/**
	 * @return
	 */
	public CrawlHost getHost() {
		return host;
	}

	/**
	 * @param host
	 */
	public void setHost(CrawlHost host) {
		this.host = host;
	}

	/**
	 * 
	 */
	public void cancelFurtherProcessing() {
		nextProcessor = null;
	}

	/**
	 * @param string
	 */
	public void setPrerequisiteUri(String string) {
		alist.putString("prerequisite-uri",string);
	}

	/**
	 * @param object
	 */
	public void setDelayFactor(int f) {
		alist.putInt(A_DELAY_FACTOR,f);
	}
	
	/**
	 * @param object
	 */
	public void setMinimumDelay(int m) {
		alist.putInt(A_MINIMUM_DELAY,m);
	}

	/**
	 * 
	 */
	public String getPrerequisiteUri() {
		return alist.getString("prerequisite-uri");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlURI("+getUURI()+")";
	}
	
	/**
	 * 
	 */
	public URI getBaseUri() {
		if (baseUri != null) {
			return baseUri.getUri();
		}
		if (!getAList().containsKey("html-base-href")) {
			return getUURI().getUri();
		}
		String base = getAList().getString("html-base-href");
		try {
			baseUri = UURI.createUURI(base);
		} catch (URISyntaxException e) {
			Object[] array = { this, base };
			controller.uriErrors.log(Level.INFO,e.getMessage(), array );
			// next best thing: use self
			baseUri = getUURI();
		}
		return getBaseUri();
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
	 * @param controller
	 */
	public void setController(CrawlController c) {
		controller = c;
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
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object other) {
		if(this==other) {
			return 0; // for exact identity only
		}
		if (((URIStoreable)other).getWakeTime()> wakeTime) {
			return -1;
		} 
		if (((URIStoreable)other).getWakeTime()< wakeTime) {
			return 1;
		} 
		// at this point, the ordering is arbitrary, but still
		// must be consistent/stable over time
		// TODOSOON: fix this
		return ((String)((URIStoreable)other).getClassKey()).compareTo(this.getClassKey());	
	}
	
/*	public boolean isFubared(){
		return ( fetchStatus < 0 && numberOfFetchAttempts >= 3);
	}*/
}
