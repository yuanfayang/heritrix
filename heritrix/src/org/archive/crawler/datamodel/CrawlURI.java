/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.URI;

import org.archive.crawler.basic.URIStoreable;
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
public class CrawlURI implements URIStoreable, CoreAttributeConstants {
	
	public static final String CONTENT_TYPE_LABEL = "content-type";
	
	private UURI baseUri;
	private AList alist = new HashtableAList();
	private UURI uuri; 
	private Object state;
	Processor nextProcessor;
	CrawlHost host;
	CrawlFetch fetch;


	/**
	 * @param uuri
	 */
	public CrawlURI(UURI u) {
		uuri=u;
	}
	
	/**
	 * @param uri
	 * @return
	 */
	public CrawlURI(URI u){
		uuri = new UURI(u);
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
	 * @return
	 */
	public Object getClassKey() {
		return uuri.getUri().getAuthority();
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
		// TODO Auto-generated method stub
		return 0;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
	 */
	public void setWakeTime(long w) {
		// TODO Auto-generated method stub
		
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
		baseUri = UURI.createUURI(getAList().getString("html-base-href"));
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

}
