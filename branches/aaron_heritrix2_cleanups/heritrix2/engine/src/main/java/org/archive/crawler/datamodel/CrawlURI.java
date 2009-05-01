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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import static org.archive.crawler.datamodel.SchedulingConstants.*;
import static org.archive.modules.fetcher.FetchStatusCodes.*;
import static org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants.*;

import org.archive.modules.ModuleAttributeConstants;
import org.archive.modules.ProcessorURI;
import org.archive.modules.credential.CredentialAvatar;
import org.archive.modules.credential.Rfc2617Credential;
import org.archive.modules.extractor.HTMLLinkContext;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.LinkContext;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.settings.SheetManager;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Base32;
import org.archive.util.Recorder;
import org.archive.util.Reporter;
import org.archive.util.SURT;


/**
 * Represents a candidate URI and the associated state it
 * collects as it is crawled.
 *
 * <p>Core state is in instance variables but a flexible
 * attribute list is also available. Use this 'bucket' to carry
 * custom processing extracted data and state across CrawlURI
 * processing.  See the {@link #putString(String, String)},
 * {@link #getString(String)}, etc. 
 *
 * @author Gordon Mohr
 */
public class CrawlURI implements ProcessorURI, Reporter, Serializable {

    private static final long serialVersionUID = 3L;


    public static final int UNCALCULATED = -1;
    

    /**
     * The URI being crawled.  It's transient to save space when storing to BDB.
     */
    private transient UURI uuri;

    
    /** Seed status */
    private boolean isSeed = false;

    
    /** String of letters indicating how this URI was reached from a seed.
     * <pre>
     * P precondition
     * R redirection
     * E embedded (as frame, src, link, codebase, etc.)
     * X speculative embed (as from javascript, some alternate-format extractors
     * L link</pre>
     * For example LLLE (an embedded image on a page 3 links from seed).
     */
    private String pathFromSeed;
    
    /**
     * Where this URI was (presently) discovered. . Transient to allow
     * more efficient custom serialization
     */
    private transient UURI via;

    /**
     * Context of URI's discovery, as per the 'context' in Link
     */
    private LinkContext viaContext;

    
    private int schedulingDirective = NORMAL;

    
    /**
     * Frontier/Scheduler lifecycle info.
     * This is an identifier set by the Frontier for its
     * purposes. Usually its the name of the Frontier queue
     * this URI gets queued to.  Values can be host + port
     * or IP, etc.
     */
    private String classKey;

    /** assigned precedence */
    private int precedence;
    
    // Processing progress
    private int fetchStatus = 0;    // default to unattempted
    private int deferrals = 0;     // count of postponements for prerequisites
    private int fetchAttempts = 0; // the number of fetch attempts that have been made
    transient private int threadNumber;

    // User agent to masquerade as when crawling this URI. If null, globals should be used
    private String userAgent = null;
    
    // From header
    // TODO: This and user-agent really belong in FetchHTTP
//    private transient String from = null;

    // Once a link extractor has finished processing this curi this will be
    // set as true
    transient private boolean linkExtractorFinished = false;

    /**
     * Protection against outlink overflow.
     * Change value by setting alternate maximum in heritrix.properties.
     */
    public static final int MAX_OUTLINKS = Integer.
        parseInt(System.getProperty(CrawlURI.class.getName() + ".maxOutLinks",
            "6000"));
    
    transient private int discardedOutlinks = 0; 
    
////////////////////////////////////////////////////////////////////
    private long contentSize = UNCALCULATED;
    private long contentLength = UNCALCULATED;

    
    /**
     * Flexible dynamic attributes list.
     * <p>
     * The attribute list is a flexible map of key/value pairs for storing
     * status of this URI for use by other processors. By convention the
     * attribute list is keyed by constants found in the
     * {@link CoreAttributeConstants} interface.  Use this list to carry
     * data or state produced by custom processors rather change the
     * classes {@link CrawlURI} or this class, CrawlURI.
     *
     * Transient to allow more efficient custom serialization.
     * 
     * Package-protected so CrawlURI can access it directly.
     */
    transient Map<String,Object> data;

    
    private transient SheetManager manager;
    private transient StateProvider provider;


    private boolean forceRevisit = false; // even if already visited

    
    /**
     * Current http recorder.
     *
     * Gets set upon successful request.  Reset at start of processing chain.
     */
    private transient Recorder httpRecorder = null;

    /**
     * Content type of a successfully fetched URI.
     *
     * May be null even on successfully fetched URI.
     */
    private String contentType = "unknown";

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

    
    transient private FetchType fetchType = FetchType.UNKNOWN;

    transient private HttpMethod method = null;
    
    /** 
     * Monotonically increasing number within a crawl;
     * useful for tending towards breadth-first ordering.
     * Will sometimes be truncated to 48 bits, so behavior
     * over 281 trillion instantiated CrawlURIs may be 
     * buggy
     */
    protected long ordinal;

    /**
     * Cache of this candidate uuri as a string.
     *
     * Profiling shows us spending about 1-2% of total elapsed time in
     * toString.
     */
    private String cachedCrawlURIString = null;
    
    /**
     * Array to hold keys of data members that persist across URI processings.
     * Any key mentioned in this list will not be cleared out at the end
     * of a pass down the processing chain.
     */
    private static final Collection<String> persistentKeys
     = new CopyOnWriteArrayList<String>(
            new String [] {A_CREDENTIAL_AVATARS_KEY});

    /**
     * A digest (hash, usually SHA1) of retrieved content-body. 
     * 
     */
    private byte[] contentDigest = null;
    private String contentDigestScheme = null;


    /**
     * Create a new instance of CrawlURI from a {@link UURI}.
     *
     * @param uuri the UURI to base this CrawlURI on.
     */
    public CrawlURI(UURI uuri) {
        this.uuri = uuri;
    }

    public static CrawlURI fromHopsViaString(String uriHopsViaContext) throws URIException {
        UURI u;
        String args[] = uriHopsViaContext.split("\\s+");
        u = UURIFactory.getInstance(args[0]);
        String pathFromSeed = (args.length > 1)?
            args[1].toString() : "";
        UURI via = (args.length > 2 && args[2].length()>1) ?
            UURIFactory.getInstance(args[2].toString()):
            null;
        LinkContext viaContext = (args.length > 3 && args[2].length()>1) ?
                new HTMLLinkContext(args[3].toString()): null;
        CrawlURI caUri = new CrawlURI(u, pathFromSeed, via, viaContext);
        return caUri;
    }
    
    /**
     * @param u uuri instance this CrawlURI wraps.
     * @param pathFromSeed
     * @param via
     * @param viaContext
     */
    public CrawlURI(UURI u, String pathFromSeed, UURI via,
            LinkContext viaContext) {
        this.uuri = u;
        this.pathFromSeed = pathFromSeed;
        this.via = via;
        this.viaContext = viaContext;
    }

    
    /**
     * Create a new instance of CrawlURI from a {@link CrawlURI}
     *
     * @param caUri the CrawlURI to base this CrawlURI on.
     * @param o Monotonically increasing number within a crawl.
     */
    public CrawlURI(CrawlURI caUri, long o) {
        this(caUri.getUURI(), caUri.getPathFromSeed(), caUri.getVia(),
            caUri.getViaContext());
        ordinal = o;
        setSeed(caUri.isSeed());
        setSchedulingDirective(caUri.getSchedulingDirective());
        this.data = caUri.data;
    }
    
    
    /**
     * @return Returns the schedulingDirective.
     */
    public int getSchedulingDirective() {
        return schedulingDirective;
    }


    /** 
     * @param priority The schedulingDirective to set.
     */
    public void setSchedulingDirective(int priority) {
        this.schedulingDirective = priority;
    }

    
    public boolean containsDataKey(String key) {
        if (data == null) {
                return false;
        }
        return data.containsKey(key);
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
            case 201  : return "HTTP-201-Success-Created";
            case 202  : return "HTTP-202-Success-Accepted";
            case 203  : return "HTTP-203-Success-Non-Authoritative";
            case 204  : return "HTTP-204-Success-No Content ";
            case 205  : return "HTTP-205-Success-Reset Content";
            case 206  : return "HTTP-206-Success-Partial Content";
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
            case S_BLOCKED_BY_CUSTOM_PROCESSOR:
                return "Heritrix(" + S_BLOCKED_BY_CUSTOM_PROCESSOR +
                ")-Blocked by custom prefetch processor";
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
            case S_DOMAIN_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_DOMAIN_PREREQUISITE_FAILURE
                        + ")-Domain prerequisite failure";
            case S_ROBOTS_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_ROBOTS_PREREQUISITE_FAILURE
                        + ")-Robots prerequisite failure";
            case S_OTHER_PREREQUISITE_FAILURE:
                return "Heritrix(" + S_OTHER_PREREQUISITE_FAILURE
                        + ")-Other prerequisite failure";
            case S_PREREQUISITE_UNSCHEDULABLE_FAILURE:
                return "Heritrix(" + S_PREREQUISITE_UNSCHEDULABLE_FAILURE
                        + ")-Prerequisite unschedulable failure";
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
        // TODO: rename, this is actually processing-loop-attempts
        return fetchAttempts++;
    }

    /**
     * Reset fetchAttempts counter.
     */
    public void resetFetchAttempts() {
        this.fetchAttempts = 0;
    }

    /**
     * Reset deferrals counter.
     */
    public void resetDeferrals() {
        this.deferrals = 0;
    }




    /**
     * Set a prerequisite for this URI.
     * <p>
     * A prerequisite is a URI that must be crawled before this URI can be
     * crawled.
     *
     * @param link Link to set as prereq.
     */
    public void setPrerequisiteUri(Object link) {
        getData().put(A_PREREQUISITE_URI, link);
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
        return getData().get(A_PREREQUISITE_URI);
    }
    
    /**
     * @return True if this CrawlURI has a prerequisite.
     */
    public boolean hasPrerequisiteUri() {
        return containsDataKey(A_PREREQUISITE_URI);
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

    /**
     * @return This crawl URI as a string wrapped with 'CrawlURI(' +
     * ')'.
     */
    public String getCrawlURIString() {
        if (this.cachedCrawlURIString == null) {
            synchronized (this) {
                if (this.cachedCrawlURIString == null) {
                    this.cachedCrawlURIString =
                        "CrawlURI(" + toString() + ")";
                }
            }
        }
        return this.cachedCrawlURIString;
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
     * @param ct Contenttype.
     */
    public void setContentType(String ct) {
        if (ct == null) {
            ct = "unknown";
        }
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
    	data = null;
    }

    /**
     * Get the size in bytes of this URI's recorded content, inclusive
     * of things like protocol headers. It is the responsibility of the 
     * classes which fetch the URI to set this value accordingly -- it is 
     * not calculated/verified within CrawlURI. 
     * 
     * This value is consulted in reporting/logging/writing-decisions.
     * 
     * @see #setContentSize()
     * @return contentSize
     */
    public long getContentSize(){
        return contentSize;
    }

    
    // TODO: Move to utils.
    protected String getClassSimpleName(final Class c) {
        String classname = c.getName();
        int index = classname.lastIndexOf('.');
        return ((index > 0 && (index + 1) < classname.length())?
            classname.substring(index + 1): classname);
    }
    

    /**
     * Get the annotations set for this uri.
     *
     * @return the annotations set for this uri.
     */
    public Collection<String> getAnnotations() {
    	@SuppressWarnings("unchecked")
    	List<String> list = (List<String>)getData().get(A_ANNOTATIONS);
        if (list == null) {
            list = new ArrayList<String>();
            getData().put(A_ANNOTATIONS, list);
        }
    	return list;
    }

    /**
     * Get the embeded hop count.
     *
     * @return the embeded hop count.
     */
    public int getEmbedHopCount() {
        int embedHops = 0;
        for(int i = pathFromSeed.length()-1; i>=0; i--) {
            if(pathFromSeed.charAt(i)==Hop.NAVLINK.getHopChar()) {
                break;
            }
            embedHops++;
        }
        return embedHops;
    }

    /**
     * Get the link hop count.
     *
     * @return the link hop count.
     */
    public int getLinkHopCount() {
        int linkHops = 0;
        for(int i = pathFromSeed.length()-1; i>=0; i--) {
            if(pathFromSeed.charAt(i)==Hop.NAVLINK.getHopChar()) {
                linkHops++;
            }
        }
        return linkHops;
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
     * For completed HTTP transactions, the length of the content-body.
     *
     * @return For completed HTTP transactions, the length of the content-body.
     */
    public long getContentLength() {
        if (this.contentLength < 0) {
            this.contentLength = (getRecorder() != null)?
                getRecorder().getResponseContentLength(): 0;
        }
        return this.contentLength;
    }
    
    /**
     * Get size of data recorded (transferred)
     * 
     * @return recorded data size
     */
    public long getRecordedSize() {
        return (getRecorder() != null) ? getRecorder()
                .getRecordedInput().getSize()
                // if unavailable fall back on content-size
                : getContentSize();
    }

    /**
     * Sets the 'content size' for the URI, which is considered inclusive of all
     * of all recorded material (such as protocol headers) or even material
     * 'virtually' considered (as in material from a previous fetch 
     * confirmed unchanged with a server). (In contrast, content-length 
     * matches the HTTP definition, that of the enclosed content-body.)
     * 
     * Should be set by a fetcher or other processor as soon as the final size
     * of recorded content is known. Setting to an artificial/incorrect value
     * may affect other reporting/processing.
     */
    public void setContentSize(long l) {
        contentSize = l;
    }

    /**
     * If true then a link extractor has already claimed this CrawlURI and
     * performed link extraction on the document content. This does not
     * preclude other link extractors that may have an interest in this
     * CrawlURI from also doing link extraction but default behavior should
     * be to not run if link extraction has already been done.
     * 
     * <p>There is an onus on link extractors to set this flag if they have
     * run.
     * 
     * <p>The only extractor of the default Heritrix set that does not
     * respect this flag is
     * {@link org.archive.crawler.extractor.ExtractorHTTP}.
     * It runs against HTTP headers, not the document content.
     * 
     * @return True if a processor has performed link extraction on this
     * CrawlURI
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
    public void linkExtractorFinished() {
        linkExtractorFinished = true;
        if(discardedOutlinks>0) {
            getAnnotations().add("dol:"+discardedOutlinks);
        }
    }

    /**
     * Notify CrawlURI it is about to be logged; opportunity
     * for self-annotation
     */
    public void aboutToLog() {
        if (fetchAttempts>1) {
            getAnnotations().add(fetchAttempts + "t");
        }
    }

    /**
     * Get the http recorder associated with this uri.
     *
     * @return Returns the httpRecorder.  May be null but its set early in
     * FetchHttp so there is an issue if its null.
     */
    public Recorder getRecorder() {
        return httpRecorder;
    }

    /**
     * Set the http recorder to be associated with this uri.
     *
     * @param httpRecorder The httpRecorder to set.
     */
    public void setRecorder(Recorder httpRecorder) {
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
        return method != null;
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
        this.contentSize = UNCALCULATED;
        this.contentLength = UNCALCULATED;
        // Clear 'links extracted' flag.
        this.linkExtractorFinished = false;
        // Clean the data map of all but registered permanent members.
        this.data = getPersistentDataMap();
    }
    
    public Map<String,Object> getPersistentDataMap() {
    	if (data == null) {
    		return null;
    	}
    	Map<String,Object> result = new HashMap<String,Object>(getData());
    	Set<String> retain = new HashSet<String>(persistentKeys);
    	
    	if (containsDataKey(A_HERITABLE_KEYS)) {
    	    @SuppressWarnings("unchecked")
            List<String> heritable = (List<String>)getData().get(A_HERITABLE_KEYS);
            retain.addAll(heritable);
    	}
    	
    	result.keySet().retainAll(retain);
    	return result;
    }

    /**
     * Make a <code>CrawlURI</code> from the passed <code>CrawlURI</code>.
     *
     * Its safe to pass a CrawlURI instance.  In this case we just return it
     * as a result. Otherwise, we create new CrawlURI instance.
     *
     * @param caUri Candidate URI.
     * @param ordinal
     * @return A crawlURI made from the passed CrawlURI.
     */
//    public static CrawlURI from(CrawlURI caUri, long ordinal) {
//        return (caUri instanceof CrawlURI)?
//            (CrawlURI)caUri: new CrawlURI(caUri, ordinal);
//    }


    /**
     * @return Credential avatars.  Null if none set.
     */
    public Set<CredentialAvatar> getCredentialAvatars() {
        @SuppressWarnings("unchecked")
        Set<CredentialAvatar> r = (Set)getData().get(A_CREDENTIAL_AVATARS_KEY);
        if (r == null) {
            r = new HashSet<CredentialAvatar>();
            getData().put(A_CREDENTIAL_AVATARS_KEY, r);
        }
        return r;
    }

    /**
     * @return True if there are avatars attached to this instance.
     */
    public boolean hasCredentialAvatars() {
        return containsDataKey(A_CREDENTIAL_AVATARS_KEY);
    }


    /**
     * Ask this URI if it was a success or not.
     *
     * Only makes sense to call this method after execution of
     * HttpMethod#execute. Regard any status larger then 0 as success
     * except for below caveat regarding 401s.  Use {@link #is2XXSuccess()} if
     * looking for a status code in the 200 range.
     *
     * <p>401s caveat: If any rfc2617 credential data present and we got a 401
     * assume it got loaded in FetchHTTP on expectation that we're to go around
     * the processing chain again. Report this condition as a failure so we
     * get another crack at the processing chain only this time we'll be making
     * use of the loaded credential data.
     *
     * @return True if ths URI has been successfully processed.
     * @see #is2XXSuccess()
     */
    public boolean isSuccess() {
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
     * @return True if status code is in the 2xx range.
     * @see #isSuccess()
     */
    public boolean is2XXSuccess() {
    	return this.fetchStatus >= 200 && this.fetchStatus < 300;
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
     * Set the retained content-digest value (usu. SHA1). 
     * 
     * @param digestValue
     * @deprecated Use {@link #setContentDigest(String scheme, byte[])}
     */
    public void setContentDigest(byte[] digestValue) {
        setContentDigest("SHA1", digestValue);
    }
    
    public void setContentDigest(final String scheme,
            final byte [] digestValue) {
        this.contentDigest = digestValue;
        this.contentDigestScheme = scheme;
    }
    
    public String getContentDigestSchemeString() {
        if (this.contentDigest == null) {
            return null;
        }
        return this.contentDigestScheme + ":" + getContentDigestString();
    }

    /**
     * Return the retained content-digest value, if any.
     * 
     * @return Digest value.
     */
    public byte[] getContentDigest() {
        return contentDigest;
    }
    
    public String getContentDigestString() {
        if (this.contentDigest == null) {
            return null;
        }
        return Base32.encode(this.contentDigest);
    }

    transient Object holder;
    transient Object holderKey;

    /**
     * Remember a 'holder' to which some enclosing/queueing
     * facility has assigned this CrawlURI
     * .
     * @param obj
     */
    public void setHolder(Object obj) {
        holder=obj;
    }

    /**
     * Return the 'holder' for the convenience of 
     * an external facility.
     *
     * @return holder
     */
    public Object getHolder() {
        return holder;
    }

    /**
     * Remember a 'holderKey' which some enclosing/queueing
     * facility has assigned this CrawlURI
     * .
     * @param obj
     */
    public void setHolderKey(Object obj) {
        holderKey=obj;
    }
    /**
     * Return the 'holderKey' for convenience of 
     * an external facility (Frontier).
     * 
     * @return holderKey 
     */
    public Object getHolderKey() {
        return holderKey;
    }

    /**
     * Get the ordinal (serial number) assigned at creation.
     * 
     * @return ordinal
     */
    public long getOrdinal() {
        return ordinal;
    }
    
    
    public void setOrdinal(long o) {
        this.ordinal = o;
    }

    /** spot for an integer cost to be placed by external facility (frontier).
     *  cost is truncated to 8 bits at times, so should not exceed 255 */
    int holderCost = UNCALCULATED;
    /**
     * Return the 'holderCost' for convenience of external facility (frontier)
     * @return value of holderCost
     */
    public int getHolderCost() {
        return holderCost;
    }

    /**
     * Remember a 'holderCost' which some enclosing/queueing
     * facility has assigned this CrawlURI
     * @param cost value to remember
     */
    public void setHolderCost(int cost) {
        holderCost = cost;
    }

    /** 
     * All discovered outbound Links (navlinks, embeds, etc.) 
     * Can either contain Link instances or CrawlURI instances, or both.
     * The LinksScoper processor converts Link instances in this collection
     * to CrawlURI instances. 
     */
    transient Collection<Link> outLinks = new HashSet<Link>();
    
    
    transient Collection<CrawlURI> outCandidates = new HashSet<CrawlURI>();
    
    /**
     * Returns discovered links.  The returned collection might be empty if
     * no links were discovered, or if something like LinksScoper promoted
     * the links to CrawlURIs.
     * 
     * @return Collection of all discovered outbound Links
     */
    public Collection<Link> getOutLinks() {
        return outLinks;
//        return Transform.subclasses(outLinks, Link.class);
    }
    
    /**
     * Returns discovered candidate URIs.  The returned collection will be
     * emtpy until something like LinksScoper promotes discovered Links
     * into CrawlURIs.
     * 
     * @return  Collection of candidate URIs
     */
    public Collection<CrawlURI> getOutCandidates() {
        return outCandidates;
    }
    
    
    
    

        
    /**
     * Set the (HTML) Base URI used for derelativizing internal URIs. 
     * 
     * @param baseHref String base href to use
     * @throws URIException if supplied string cannot be interpreted as URI
     */
    public void setBaseURI(String baseHref) throws URIException {
        getData().put(A_HTML_BASE, UURIFactory.getInstance(baseHref));
    }
      
    /**
     * Get the (HTML) Base URI used for derelativizing internal URIs. 
     *
     * @return UURI base URI previously set 
     */  
    public UURI getBaseURI() {
        if (!containsDataKey(A_HTML_BASE)) {
            return getUURI();
        }
        return (UURI)getData().get(A_HTML_BASE);
    }
    
    /**
     * Add the key of  items you want to persist across
     * processings.
     * @param key Key to add.
     */
    public static Collection<String> getPersistentDataKeys() {
    	return persistentKeys;
    }

    public void addPersistentDataMapKey(String s) {
        if (!persistentKeys.contains(s)) {
            addDataPersistentMember(s);
        }
    }
    
    /**
     * Add the key of data map items you want to persist across
     * processings.
     * @param key Key to add.
     */
    public static void addDataPersistentMember(String key) {
        persistentKeys.add(key);
    }
    
    /**
     * Remove the key from those data map members persisted. 
     * @param key Key to remove.
     * @return True if list contained the element.
     */
    public static boolean removeDataPersistentMember(String key) {
        return persistentKeys.remove(key);
    }

    /**
     * Custom serialization writing an empty 'outLinks' as null. Estimated
     * to save ~20 bytes in serialized form. 
     * 
     * @param stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeUTF(uuri.toString());
        stream.writeObject((via == null) ? null : via.getURI());
        stream.writeObject((data==null || data.isEmpty()) ? null : data);
        stream.writeObject((outLinks.isEmpty()) ? null : outLinks);
        stream.writeObject((outCandidates.isEmpty()) ? null : outCandidates);
    }

    /**
     * Custom deserialization recreating empty HashSet from null in 'outLinks'
     * slot. 
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        uuri = readUuri(stream.readUTF());
        via = readUuri((String)stream.readObject());
        @SuppressWarnings("unchecked")
        Map<String,Object> temp = (Map<String,Object>)stream.readObject();
        this.data = temp;
        
        @SuppressWarnings("unchecked")
        HashSet<Link> ol = (HashSet<Link>) stream.readObject();
        outLinks = (ol == null) ? new HashSet<Link>() : ol;
        @SuppressWarnings("unchecked")
        HashSet<CrawlURI> oc = (HashSet<CrawlURI>)stream.readObject();
        outCandidates = (oc == null) ? new HashSet<CrawlURI>() : oc;
    }

    
    
    /**
     * Read a UURI from a String, handling a null or URIException
     * 
     * @param u String or null from which to create UURI
     * @return the best UURI instance creatable
     */
    protected UURI readUuri(String u) {
        if (u == null) {
            return null;
        }
        try {
            return UURIFactory.getInstance(u);
        } catch (URIException ux) {
            // simply continue to next try
        }
        try {
            // try adding an junk scheme
            return UURIFactory.getInstance("invalid:" + u);
        } catch (URIException ux) {
            ux.printStackTrace();
            // ignored; method continues
        }
        try {
            // return total junk
            return UURIFactory.getInstance("invalid:");
        } catch (URIException e) {
            e.printStackTrace();
            return null;
        }
    }
    


    public String getDNSServerIPLabel() {
        if (data == null) {
            return null;
        } else {
            return (String)data.get(A_DNS_SERVER_IP_LABEL);
        }
    }

    public long getFetchBeginTime() {
        if (containsDataKey(ModuleAttributeConstants.A_FETCH_BEGAN_TIME)) {
            return (Long)getData().get(ModuleAttributeConstants.A_FETCH_BEGAN_TIME);
        } else {
            return 1L;
        }
    }

    public long getFetchCompletedTime() {
        if (containsDataKey(A_FETCH_COMPLETED_TIME)) {
            return (Long)getData().get(A_FETCH_COMPLETED_TIME);
        } else {
            return 0L;
        }
    }

    public long getFetchDuration() {
        if (!containsDataKey(A_FETCH_COMPLETED_TIME)) {
            return -1;
        }

        long completedTime = getFetchCompletedTime();
        long beganTime = getFetchBeginTime();
        return completedTime - beganTime;
    }
    
    public FetchType getFetchType() {
        return fetchType;
    }
    
    public Collection<Throwable> getNonFatalFailures() {
    	@SuppressWarnings("unchecked")
    	List<Throwable> list = (List)getData().get(A_NONFATAL_ERRORS);
    	if (list == null) {
    		list = new ArrayList<Throwable>();
    		getData().put(A_NONFATAL_ERRORS, list);
    	}
    	
    	// FIXME: Previous code automatically added annotation when "localized error"
    	// was added, override collection to implement that?
        return list;
    }


    public void setDNSServerIPLabel(String label) {
        getData().put(A_DNS_SERVER_IP_LABEL, label);
    }

    public void setError(String msg) {
        // TODO: Figure out where this is read, if ever.
        getData().put("error", msg);
    }

    public void setFetchBeginTime(long time) {
    	getData().put(ModuleAttributeConstants.A_FETCH_BEGAN_TIME, time);
    }

    public void setFetchCompletedTime(long time) {
        getData().put(A_FETCH_COMPLETED_TIME, time);
    }

    public void setFetchType(FetchType type) {
        fetchType = type;
    }

    public void setHttpMethod(HttpMethod method) {
        this.method = method;
        if (method instanceof PostMethod) {
            fetchType = FetchType.HTTP_POST;
        } else if (method instanceof GetMethod) {
            fetchType = FetchType.HTTP_GET;
        } else {
            fetchType = FetchType.UNKNOWN;
        }
    }



    public void setForceRetire(boolean b) {
        getData().put(A_FORCE_RETIRE, b);
    }


    public boolean isWaitReevaluated() {
        if (data == null) {
            return false;
        } else {
            return (Boolean)data.get(A_WAIT_REEVALUATED);
        }
    }
    
    
    public void setWaitReevaluated(boolean b) {
        getData().put(A_WAIT_REEVALUATED, b);
    }

    
    public static enum ContentState { CHANGED, UNCHANGED, UNKNOWN };
    
    public ContentState getContentState() {
        if (data == null) {
            return ContentState.UNKNOWN;
        } else {
            return (ContentState)data.get(A_CONTENT_STATE_KEY);
        }
    }


    public void setContentState(ContentState state) {
        getData().put(A_CONTENT_STATE_KEY, state);
    }

    
    public long getFetchOverdueTime() {
        if (data != null) {
            return (Long)data.get(A_FETCH_OVERDUE);
        }
        return 0L; // TODO: Determine sane default for this
    }
    
    
    public void setWaitInterval(long wi) {
        getData().put(A_WAIT_INTERVAL, wi);
    }


    public HttpMethod getHttpMethod() {
        return method;
    }




    public void setBaseURI(UURI base) {
        getData().put(A_HTML_BASE, base);
    }

    
    
    public long getNextProcessingTime() {
        if (data != null) {
            return (Long)data.get(A_TIME_OF_NEXT_PROCESSING);
        } else {
            return 0L;
        }
    }
    
    
    public void setNextProcessingTime(long t) {
    	getData().put(A_TIME_OF_NEXT_PROCESSING, t);
    }
    
    
    public Map<String,Object> getData() {
        if (data == null) {
            data = new HashMap<String,Object>();
        }
        return data;
    }

    
    /**
     * Set the <tt>isSeed</tt> attribute of this URI.
     * @param b Is this URI a seed, true or false.
     */
    public void setSeed(boolean b) {
        this.isSeed = b;
        if (this.isSeed) {
            if(pathFromSeed==null) {
                this.pathFromSeed = "";
            }
//          seeds created on redirect must have a via to be recognized; don't clear
//            setVia(null);
        }
    }

    
    /**
     * @return Whether seeded.
     */
    public boolean isSeed() {
        return this.isSeed;
    }

    
    /**
     * @return UURI
     */
    public UURI getUURI() {
        return this.uuri;
    }

    
    /**
     * @return path (hop-types) from seed
     */
    public String getPathFromSeed() {
        return this.pathFromSeed;
    }

    /**
     * @return URI via which this one was discovered
     */
    public UURI getVia() {
        return this.via;
    }
    
    
    public void setVia(UURI via) {
        this.via = via;
    }


    /**
     * @return CharSequence context in which this one was discovered
     */
    public LinkContext getViaContext() {
        return this.viaContext;
    }

    
    /**
     * @return True if this CrawlURI was result of a redirect:
     * i.e. Its parent URI redirected to here, this URI was what was in 
     * the 'Location:' or 'Content-Location:' HTTP Header.
     */
    public boolean isLocation() {
        return this.pathFromSeed != null && this.pathFromSeed.length() > 0 &&
            this.pathFromSeed.charAt(this.pathFromSeed.length() - 1) ==
                Hop.REFER.getHopChar();
    }

    
    public void setStateProvider(SheetManager manager) {
        this.manager = manager;
        this.provider = manager.findConfig(SURT.fromURI(toString()));
    }
    
    
    public StateProvider getStateProvider() {
        return provider;
    }

    
    public <T> T get(Object module, Key<T> key) {
        if (provider == null) {
            throw new AssertionError("ToeThread never set up CrawlURI's sheet.");
        }
        return provider.get(module, key);
    }


    //
    // Reporter implementation
    //

    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }
    
    public void singleLineReportTo(PrintWriter w) {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf(".")+1);
        w.print(className);
        w.print(" ");
        w.print(getUURI().toString());
        w.print(" ");
        w.print(pathFromSeed);
        w.print(" ");
        w.print(flattenVia());
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "className uri hopsPath viaUri";
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        // none but default: empty options
        return new String[] {};
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.lang.String, java.io.Writer)
     */
    public void reportTo(String name, PrintWriter writer) {
        singleLineReportTo(writer);
        writer.print("\n");
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) throws IOException {
        reportTo(null,writer);
    }

    
    /**
     * Method returns string version of this URI's referral URI.
     * @return String version of referral URI
     */
    public String flattenVia() {
        return (via == null)? "": via.toString();
    }

    
    public String getSourceTag() {
        return (String)getData().get(A_SOURCE_TAG);
    }
    
    
    public void setSourceTag(String sourceTag) {
        getData().put(A_SOURCE_TAG, sourceTag);
        makeHeritable(A_SOURCE_TAG);
    }

    
    /** Make the given key 'heritable', meaning its value will be 
     * added to descendant CrawlURIs. Only keys with immutable
     * values should be made heritable -- the value instance may 
     * be shared until the data map is serialized/deserialized. 
     * 
     * @param key to make heritable
     */
    public void makeHeritable(String key) {
        @SuppressWarnings("unchecked")
        List<String> heritableKeys = (List<String>)data.get(A_HERITABLE_KEYS);
        if (heritableKeys == null) {
            heritableKeys = new ArrayList<String>();
            data.put(A_HERITABLE_KEYS, heritableKeys);
        }
        heritableKeys.add(key);
    }
    
    /** Make the given key non-'heritable', meaning its value will 
     * not be added to descendant CrawlURIs. Only meaningful if
     * key was previously made heritable.  
     * 
     * @param key to make non-heritable
     */
    public void makeNonHeritable(String key) {
        List heritableKeys = (List)data.get(A_HERITABLE_KEYS);
        if(heritableKeys == null) {
            return;
        }
        heritableKeys.remove(key);
        if(heritableKeys.size()==1) {
            // only remaining heritable key is itself; disable completely
            data.remove(A_HERITABLE_KEYS);
        }
    }

    
    /**
     * Get the token (usually the hostname + port) which indicates
     * what "class" this CrawlURI should be grouped with,
     * for the purposes of ensuring only one item of the
     * class is processed at once, all items of the class
     * are held for a politeness period, etc.
     *
     * @return Token (usually the hostname) which indicates
     * what "class" this CrawlURI should be grouped with.
     */
    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String key) {
        classKey = key;
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
        return forceRevisit;
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
        forceRevisit = b;
    }

    
    /**
     * Tally up the number of transitive (non-simple-link) hops at
     * the end of this CrawlURI's pathFromSeed.
     * 
     * In some cases, URIs with greater than zero but less than some
     * threshold such hops are treated specially. 
     * 
     * <p>TODO: consider moving link-count in here as well, caching
     * calculation, and refactoring CrawlScope.exceedsMaxHops() to use this. 
     * 
     * @return Transhop count.
     */
    public int getTransHops() {
        String path = getPathFromSeed();
        int transCount = 0;
        for(int i=path.length()-1;i>=0;i--) {
            if(path.charAt(i)==Hop.NAVLINK.getHopChar()) {
                break;
            }
            transCount++;
        }
        return transCount;
    }

    
    /**
     * Inherit (copy) the relevant keys-values from the ancestor. 
     * 
     * @param ancestor
     */
    protected void inheritFrom(CrawlURI ancestor) {
        Map<String,Object> adata = ancestor.getData();
        @SuppressWarnings("unchecked")
        List<String> heritableKeys = (List<String>)adata.get(A_HERITABLE_KEYS);
        Map<String,Object> thisData = getData();
        if (heritableKeys != null) {
            for (String key: heritableKeys) {
                thisData.put(key, adata.get(key));
            }
        }
    }
    
    /**
     * Utility method for creation of CandidateURIs found extracting
     * links from this CrawlURI.
     * @param baseUURI BaseUURI for <code>link</code>.
     * @param link Link to wrap CandidateURI in.
     * @return New candidateURI wrapper around <code>link</code>.
     * @throws URIException
     */
    public CrawlURI createCrawlURI(UURI baseUURI, Link link)
    throws URIException {
        UURI u = (link.getDestination() instanceof UURI)?
            (UURI)link.getDestination():
            UURIFactory.getInstance(baseUURI,
                link.getDestination().toString());
        CrawlURI newCaURI = new CrawlURI(u, 
                getPathFromSeed() + link.getHopType().getHopChar(),
                getUURI(), link.getContext());
        newCaURI.inheritFrom(this);
        newCaURI.setStateProvider(manager);
        return newCaURI;
    }


    /**
     * Utility method for creation of CandidateURIs found extracting
     * links from this CrawlURI.
     * @param baseUURI BaseUURI for <code>link</code>.
     * @param link Link to wrap CandidateURI in.
     * @param scheduling How new CandidateURI should be scheduled.
     * @param seed True if this CandidateURI is a seed.
     * @return New candidateURI wrapper around <code>link</code>.
     * @throws URIException
     */
    public CrawlURI createCrawlURI(UURI baseUURI, Link link,
        int scheduling, boolean seed)
    throws URIException {
        final CrawlURI caURI = createCrawlURI(baseUURI, link);
        caURI.setSchedulingDirective(scheduling);
        caURI.setSeed(seed);
        return caURI;
    }

    
    /**
     * @return The UURI this CandidateURI wraps as a string 
     */
    public String toString() {
        return getUURI().toString();
    }

    
    public void incrementDiscardedOutLinks() {
        discardedOutlinks++;
    }

    /**
     * @return the precedence
     */
    public int getPrecedence() {
        return precedence;
    }

    /**
     * @param precedence the precedence to set
     */
    public void setPrecedence(int precedence) {
        this.precedence = precedence;
    }
}