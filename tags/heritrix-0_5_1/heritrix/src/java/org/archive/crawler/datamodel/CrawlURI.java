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

import org.archive.crawler.basic.URIStoreable;
import org.archive.crawler.fetcher.FetchDNS;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ProcessorChain;
import org.archive.util.HttpRecorder;

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
    private Processor nextProcessor;
    private ProcessorChain nextProcessorChain;
    private int fetchStatus = 0;    // default to unattempted
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
     * Current http recorder. 
     * 
     * Gets set upon successful request.  Reset at start of processing chain.
     */
    private HttpRecorder httpRecorder = null;
    
    /**
     * Content type of a successfully fetched URI.
     * 
     * May be null even on successfully fetched URI.
     */
    private String contentType = null;


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
     * Takes a status code and converts it into a human readable string.
     * @param code the status code
     * @return a human readable string declaring what the status code is.
     */
    public static String fetchStatusCodesToString(int code){
        switch(code){
            // DNS
            case S_DNS_SUCCESS : return "DNS-1-OK"; 
            // HTTP Informational 1xx
            case 100  : return "HTTP-100-Info-Continue";
            case 101  : return "HTTP-101-Info-Switching Protocols";
            // HTTP Successful 2xx
            case 200  : return "HTTP-200-Success-OK";
            case 201  : return "HTTP-200-Success-Created";
            case 202  : return "HTTP-200-Success-Accepted";
            case 203  : return "HTTP-200-Success-Non-Authoritative";
            case 204  : return "HTTP-200-Success-No Content ";
            case 205  : return "HTTP-200-Success-Reset Content";
            case 206  : return "HTTP-200-Success-Partial Content";
            // HTTP Redirection 3xx
            case 300  : return "HTTP-300-Redirect-Multiple Choices";
            case 301  : return "HTTP-301-Redirect-Moved Permanently";
            case 302  : return "HTTP-302-Redirect-Found";
            case 303  : return "HTTP-303-Redirect-See Other";
            case 304  : return "HTTP-304-Redirect-Not Modified";
            case 305  : return "HTTP-305-Redirect-Use Proxy";
            case 307  : return "HTTP-307-Redirect-Temporary Redirect";
            // HTTP Client Error 4xx
            case 400  : return "HTTP-400-ClientErr-Bad Request";
            case 401  : return "HTTP-401-ClientErr-Unauthorized";
            case 402  : return "HTTP-402-ClientErr-Payment Required";
            case 403  : return "HTTP-403-ClientErr-Forbidden";
            case 404  : return "HTTP-404-ClientErr-Not Found";
            case 405  : return "HTTP-405-ClientErr-Method Not Allowed";
            case 407  : return "HTTP-406-ClientErr-Not Acceptable";
            case 408  : return "HTTP-407-ClientErr-Proxy Authentication Required";
            case 409  : return "HTTP-408-ClientErr-Request Timeout";
            case 410  : return "HTTP-409-ClientErr-Conflict";
            case 406  : return "HTTP-410-ClientErr-Gone";
            case 411  : return "HTTP-411-ClientErr-Length Required";
            case 412  : return "HTTP-412-ClientErr-Precondition Failed";
            case 413  : return "HTTP-413-ClientErr-Request Entity Too Large";
            case 414  : return "HTTP-414-ClientErr-Request-URI Too Long";
            case 415  : return "HTTP-415-ClientErr-Unsupported Media Type";
            case 416  : return "HTTP-416-ClientErr-Requested Range Not Satisfiable";
            case 417  : return "HTTP-417-ClientErr-Expectation Failed";
            // HTTP Server Error 5xx
            case 500  : return "HTTP-500-ServerErr-Internal Server Error";
            case 501  : return "HTTP-501-ServerErr-Not Implemented";
            case 502  : return "HTTP-502-ServerErr-Bad Gateway";
            case 503  : return "HTTP-503-ServerErr-Service Unavailable";
            case 504  : return "HTTP-504-ServerErr-Gateway Timeout";
            case 505  : return "HTTP-505-ServerErr-HTTP Version Not Supported";
            // Heritrix internal codes (all negative numbers
            case S_BLOCKED_BY_USER:
                return "Heritrix(" + S_BLOCKED_BY_USER + ")-Blocked by user";
            case S_DELETED_BY_USER:
                return "Heritrix(" + S_DELETED_BY_USER + ")-Deleted by user";
            case S_CONNECT_FAILED:
                return "Heritrix(" + S_CONNECT_FAILED + ")-Connection failed";
            case S_CONNECT_LOST:
                return "Heritrix(" + S_CONNECT_LOST + ")-Connection lost";
            case S_DEEMED_CHAFF:
                return "Heritrix(" + S_DEEMED_CHAFF + ")-Deemed chaff";
            case S_DEFERRED:
                return "Heritrix(" + S_DEFERRED + ")-Deferred";
            case S_DOMAIN_UNRESOLVABLE:
                return "Heritrix(" + S_DOMAIN_UNRESOLVABLE
                        + ")-Domain unresolvable";
            case S_OUT_OF_SCOPE:
                return "Heritrix(" + S_OUT_OF_SCOPE + ")-Out of scope";
            case S_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_PREREQUISITE_FAILURE
                        + ")-Prerequisite failure";
            case S_ROBOTS_PRECLUDED:
                return "Heritrix(" + S_ROBOTS_PRECLUDED + ")-Robots precluded";
            case S_RUNTIME_EXCEPTION:
                return "Heritrix(" + S_RUNTIME_EXCEPTION
                        + ")-Runtime exception";
            case S_SERIOUS_ERROR:
                return "Heritrix(" + S_SERIOUS_ERROR + ")-Serious error";
            case S_TIMEOUT:
                return "Heritrix(" + S_TIMEOUT + ")-Timeout";
            case S_TOO_MANY_EMBED_HOPS:
                return "Heritrix(" + S_TOO_MANY_EMBED_HOPS
                        + ")-Too many embed hops";
            case S_TOO_MANY_LINK_HOPS:
                return "Heritrix(" + S_TOO_MANY_LINK_HOPS
                        + ")-Too many link hops";
            case S_TOO_MANY_RETRIES:
                return "Heritrix(" + S_TOO_MANY_RETRIES + ")-Too many retries";
            case S_UNATTEMPTED:
                return "Heritrix(" + S_UNATTEMPTED + ")-Unattempted";
            case S_UNFETCHABLE_URI:
                return "Heritrix(" + S_UNFETCHABLE_URI + ")-Unfetchable URI";
            // Unknown return code
            default : return Integer.toString(code);
        }
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
    
    public ProcessorChain nextProcessorChain() {
        return nextProcessorChain;
    }
    
    /**
     * @param processor
     */
    public void setNextProcessor(Processor processor) {
        nextProcessor = processor;
    }
    
    public void setNextProcessorChain(ProcessorChain nextProcessorChain) {
        this.nextProcessorChain = nextProcessorChain;
    }

    /**
     * @return Token (usually the hostname) which indicates
     * what "class" this CrawlURI should be grouped with,
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
                return getUURI().getURIString();
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

//    /**
//     *
//     */
//    public void cancelFurtherProcessing() {
//        nextProcessor = null;
//    }

    /**
     * @param stringOrUURI
     */
    public void setPrerequisiteUri(Object stringOrUURI) {
        alist.putObject(A_PREREQUISITE_URI,stringOrUURI);
    }

    public Object getPrerequisiteUri() {
        return alist.getObject(A_PREREQUISITE_URI);
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "CrawlURI("+getUURI()+")";
    }

    /**
     * @return Fetched URIs content type.  May be null.
     */
    public String getContentType() {
        return this.contentType;
    }
    
    /**
     * Set a fetched uri's content type.
     * 
     * @param ct Contenttype.  May be null.
     */
    public void setContentType(String ct) {
        this.contentType = ct;
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
     *
     */
    public void reconstitute() {
        if (alist == null) {
            alist = new HashtableAList();
        }
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
            annotations += ","+annotation;
        } else {
            annotations = annotation;
        }

        alist.putString(A_ANNOTATIONS,annotations);
    }

    public String getAnnotations() {
        if(alist.containsKey(A_ANNOTATIONS)) {
            return alist.getString(A_ANNOTATIONS);
        } else {
            return "";
        }

    }
//    /**
//     * @param sourceCuri
//     */
//    public void setViaLinkFrom(CrawlURI sourceCuri) {
//        via = sourceCuri;
//        // reset embedCount -- but only back to 1 if >0, so special embed handling still applies
//        embedHopCount = (embedHopCount > 0) ? 1 : 0;
//        int candidateLinkHopCount = sourceCuri.getLinkHopCount()+1;
//        if (linkHopCount == -1) {
//            linkHopCount = candidateLinkHopCount;
//            return;
//        }
//        if (linkHopCount > candidateLinkHopCount) {
//            linkHopCount = candidateLinkHopCount;
//        }
//    }

//    /**
//     * @param sourceCuri
//     */
//    public void setViaEmbedFrom(CrawlURI sourceCuri) {
//        via = sourceCuri;
//        int candidateLinkHopCount = sourceCuri.getLinkHopCount();
//        if (linkHopCount == -1) {
//            linkHopCount = candidateLinkHopCount;
//        } else if (linkHopCount > candidateLinkHopCount) {
//            linkHopCount = candidateLinkHopCount;
//        }
//        int candidateEmbedHopCount = sourceCuri.getEmbedHopCount()+1;
//        if (embedHopCount == -1) {
//            embedHopCount = candidateEmbedHopCount;
//        } else if (embedHopCount > candidateEmbedHopCount) {
//            embedHopCount = candidateEmbedHopCount;
//        }
//    }


/*    public boolean isFubared(){
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
    public void skipToProcessor(ProcessorChain processorChain, Processor processor) {
        setNextProcessorChain(processorChain);
        setNextProcessor(processor);
    }

    public void skipToProcessorChain(ProcessorChain processorChain) {
        setNextProcessorChain(processorChain);
        setNextProcessor(null);
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
        if (this.contentLength < 0) {
            this.contentLength = getHttpRecorder().getResponseContentLength();
        }
        return this.contentLength;
    }

    /**
     * @param l
     */
    public void setContentSize(long l) {
        contentSize = l;
    }

    /**
     * Add string version of the link to specified collection.
     * 
     * @param link Link to be added to collection.
     * @param collectionName Name of the collection.
     */
    public void addLinkToCollection(String link, String collectionName) {
        addToNamedSet(collectionName, link);
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

    /**
     * Notify CrawlURI it is about to be logged; opportunity
     * for self-annotation
     */
    public void aboutToLog() {
        if (fetchAttempts>1) {
            addAnnotation(fetchAttempts+"t");
        }
    }
    
    /**
     * @return Returns the httpRecorder.  May be null.
     */
    public HttpRecorder getHttpRecorder() {
        return httpRecorder;
    }

    /**
     * @param httpRecorder The httpRecorder to set.
     */
    public void setHttpRecorder(HttpRecorder httpRecorder) {
        this.httpRecorder = httpRecorder;
    }
    
    /**
     * @return True if this is a http transaction.
     */
    public boolean isHttpTransaction() {
        return getAList().containsKey(A_HTTP_TRANSACTION);
    }

    /**
     * Clean up after a run through the processing chain.
     * 
     * Called on the end of processing chain by Frontier#finish.  Null out any
     * state gathered during processing.
     */
    public void processingCleanup() {
        this.httpRecorder = null; 

        // Allow get to be GC'd.
        if (this.alist != null)
        {
            this.alist.remove(A_HTTP_TRANSACTION);
        }
    }
}
