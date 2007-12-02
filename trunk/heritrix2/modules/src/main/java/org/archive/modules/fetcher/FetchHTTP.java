/* FetchHTTP.java
 *
 * $Id$
 *
 * Created on Jun 5, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.modules.fetcher;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.auth.DigestScheme;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import static org.archive.modules.ProcessorURI.FetchType.HTTP_POST;
import static org.archive.modules.fetcher.FetchErrors.*;
import static org.archive.modules.fetcher.FetchStatusCodes.*;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.*;

import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.credential.Credential;
import org.archive.modules.credential.CredentialAvatar;
import org.archive.modules.credential.CredentialStore;
import org.archive.modules.credential.Rfc2617Credential;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRuleSequence;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.ServerCache;
import org.archive.net.UURI;
import org.archive.httpclient.ConfigurableX509TrustManager;
import org.archive.httpclient.HttpRecorderGetMethod;
import org.archive.httpclient.HttpRecorderMethod;
import org.archive.httpclient.HttpRecorderPostMethod;
import org.archive.httpclient.SingleHttpConnectionManager;
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
import org.archive.io.RecorderTooMuchHeaderException;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Nullable;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Recorder;

/**
 * HTTP fetcher that uses <a
 * href="http://jakarta.apache.org/commons/httpclient/">Apache Jakarta Commons
 * HttpClient</a> library.
 * 
 * @author Gordon Mohr
 * @author Igor Ranitovic
 * @author others
 * @version $Id$
 */
public class FetchHTTP extends Processor implements Initializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
            .classnameBasedUID(FetchHTTP.class, 1);

    private static Logger logger = Logger.getLogger(FetchHTTP.class.getName());

    /**
     * Proxy host IP (set only if needed).
     */
    @Expert
    final public static Key<String> HTTP_PROXY_HOST = Key.make("");

    /**
     * Proxy port (set only if needed).
     */
    @Expert
    final public static Key<Integer> HTTP_PROXY_PORT = Key.make(0);

    /**
     * If the fetch is not completed in this number of seconds, give up (and
     * retry later).
     */
    final public static Key<Integer> TIMEOUT_SECONDS = Key.make(1200);

    /**
     * If the socket is unresponsive for this number of milliseconds, give up.
     * Set to zero for no timeout (Not. recommended. Could hang a thread on an
     * unresponsive server). This timeout is used timing out socket opens and
     * for timing out each socket read. Make sure this value is &lt;
     * {@link #TIMEOUT_SECONDS} for optimal configuration: ensures at least one
     * retry read.
     */
    @Expert
    final public static Key<Integer> SOTIMEOUT_MS = Key.make(20000);

    /**
     * Maximum length in bytes to fetch. Fetch is truncated at this length. A
     * value of 0 means no limit.
     */
    final public static Key<Long> MAX_LENGTH_BYTES = Key.make(0L);


    /**
     * Accept Headers to include in each request. Each must be the complete
     * header, e.g., 'Accept-Language: en'.
     */
    final public static Key<List<String>> ACCEPT_HEADERS = Key
            .makeList(String.class);

    /**
     * The character encoding to use for files that do not have one specified in
     * the HTTP response headers. Default: ISO-8859-1.
     */
    @Expert
    final public static Key<String> DEFAULT_ENCODING = Key
            .make("ISO-8859-1");

    /**
     * Whether or not to perform an on-the-fly digest hash of retrieved
     * content-bodies.
     */
    @Expert
    final public static Key<Boolean> DIGEST_CONTENT = Key.make(true);
 
 
    /**
     * Which algorithm (for example MD5 or SHA-1) to use to perform an
     * on-the-fly digest hash of retrieved content-bodies.
     */
    @Expert
    final public static Key<String> DIGEST_ALGORITHM = Key.make("sha1");


    /**
     * The maximum KB/sec to use when fetching data from a server. The default
     * of 0 means no maximum.
     */
    @Expert
    final public static Key<Integer> FETCH_BANDWIDTH = Key.make(0);


    final public static Key<UserAgentProvider> USER_AGENT_PROVIDER =
        Key.makeAuto(UserAgentProvider.class);

    /**
     * SSL certificate trust level. Range is from the default 'open' (trust all
     * certs including expired, selfsigned, and those for which we do not have a
     * CA) through 'loose' (trust all valid certificates including selfsigned),
     * 'normal' (all valid certificates not including selfsigned) to 'strict'
     * (Cert is valid and DN must match servername).
     */
    @Immutable @Expert
    final public static Key<String> TRUST_LEVEL = Key
            .make(ConfigurableX509TrustManager.DEFAULT);

    public static final String SHA1 = "sha1";

    private transient HttpClient http = null;

    /**
     * How many 'instant retries' of HttpRecoverableExceptions have occurred
     * 
     * Would like it to be 'long', but longs aren't atomic
     */
    private int recoveryRetries = 0;


    /**
     * DecideRules applied after receipt of HTTP response headers but before we
     * start to download the body. If any filter returns FALSE, the fetch is
     * aborted. Prerequisites such as robots.txt by-pass filtering (i.e. they
     * cannot be midfetch aborted.
     */
    final public static Key<DecideRuleSequence> MIDFETCH_RULES = Key
            .make(new DecideRuleSequence());

    // see [ 1379040 ] regex for midfetch filter not being stored in crawl order
    // http://sourceforge.net/support/tracker.php?aid=1379040
    // this.midfetchfilters.setExpertSetting(true);

    /**
     * What to log if midfetch abort.
     */
    private static final String MIDFETCH_ABORT_LOG = "midFetchAbort";

    /**
     * Send 'Connection: close' header with every request.
     */
    @Expert
    final public static Key<Boolean> SEND_CONNECTION_CLOSE = Key
            .make(true);

    private static final Header HEADER_SEND_CONNECTION_CLOSE = new Header(
            "Connection", "close");

    /**
     * Send 'Referer' header with every request.
     * <p>
     * The 'Referer' header contans the location the crawler came from, the page
     * the current URI was discovered in. The 'Referer' usually is logged on the
     * remote server and can be of assistance to webmasters trying to figure how
     * a crawler got to a particular area on a site.
     */
    @Expert
    final public static Key<Boolean> SEND_REFERER = Key.make(true);

    /**
     * Send 'Range' header when a limit ({@link #MAX_LENGTH_BYTES}) on
     * document size.
     * <p>
     * Be polite to the HTTP servers and send the 'Range' header, stating that
     * you are only interested in the first n bytes. Only pertinent if
     * {@link #MAX_LENGTH_BYTES} &gt; 0. Sending the 'Range' header results in a
     * '206 Partial Content' status response, which is better than just cutting
     * the response mid-download. On rare occasion, sending 'Range' will
     * generate '416 Request Range Not Satisfiable' response.
     */
    @Expert
    final public static Key<Boolean> SEND_RANGE = Key.make(false);

    
    /**
     * Send 'If-Modified-Since' header, if previous 'Last-Modified' fetch
     * history information is available in URI history.
     */
    @Expert
    final public static Key<Boolean> SEND_IF_MODIFIED_SINCE = Key.make(true);

    /**
     * Send 'If-None-Match' header, if previous 'Etag' fetch history information
     * is available in URI history.
     */
    @Expert
    final public static Key<Boolean> SEND_IF_NONE_MATCH = Key.make(true);

    
    public static final String REFERER = "Referer";

    public static final String RANGE = "Range";

    public static final String RANGE_PREFIX = "bytes=0-";

    public static final String HTTP_SCHEME = "http";

    public static final String HTTPS_SCHEME = "https";

    /**
     * 
     */
    @Immutable @Nullable
    final public static Key<CookieStorage> COOKIE_STORAGE = 
        Key.make(CookieStorage.class, new SimpleCookieStorage());

    /**
     * Disable cookie handling.
     */
    final public static Key<Boolean> IGNORE_COOKIES = Key.make(false);


    /**
     * Local IP address or hostname to use when making connections (binding
     * sockets). When not specified, uses default local address(es).
     */
    final public static Key<String> LOCAL_ADDRESS = Key.make("");

    
    /**
     * Used to store credentials.
     */
    @Immutable
    final public static Key<CredentialStore> CREDENTIAL_STORE =
        Key.makeAuto(CredentialStore.class);
    
    
    /**
     * Used to do DNS lookups.
     */
    @Immutable
    final public static Key<ServerCache> SERVER_CACHE =
        Key.makeAuto(ServerCache.class);
//        Key.make(ServerCache.class, null);


    static {
        Protocol.registerProtocol("http", new Protocol("http",
                new HeritrixProtocolSocketFactory(), 80));
        try {
            ProtocolSocketFactory psf = new HeritrixSSLProtocolSocketFactory();
            Protocol p = new Protocol("https", psf, 443); 
            Protocol.registerProtocol("https", p);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
     

    // static final String SERVER_CACHE_KEY = "heritrix.server.cache";
    static final String SSL_FACTORY_KEY = "heritrix.ssl.factory";

    /***************************************************************************
     * Socket factory that has the configurable trust manager installed.
     */
    private transient SSLSocketFactory sslfactory = null;
    private String trustLevel;

    private CredentialStore credentialStore;

    private ServerCache serverCache;

    /**
     * Constructor.
     */
    public FetchHTTP() {
    }

    protected void innerProcess(final ProcessorURI curi)
            throws InterruptedException {
        // Note begin time
        curi.setFetchBeginTime(System.currentTimeMillis());

        // Get a reference to the HttpRecorder that is set into this ToeThread.
        Recorder rec = curi.getRecorder();

        // Shall we get a digest on the content downloaded?
        boolean digestContent = curi.get(this, DIGEST_CONTENT);
        String algorithm = null;
        if (digestContent) {
            algorithm = curi.get(this, DIGEST_ALGORITHM);
            rec.getRecordedInput().setDigest(algorithm);
        } else {
            // clear
            rec.getRecordedInput().setDigest((MessageDigest)null);
        }

        // Below we do two inner classes that add check of midfetch
        // filters just as we're about to receive the response body.
        String curiString = curi.getUURI().toString();
        HttpMethodBase method = null;
        if (curi.getFetchType() == HTTP_POST) {
            method = new HttpRecorderPostMethod(curiString, rec) {
                protected void readResponseBody(HttpState state,
                        HttpConnection conn) throws IOException, HttpException {
                    addResponseContent(this, curi);
                    if (checkMidfetchAbort(curi, this.httpRecorderMethod, conn)) {
                        doAbort(curi, this, MIDFETCH_ABORT_LOG);
                    } else {
                        super.readResponseBody(state, conn);
                    }
                }
            };
        } else {
            method = new HttpRecorderGetMethod(curiString, rec) {
                protected void readResponseBody(HttpState state,
                        HttpConnection conn) throws IOException, HttpException {
                    addResponseContent(this, curi);
                    if (checkMidfetchAbort(curi, this.httpRecorderMethod, conn)) {
                        doAbort(curi, this, MIDFETCH_ABORT_LOG);
                    } else {
                        super.readResponseBody(state, conn);
                    }
                }
            };
        }

        HostConfiguration customConfigOrNull = configureMethod(curi, method);

        // Populate credentials. Set config so auth. is not automatic.
        boolean addedCredentials = populateCredentials(curi, method);
        method.setDoAuthentication(addedCredentials);

        // set hardMax on bytes (if set by operator)
        long hardMax = getMaxLength(curi);
        // set overall timeout (if set by operator)
        long timeoutMs = 1000 * getTimeout(curi);
        // Get max fetch rate (bytes/ms). It comes in in KB/sec
        long maxRateKBps = getMaxFetchRate(curi);
        rec.getRecordedInput().setLimits(hardMax, timeoutMs, maxRateKBps);

        try {
            this.http.executeMethod(customConfigOrNull, method);
        } catch (RecorderTooMuchHeaderException ex) {
            // when too much header material, abort like other truncations
            doAbort(curi, method, HEADER_TRUNC);
        } catch (IOException e) {
            failedExecuteCleanup(method, curi, e);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions in native
            // code... see
            // http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            failedExecuteCleanup(method, curi, e);
            return;
        }

        // set softMax on bytes to get (if implied by content-length)
        long softMax = method.getResponseContentLength();

        try {
            if (!method.isAborted()) {
                // Force read-to-end, so that any socket hangs occur here,
                // not in later modules.
                rec.getRecordedInput().readFullyOrUntil(softMax);
            }
        } catch (RecorderTimeoutException ex) {
            doAbort(curi, method, TIMER_TRUNC);
        } catch (RecorderLengthExceededException ex) {
            doAbort(curi, method, LENGTH_TRUNC);
        } catch (IOException e) {
            cleanup(curi, e, "readFully", S_CONNECT_LOST);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            cleanup(curi, e, "readFully", S_CONNECT_LOST);
            return;
        } finally {
            // ensure recording has stopped
            rec.closeRecorders();
            if (!method.isAborted()) {
                method.releaseConnection();
            }
            // Note completion time
            curi.setFetchCompletedTime(System.currentTimeMillis());
            // Set the response charset into the HttpRecord if available.
            setCharacterEncoding(curi, rec, method);
            setSizes(curi, rec);
        }

        if (digestContent) {
            curi.setContentDigest(algorithm, 
                rec.getRecordedInput().getDigestValue());
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(((curi.getFetchType() == HTTP_POST) ? "POST" : "GET")
                    + " " + curi.getUURI().toString() + " "
                    + method.getStatusCode() + " "
                    + rec.getRecordedInput().getSize() + " "
                    + curi.getContentType());
        }

        if (isSuccess(curi) && addedCredentials) {
            // Promote the credentials from the ProcessorURI to the CrawlServer
            // so they are available for all subsequent ProcessorURIs on this
            // server.
            promoteCredentials(curi);
            if (logger.isLoggable(Level.FINE)) {
                // Print out the cookie. Might help with the debugging.
                Header setCookie = method.getResponseHeader("set-cookie");
                if (setCookie != null) {
                    logger.fine(setCookie.toString().trim());
                }
            }
        } else if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            // 401 is not 'success'.
            handle401(method, curi);
        }

        if (rec.getRecordedInput().isOpen()) {
            logger.severe(curi.toString() + " RIS still open. Should have"
                    + " been closed by method release: "
                    + Thread.currentThread().getName());
            try {
                rec.getRecordedInput().close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "second-chance RIS close failed", e);
            }
        }
    }

    /**
     * Update CrawlURI internal sizes based on current transaction (and
     * in the case of 304s, history) 
     * 
     * @param curi CrawlURI
     * @param rec HttpRecorder
     */
    protected void setSizes(ProcessorURI curi, Recorder rec) {
        // set reporting size
        curi.setContentSize(rec.getRecordedInput().getSize());
        // special handling for 304-not modified
        if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED
                && curi.containsDataKey(A_FETCH_HISTORY)) {
            Map history[] = (Map[])curi.getData().get(A_FETCH_HISTORY);
            if (history[0] != null
                    && history[0]
                            .containsKey(A_REFERENCE_LENGTH)) {
                long referenceLength = (Long) history[0].get(A_REFERENCE_LENGTH);
                // carry-forward previous 'reference-length' for future
                curi.getData().put(A_REFERENCE_LENGTH, referenceLength);
                // increase content-size to virtual-size for reporting
                curi.setContentSize(rec.getRecordedInput().getSize()
                        + referenceLength);
            }
        }
    }
    
    protected void doAbort(ProcessorURI curi, HttpMethod method,
            String annotation) {
        curi.getAnnotations().add(annotation);
        curi.getRecorder().close();
        method.abort();
    }

    protected boolean checkMidfetchAbort(ProcessorURI curi,
            HttpRecorderMethod method, HttpConnection conn) {
        if (curi.isPrerequisite()) {
            return false;
        }
        DecideRuleSequence seq = curi.get(this, MIDFETCH_RULES);
        DecideResult r = seq.decisionFor(curi);
        if (r != DecideResult.REJECT) {
            return false;
        }
        method.markContentBegin(conn);
        return true;
    }

    /**
     * This method populates <code>curi</code> with response status and
     * content type.
     * 
     * @param curi
     *            ProcessorURI to populate.
     * @param method
     *            Method to get response status and headers from.
     */
    protected void addResponseContent(HttpMethod method, ProcessorURI curi) {
        curi.setFetchStatus(method.getStatusCode());
        Header ct = method.getResponseHeader("content-type");
        curi.setContentType((ct == null) ? null : ct.getValue());
        // Save method into curi too. Midfetch filters may want to leverage
        // info in here.
        curi.setHttpMethod(method);
    }

    /**
     * Set the character encoding based on the result headers or default.
     * 
     * The HttpClient returns its own default encoding ("ISO-8859-1") if one
     * isn't specified in the Content-Type response header. We give the user the
     * option of overriding this, so we need to detect the case where the
     * default is returned.
     * 
     * Now, it may well be the case that the default returned by HttpClient and
     * the default defined by the user are the same.
     * 
     * @param rec
     *            Recorder for this request.
     * @param method
     *            Method used for the request.
     */
    private void setCharacterEncoding(ProcessorURI uri, final Recorder rec,
            final HttpMethod method) {
        String encoding = null;

        try {
            encoding = ((HttpMethodBase) method).getResponseCharSet();
            if (encoding == null
                    || encoding.equals(DEFAULT_ENCODING.getDefaultValue())) {
                encoding = uri.get(this, DEFAULT_ENCODING);
            }
        } catch (Exception e) {
            logger.warning("Failed get default encoding: "
                    + e.getLocalizedMessage());
        }
        rec.setCharacterEncoding(encoding);
    }

    /**
     * Cleanup after a failed method execute.
     * 
     * @param curi
     *            ProcessorURI we failed on.
     * @param method
     *            Method we failed on.
     * @param exception
     *            Exception we failed with.
     */
    private void failedExecuteCleanup(final HttpMethod method,
            final ProcessorURI curi, final Exception exception) {
        cleanup(curi, exception, "executeMethod", S_CONNECT_FAILED);
        method.releaseConnection();
    }

    /**
     * Cleanup after a failed method execute.
     * 
     * @param curi
     *            ProcessorURI we failed on.
     * @param exception
     *            Exception we failed with.
     * @param message
     *            Message to log with failure. FIXME: Seems ignored
     * @param status
     *            Status to set on the fetch.
     */
    private void cleanup(final ProcessorURI curi, final Exception exception,
            final String message, final int status) {
        // message ignored!
        curi.getNonFatalFailures().add(exception);
        curi.setFetchStatus(status);
        curi.getRecorder().close();
    }

    @Override
    public ProcessResult process(ProcessorURI uri) throws InterruptedException {
        if (uri.getFetchStatus() < 0) {
            // already marked as errored, this pass through
            // skip to end
            return ProcessResult.FINISH;
        } else {
            return super.process(uri);
        }
    }

    /**
     * Can this processor fetch the given ProcessorURI. May set a fetch status
     * if this processor would usually handle the ProcessorURI, but cannot in
     * this instance.
     * 
     * @param curi
     * @return True if processor can fetch.
     */
    @Override
    protected boolean shouldProcess(ProcessorURI curi) {
        String scheme = curi.getUURI().getScheme();
        if (!(scheme.equals("http") || scheme.equals("https"))) {
            // handles only plain http and https
            return false;
        }

        CrawlHost host = getHostFor(curi.getUURI());
        if (host.getIP() == null && host.hasBeenLookedUp()) {
            curi.setFetchStatus(S_DOMAIN_PREREQUISITE_FAILURE);
            return false;
        }

        return true;
    }

    /**
     * Configure the HttpMethod setting options and headers.
     * 
     * @param curi
     *            ProcessorURI from which we pull configuration.
     * @param method
     *            The Method to configure.
     */
    protected HostConfiguration configureMethod(ProcessorURI curi,
            HttpMethod method) {
        // Don't auto-follow redirects
        method.setFollowRedirects(false);

        // // set soTimeout
        // method.getParams().setSoTimeout(
        // ((Integer) getUncheckedAttribute(curi, ATTR_SOTIMEOUT_MS))
        // .intValue());

        // Set cookie policy.
        boolean ignoreCookies = curi.get(this, IGNORE_COOKIES);
        method.getParams().setCookiePolicy(
                ignoreCookies ? CookiePolicy.IGNORE_COOKIES
                        : CookiePolicy.BROWSER_COMPATIBILITY);

        // Use only HTTP/1.0 (to avoid receiving chunked responses)
        method.getParams().setVersion(HttpVersion.HTTP_1_0);

        UserAgentProvider uap = curi.get(this, USER_AGENT_PROVIDER);
        String from = uap.getFrom(curi);
        String userAgent = curi.getUserAgent();
        if (userAgent == null) {
            userAgent = uap.getUserAgent(curi);
        }
        
        method.setRequestHeader("User-Agent", userAgent);
        method.setRequestHeader("From", from);

        // Set retry handler.
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new HeritrixHttpMethodRetryHandler());

        final long maxLength = getMaxLength(curi);
        if (maxLength > 0 && curi.get(this, SEND_RANGE)) {
            method.addRequestHeader(RANGE, RANGE_PREFIX.concat(Long
                    .toString(maxLength - 1)));
        }

        if (curi.get(this, SEND_CONNECTION_CLOSE)) {
            method.addRequestHeader(HEADER_SEND_CONNECTION_CLOSE);
        }

        if (curi.get(this, SEND_REFERER)) {
            // RFC2616 says no referer header if referer is https and the url
            // is not
            String via = flattenVia(curi);
            if (via != null
                    && via.length() > 0
                    && !(via.startsWith(HTTPS_SCHEME) && curi.getUURI()
                            .getScheme().equals(HTTP_SCHEME))) {
                method.setRequestHeader(REFERER, via);
            }
        }

        if (!curi.isPrerequisite()) {
            setConditionalGetHeader(curi, method, SEND_IF_MODIFIED_SINCE, 
                    A_LAST_MODIFIED_HEADER, "If-Modified-Since");
            setConditionalGetHeader(curi, method, SEND_IF_NONE_MATCH, 
                    A_ETAG_HEADER, "If-None-Match");
        }
        
        // TODO: What happens if below method adds a header already
        // added above: e.g. Connection, Range, or Referer?
        setAcceptHeaders(curi, method);

        return configureProxy(curi);
    }

    /**
     * Set the given conditional-GET header, if the setting is enabled and
     * a suitable value is available in the URI history. 
     * @param curi source CrawlURI
     * @param method HTTP operation pending
     * @param setting true/false enablement setting name to consult
     * @param sourceHeader header to consult in URI history
     * @param targetHeader header to set if possible
     */
    protected void setConditionalGetHeader(ProcessorURI curi, HttpMethod method, 
            Key<Boolean> setting, String sourceHeader, String targetHeader) {
        if (curi.get(this, setting)) {
            try {
                Map[] history = (Map[])curi.getData().get(A_FETCH_HISTORY);
                int previousStatus = (Integer) history[0].get(A_STATUS);
                if(previousStatus<=0) {
                    // do not reuse headers from any broken fetch
                    return; 
                }
                String previousValue = (String) history[0].get(sourceHeader);
                if(previousValue!=null) {
                    method.setRequestHeader(targetHeader, previousValue);
                }
            } catch (RuntimeException e) {
                // for absent key, bad index, etc. just do nothing
            }
        }
    }
    
    /**
     * Setup proxy, based on attributes in ProcessorURI and settings, for this
     * ProcessorURI only.
     * 
     * @return HostConfiguration customized as necessary, or null if no
     *         customization required
     */
    private HostConfiguration configureProxy(StateProvider curi) {
        String proxy = (String) getAttributeEither(curi, HTTP_PROXY_HOST);
        int port = -1;
        if (proxy.length() == 0) {
            proxy = null;
        } else {
            port = (Integer) getAttributeEither(curi, HTTP_PROXY_PORT);            
        }
        return configureProxy(proxy, port);
    }
    
    private HostConfiguration configureProxy(String proxy, int port) {
        HostConfiguration config = http.getHostConfiguration();
        if (config.getProxyHost() == proxy && config.getProxyPort() == port) {
            // no change
            return null;
        }
        if (proxy != null && proxy.equals(config.getProxyHost())
                && config.getProxyPort() == port) {
            // no change
            return null;
        }
        config = new HostConfiguration(config); // copy of config
        System.out.println("Configuring " + proxy + ":" + port);
        config.setProxy(proxy, port);
        return config;
    }

    /**
     * Get a value either from inside the ProcessorURI instance, or from
     * settings (module attributes).
     * 
     * @param curi
     *            ProcessorURI to consult
     * @param key
     *            key to lookup
     * @return value from either ProcessorURI (preferred) or settings
     */
    protected Object getAttributeEither(StateProvider provider, Key<?> key) {
        if (provider instanceof ProcessorURI) {
            ProcessorURI curi = (ProcessorURI) provider;
            Object r = curi.getData().get(key.getFieldName());
            if (r != null) {
                return r;
            }
        }
        return provider.get(this, key);
    }

    /**
     * Add credentials if any to passed <code>method</code>.
     * 
     * Do credential handling. Credentials are in two places. 1. Credentials
     * that succeeded are added to the CrawlServer (Or rather, avatars for
     * credentials are whats added because its not safe to keep around
     * references to credentials). 2. Credentials to be tried are in the curi.
     * Returns true if found credentials to be tried.
     * 
     * @param curi
     *            Current ProcessorURI.
     * @param method
     *            The method to add to.
     * @return True if prepopulated <code>method</code> with credentials AND
     *         the credentials came from the <code>curi</code>, not from the
     *         CrawlServer. The former is special in that if the
     *         <code>curi</curi> credentials
     * succeed, then the caller needs to promote them from the ProcessorURI to the
     * CrawlServer so they are available for all subsequent ProcessorURIs on this
     * server.
     */
    private boolean populateCredentials(ProcessorURI curi, HttpMethod method) {
        // First look at the server avatars. Add any that are to be volunteered
        // on every request (e.g. RFC2617 credentials). Every time creds will
        // return true when we call 'isEveryTime().
        String serverKey;
        try {
            serverKey = CrawlServer.getServerKey(curi.getUURI());
        } catch (URIException e) {
            return false;
        }
        CrawlServer server = serverCache.getServerFor(serverKey);
        if (server.hasCredentialAvatars()) {
            for (CredentialAvatar ca : server.getCredentialAvatars()) {
                Credential c = ca.getCredential(credentialStore, curi);
                if (c.isEveryTime()) {
                    c.populate(curi, this.http, method, ca.getPayload());
                }
            }
        }

        boolean result = false;

        // Now look in the curi. The Curi will have credentials loaded either
        // by the handle401 method if its a rfc2617 or it'll have been set into
        // the curi by the preconditionenforcer as this login uri came through.
        for (CredentialAvatar ca : curi.getCredentialAvatars()) {
            Credential c = ca.getCredential(credentialStore, curi);
            if (c.populate(curi, this.http, method, ca.getPayload())) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Promote successful credential to the server.
     * 
     * @param curi
     *            ProcessorURI whose credentials we are to promote.
     */
    private void promoteCredentials(final ProcessorURI curi) {
        Set<CredentialAvatar> avatars = curi.getCredentialAvatars();
        for (Iterator<CredentialAvatar> i = avatars.iterator(); i.hasNext();) {
            CredentialAvatar ca = i.next();
            i.remove();
            // The server to attach too may not be the server that hosts
            // this passed curi. It might be of another subdomain.
            // The avatar needs to be added to the server that is dependent
            // on this precondition. Find it by name. Get the name from
            // the credential this avatar represents.
            Credential c = credentialStore.getCredential(curi, ca);
            String cd = c.getCredentialDomain(curi);
            if (cd != null) {
                CrawlServer cs = serverCache.getServerFor(cd);
                if (cs != null) {
                    cs.addCredentialAvatar(ca);
                }
            }
        }
    }

    /**
     * Server is looking for basic/digest auth credentials (RFC2617). If we have
     * any, put them into the ProcessorURI and have it come around again.
     * Presence of the credential serves as flag to frontier to requeue
     * promptly. If we already tried this domain and still got a 401, then our
     * credentials are bad. Remove them and let this curi die.
     * 
     * @param method
     *            Method that got a 401.
     * @param curi
     *            ProcessorURI that got a 401.
     */
    protected void handle401(final HttpMethod method, final ProcessorURI curi) {
        AuthScheme authscheme = getAuthScheme(method, curi);
        if (authscheme == null) {
            return;
        }
        String realm = authscheme.getRealm();

        /*
         * ======================================================= // Look to
         * see if this curi had rfc2617 avatars loaded. If so, are // any of
         * them for this realm? If so, then the credential failed // if we got a
         * 401 and it should be let die a natural 401 death. if
         * (curi.detachRfc2617Credential(realm)) { // Then, already tried this
         * credential. Remove ANY rfc2617 // credential since presence of a
         * rfc2617 credential serves // as flag to frontier to requeue this curi
         * and let the curi // die a natural death. logger.warning("Auth failed
         * (401) though supplied realm " + realm + " to " + curi.toString());
         * return; } curi.attachRfc2617Credential(realm);
         * =============================================================
         */

        // Look to see if this curi had rfc2617 avatars loaded. If so, are
        // any of them for this realm? If so, then the credential failed
        // if we got a 401 and it should be let die a natural 401 death.
        Set curiRfc2617Credentials = getCredentials(curi,
                Rfc2617Credential.class);
        Rfc2617Credential extant = Rfc2617Credential.getByRealm(
                curiRfc2617Credentials, realm, curi);
        if (extant != null) {
            // Then, already tried this credential. Remove ANY rfc2617
            // credential since presence of a rfc2617 credential serves
            // as flag to frontier to requeue this curi and let the curi
            // die a natural death.
            extant.detachAll(curi);
            logger.warning("Auth failed (401) though supplied realm " + realm
                    + " to " + curi.toString());
        } else {
            // Look see if we have a credential that corresponds to this
            // realm in credential store. Filter by type and credential
            // domain. If not, let this curi die. Else, add it to the
            // curi and let it come around again. Add in the AuthScheme
            // we got too. Its needed when we go to run the Auth on
            // second time around.
            String serverKey = getServerKey(curi);
            CrawlServer server = serverCache.getServerFor(serverKey);
            Set storeRfc2617Credentials = credentialStore.subset(curi,
                    Rfc2617Credential.class, server.getName());
            if (storeRfc2617Credentials == null
                    || storeRfc2617Credentials.size() <= 0) {
                logger.info("No rfc2617 credentials for " + curi);
            } else {
                Rfc2617Credential found = Rfc2617Credential.getByRealm(
                        storeRfc2617Credentials, realm, curi);
                if (found == null) {
                    logger.info("No rfc2617 credentials for realm " + realm
                            + " in " + curi);
                } else {
                    found.attach(curi, authscheme.getRealm());
                    logger.info("Found credential for realm " + realm
                            + " in store for " + curi.toString());
                }
            }
        }
    }

    /**
     * @param method
     *            Method that got a 401.
     * @param curi
     *            ProcessorURI that got a 401.
     * @return Returns first wholesome authscheme found else null.
     */
    protected AuthScheme getAuthScheme(final HttpMethod method,
            final ProcessorURI curi) {
        Header[] headers = method.getResponseHeaders("WWW-Authenticate");
        if (headers == null || headers.length <= 0) {
            logger.info("We got a 401 but no WWW-Authenticate challenge: "
                    + curi.toString());
            return null;
        }

        Map authschemes = null;
        try {
            authschemes = AuthChallengeParser.parseChallenges(headers);
        } catch (MalformedChallengeException e) {
            logger.info("Failed challenge parse: " + e.getMessage());
        }
        if (authschemes == null || authschemes.size() <= 0) {
            logger.info("We got a 401 and WWW-Authenticate challenge"
                    + " but failed parse of the header " + curi.toString());
            return null;
        }

        AuthScheme result = null;
        // Use the first auth found.
        for (Iterator i = authschemes.keySet().iterator(); result == null
                && i.hasNext();) {
            String key = (String) i.next();
            String challenge = (String) authschemes.get(key);
            if (key == null || key.length() <= 0 || challenge == null
                    || challenge.length() <= 0) {
                logger.warning("Empty scheme: " + curi.toString() + ": "
                        + headers);
            }
            AuthScheme authscheme = null;
            if (key.equals("basic")) {
                authscheme = new BasicScheme();
            } else if (key.equals("digest")) {
                authscheme = new DigestScheme();
            } else {
                logger.info("Unsupported scheme: " + key);
                continue;
            }

            try {
                authscheme.processChallenge(challenge);
            } catch (MalformedChallengeException e) {
                logger.info(e.getMessage() + " " + curi + " " + headers);
                continue;
            }
            if (authscheme.isConnectionBased()) {
                logger.info("Connection based " + authscheme);
                continue;
            }

            if (authscheme.getRealm() == null
                    || authscheme.getRealm().length() <= 0) {
                logger.info("Empty realm " + authscheme + " for " + curi);
                continue;
            }
            result = authscheme;
        }

        return result;
    }

    /**
     * @param curi
     *            ProcessorURI that got a 401.
     * @param type
     *            Class of credential to get from curi.
     * @return Set of credentials attached to this curi.
     */
    private Set<Credential> getCredentials(ProcessorURI curi, Class type) {
        Set<Credential> result = null;

        if (curi.hasCredentialAvatars()) {
            for (Iterator i = curi.getCredentialAvatars().iterator(); i
                    .hasNext();) {
                CredentialAvatar ca = (CredentialAvatar) i.next();
                if (ca.match(type)) {
                    if (result == null) {
                        result = new HashSet<Credential>();
                    }
                    result.add(ca.getCredential(credentialStore, curi));
                }
            }
        }
        return result;
    }

    public void initialTasks(StateProvider defaults) {
        super.initialTasks(defaults);
        
        this.serverCache = defaults.get(this, SERVER_CACHE);
        this.credentialStore = defaults.get(this, CREDENTIAL_STORE);

        configureHttp(defaults);

        CookieStorage cm = defaults.get(this, COOKIE_STORAGE);
        if (cm != null) {        
            cm.initialTasks(defaults);
            http.getState().setCookiesMap(cm.getCookiesMap());
        }

        this.trustLevel = defaults.get(this, TRUST_LEVEL);
        setSSLFactory();
    }
    
    
    private void setSSLFactory() {
        // I tried to get the default KeyManagers but doesn't work unless you
        // point at a physical keystore. Passing null seems to do the right
        // thing so we'll go w/ that.
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null,
                    new TrustManager[] { new ConfigurableX509TrustManager(
                            trustLevel) }, null);
            this.sslfactory = context.getSocketFactory();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed configure of ssl context "
                    + e.getMessage(), e);
        }
        
    }


    public void finalTasks(StateProvider defaults) {
        // At the end save cookies to the file specified in the order file.
        CookieStorage cs = defaults.get(this, COOKIE_STORAGE);
        if (cs != null) {
            @SuppressWarnings("unchecked")
            Map<String, Cookie> map = http.getState().getCookiesMap();
            cs.saveCookiesMap(map);
            cs.finalTasks(defaults);
        }
        cleanupHttp();
        super.finalTasks(defaults);
    }

    /**
     * Perform any final cleanup related to the HttpClient instance.
     */
    protected void cleanupHttp() {
    }

    protected void configureHttp(StateProvider defaults) {
        int soTimeout = defaults.get(this, SOTIMEOUT_MS);
        String addressStr = defaults.get(this, LOCAL_ADDRESS);
        String proxy = (String) getAttributeEither(defaults, HTTP_PROXY_HOST);
        int port = -1;
        if (proxy.length() == 0) {
            proxy = null;
        } else {
            port = (Integer) getAttributeEither(defaults, HTTP_PROXY_PORT);            
        }

        configureHttp(soTimeout, addressStr, proxy, port);
    }
    
    protected void configureHttp(int soTimeout, String addressStr,
            String proxy, int port) {
        // Get timeout. Use it for socket and for connection timeout.
        int timeout = (soTimeout > 0) ? soTimeout : 0;

        // HttpConnectionManager cm = new ThreadLocalHttpConnectionManager();
        HttpConnectionManager cm = new SingleHttpConnectionManager();

        // TODO: The following settings should be made in the corresponding
        // HttpConnectionManager, not here.
        HttpConnectionManagerParams hcmp = cm.getParams();
        hcmp.setConnectionTimeout(timeout);
        hcmp.setStaleCheckingEnabled(true);
        // Minimizes bandwidth usage. Setting to true disables Nagle's
        // algorithm. IBM JVMs < 142 give an NPE setting this boolean
        // on ssl sockets.
        hcmp.setTcpNoDelay(false);

        this.http = new HttpClient(cm);
        HttpClientParams hcp = this.http.getParams();
        // Set default socket timeout.
        hcp.setSoTimeout(timeout);
        // Set client to be version 1.0.
        hcp.setVersion(HttpVersion.HTTP_1_0);

        if (addressStr != null && addressStr.length() > 0) {
            try {
                InetAddress localAddress = InetAddress.getByName(addressStr);
                this.http.getHostConfiguration().setLocalAddress(localAddress);
            } catch (UnknownHostException e) {
                // Convert all to RuntimeException so get an exception out
                // if initialization fails.
                throw new RuntimeException("Unknown host " + addressStr
                        + " in local-address");
            }
        }

        // configureHttpCookies(defaults);

        // Configure how we want the method to act.
        this.http.getParams().setParameter(
                HttpMethodParams.SINGLE_COOKIE_HEADER, new Boolean(true));
        this.http.getParams().setParameter(
                HttpMethodParams.UNAMBIGUOUS_STATUS_LINE, new Boolean(false));
        this.http.getParams().setParameter(
                HttpMethodParams.STRICT_TRANSFER_ENCODING, new Boolean(false));
        this.http.getParams().setIntParameter(
                HttpMethodParams.STATUS_LINE_GARBAGE_LIMIT, 10);

        if ((proxy != null) && (proxy.length() == 0)) {
            proxy = null;
        }
        HostConfiguration configOrNull = configureProxy(proxy, port);
        if (configOrNull != null) {
            // global proxy settings are in effect
            this.http.setHostConfiguration(configOrNull);
        }

        hcmp.setParameter(SSL_FACTORY_KEY, this.sslfactory);
    }


    /**
     * @param curi
     *            Current ProcessorURI. Used to get context.
     * @return Timeout value for total request.
     */
    private int getTimeout(ProcessorURI curi) {
        return curi.get(this, TIMEOUT_SECONDS);
    }

    private int getMaxFetchRate(ProcessorURI curi) {
        return curi.get(this, FETCH_BANDWIDTH);
    }

    private long getMaxLength(ProcessorURI curi) {
        return curi.get(this, MAX_LENGTH_BYTES);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.fetcher.FetchHTTP\n");
        ret.append("  Function:          Fetch HTTP URIs\n");
        ret.append("  ProcessorURIs handled: " + this.getURICount() + "\n");
        ret.append("  Recovery retries:   " + this.recoveryRetries + "\n\n");

        return ret.toString();
    }


    private void setAcceptHeaders(ProcessorURI curi, HttpMethod get) {
        List<String> acceptHeaders = curi.get(this, ACCEPT_HEADERS);
        if (acceptHeaders.isEmpty()) {
            return;
        }
        for (String hdr : acceptHeaders) {
            String[] nvp = hdr.split(": +");
            if (nvp.length == 2) {
                get.setRequestHeader(nvp[0], nvp[1]);
            } else {
                logger.warning("Invalid accept header: " + hdr);
            }
        }
    }

    // custom serialization

    private String getLocalAddress() {
        HostConfiguration hc = http.getHostConfiguration();
        if (hc == null) {
            return "";
        }
        
        InetAddress addr = hc.getLocalAddress();
        if (addr == null) {
            return "";
        }
        
        String r = addr.getHostName();
        if (r == null) {
            return "";
        }
        
        return r;
    }
    
    
    private String getProxyHost() {
        HostConfiguration hc = http.getHostConfiguration();
        if (hc == null) {
            return "";
        }
        
        String r = hc.getProxyHost();
        if (r == null) {
            return "";
        }
        
        return r;
    }
    
    
    private int getProxyPort() {
        HostConfiguration hc = http.getHostConfiguration();
        if (hc == null) {
            return -1;
        }
        
        return hc.getProxyPort();
    }


    private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();

         // Special handling for http since it isn't Serializable itself
         stream.writeInt(http.getParams().getSoTimeout());
         stream.writeUTF(getLocalAddress());
         stream.writeUTF(getProxyHost());
         stream.writeInt(getProxyPort());
     }


    private void readObject(ObjectInputStream stream) 
     throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         
         int soTimeout = stream.readInt();
         String localAddress = stream.readUTF();
         String proxy = stream.readUTF();
         int port = stream.readInt();
         
         configureHttp(soTimeout, localAddress, proxy, port);
         setSSLFactory();
     }


    /**
     * @return Returns the http instance.
     */
    protected HttpClient getHttp() {
        return this.http;
    }

    private static String getServerKey(ProcessorURI uri) {
        try {
            return CrawlServer.getServerKey(uri.getUURI());
        } catch (URIException e) {
            logger.severe(e.getMessage() + ": " + uri);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CrawlHost} associated with <code>curi</code>.
     * 
     * @param uuri
     *            CandidateURI we're to return Host for.
     * @return CandidateURI instance that matches the passed Host name.
     */
    private CrawlHost getHostFor(UURI uuri) {
        CrawlHost h = null;
        try {
            h = serverCache.getHostFor(uuri.getReferencedHost());
        } catch (URIException e) {
            e.printStackTrace();
        }
        return h;
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(FetchHTTP.class);
    }
}
