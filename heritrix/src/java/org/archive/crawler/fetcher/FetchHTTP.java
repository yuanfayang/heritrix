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
package org.archive.crawler.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpConstants;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.HttpAuthenticator;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.protocol.Protocol;
import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.datamodel.credential.CredentialAvatar;
import org.archive.crawler.datamodel.credential.Rfc2617Credential;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.settings.StringList;
import org.archive.httpclient.CloseConnectionMarker;
import org.archive.httpclient.ConfigurableTrustManagerProtocolSocketFactory;
import org.archive.httpclient.HttpRecorderGetMethod;
import org.archive.httpclient.HttpRecorderPostMethod;
import org.archive.httpclient.PatchedHttpClient;
import org.archive.httpclient.SingleHttpConnectionManager;
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
import org.archive.util.ArchiveUtils;
import org.archive.util.ConfigurableX509TrustManager;
import org.archive.util.HttpRecorder;

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
public class FetchHTTP extends Processor
    	implements CoreAttributeConstants, FetchStatusCodes {
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(FetchHTTP.class,1);
    
    private static Logger logger = Logger.getLogger(FetchHTTP.class.getName());

    public static final String ATTR_HTTP_PROXY_HOST = "http-proxy-host";
    public static final String ATTR_HTTP_PROXY_PORT = "http-proxy-port";
    public static final String ATTR_TIMEOUT_SECONDS = "timeout-seconds";
    public static final String ATTR_SOTIMEOUT_MS = "sotimeout-ms";
    public static final String ATTR_MAX_LENGTH_BYTES = "max-length-bytes";
    public static final String ATTR_LOAD_COOKIES = "load-cookies-from-file";
    public static final String ATTR_SAVE_COOKIES = "save-cookies-to-file";
    public static final String ATTR_ACCEPT_HEADERS = "accept-headers";
    public static final String ATTR_DEFAULT_ENCODING = "default-encoding";
    public static final String ATTR_SHA1_CONTENT = "sha1-content";
   
    /**
     * SSL trust level setting attribute name.
     */
    public static final String ATTR_TRUST = "trust-level";
    
    private static Integer DEFAULT_TIMEOUT_SECONDS = new Integer(1200);
    private static Integer DEFAULT_SOTIMEOUT_MS = new Integer(20000);
    private static Long DEFAULT_MAX_LENGTH_BYTES = new Long(Long.MAX_VALUE);

    /**
     * Default character encoding to use for pages that do not specify.
     * Instead of using HttpConstants.DEFAULT_CONTENT_CHARSET directly, define
     * this here so the definition can be trivially changed later.
     */
    private static String DEFAULT_DEFAULT_ENCODING =
        HttpConstants.DEFAULT_CONTENT_CHARSET;

    /**
     * Default whether to perform on-the-fly SHA1 hashing of content-bodies.
     */
    private static Boolean DEFAULT_SHA1_CONTENT = new Boolean(true);
    
   /**
     * Default setting for HttpClient's "strict mode".
     * In strict mode, Cookies are served on a single header.
     */
    private static final boolean DEFAULT_HTTPCLIENT_STRICT = true;

    transient PatchedHttpClient http = null;

    private int soTimeout;

    /**
     * How many 'instant retries' of HttpRecoverableExceptions have occurred
     * 
     * Would like it to be 'long', but longs aren't atomic
     */
    private int recoveryRetries = 0;

    // Would like to be 'long', but longs aren't atomic
    private int curisHandled = 0;

    /**
     * Constructor.
     *
     * @param name Name of this processor.
     */
    public FetchHTTP(String name) {
        super(name, "HTTP Fetcher");
        Type e;
        addElementToDefinition(new SimpleType(ATTR_TIMEOUT_SECONDS,
            "If the fetch is not completed in this number of seconds,"
            + " give up", DEFAULT_TIMEOUT_SECONDS));
        e = addElementToDefinition(new SimpleType(ATTR_SOTIMEOUT_MS,
            "If the socket is unresponsive for this number of milliseconds, "
            + "give up (and retry later)", DEFAULT_SOTIMEOUT_MS));
        e.setExpertSetting(true);
        addElementToDefinition(new SimpleType(ATTR_MAX_LENGTH_BYTES,
            "Max length in bytes to fetch (truncate at this length)",
            DEFAULT_MAX_LENGTH_BYTES));
        e = addElementToDefinition(new SimpleType(ATTR_LOAD_COOKIES,
            "File to preload cookies from", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SAVE_COOKIES,
            "When crawl finishes save cookies to this file", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_TRUST,
            "SSL certificate trust level.  Range is from the default 'open'"
            + " (trust all certs including expired, selfsigned, and those for"
            + " which we do not have a CA) through 'loose' (trust all valid"
            + " certificates including selfsigned), 'normal' (all valid"
            + " certificates not including selfsigned) to 'strict' (Cert is"
            + " valid and DN must match servername)",
            ConfigurableX509TrustManager.DEFAULT,
            ConfigurableX509TrustManager.LEVELS_AS_ARRAY));
        e.setOverrideable(false);
        e.setExpertSetting(true);
        e = addElementToDefinition(new StringList(ATTR_ACCEPT_HEADERS,
            "Accept Headers to include in each request. Each must be the"
            + " complete header, e.g., 'Accept-Language: en'"));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_HTTP_PROXY_HOST,
            "Proxy hostname (set only if needed)", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_HTTP_PROXY_PORT,
            "Proxy port (set only if needed)", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_DEFAULT_ENCODING,
            "The character encoding to use for files that do not have one" +
            " specified in the HTTP response headers.  Default: " +
            "ISO-8859-1.",
            DEFAULT_DEFAULT_ENCODING));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SHA1_CONTENT,
                "Whether or not to perform an on-the-fly SHA1 hash of" +
                "retrieved content-bodies.",
                DEFAULT_SHA1_CONTENT));
            e.setExpertSetting(true);
    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (!canFetch(curi)) {
            // Cannot fetch this, due to protocol, retries, or other problems
            return;
        }

        this.curisHandled++;

        // Note begin time
        curi.getAList().putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());

        // Get a reference to the HttpRecorder that is set into this ToeThread.
        HttpRecorder rec = HttpRecorder.getHttpRecorder();
        boolean sha1Content = ((Boolean) getUncheckedAttribute(curi,
                ATTR_SHA1_CONTENT)).booleanValue();
        if(sha1Content) {
            rec.getRecordedInput().setSha1Digest();
        } else {
            // clear
            rec.getRecordedInput().setDigest(null);
        }
        HttpMethod method = curi.isPost()?
            (HttpMethod)new HttpRecorderPostMethod(
                curi.getUURI().toString(), rec):
            (HttpMethod)new HttpRecorderGetMethod(
                curi.getUURI().toString(), rec);
        configureMethod(curi, method);
        maybeSetAcceptHeaders(curi, method);
        boolean addedCredentials = populateCredentials(curi, method);
        int immediateRetries = 0;
        while (true) {
            // Retry until success (break) or unrecoverable exception
            // (early return)
            try {
                // TODO: make this initial reading subject to the same
                // length/timeout limits; currently only the soTimeout
                // is effective here, once the connection succeeds
                this.http.executeMethod(method);
                break;
            } catch (HttpRecoverableException e) {
                checkForInterrupt();
                if (immediateRetries < getMaxImmediateRetries()) {
                    // See "[ 910219 ] [httpclient] unable...starting with"
                    // http://sourceforge.net/tracker/?group_id=73833&atid=539099&func=detail&aid=910219
                    // for the justification for this loop.
                    this.recoveryRetries++;
                    immediateRetries++;
                    continue;
                } else {
                    // Treat as connect failed
                    failedExecuteCleanup(method, curi, e);
                    return;
                }
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
        }

        try {
            // Force read-to-end, so that any socket hangs occur here,
            // not in later modules
            rec.getRecordedInput().readFullyOrUntil(getMaxLength(curi),
                1000 * getTimeout(curi));
        } catch (RecorderTimeoutException ex) {
            curi.addAnnotation("timeTrunc");
            if (method instanceof CloseConnectionMarker) {
                ((CloseConnectionMarker)method).closeConnection();
            } else {
                logger.severe("Exceeded download time limit but method does" +
                    " not support close.");
            }
        } catch (RecorderLengthExceededException ex) {
            curi.addAnnotation("lengthTrunc");
            if (method instanceof CloseConnectionMarker) {
                ((CloseConnectionMarker)method).closeConnection();
            } else {
                logger.severe("Exceeded download size limit but method does" +
                    " not support close.");
            }
        } catch (IOException e) {
            cleanup(method, curi, e, "readFully", S_CONNECT_LOST);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            cleanup(method, curi, e, "readFully", S_CONNECT_LOST);
            return;
        } finally {
            method.releaseConnection();
        }

        // Note completion time
        curi.getAList().putLong(A_FETCH_COMPLETED_TIME,
            System.currentTimeMillis());

        // Set the response charset into the HttpRecord if available.
        setCharacterEncoding(rec, method);

        // Set httpRecorder into curi for convenience of subsequent processors.
        curi.setHttpRecorder(rec);

        int statusCode = method.getStatusCode();
        long contentSize = curi.getHttpRecorder().getRecordedInput().getSize();
        curi.setContentSize(contentSize);
        curi.setFetchStatus(statusCode);
        Header ct = method.getResponseHeader("content-type");
        curi.setContentType((ct == null)? null: ct.getValue());
        curi.setContentDigest(rec.getRecordedInput().getDigestValue());
        if (logger.isLoggable(Level.FINE)) {
            logger.fine((curi.isPost()? "POST": "GET") + " " +
            		curi.getUURI().toString() + " " + statusCode + " " +
                    contentSize + " " + curi.getContentType());
        }

        if (curi.isSuccess() && addedCredentials) {
            // Promote the credentials from the CrawlURI to the CrawlServer
            // so they are available for all subsequent CrawlURIs on this
            // server.
            promoteCredentials(curi);
            if (logger.isLoggable(Level.FINE)) {
                // Print out the cookie.  Might help with the debugging.
                Header setCookie = method.getResponseHeader("set-cookie");
                if (setCookie != null) {
                    logger.fine(setCookie.toString().trim());
                }
            }
        } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            // 401 is not 'success'.
            handle401(method, curi);
        }

        // Save off the GetMethod just in case needed by subsequent processors.
        curi.getAList().putObject(A_HTTP_TRANSACTION, method);
    }

    /**
     * Set the character encoding based on the result headers or default.
     *
     * The HttpClient returns its own default encoding ("ISO-8859-1") if one
     * isn't specified in the Content-Type response header. We give the user
     * the option of overriding this, so we need to detect the case where the
     * default is returned.
     *
     * Now, it may well be the case that the default returned by HttpClient
     * and the default defined by the user are the same.
     * 
     * @param rec Recorder for this request.
     * @param method Method used for the request.
     */
    private void setCharacterEncoding(final HttpRecorder rec,
                                      final HttpMethod method)
    {
        String encoding = null;

        try {
            encoding = ((HttpMethodBase) method).getResponseCharSet();
            if (encoding == null ||
                    encoding.equals(HttpConstants.DEFAULT_CONTENT_CHARSET)) {
                encoding = (String) getAttribute(ATTR_DEFAULT_ENCODING);
            }
        } catch (Exception e) {
            logger.warning("Failed get default encoding: " +
                e.getLocalizedMessage());
        }
        rec.setCharacterEncoding(encoding);
    }

    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param method Method we failed on.
     * @param exception Exception we failed with.
     */
    private void failedExecuteCleanup(final HttpMethod method,
            final CrawlURI curi, final Exception exception) {
        cleanup(method, curi, exception, "executeMethod", S_CONNECT_FAILED);
    }
    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param method Method we failed on.
     * @param exception Exception we failed with.
     * @param message Message to log with failure.
     * @param status Status to set on the fetch.
     */
    private void cleanup(final HttpMethod method, final CrawlURI curi,
            final Exception exception, final String message, final int status) {
        curi.addLocalizedError(this.getName(), exception, message);
        curi.setFetchStatus(status);

        // Its ok if releaseConnection is called multiple times: i.e. here and
        // in the finally that is at end of one of the innerProcess blocks
        // above.
        method.releaseConnection();
    }

    /**
     * @return maximum immediate retures.
     */
    private int getMaxImmediateRetries() {
        // TODO make configurable
        return 5;
    }

    /**
     * Can this processor fetch the given CrawlURI. May set a fetch
     * status if this processor would usually handle the CrawlURI,
     * but cannot in this instance.
     *
     * @param curi
     * @return True if processor can fetch.
     */
    private boolean canFetch(CrawlURI curi) {
        String scheme = curi.getUURI().getScheme();
         if (!(scheme.equals("http") || scheme.equals("https"))) {
             // handles only plain http and https
             return false;
         }

//         System.out.println(curi.toString() + " : " + curi.getFetchAttempts());
//         if (curi.getFetchAttempts() >= getMaxFetchAttempts(curi)) {
//             curi.setFetchStatus(S_TOO_MANY_RETRIES);
//             return false;
//         }

         // make sure the dns lookup succeeded
         if (curi.getServer().getHost().getIP() == null
             && curi.getServer().getHost().hasBeenLookedUp()) {
             curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
             return false;
         }
        return true;
    }

    /**
     * Configure the HttpMethod setting options and headers.
     *
     * @param curi CrawlURI from which we pull configuration.
     * @param get The GetMethod to configure.
     */
    private void configureMethod(CrawlURI curi, HttpMethod method)
    {
        // Don't auto-follow redirects
        method.setFollowRedirects(false);

        // Set strict on the client; whatever the client's mode overrides
        // the methods mode inside in the depths of executeMethod.
        this.http.setStrictMode(DEFAULT_HTTPCLIENT_STRICT);

        try {
            String proxy = (String) getAttribute(ATTR_HTTP_PROXY_HOST);
            if (proxy.equals("") != true) {
                this.http.setHttpProxy(proxy);
                this.http.setHttpProxyport(
                    Integer.parseInt(((String)getAttribute(ATTR_HTTP_PROXY_PORT))));
            }
        } catch (AttributeNotFoundException e) {
            logger.warning("Failed get of proxy settings: " +
                e.getLocalizedMessage());
        } catch (MBeanException e) {
            logger.warning("Failed get of proxy settings: " +
                e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning("Failed get of proxy settings: " +
                e.getLocalizedMessage());
        }

        // Use only HTTP/1.0 (to avoid receiving chunked responses)
        ((HttpMethodBase)method).setHttp11(false);

        CrawlOrder order = getSettingsHandler().getOrder();
        String userAgent = curi.getUserAgent();
        if (userAgent == null) {
            userAgent = order.getUserAgent(curi);
        }
        method.setRequestHeader("User-Agent", userAgent);
        method.setRequestHeader("From", order.getFrom(curi));
    }

    /**
     * Add credentials if any to passed <code>method</code>.
     *
     * Do credential handling.  Credentials are in two places.  1. Credentials
     * that succeeded are added to the CrawlServer (Or rather, avatars for
     * credentials are whats added because its not safe to keep around
     * references to credentials).  2. Credentials to be tried are in the curi.
     * Returns true if found credentials to be tried.
     *
     * @param curi Current CrawlURI.
     * @param method The method to add to.
     * @return True if prepopulated <code>method</code> with credentials AND the
     * credentials came from the <code>curi</code>, not from the CrawlServer.
     * The former is  special in that if the <code>curi</curi> credentials
     * succeed, then the caller needs to promote them from the CrawlURI to the
     * CrawlServer so they are available for all subsequent CrawlURIs on this
     * server.
     */
    private boolean populateCredentials(CrawlURI curi, HttpMethod method) {

        // First look at the server avatars. Add any that are to be volunteered
        // on every request (e.g. RFC2617 credentials).  Every time creds will
        // return true when we call 'isEveryTime().
        if (curi.getServer().hasCredentialAvatars()) {
            Set avatars = curi.getServer().getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                if (c.isEveryTime()) {
                    c.populate(curi, this.http, method, ca.getPayload());
                }
            }
        }

        boolean result = false;

        // Now look in the curi.  The Curi will have credentials loaded either
        // by the handle401 method if its a rfc2617 or it'll have been set into
        // the curi by the preconditionenforcer as this login uri came through.
        if (curi.hasCredentialAvatars()) {
            Set avatars = curi.getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                if (c.populate(curi, this.http, method, ca.getPayload())) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Promote successful credential to the server.
     *
     * @param curi CrawlURI whose credentials we are to promote.
     * @param method Method used.
     */
    private void promoteCredentials(final CrawlURI curi) {
        if (!curi.hasCredentialAvatars()) {
            logger.severe("No credentials to promote when there should be " +
                curi);
        } else {
            Set avatars = curi.getCredentialAvatars();
            for (Iterator i = avatars.iterator(); i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                curi.removeCredentialAvatar(ca);
                // The server to attach too may not be the server that hosts
                // this passed curi.  It might be of another subdomain.
                // The avatar needs to be added to the server that is dependent
                // on this precondition.  Find it by name.  Get the name from
                // the credential this avatar represents.
                Credential c = ca.getCredential(getSettingsHandler(), curi);
                String cd = null;
                try {
                    cd = c.getCredentialDomain(curi);
                }
                catch (AttributeNotFoundException e) {
                    logger.severe("Failed to get cred domain for " + curi +
                        " for " + ca + ": " + e.getMessage());
                }
                if (cd != null) {
                    CrawlServer cs
                        = getController().getServerCache().getServerFor(cd);
                    if (cs != null) {
                        cs.addCredentialAvatar(ca);
                    }
                }
            }
        }
    }

    /**
     * Server is looking for basic/digest auth credentials (RFC2617). If we have
     * any, put them into the CrawlURI and have it come around again. Presence
     * of the credential serves as flag to frontier to requeue promptly. If we
     * already tried this domain and still got a 401, then our credentials are
     * bad. Remove them and let this curi die.
     *
     * @param get Method that got a 401.
     * @param curi CrawlURI that got a 401.
     */
    private void handle401(final HttpMethod method, final CrawlURI curi) {

        AuthScheme authscheme = getAuthScheme(method, curi);
        if (authscheme == null) {
            return;
        }

        String realm = authscheme.getRealm();
        if (realm == null) {
            return;
        }

        // Look to see if this curi had rfc2617 avatars loaded.  If so, are
        // any of them for this realm?  If so, then the credential failed if
        // we got a 401 and it should be let die a natural 401 death.
        Set curiRfc2617Credentials =
            getCredentials(getSettingsHandler(), curi, Rfc2617Credential.class);
        Rfc2617Credential extant = Rfc2617Credential.
            getByRealm(curiRfc2617Credentials, realm, curi);
        if (extant != null) {
            // Then, already tried this credential.  Remove ANY rfc2617
            // credential since presence of a rfc2617 credential serves
            // as flag to frontier to requeue this curi and let the curi
            // die a natural death.
            extant.detachAll(curi);
            logger.fine("Auth failed (401) though supplied realm " +
                realm + " to " + curi.toString());
        } else {
            // Look see if we have a credential that corresponds to this realm
            // in credential store.  Filter by type and credential domain.  If
            // not, let this curi die. Else, add it to the curi and let it come
            // around again. Add in the AuthScheme we got too.  Its needed when
            // we go to run the Auth on second time around.
            CredentialStore cs =
                CredentialStore.getCredentialStore(getSettingsHandler());
            if (cs == null) {
                logger.severe("No credential store for " + curi);
            } else {
                Set storeRfc2617Credentials = cs.subset(curi,
                    Rfc2617Credential.class, curi.getServer().getName());
                if (storeRfc2617Credentials == null ||
                    storeRfc2617Credentials.size() <= 0) {
                    logger.fine("No rfc2617 credentials for " + curi);
                } else {
                    Rfc2617Credential found = Rfc2617Credential.
                    		getByRealm(storeRfc2617Credentials, realm, curi);
                    if (found == null) {
                        logger.fine("No rfc2617 credentials for realm " +
                            realm + " in " + curi);
                    } else {
                        found.attach(curi, authscheme);
                        logger.fine("Found credential for realm " + realm +
                            " in store for " + curi.toString());
                    }
                }
            }
        }
    }

    /**
     * @param handler Settings Handler.
     * @param curi CrawlURI that got a 401.
     * @param type Class of credential to get from curi.
     * @return Set of credentials attached to this curi.
     */
    private Set getCredentials(SettingsHandler handler, CrawlURI curi,
            Class type) {
        Set result = null;

        if (curi.hasCredentialAvatars()) {
            for (Iterator i = curi.getCredentialAvatars().iterator();
                    i.hasNext();) {
                CredentialAvatar ca = (CredentialAvatar)i.next();
                if (ca.match(type)) {
                    if (result == null) {
                        result = new HashSet();
                    }
                    result.add(ca.getCredential(handler, curi));
                }
            }
        }
        return result;
    }

    /**
     * @param get Method that got a 401.
     * @param curi CrawlURI that got a 401.
     * @return Authscheme made from the authenticate header or null if failed to
     * get it.
     */
    private AuthScheme getAuthScheme(final HttpMethod method,
            final CrawlURI curi) {
        AuthScheme result = null;
        Header header = method.getResponseHeader(HttpAuthenticator.WWW_AUTH);
        if (header == null) {
            logger.info("No " + HttpAuthenticator.WWW_AUTH + " headers though" +
                " we got a 401: " + curi);
        } else {
            try {
                result =
                    HttpAuthenticator.selectAuthScheme(new Header[] {header});
            } catch (MalformedChallengeException e) {
                logger.severe("Failed to get auth headers: " + e.toString() +
                    " " + curi.toString());
            } catch (UnsupportedOperationException uoe) {
                // This is probably a message like this:
                // Authentication scheme(s) not supported:
                // {negotiate,=Negotiate, NTLM}
                // Log it as a warning.  Not much we can do about it.  Return
                // null.  We'll get the 401 in the arcs and a page that says
                // something like 'Access denied'.
                logger.info(curi + ": " + uoe);
            }
        }
        return result;
    }

    public void initialTasks() {
        super.initialTasks();
        this.soTimeout = getSoTimeout(null);
        setupHttp();

        // load cookies from a file if specified in the order file.
        loadCookies();
    }

    void setupHttp() throws RuntimeException {
		CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
        SingleHttpConnectionManager connectionManager =
            new SingleHttpConnectionManager();
        this.http = new PatchedHttpClient(connectionManager);

        try {
            String trustLevel = (String) getAttribute(ATTR_TRUST);
            Protocol.registerProtocol("https", new Protocol("https",
                    new ConfigurableTrustManagerProtocolSocketFactory(
                            trustLevel), 443));
        }

        catch (Exception e) {
            // Convert all to RuntimeException so get an exception out if
            // initialization fails.
            throw new RuntimeException(
                    "Failed initialization getting attributes: "
                            + e.getMessage());
        }

        // Considered same as overall timeout, for now.
        // TODO: When HTTPClient stops using a monitor 'waitingThread'
        // thread to watch over the getting of the socket from socket
        // factory and instead supports the java.net.Socket#connect timeout.
        // http.setConnectionTimeout((int)timeout);
        // set per-read() timeout: overall timeout will be checked at least
        // this
        // frequently
        this.http.setTimeout(this.soTimeout);
	}

    private int getSoTimeout(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer) getAttribute(ATTR_SOTIMEOUT_MS, curi);
        } catch (Exception e) {
            res = DEFAULT_SOTIMEOUT_MS;
        }
        return res.intValue();
    }

    private int getTimeout(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer) getAttribute(ATTR_TIMEOUT_SECONDS, curi);
        } catch (Exception e) {
            res = DEFAULT_TIMEOUT_SECONDS;
        }
        return res.intValue();
    }

    private long getMaxLength(CrawlURI curi) {
        Long res;
        try {
            res = (Long) getAttribute(ATTR_MAX_LENGTH_BYTES, curi);
        } catch (Exception e) {
            res = DEFAULT_MAX_LENGTH_BYTES;
        }
        return res.longValue();
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
     * @param cookiesFile file in the Netscape's 'cookies.txt' format.
     */
    public void loadCookies(String cookiesFile) {
        // Do nothing if cookiesFile is not specified.
        if (cookiesFile == null || cookiesFile.length() <= 0) {
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(cookiesFile, "r");
            String[] cookieParts;
            String line;
            Cookie cookie = null;
            while ((line = raf.readLine()) != null) {
                // Line that starts with # is commented line, therefore skip it.
                if (!line.startsWith("#")) {
                    cookieParts = line.split("\\t");
                    if (cookieParts.length == 7) {
                        // Create cookie with not expiration date (-1 value).
                        // TODO: add this as an option.
                        cookie =
                            new Cookie(cookieParts[0], cookieParts[5],
                                cookieParts[6], cookieParts[2], -1,
                                Boolean.valueOf(cookieParts[3]).booleanValue());

                        if (cookieParts[1].toLowerCase().equals("true")) {
                            cookie.setDomainAttributeSpecified(true);
                        } else {
                            cookie.setDomainAttributeSpecified(false);
                        }
                        this.http.getState().addCookie(cookie);
                        logger.fine(
                            "Adding cookie: " + cookie.toExternalForm());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // We should probably throw FatalConfigurationException.
            System.out.println("Could not find file: " + cookiesFile
                    + " (Element: " + ATTR_LOAD_COOKIES + ")");

        } catch (IOException e) {
            // We should probably throw FatalConfigurationException.
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.fetcher.FetchHTTP\n");
        ret.append("  Function:          Fetch HTTP URIs\n");
        ret.append("  CrawlURIs handled: " + this.curisHandled + "\n");
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
    public void loadCookies() {
        try {
            loadCookies((String) getAttribute(ATTR_LOAD_COOKIES));
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }
    /**
     * Saves cookies to the file specified in the order file.
     *
     * Output file is in the Netscape 'cookies.txt' format.
     *
     */
    public void saveCookies() {
        try {
            saveCookies((String) getAttribute(ATTR_SAVE_COOKIES));
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }
    /**
     * Saves cookies to a file.
     *
     * Output file is in the Netscape 'cookies.txt' format.
     *
     * @param saveCookiesFile output file.
     */
    public void saveCookies(String saveCookiesFile) {
        // Do nothing if cookiesFile is not specified.
        if (saveCookiesFile == null || saveCookiesFile.length() <= 0) {
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(saveCookiesFile));
            Cookie cookies[] = this.http.getState().getCookies();
            String tab ="\t";
            out.write("# Heritrix Cookie File\n".getBytes());
            out.write(
                "# This file is the Netscape cookies.txt format\n\n".getBytes());
            for (int i = 0; i < cookies.length; i++) {
                StringBuffer line = new StringBuffer();
                line.append(cookies[i].getDomain());
                line.append(tab);
                line.append(
                    cookies[i].isDomainAttributeSpecified() == true
                        ? "TRUE"
                        : "FALSE");
                line.append(tab);
                line.append(cookies[i].getPath());
                line.append(tab);
                line.append(
                    cookies[i].getSecure() == true ? "TRUE" : "FALSE");
                line.append(tab);
                line.append(cookies[i].getName());
                line.append(tab);
                line.append(cookies[i].getValue());
                line.append("\n");
                out.write(line.toString().getBytes());
            }
        } catch (FileNotFoundException e) {
            // We should probably throw FatalConfigurationException.
            System.out.println("Could not find file: " + saveCookiesFile
                    + " (Element: " + ATTR_SAVE_COOKIES + ")");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * At the end save cookies to the file specified in the order file.
     *
     * @see org.archive.crawler.framework.Processor#finalTasks()
     */
    public void finalTasks() {
        saveCookies();
        super.finalTasks();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    protected void listUsedFiles(List list) {
        // List the cookies files
        // Add seed file
        try {
            String tmp = (String)getAttribute(ATTR_LOAD_COOKIES);
            if(tmp != null && tmp.length() > 0){
                File file = getSettingsHandler().
                        getPathRelativeToWorkingDirectory(tmp);
                list.add(file.getAbsolutePath());
            }
            tmp = (String)getAttribute(ATTR_SAVE_COOKIES);
            if(tmp != null && tmp.length() > 0){
                File file = getSettingsHandler().
                        getPathRelativeToWorkingDirectory(tmp);
                list.add(file.getAbsolutePath());
            }
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReflectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void maybeSetAcceptHeaders(CrawlURI curi, HttpMethod get) {
        try {
            StringList accept_headers = (StringList) getAttribute(ATTR_ACCEPT_HEADERS, curi);
            if (!accept_headers.isEmpty()) {
                for (ListIterator i = accept_headers.listIterator(); i.hasNext();) {
                    String hdr = (String) i.next();
                    String[] nvp = hdr.split(": +");
                    if (nvp.length == 2) {
                        get.setRequestHeader(nvp[0], nvp[1]);
                    }
                    else {
                        logger.warning("Invalid accept header: " + hdr);
                    }
                }
            }
        }
        catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
    }

    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        // save cookies
        stream.writeObject(http.getState().getCookies());
    }
    
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Cookie cookies[] = (Cookie[]) stream.readObject();
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new PostRestore(cookies) );
    }
    
    class PostRestore implements Runnable {
        Cookie cookies[];
        public PostRestore(Cookie cookies[]) {
            this.cookies = cookies;
        }
    	public void run() {
            setupHttp();
            for(int i = 0; i < cookies.length; i++) {
                FetchHTTP.this.http.getState().addCookie(cookies[i]);
            }
        }
    }
}
