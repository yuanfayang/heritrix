/* FetchHTTP.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.Processor;
import org.archive.httpclient.ConfigurableTrustManagerProtocolSocketFactory;
import org.archive.httpclient.HttpRecorderGetMethod;
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
    
    public static final String ATTR_TIMEOUT_SECONDS = "timeout-seconds";
    public static final String ATTR_SOTIMEOUT_MS = "sotimeout-ms";
    public static final String ATTR_MAX_LENGTH_BYTES = "max-length-bytes";
    public static final String ATTR_MAX_FETCH_ATTEMPTS = "max-fetch-attempts";
    public static final String ATTR_LOAD_COOKIES = "cookies-file";

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
    

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.fetcher.FetchHTTP");

    HttpClient http = null;

    private int soTimeout;
    
    /**
     * If this processor has been initialized.
     */
    private boolean initialized = false;

    
    /**
     * Constructor.
     * 
     * @param name Name of this processor.
     */
    public FetchHTTP(String name) {
        super(name, "HTTP Fetcher");
        addElementToDefinition(new SimpleType(ATTR_TIMEOUT_SECONDS,
            "If the fetch is not completed in this number of seconds,"
            + " give up", DEFAULT_TIMEOUT_SECONDS));
        addElementToDefinition(new SimpleType(ATTR_SOTIMEOUT_MS,
            "If the socket is unresponsive for this number of seconds, give up"
            + " (and retry)", DEFAULT_SOTIMEOUT_MS));
        addElementToDefinition(new SimpleType(ATTR_MAX_LENGTH_BYTES,
            "Max length in bytes to fetch (truncate at this length)", 
            DEFAULT_MAX_LENGTH_BYTES));
        addElementToDefinition(new SimpleType(ATTR_MAX_FETCH_ATTEMPTS,
            "Max number of fetches to attempt", DEFAULT_MAX_FETCH_ATTEMPTS));
        addElementToDefinition(new SimpleType(ATTR_LOAD_COOKIES,
            "File to preload cookies from", ""));
        addElementToDefinition(new SimpleType(ATTR_STRICT,
                "Strict adherence to HTTP protocol.  At a minimum all cookies"
                + " will be served on one line only",
                new Boolean(this.strict)));
        SimpleType trustLevel = new SimpleType(ATTR_TRUST,
            "SSL certificate trust level.  Range is from the default 'open'"
            + " (trust all certs including expired, selfsigned, and those for"
            + " which we do not have a CA) through 'loose' (trust all valid"
            + " certificates including selfsigned), 'normal' (all valid"
            + " certificates not including selfsigned) to 'strict' (Cert is"
            + " valid and DN must match servername)",
            ConfigurableX509TrustManager.DEFAULT,
            ConfigurableX509TrustManager.LEVELS_AS_ARRAY);
        trustLevel.setOverrideable(false);
        addElementToDefinition(trustLevel);
    }

    protected void innerProcess(CrawlURI curi) {
        initialize();
        
        // Clear any httpRecorder so subsequent processing doesn't mistakenly 
        // think it current.
        curi.setHttpRecorder(null);
        
        if (!canFetch(curi)) {
            // cannot fetch this, due to protocol, retries, or other problems
            return;
        }

        // Note begin time
        long now = System.currentTimeMillis();
        curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
        
        // Get a reference to the HttpRecorder that is set into this ToeThread.
        HttpRecorder rec = HttpRecorder.getHttpRecorder();
        
        // Get and configure a new GetMethod.
        HttpRecorderGetMethod get =
            new HttpRecorderGetMethod(curi.getUURI().getURIString(), rec);
        configureGetMethod(curi, get);

        try {
            // TODO: make this initial reading subject to the same
            // length/timeout limits; currently only the soTimeout
            // is effective here, once the connection succeeds
            this.http.executeMethod(get);
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "executeMethod");
            curi.setFetchStatus(S_CONNECT_FAILED);
            rec.closeRecorders();
            get.releaseConnection();
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // for weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            curi.addLocalizedError(this.getName(), e, "executeMethod");
            curi.setFetchStatus(S_CONNECT_FAILED);
            rec.closeRecorders();
            get.releaseConnection();
            return;
        }

        try {
            // force read-to-end, so that any socket hangs occur here,
            // not in later modules
            rec.getRecordedInput().readFullyOrUntil(
                getMaxLength(curi),
                1000 * getTimeout(curi));
        } catch (RecorderTimeoutException ex) {
            curi.addAnnotation("timeTrunc");
        } catch (RecorderLengthExceededException ex) {
            curi.addAnnotation("lengthTrunc");
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "readFully");
            curi.setFetchStatus(S_CONNECT_LOST);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            // for weird windows-only ArrayIndex exceptions from native code
            // see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
            // treating as if it were an IOException
            curi.addLocalizedError(this.getName(),e,"readFully");
            curi.setFetchStatus(S_CONNECT_LOST);
            return;
        } finally {
            rec.closeRecorders();
            get.releaseConnection();
        }

        // Note completion time
        curi.getAList().putLong(A_FETCH_COMPLETED_TIME,
            System.currentTimeMillis());

        // Set current httpRecorder into curi for convenience of subsequent
        // processors.
        curi.setHttpRecorder(rec);
        
        long contentSize = curi.getHttpRecorder().getRecordedInput().getSize();
        logger.fine(curi.getUURI().getURIString() + ": " +
            get.getStatusCode() + " " + contentSize);
        curi.setContentSize(contentSize);
        curi.setFetchStatus(get.getStatusCode());
        Header ct = get.getResponseHeader("content-type");
        curi.setContentType((ct == null)? null: ct.getValue());

        // Save off the GetMethod just in case needed by subsequent processors.
        curi.getAList().putObject(A_HTTP_TRANSACTION, get);
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
             // only handles plain http for now
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

    public void initialize()
    {
        if (!this.initialized)
        {
            this.soTimeout = getSoTimeout(null);
            CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
            MultiThreadedHttpConnectionManager connectionManager =
                new MultiThreadedHttpConnectionManager();
            // Ensure there will be as many http connections available as
            // worker threads
            connectionManager.setMaxTotalConnections(getController().
                getToeCount());
            this.http = new HttpClient(connectionManager);

            try
            {
                String trustLevel = (String)getAttribute(ATTR_TRUST);
                Protocol.registerProtocol("https", new Protocol("https", 
                    new ConfigurableTrustManagerProtocolSocketFactory(trustLevel), 443));
            }
            
            catch (Exception e)
            {
                // Convert all to RuntimeException so get an exception out if
                // initialization fails.
                throw new RuntimeException("Failed setting SSL Protocol: " +
                    e.getMessage());
            }

            // load cookies from a file if specified in the order file.
            try {
                loadCookies((String) getAttribute(ATTR_LOAD_COOKIES));
            } catch (MBeanException e) {
                logger.warning(e.getLocalizedMessage());
            } catch (ReflectionException e) {
                logger.warning(e.getLocalizedMessage());
            } catch (AttributeNotFoundException e) {
                logger.warning(e.getLocalizedMessage());
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
            this.initialized = true;
        }
    }

    private boolean getStrict(CrawlURI curi) {
        Boolean isStrict = null;
        try
        {
            isStrict  = (Boolean)getAttribute(ATTR_STRICT, curi);
        }
        catch (Exception e)
        {
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
    private void loadCookies(String cookiesFile) {
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
                            new Cookie(
                                cookieParts[0],
                                cookieParts[5],
                                cookieParts[6],
                                cookieParts[2],
                                -1,
                                (new Boolean(cookieParts[3])).booleanValue());
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
            System.out.println(
                "Could not find file: "
                    + cookiesFile
                    + " (Element: "
                    + ATTR_LOAD_COOKIES
                    + ")");
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
}
