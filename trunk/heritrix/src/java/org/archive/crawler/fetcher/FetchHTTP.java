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
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.HttpAuthenticator;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.datamodel.credential.Rfc2617Credential;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.httpclient.ConfigurableTrustManagerProtocolSocketFactory;
import org.archive.httpclient.HttpRecorderGetMethod;
import org.archive.httpclient.PatchedHttpClient;
import org.archive.httpclient.SingleHttpConnectionManager;
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
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
    
    private static Logger logger = Logger.getLogger(FetchHTTP.class.getName());
    
    public static final String ATTR_TIMEOUT_SECONDS = "timeout-seconds";
    public static final String ATTR_SOTIMEOUT_MS = "sotimeout-ms";
    public static final String ATTR_MAX_LENGTH_BYTES = "max-length-bytes";
    public static final String ATTR_MAX_FETCH_ATTEMPTS = "max-fetch-attempts";
    public static final String ATTR_LOAD_COOKIES = "load-cookies-from-file";
    public static final String ATTR_SAVE_COOKIES = "save-cookies-to-file";

    private static Integer DEFAULT_TIMEOUT_SECONDS = new Integer(1200);
    private static Integer DEFAULT_SOTIMEOUT_MS = new Integer(20000);
    private static Long DEFAULT_MAX_LENGTH_BYTES = new Long(Long.MAX_VALUE);
    private static Integer DEFAULT_MAX_FETCH_ATTEMPTS = new Integer(10);

    /**
     * HTTP protocol adherence strictness.
     *
     * Can be true or false.
     */
    public static final String ATTR_STRICT = "strict";

    /**
     * Default setting for protocol strictness.
     */
    private static final boolean DEFAULT_STRICT = false;

    /**
     * Setting for protocol strictness.
     */
    private boolean strict = DEFAULT_STRICT;
    
    /**
     * SSL trust level setting attribute name.
     */
    public static final String ATTR_TRUST = "trust-level";

    PatchedHttpClient http = null;

    private int soTimeout;
    
    /**
     * Reference to the credential store.
     */
    private CredentialStore credentialStore = null;
    
    /**
     * How many 'instant retries' of HttpRecoverableExceptions have occurred
     */
    // Would like to be 'long', but longs aren't atomic
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
        addElementToDefinition(new SimpleType(ATTR_MAX_FETCH_ATTEMPTS,
            "Max number of fetches to attempt", DEFAULT_MAX_FETCH_ATTEMPTS));
        e = addElementToDefinition(new SimpleType(ATTR_LOAD_COOKIES,
            "File to preload cookies from", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_SAVE_COOKIES,
            "When crawl finishes save cookies to this file", ""));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_STRICT,
                "Strict adherence to HTTP protocol.  At a minimum all cookies"
                + " will be served on one line only",
                new Boolean(this.strict)));
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
    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if (!canFetch(curi)) {
            // Cannot fetch this, due to protocol, retries, or other problems
            return;
        }

        this.curisHandled++;
        // Note begin time
        long now = System.currentTimeMillis();
        curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
        
        // Get a reference to the HttpRecorder that is set into this ToeThread.
        HttpRecorder rec = HttpRecorder.getHttpRecorder();
        
        // Get and configure a new GetMethod.
        HttpRecorderGetMethod get =
            new HttpRecorderGetMethod(curi.getUURI().getURIString(), rec);
        configureGetMethod(curi, get);
        boolean addCrawlURICredentials = populateRfc2617Credentials(curi, get);
        int immediateRetries = 0;
        while (true) {
            // Retry until success (break) or unrecoverable exception
            // (early return)
            try {
                // TODO: make this initial reading subject to the same
                // length/timeout limits; currently only the soTimeout
                // is effective here, once the connection succeeds
                this.http.executeMethod(get);
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
                    failedExecuteCleanup(get, curi, e);
                    return;
                }
            } catch (IOException e) {
                failedExecuteCleanup(get, curi, e);
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                // For weird windows-only ArrayIndex exceptions in native
                // code... see
                // http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
                // treating as if it were an IOException
                failedExecuteCleanup(get, curi, e);
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
        } catch (RecorderLengthExceededException ex) {
            curi.addAnnotation("lengthTrunc");
        } catch (IOException e) {
            cleanup(get, curi, e, "readFully", S_CONNECT_LOST);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // For weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            cleanup(get, curi, e, "readFully", S_CONNECT_LOST);
            return;
        } finally {
            get.releaseConnection();
        }
        
        // Note completion time
        curi.getAList().putLong(A_FETCH_COMPLETED_TIME,
            System.currentTimeMillis());

        // Set the response charset into the HttpRecord if available.
        rec.setCharacterEncoding(get.getResponseCharSet());
        
        // Set httpRecorder into curi for convenience of subsequent processors.
        curi.setHttpRecorder(rec);
        
        int statusCode = get.getStatusCode();
        long contentSize = curi.getHttpRecorder().getRecordedInput().getSize();
        curi.setContentSize(contentSize);
        curi.setFetchStatus(statusCode);
        Header ct = get.getResponseHeader("content-type");
        curi.setContentType((ct == null)? null: ct.getValue());
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(curi.getUURI().getURIString() + " " + statusCode + " " +
                contentSize + " " + curi.getContentType());
        }
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            handle401(get, curi);
        } else if (curi.isSuccess() && addCrawlURICredentials) {
            // Promote the credentials from the CrawlURI to the CrawlServer
            // so they are available for all subsequent CrawlURIs on this
            // server.
            promoteCredentials(curi);
        }

        // Save off the GetMethod just in case needed by subsequent processors.
        curi.getAList().putObject(A_HTTP_TRANSACTION, get);
    }

    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param get Method we failed on.
     * @param exception Exception we failed with.
     */
    private void failedExecuteCleanup(final HttpRecorderGetMethod get,
            final CrawlURI curi, final Exception exception) {
        cleanup(get, curi, exception, "executeMethod", S_CONNECT_FAILED);
    }
    /**
     * Cleanup after a failed method execute.
     * @param curi CrawlURI we failed on.
     * @param get Method we failed on.
     * @param exception Exception we failed with.
     * @param message Message to log with failure.
     * @param status Status to set on the fetch.
     */
    private void cleanup(final HttpRecorderGetMethod get, final CrawlURI curi,
            final Exception exception, final String message, final int status) {
        curi.addLocalizedError(this.getName(), exception, message);
        curi.setFetchStatus(status);

        // Its ok if releaseConnection is called multiple times: i.e. here and
        // in the finally that is at end of one of the innerProcess blocks
        // above.
        get.releaseConnection();
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

         // only try so many times...
         if (curi.getFetchAttempts() >= getMaxFetchAttempts(curi)) {
             curi.setFetchStatus(S_TOO_MANY_RETRIES);
             return false;
         }

         // make sure the dns lookup succeeded
         if (curi.getServer().getHost().getIP() == null
             && curi.getServer().getHost().hasBeenLookedUp()) {
             curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
             return false;
         }
        return true;
    }

    /**
     * Configure the GetMethod setting options and headers.
     *
     * @param curi CrawlURI from which we pull configuration.
     * @param get The GetMethod to configure.
     */
    private void configureGetMethod(CrawlURI curi, GetMethod get)
    {
        // don't auto-follow redirects
        get.setFollowRedirects(false);
        // Use only HTTP/1.0 (to avoid receiving chunked responses)
        get.setHttp11(false);
        // Set strict on the client; whatever the client's mode overrides
        // the methods mode inside in the depths of executeMethod.
        this.http.setStrictMode(getStrict(curi));

        String userAgent = curi.getUserAgent();
        if (userAgent == null) {
            userAgent = getSettingsHandler().getOrder().getUserAgent(curi);
        }
        get.setRequestHeader("User-Agent", userAgent);
        get.setRequestHeader("From",
            getSettingsHandler().getOrder().getFrom(curi));
    }

    /**
     * Add rfc2617 credentials.
     *
     * Do credential handling.  Credentials are in two places.  Credentials
     * that succeeded are added to the CrawlServer.  Credentials to be tried
     * are in the curi.  Returns true if credentials to be tried.
     * 
     * @param curi Current CrawlURI.
     * @param get The method to add to.
     * @return True if prepopulated url with credentials AND the credentials 
     * came from the URI, not from the CrawlServer.  The latter condition is 
     * special in that if the credentials succeed, then they need to be 
     * promoted from the CrawlURI to the CrawlServer so they are available for
     * all subsequent CrawlURIs on this server.
     */
    private boolean populateRfc2617Credentials(final CrawlURI curi,
            final GetMethod get) {
        boolean addCrawlURICredentials = false;
        if (curi.getServer().hasCredentials()) {
            addRfc2617Credentials(curi, get, curi.getServer().getCredentials());
        }
        if (curi.hasCredentials()) {
            addCrawlURICredentials = addRfc2617Credentials(curi, get,
                curi.getCredentials());
        }
        return addCrawlURICredentials;
    }

    /**
     * @param curi CrawlURI whose credentials we are to promote.
     */
    private void promoteCredentials(final CrawlURI curi)
    {
        Set rfc2617credentials = Credential.
            filterCredentials(curi.getCredentials(), Rfc2617Credential.class);
        if (rfc2617credentials != null && rfc2617credentials.size() > 0) {
            for (Iterator i = rfc2617credentials.iterator(); i.hasNext();) {
                Rfc2617Credential c = (Rfc2617Credential)i.next();
                curi.getServer().addCredential(c);
                logger.fine("Promoted credential " + c + " to " +
                    curi.getServer().getName());
            }
        }
    }
    
    /**
     * @param curi Current CrawlURI.
     * @param get Method to add the credentials to.
     * @param credentials List of credentials.  Contains mixed types.
     * @return True if prepopulated url with credentials.
     */
    private boolean addRfc2617Credentials(final CrawlURI curi,
            final GetMethod get, final Set credentials) {
        
        boolean result = false;
        Set rfc2617credentials = Credential.filterCredentials(credentials,
            Rfc2617Credential.class);
        if (rfc2617credentials == null || rfc2617credentials.size() <= 0) {
            return result; // Early return.  No creds to be found.
        }
        
        for (Iterator i = rfc2617credentials.iterator(); i.hasNext();) {
            Rfc2617Credential c = (Rfc2617Credential)i.next();
            // HttpClient wants to get a 401 before it offers the
            // credentials -- even if preemptive is set.  Means I have
            // to handle the WWW-Authenticate header stuff myself.
            AuthScheme authscheme = c.getAuthScheme();
            if (authscheme == null) {
                logger.severe("No authscheme though credentials: " +
                    curi.toString());
                continue;
            }
            
            try {
                // Always add the credential to HttpState. Doing this
                // because no way of removing the credential once added AND
                // there is a bug in the credentials management system
                // in that it always sets URI root to null: it means the key
                // used to find a credential is NOT realm + root URI but
                // just the realm.  Unless I set it everytime, there is
                // possibility that as this thread progresses, it might
                // come across a realm already loaded but the login and 
                // password are from another server.  We'll get a failed
                // authentication that'd be difficult to explain.
                //
                // Have to make a UsernamePasswordCredentials.  The
                // httpclient auth code does an instanceof down in its
                // guts.
                UsernamePasswordCredentials upc = 
                    new UsernamePasswordCredentials(c.getLogin(curi), 
                        c.getPassword(curi));
                this.http.getState().setCredentials(authscheme.getRealm(),
                        /*curi.getServer().getName()*/null, upc);
                try
                {
                    boolean done = HttpAuthenticator.
                      authenticate(authscheme, get, null, this.http.getState());
                    result = done;
                    logger.fine("Credentials for realm " +
                        authscheme.getRealm() + " for curi " + curi.toString() +
                        " added to request: " + done);
                } catch (AuthenticationException e) {
                    logger.severe("Failed setting auth for " +
                        authscheme.getRealm() + " for " +
                        curi.toString() + ": " + e.getMessage());
                }
            }
            
            catch (AttributeNotFoundException e)
            {
                logger.severe("Failed to set rfc2617 credential " +
                    c.toString() + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Server is looking for basic/digest auth credentials (RFC2617).
     * If we have any, put them into the CrawlURI and have it come
     * around again.  Presence of the credential serves as flag to frontier to 
     * requeue promptly.  If we already tried this domain and still got a
     * 401, then our credentials are bad. Remove them and let this curi die.
     * @param get Method that got a 401.
     * @param curi CrawlURI that got a 401.
     */
    private void handle401(final HttpRecorderGetMethod get,
            final CrawlURI curi) {
        
        AuthScheme authscheme = null;
        String realm = null;
        Header header = get.getResponseHeader(HttpAuthenticator.WWW_AUTH);
        if (header == null) {
            logger.info("No " + HttpAuthenticator.WWW_AUTH + " headers though" +
                " we got a 401: " + curi.toString());
        } else {
            try
            {
                authscheme = HttpAuthenticator.
                    selectAuthScheme(new Header[] {header});
                realm = authscheme.getRealm();
            }
            catch (MalformedChallengeException e)
            {
                logger.severe("Failed to get auth headers: " + e.toString() +
                    " " + curi.toString());
            }
        }
        
        if (realm != null) {
            Set curiRfc2617Credentials = curi.getCredentials();
            // If realm already in the curi credentials, then we've
            // tried this credential and it failed.  Let this curi die
            // with a 401.
            Rfc2617Credential extant = Rfc2617Credential.
                getByRealm(curiRfc2617Credentials, realm, curi);
            // TODO: Check that auth scheme is same.
            if (extant != null) {
                // Then, already tried this credential.  Remove ANY rfc2617 
                // credential since presence of a rfc2617 credential serves as
                // flag to frontier to requeue this curi and let the curi die a
                // natural death.
                curi.removeCredentials(Rfc2617Credential.class);
                logger.fine("Auth failed (401) though supplied realm " +
                    realm + " to " + curi.toString());
            } else {
                // Look see if we have a credential that corresponds to this 
                // realm in credential store.  Filter by type and credential
                // domain.  If not, let this curi die.
                // Else, add it to the curi and let it come around again.
                // Add in the AuthScheme we got too.  Its needed when we go
                // to run the Auth on second time around.
                Set storeRfc2617Credentials = Credential.
                     filterCredentials(this.credentialStore.iterator(curi),
                         Rfc2617Credential.class, curi,
                         curi.getServer().getName());
                Rfc2617Credential found = Rfc2617Credential.
                    getByRealm(storeRfc2617Credentials, realm, curi);
                if (found != null) {
                    found.setAuthScheme(authscheme);
                    curi.addCredential(found);
                    logger.fine("Found credential realm " + realm + " in " +
                        " store for " + curi.toString());
                }
            }
        }
    }

    public void initialTasks() {
        this.soTimeout = getSoTimeout(null);
        CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
        SingleHttpConnectionManager connectionManager = new SingleHttpConnectionManager();
        this.http = new PatchedHttpClient(connectionManager);

        try {
            String trustLevel = (String) getAttribute(ATTR_TRUST);
            Protocol.registerProtocol("https", new Protocol("https",
                    new ConfigurableTrustManagerProtocolSocketFactory(
                            trustLevel), 443));

            // Get a credential store object reference.
            this.credentialStore = (CredentialStore) this.getSettingsHandler()
                    .getOrder().getAttribute(CredentialStore.ATTR_NAME);
        }

        catch (Exception e) {
            // Convert all to RuntimeException so get an exception out if
            // initialization fails.
            throw new RuntimeException(
                    "Failed initialization getting attributes: "
                            + e.getMessage());
        }

        // load cookies from a file if specified in the order file.
        loadCookies();

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

    private boolean getStrict(CrawlURI curi) {
        Boolean isStrict = null;
        try {
            isStrict = (Boolean) getAttribute(ATTR_STRICT, curi);
        } catch (Exception e) {
            isStrict = new Boolean(DEFAULT_STRICT);
        }
        return isStrict.booleanValue();
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

    private int getMaxFetchAttempts(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer) getAttribute(ATTR_MAX_FETCH_ATTEMPTS, curi);
        } catch (Exception e) {
            res = DEFAULT_MAX_FETCH_ATTEMPTS;
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
}
