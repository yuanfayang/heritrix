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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.credential.CredentialAvatar;
import org.archive.crawler.datamodel.credential.Rfc2617Credential;
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
 * <p>Core state is in instance variables, but a flexible
 * attribute list is also available.
 *
 * <p>Should only be instantiated via URIStore.getCrawlURI(...),
 * which will assure only one CrawlURI can exist per
 * UURI within a distinct "crawler".
 *
 * @author Gordon Mohr
 */
public class CrawlURI extends CandidateURI
    implements CoreAttributeConstants, FetchStatusCodes {
    
    /**
     * When neat host-based class-key fails us
     */
    private static String DEFAULT_CLASS_KEY = "default...";
    
    // INHERITED FROM CANDIDATEURI
    // uuri: core identity: the "usable URI" to be crawled
    // isSeed
    // inScopeVersion
    // pathFromSeed
    // via

    // Scheduler lifecycle info
    private String classKey; // cached classKey value

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
    private transient CrawlServer server;

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
     * Key to get credential avatars from A_LIST.
     */
    private static final String A_CREDENTIAL_AVATARS_KEY = "credential-avatars";
    
    /**
     * True if this CrawlURI has been deemed a prerequisite by the
     * {@link org.archive.crawler.prefetch.PreconditionEnforcer}.
     * 
     * This flag is used at least inside in the precondition enforcer so that
     * subsequent prerequisite tests know to let this CrawlURI through because
     * its a prerequisite needed by an earlier prerequisite tests (e.g. If 
     * this is a robots.txt, then the subsequent login credentials prereq
     * test must not throw it out because its not a login curi).
     */
    private boolean prerequisite = false;

    /**
     * Set to true if this <code>curi</code> is to be POST'd rather than GET-d.
     */
    private boolean post = false;
    

    /**
     * Create a new instance of CrawlURI from a {@link UURI}.
     * 
     * @param uuri the UURI to base this CrawlURI on.
     */
    public CrawlURI(UURI uuri) {
        super(uuri);
    }

    /**
     * Create a new instance of CrawlURI from a {@link CandidateURI}
     * 
     * @param caUri the CandidateURI to base this CrawlURI on.
     */
    public CrawlURI(CandidateURI caUri) {
        super(caUri.getUURI());
        setIsSeed(caUri.isSeed());
        setPathFromSeed(caUri.getPathFromSeed());
        setVia(caUri.getVia());
    }

    /**
     * Takes a status code and converts it into a human readable string.
     * 
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
            case S_PROCESSING_THREAD_KILLED:
                return "Heritrix(" + S_PROCESSING_THREAD_KILLED + ")-" + 
                    "Processing thread killed";
            // Unknown return code
            default : return Integer.toString(code);
        }
    }


    /**
     * Return the overall/fetch status of this CrawlURI for its
     * current trip through the processing loop. 
     * 
     * @return a value from FetchStatusCodes
     */
    public int getFetchStatus(){
        return fetchStatus;
    }

    /**
     * Set the overall/fetch status of this CrawlURI for
     * its current trip through the processing loop.
     * 
     * @param newstatus a value from FetchStatusCodes
     */
    public void setFetchStatus(int newstatus){
        fetchStatus = newstatus;
    }

    /**
     * Get the number of attempts at getting the document referenced by this
     * URI.
     * 
     * @return the number of attempts at getting the document referenced by this
     *         URI.
     */
    public int getFetchAttempts() {
        return fetchAttempts;
    }

    /**
     * Increment the number of attempts at getting the document referenced by
     * this URI.
     * 
     * @return the number of attempts at getting the document referenced by this
     *         URI.
     */
    public int incrementFetchAttempts() {
        return fetchAttempts++;
    }

    /**
     * Get the next processor to process this URI.
     * 
     * @return the processor that should process this URI next.
     */
    public Processor nextProcessor() {
        return nextProcessor;
    }
    
    /**
     * Get the processor chain that should be processing this URI after the
     * current chain is finished with it.
     * 
     * @return the next processor chain to process this URI.
     */
    public ProcessorChain nextProcessorChain() {
        return nextProcessorChain;
    }
    
    /**
     * Set the next processor to process this URI.
     * 
     * @param processor the next processor to process this URI.
     */
    public void setNextProcessor(Processor processor) {
        nextProcessor = processor;
    }
    
    /**
     * Set the next processor chain to process this URI.
     * 
     * @param nextProcessorChain the next processor chain to process this URI.
     */
    public void setNextProcessorChain(ProcessorChain nextProcessorChain) {
        this.nextProcessorChain = nextProcessorChain;
    }

    /**
     * Get the token (usually the hostname) which indicates
     * what "class" this CrawlURI should be grouped with,
     * for the purposes of ensuring only one item of the
     * class is processed at once, all items of the class
     * are held for a politeness period, etc.
     * 
     * @return Token (usually the hostname) which indicates
     * what "class" this CrawlURI should be grouped with.
     */
    public String getClassKey() throws URIException {
        if(classKey==null) {
            classKey = calculateClassKey();
        }
        return classKey;
    }

    private String calculateClassKey() throws URIException {
        String scheme = getUURI().getScheme();
        if (scheme.equals("dns")){
            return FetchDNS.parseTargetDomain(this);
        }
        String host = getUURI().getHost();
        if (host == null) {
            String authority =  getUURI().getAuthority();
            if(authority == null) {
                return DEFAULT_CLASS_KEY;
            } else {
                return authority;
            }
        } else {
            return host;
        }
    }


    /**
     * Get the attribute list.
     * <p>
     * The attribute list is a flexible map of key/value pairs for storing
     * status of this URI for use by other processors. By convention the
     * attribute list is keyed by constants found in the
     * {@link CoreAttributeConstants}interface.
     * 
     * @return the attribute list.
     */
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
     * Set the CrawlServer which this URI is to be associated with.
     * 
     * @param host the CrawlServer which this URI is to be associated with.
     */
    public void setServer(CrawlServer host) {
        this.server = host;
    }
    
    /**
     * Do all actions associated with setting a <code>CrawlURI</code> as 
     * requiring a prerequisite.
     * 
     * @param lastProcesorChain Last processor chain reference.  This chain is
     * where this <code>CrawlURI</code> goes next.
     * @param stringOrUURI Object to set a prerequisite.
     */
    public void markPrerequisite(Object stringOrUURI,
            ProcessorChain lastProcesorChain) {
        setPrerequisiteUri(stringOrUURI);   
        incrementDeferrals();
        setFetchStatus(S_DEFERRED);
        skipToProcessorChain(lastProcesorChain);
    }
    
    /**
     * Set a prerequisite for this URI.
     * <p>
     * A prerequisite is a URI that must be crawled before this URI can be
     * crawled.
     * 
     * @param stringOrUURI Either a string or a URI representation of a URI.
     */
    protected void setPrerequisiteUri(Object stringOrUURI) {
        this.alist.putObject(A_PREREQUISITE_URI,stringOrUURI);
    }

    /**
     * Get the prerequisite for this URI.
     * <p>
     * A prerequisite is a URI that must be crawled before this URI can be
     * crawled.
     * 
     * @return the prerequisite for this URI or null if no prerequisite.
     */
    public Object getPrerequisiteUri() {
        return this.alist.getObject(A_PREREQUISITE_URI);
    }
    
    /**
     * Returns true if this CrawlURI is a prerequisite.
     * 
     * @return true if this CrawlURI is a prerequisite.
     */
    public boolean isPrerequisite() {
        return this.prerequisite;
    }
    
    /**
     * Set if this CrawlURI is itself a prerequisite URI.
     * 
     * @param prerequisite True if this CrawlURI is itself a prerequiste uri.
     */
    public void setPrerequisite(boolean prerequisite) {
        this.prerequisite = prerequisite;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "CrawlURI("+getURIString()+")";
    }

    /**
     * Get the content type of this URI.
     * 
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
     * Set the number of the ToeThread responsible for processing this uri.
     * 
     * @param i the ToeThread number.
     */
    public void setThreadNumber(int i) {
        threadNumber = i;
    }

    /**
     * Get the number of the ToeThread responsible for processing this uri.
     * 
     * @return the ToeThread number.
     */
    public int getThreadNumber() {
        return threadNumber;
    }

    /**
     * Increment the deferral count.
     *
     */
    public void incrementDeferrals() {
        deferrals++;
    }

    /**
     * Get the deferral count.
     * 
     * @return the deferral count.
     */
    public int getDeferrals() {
        return deferrals;
    }

    /**
     * Remove all attributes set on this uri.
     * <p>
     * This methods removes the attribute list.
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

    /**
     * Set the attribute list.
     * <p>
     * The attribute list is a flexible map of key/value pairs for storing
     * status of this URI for use by other processors. By convention the
     * attribute list is keyed by constants found in the
     * {@link CoreAttributeConstants}interface.
     * 
     * @param a the attribute list to set.
     */
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

    /**
     * Add an annotation.
     * 
     * @param annotation the annotation to add.
     */
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

    /**
     * Get the annotations set for this uri.
     * 
     * @return the annotations set for this uri.
     */
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

    /**
     * Get the embeded hop count.
     * 
     * @return the embeded hop count.
     */
    public int getEmbedHopCount() {
        return embedHopCount;
    }

    /**
     * Get the link hop count.
     * 
     * @return the link hop count.
     */
    public int getLinkHopCount() {
        return linkHopCount;
    }

    /**
     * Mark this uri as being a seed.
     *
     */
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
     * Set which processor should be the next processor to process this uri
     * instead of using the default next processor.
     * 
     * @param processorChain the processor chain to skip to.
     * @param processor the processor in the processor chain to skip to.
     */
    public void skipToProcessor(ProcessorChain processorChain,
            Processor processor) {
        setNextProcessorChain(processorChain);
        setNextProcessor(processor);
    }

    /**
     * Set which processor chain should be processing this uri next.
     * 
     * @param processorChain the processor chain to skip to.
     */
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
     * Get the http recorder associated with this uri.
     * 
     * @return Returns the httpRecorder.  May be null.
     */
    public HttpRecorder getHttpRecorder() {
        return httpRecorder;
    }

    /**
     * Set the http recorder to be associated with this uri.
     * 
     * @param httpRecorder The httpRecorder to set.
     */
    public void setHttpRecorder(HttpRecorder httpRecorder) {
        this.httpRecorder = httpRecorder;
    }
    
    /**
     * Return true if this is a http transaction.
     * 
     * TODO: Compound this and {@link #isPost()} method so that there is one
     * place to go to find out if get http, post http, ftp, dns.
     * 
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
        this.fetchStatus = S_UNATTEMPTED;
        this.setPrerequisite(false);
        if (this.alist != null) {
            // Let current get method to be GC'd.
            this.alist.remove(A_HTTP_TRANSACTION);
            // Discard any ideas of prereqs -- may no longer be valid.
            this.alist.remove(A_PREREQUISITE_URI);
        }
    }
    
    /**  
     * Make a <code>CrawlURI</code> from the passed <code>CandidateURI</code>.
     *
     * Its safe to pass a CrawlURI instance.  In this case we just return it
     * as a result. Otherwise, we create new CrawlURI instance.
     *
     * @param caUri Candidate URI.
     * @return A crawlURI made from the passed CandidateURI.
     */
    public static CrawlURI from(CandidateURI caUri) {
        if (caUri instanceof CrawlURI) {
            return (CrawlURI) caUri;
        }
        return new CrawlURI(caUri);
    }
    
    /**
     * @param avatars Credential avatars to save off.
     */
    private void setCredentialAvatars(Set avatars) {
        this.alist.putObject(A_CREDENTIAL_AVATARS_KEY, avatars);
    }
    
    /**
     * @return Credential avatars.  Null if none set.
     */
    public Set getCredentialAvatars() {
        return (Set)this.alist.getObject(A_CREDENTIAL_AVATARS_KEY);
    }
    
    /**
     * @return True if there are avatars attached to this instance.
     */
    public boolean hasCredentialAvatars() {
        return getCredentialAvatars() != null &&
            getCredentialAvatars().size() > 0;
    }
    
    /**
     * Add an avatar.
     * 
     * We do lazy instantiation.
     * 
     * @param ca Credential avatar to add to set of avatars.
     */
    public void addCredentialAvatar(CredentialAvatar ca) {  
    	    Set avatars = getCredentialAvatars();
    	    if (avatars == null) {
    	    	    avatars = new HashSet();
    	    	    	setCredentialAvatars(avatars);
    	    }
    	    avatars.add(ca);
    }
    
    /**
     * Remove all credential avatars from this crawl uri.
     */
    public void removeCredentialAvatars() {
        if (hasCredentialAvatars()) {
            this.alist.remove(A_CREDENTIAL_AVATARS_KEY);
        }
    }
    
    /**
     * Remove all credential avatars from this crawl uri.
     * @param ca Avatar to remove.
     * @return True if we removed passed parameter.  False if no operation 
     * performed.
     */
    public boolean removeCredentialAvatar(CredentialAvatar ca) {
        boolean result = false;
        Set avatars = getCredentialAvatars();
        if (avatars != null && avatars.size() > 0) {
            result = avatars.remove(ca);
        }
        return result;
    }
    
    /**
     * Ask this URI if it was a success or not.
     * 
     * Only makes sense to call this method after execution of
     * HttpMethod#execute. Regard any status larger then 0 as success
     * except for below caveat regarding 401s.
     * 
     * <p>401s caveat: If any rfc2617 credential data present and we got a 401 
     * assume it got loaded in FetchHTTP on expectation that we're to go around
     * the processing chain again. Report this condition as a failure so we
     * get another crack at the processing chain only this time we'll be making
     * use of the loaded credential data.
     * 
     * @return True if ths URI has been successfully processed.
     */
    public boolean isSuccess()
    {
        boolean result = false;
        int statusCode = this.fetchStatus;
        if (statusCode == HttpStatus.SC_UNAUTHORIZED &&
            hasRfc2617CredentialAvatar()) {
            result = false;
        } else {
            result = (statusCode > 0);
        }
        return result;
    }

    /**
	 * @return True if we have an rfc2617 payload.
	 */
	public boolean hasRfc2617CredentialAvatar() {
	    boolean result = false;
	    Set avatars = getCredentialAvatars();
	    if (avatars != null && avatars.size() > 0) {
	        for (Iterator i = avatars.iterator(); i.hasNext();) {
	            if (((CredentialAvatar)i.next()).
	                match(Rfc2617Credential.class)) {
	                result = true;
	                break;
	            }
	        }
	    }
        return result;
	}

    /**
     * Set whether this URI should be fetched by sending a HTTP POST request.
     * Else a HTTP GET request will be used.
     * 
     * @param b Set whether this curi is to be POST'd.  Else its to be GET'd.
     */
    public void setPost(boolean b) {
        this.post = b;
    }
    
    /**
     * Returns true if this URI should be fetched by sending a HTTP POST request.
     * 
     * 
     * TODO: Compound this and {@link #isHttpTransaction()} method so that there
     * is one place to go to find out if get http, post http, ftp, dns.
     * 
     * @return Returns is this CrawlURI instance is to be posted.
     */
    public boolean isPost() {
        return this.post;
    }
}
