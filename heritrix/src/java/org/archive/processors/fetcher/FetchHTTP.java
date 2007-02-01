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
package org.archive.processors.fetcher;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.net.UURI;
import org.archive.processors.credential.Credential;
import org.archive.processors.credential.CredentialAvatar;
import org.archive.processors.credential.CredentialStore;
import org.archive.processors.credential.Rfc2617Credential;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.processors.util.CrawlHost;
import org.archive.processors.util.CrawlServer;
import org.archive.processors.util.ServerCache;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.processors.ProcessResult;
import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import static org.archive.processors.ProcessorURI.FetchType.HTTP_POST;
import org.archive.httpclient.ConfigurableX509TrustManager;
import org.archive.httpclient.HttpRecorderGetMethod;
import org.archive.httpclient.HttpRecorderMethod;
import org.archive.httpclient.HttpRecorderPostMethod;
import org.archive.httpclient.SingleHttpConnectionManager;
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
import org.archive.io.RecorderTooMuchHeaderException;
import org.archive.state.Dependency;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Key;
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
public class FetchHTTP extends Processor implements CoreAttributeConstants,
        FetchStatusCodes, CrawlStatusListener {
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
     * File to preload cookies from.
     */
    // final public static Key<String> LOAD_COOKIES_FROM_FILE =
    // Key.make("");
    /**
     * When crawl finishes save cookies to this file.
     */
    // final public static Key<String> SAVE_COOKIES_FROM_FILE =
    // Key.make("");
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
     * Whether or not to perform an on-the-fly SHA1 hash of retrieved
     * content-bodies.
     */
    @Expert
    final public static Key<Boolean> SHA1_CONTENT = Key.make(true);

    /**
     * The maximum KB/sec to use when fetching data from a server. The default
     * of 0 means no maximum.
     */
    @Expert
    final public static Key<Integer> FETCH_BANDWIDTH = Key.make(0);

    /**
     * SSL trust level setting attribute name.
     */

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
     * Count of crawl uris handled. Would like to be 'long', but longs aren't
     * atomic
     */
    // private int curisHandled = 0;
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

    public static final String REFERER = "Referer";

    public static final String RANGE = "Range";

    public static final String RANGE_PREFIX = "bytes=0-";

    public static final String HTTP_SCHEME = "http";

    public static final String HTTPS_SCHEME = "https";

    /**
     * 
     */
    @Immutable
    final public static Key<CookieStorage> COOKIE_STORAGE = 
        Key.make(CookieStorage.class, new SimpleCookieStorage());

    /**
     * Disable cookie handling.
     */
    final public static Key<Boolean> IGNORE_COOKIES = Key.make(false);

    /**
     * Store cookies in BDB-backed map.
     */
    // final public static Key<Boolean> USE_BDB_FOR_COOKIES = Key.make(true);
    /**
     * Local IP address or hostname to use when making connections (binding
     * sockets). When not specified, uses default local address(es).
     */
    final public static Key<String> LOCAL_ADDRESS = Key.make("");

    
    /**
     * Used to store credentials.
     */
    @Dependency
    final public static Key<CredentialStore> CREDENTIAL_STORE =
        Key.make(CredentialStore.class, null);
    
    
    /**
     * Used to do DNS lookups.
     */
    @Dependency
    final public static Key<ServerCache> SERVER_CACHE =
        Key.make(ServerCache.class, null);
    
    
    /**
     * Database backing cookie map, if using BDB
     */
    // protected Database cookieDb;
    /**
     * Name of cookie BDB Database
     */
    // public static final String COOKIEDB_NAME = "http_cookies";
    /*
     * FIXME: This needs to live somewhere else static {
     * Protocol.registerProtocol("http", new Protocol("http", new
     * HeritrixProtocolSocketFactory(), 80)); try {
     * Protocol.registerProtocol("https", new Protocol("https",
     * ((ProtocolSocketFactory) new HeritrixSSLProtocolSocketFactory()), 443)); }
     * catch (KeyManagementException e) { e.printStackTrace(); } catch
     * (KeyStoreException e) { e.printStackTrace(); } catch
     * (NoSuchAlgorithmException e) { e.printStackTrace(); } }
     */

    // static final String SERVER_CACHE_KEY = "heritrix.server.cache";
    static final String SSL_FACTORY_KEY = "heritrix.ssl.factory";

    /***************************************************************************
     * Socket factory that has the configurable trust manager installed.
     */
    private SSLSocketFactory sslfactory = null;

    final private CredentialStore credentialStore;

    final private ServerCache serverCache;

    /**
     * Constructor.
     */
    public FetchHTTP(ServerCache cache, CredentialStore cs) {
        this.serverCache = cache;
        this.credentialStore = cs;
    }

    protected void innerProcess(final ProcessorURI curi)
            throws InterruptedException {
        // Note begin time
        curi.setFetchBeginTime(System.currentTimeMillis());

        // Get a reference to the HttpRecorder that is set into this ToeThread.
        Recorder rec = curi.getRecorder();

        // Shall we get a digest on the content downloaded?
        boolean sha1Content = curi.get(this, SHA1_CONTENT);
        if (sha1Content) {
            rec.getRecordedInput().setSha1Digest();
        } else {
            // clear
            rec.getRecordedInput().setDigest(null);
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

        // set hardMax on bytes (if set by operator)
        long hardMax = getMaxLength(curi);

        // Get max fetch rate (bytes/ms). It comes in in KB/sec, which
        // requires nothing to normalize.
        int maxFetchRate = getMaxFetchRate(curi);

        try {
            if (!method.isAborted()) {
                // Force read-to-end, so that any socket hangs occur here,
                // not in later modules.
                rec.getRecordedInput().readFullyOrUntil(softMax, hardMax,
                        1000 * getTimeout(curi), maxFetchRate);
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
            curi.setContentSize(rec.getRecordedInput().getSize());
        }

        curi.setContentDigest(SHA1, rec.getRecordedInput().getDigestValue());
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

        method.setRequestHeader("User-Agent", curi.getUserAgent());
        method.setRequestHeader("From", curi.getFrom());

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

        // TODO: What happens if below method adds a header already
        // added above: e.g. Connection, Range, or Referer?
        setAcceptHeaders(curi, method);

        return configureProxy(curi);
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
        HostConfiguration config = this.http.getHostConfiguration();
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
        // this.getController().addCrawlStatusListener(this);
        configureHttp(defaults);

        CookieStorage cm = defaults.get(this, COOKIE_STORAGE);
        // load cookies from a file if specified in the order file.
        http.getState().setCookiesMap(cm.loadCookiesMap());

        // I tried to get the default KeyManagers but doesn't work unless you
        // point at a physical keystore. Passing null seems to do the right
        // thing so we'll go w/ that.
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null,
                    new TrustManager[] { new ConfigurableX509TrustManager(
                            defaults.get(this, TRUST_LEVEL)) }, null);
            this.sslfactory = context.getSocketFactory();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed configure of ssl context "
                    + e.getMessage(), e);
        }
    }

    public void finalTasks(StateProvider defaults) {
        // At the end save cookies to the file specified in the order file.
        CookieStorage cs = defaults.get(this, COOKIE_STORAGE);
        @SuppressWarnings("unchecked")
        Map<String, Cookie> map = http.getState().getCookiesMap();
        cs.saveCookiesMap(map);
        cleanupHttp();
        super.finalTasks(defaults);
    }

    /**
     * Perform any final cleanup related to the HttpClient instance.
     */
    protected void cleanupHttp() {
        // if(cookieDb!=null) {
        // try {
        // cookieDb.close();
        // } catch (DatabaseException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }
    }

    protected void configureHttp(StateProvider defaults) {
        // Get timeout. Use it for socket and for connection timeout.
        int soTimeout = defaults.get(this, SOTIMEOUT_MS);
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

        String addressStr = defaults.get(this, LOCAL_ADDRESS);
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

        HostConfiguration configOrNull = configureProxy(defaults);
        if (configOrNull != null) {
            // global proxy settings are in effect
            this.http.setHostConfiguration(configOrNull);
        }

        hcmp.setParameter(SSL_FACTORY_KEY, this.sslfactory);
    }

    /**
     * Set the HttpClient HttpState instance to use a BDB-backed StoredSortedMap
     * for cookie storage, if that option is chosen.
     */
    /*
     * private void configureHttpCookies(StateProvider defaults) { // If
     * Bdb-backed cookies chosen, replace map in HttpState if(defaults.get(this,
     * USE_BDB_FOR_COOKIES)) { try { Environment env =
     * getController().getBdbEnvironment(); StoredClassCatalog classCatalog =
     * getController().getClassCatalog(); DatabaseConfig dbConfig = new
     * DatabaseConfig(); dbConfig.setTransactional(false);
     * dbConfig.setAllowCreate(true); cookieDb = env.openDatabase(null,
     * COOKIEDB_NAME, dbConfig); StoredSortedMap cookiesMap = new
     * StoredSortedMap(cookieDb, new StringBinding(), new
     * SerialBinding(classCatalog, Cookie.class), true);
     * this.http.getState().setCookiesMap(cookiesMap); } catch
     * (DatabaseException e) { // TODO Auto-generated catch block
     * logger.severe(e.getMessage()); e.printStackTrace(); } } }
     */

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

    /**
     * Load cookies from a file before the first fetch.
     * <p>
     * The file is a text file in the Netscape's 'cookies.txt' file format.<br>
     * Example entry of cookies.txt file:<br>
     * <br>
     * www.archive.org FALSE / FALSE 1074567117 details-visit texts-cralond<br>
     * <br>
     * Each line has 7 tab-separated fields:<br>
     * <li>1. DOMAIN: The domain that created and have access to the cookie
     * value.
     * <li>2. FLAG: A TRUE or FALSE value indicating if hosts within the given
     * domain can access the cookie value.
     * <li>3. PATH: The path within the domain that the cookie value is valid
     * for.
     * <li>4. SECURE: A TRUE or FALSE value indicating if to use a secure
     * connection to access the cookie value.
     * <li>5. EXPIRATION: The expiration time of the cookie value (unix style.)
     * <li>6. NAME: The name of the cookie value
     * <li>7. VALUE: The cookie value
     * 
     * @param cookiesFile
     *            file in the Netscape's 'cookies.txt' format.
     */
    /*
     * public void loadCookies(String cookiesFile) { // Do nothing if
     * cookiesFile is not specified. if (cookiesFile == null ||
     * cookiesFile.length() <= 0) { return; } RandomAccessFile raf = null; try {
     * raf = new RandomAccessFile(cookiesFile, "r"); String[] cookieParts;
     * String line; Cookie cookie = null; while ((line = raf.readLine()) !=
     * null) { // Line that starts with # is commented line, therefore skip it.
     * if (!line.startsWith("#")) { cookieParts = line.split("\\t"); if
     * (cookieParts.length == 7) { // Create cookie with not expiration date (-1
     * value). // TODO: add this as an option. cookie = new
     * Cookie(cookieParts[0], cookieParts[5], cookieParts[6], cookieParts[2],
     * -1, Boolean.valueOf(cookieParts[3]).booleanValue());
     * 
     * if (cookieParts[1].toLowerCase().equals("true")) {
     * cookie.setDomainAttributeSpecified(true); } else {
     * cookie.setDomainAttributeSpecified(false); }
     * this.http.getState().addCookie(cookie); logger.fine( "Adding cookie: " +
     * cookie.toExternalForm()); } } } } catch (FileNotFoundException e) { // We
     * should probably throw FatalConfigurationException.
     * System.out.println("Could not find file: " + cookiesFile + " (Element: " +
     * ATTR_LOAD_COOKIES + ")"); } catch (IOException e) { // We should probably
     * throw FatalConfigurationException. e.printStackTrace(); } finally { try {
     * if (raf != null) { raf.close(); } } catch (IOException e) {
     * e.printStackTrace(); } } }
     */

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

    /**
     * Load cookies from the file specified in the order file.
     * 
     * <p>
     * The file is a text file in the Netscape's 'cookies.txt' file format.<br>
     * Example entry of cookies.txt file:<br>
     * <br>
     * www.archive.org FALSE / FALSE 1074567117 details-visit texts-cralond<br>
     * <br>
     * Each line has 7 tab-separated fields:<br>
     * <li>1. DOMAIN: The domain that created and have access to the cookie
     * value.
     * <li>2. FLAG: A TRUE or FALSE value indicating if hosts within the given
     * domain can access the cookie value.
     * <li>3. PATH: The path within the domain that the cookie value is valid
     * for.
     * <li>4. SECURE: A TRUE or FALSE value indicating if to use a secure
     * connection to access the cookie value.
     * <li>5. EXPIRATION: The expiration time of the cookie value (unix style.)
     * <li>6. NAME: The name of the cookie value
     * <li>7. VALUE: The cookie value
     */
    /*
     * public void loadCookies(StateProvider defaults) {
     * loadCookies(defaults.get(this, LOAD_COOKIES_FROM_FILE)); }
     */

    /**
     * Saves cookies to the file specified in the order file.
     * 
     * Output file is in the Netscape 'cookies.txt' format.
     * 
     */
    /*
     * public void saveCookies() { try { saveCookies((String)
     * getAttribute(ATTR_SAVE_COOKIES)); } catch (MBeanException e) {
     * logger.warning(e.getLocalizedMessage()); } catch (ReflectionException e) {
     * logger.warning(e.getLocalizedMessage()); } catch
     * (AttributeNotFoundException e) { logger.warning(e.getLocalizedMessage()); } }
     */

    /**
     * Saves cookies to a file.
     * 
     * Output file is in the Netscape 'cookies.txt' format.
     * 
     * @param saveCookiesFile
     *            output file.
     */
    /*
     * public void saveCookies(String saveCookiesFile) { // Do nothing if
     * cookiesFile is not specified. if (saveCookiesFile == null ||
     * saveCookiesFile.length() <= 0) { return; }
     * 
     * FileOutputStream out = null; try { out = new FileOutputStream(new
     * File(saveCookiesFile)); @SuppressWarnings("unchecked") Map<String,Cookie>
     * cookies = http.getState().getCookiesMap(); String tab ="\t"; out.write("#
     * Heritrix Cookie File\n".getBytes()); out.write( "# This file is the
     * Netscape cookies.txt format\n\n".getBytes()); for (Cookie cookie:
     * cookies.values()) { MutableString line = new MutableString(1024 * 2); //
     * Guess an initial size line.append(cookie.getDomain()); line.append(tab);
     * line.append( cookie.isDomainAttributeSpecified() == true ? "TRUE" :
     * "FALSE"); line.append(tab); line.append(cookie.getPath());
     * line.append(tab); line.append( cookie.getSecure() == true ? "TRUE" :
     * "FALSE"); line.append(tab); line.append(cookie.getName());
     * line.append(tab); line.append(cookie.getValue()); line.append("\n");
     * out.write(line.toString().getBytes()); } } catch (FileNotFoundException
     * e) { // We should probably throw FatalConfigurationException.
     * System.out.println("Could not find file: " + saveCookiesFile + "
     * (Element: " + this.SAVE_COOKIES_FROM_FILE + ")"); } catch (IOException e) {
     * e.printStackTrace(); } finally { try { if (out != null) { out.close(); } }
     * catch (IOException e) { e.printStackTrace(); } } }
     */

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    /*
     * protected void listUsedFiles(List<String> list) { // List the cookies
     * files // Add seed file try { String tmp =
     * (String)getAttribute(ATTR_LOAD_COOKIES); if(tmp != null && tmp.length() >
     * 0){ File file = getSettingsHandler().
     * getPathRelativeToWorkingDirectory(tmp); list.add(file.getAbsolutePath()); }
     * tmp = (String)getAttribute(ATTR_SAVE_COOKIES); if(tmp != null &&
     * tmp.length() > 0){ File file = getSettingsHandler().
     * getPathRelativeToWorkingDirectory(tmp); list.add(file.getAbsolutePath()); } }
     * catch (AttributeNotFoundException e) { // TODO Auto-generated catch block
     * e.printStackTrace(); } catch (MBeanException e) { // TODO Auto-generated
     * catch block e.printStackTrace(); } catch (ReflectionException e) { //
     * TODO Auto-generated catch block e.printStackTrace(); } }
     */

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
    // Code removed: Only useful for handling cookies; cookie responsibility
    // has been delegated to CookieStorage
    /*
     * private void writeObject(ObjectOutputStream stream) throws IOException {
     * stream.defaultWriteObject(); // save cookies
     * @SuppressWarnings("unchecked") Collection<Cookie> c =
     * http.getState().getCookiesMap().values(); Cookie[] cookies =
     * c.toArray(new Cookie[c.size()]); stream.writeObject(cookies); }
     * 
     * private void readObject(ObjectInputStream stream) throws IOException,
     * ClassNotFoundException { stream.defaultReadObject(); Cookie cookies[] =
     * (Cookie[]) stream.readObject(); ObjectPlusFilesInputStream coistream =
     * (ObjectPlusFilesInputStream)stream; coistream.registerFinishTask( new
     * PostRestore(cookies) ); }
     * 
     * class PostRestore implements Runnable { Cookie cookies[]; public
     * PostRestore(Cookie cookies[]) { this.cookies = cookies; } public void
     * run() { configureHttp(); for(int i = 0; i < cookies.length; i++) {
     * getHttp().getState().addCookie(cookies[i]); } } }
     */

    /**
     * @return Returns the http instance.
     */
    protected HttpClient getHttp() {
        return this.http;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlCheckpoint(StateProvider defaults, File checkpointDir) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        this.http = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
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
}
