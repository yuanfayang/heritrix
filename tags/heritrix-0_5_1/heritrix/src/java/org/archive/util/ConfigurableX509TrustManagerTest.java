/* ConfigurableX509TrustManagerTest
 * 
 * Created on Feb 18, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * Test configurable trust.
 * 
 * This class is usually commented out doing nought because tests against 
 * real sites out on the net whose condition can change at any time is too 
 * flakey a pretext for this suite to be included as part of general unit
 * test.  Not only that but the machine you are running on -- the JVM
 * you are using -- will effect the outcome of these tests.
 * Enable this suite when doing ConfigurableX509TrustManager 
 * development/testing.
 * 
 * <p>TODO: Turn this all into a selftest installing my own certificates
 * of the various types.
 * 
 * <p>Turn on debugging flags to help figure failures:
 * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html#Debug">Debugging
 * Utilities</a>.
 * @author stack
 * @version $Id$
 */
public class ConfigurableX509TrustManagerTest extends TestCase
{
    /**
     * Logging instance.
     */
    private static Logger logger = Logger.getLogger(
        "org.archive.httpclient.ConfigurableX509TrustManagerTest");
    
    /**
     * Site w/ selfsigned certificate.
     */
    private static final String SELF_SIGNED_URL = "https://foia.aphis.usda.gov";
    
    /**
     * This is an url that is signed by a CA not in the IBM cacerts truststore
     * but which is in the heritrix truststore.
     */
    private static final String CA_NOT_IN_IBM_TRUSTSTORE =
        "https://www.verisign.com/";
    
    /**
     * URLs that should work w/ the LOOSE trust level but that fail w/ 
     * default httpclient ssl.
     */
    private static final String [] LOOSE_URLS =
    {
        SELF_SIGNED_URL
    };
    
    /**
     * URLS that throw exceptions using default httpclient ssl socket factory
     * but if trust manager set to OPEN, then we get into the site.
     */
    private static final String [] OPEN_URLS =
    {
        SELF_SIGNED_URL,
        
        // Gives back a 40x if we use our 'open' TrustManager otherwise 
        // is 'certificate expired' message ('certificate expired' seems to be
        // message that comes out if we don't have the CA in our chain).
        //
        "https://s.as-us.falkag.net/",
            
        // This site is signed by a chain of DOD CAs.  W/ our 'open' TrustManger
        // we can get in.
        "https://3fssgweb1.3fssg.usmc.mil/exchange/logon.asp",
            
        // Interesting because signed by DOD whose CA we don't have in our 
        // chain AND the cert has a name different from server name.  Our
        // trustmanager set to 'open' gets us in where normally we'd 'unknown
        // certificate'.
        "https://www.mmsb.usmc.mil/",
        "https://netmail.3mawcpen.usmc.mil/",
        "https://138.156.136.5/exchange/logon.asp",
                   
        // Theses two certs are signed by the DOD.  If we use our trustmanager
        // set to 'open', then we can get in.  The first puts up a login
        // page that you need to post a login/password to.  The other one just
        // lets us in.
        //         
        "https://itprocurement.hqmc.usmc.mil/",
        "https://rmb.ogden.disa.mil/",
          
        // These are expired certificates.  Exceptions are 'unknown certificate'.
        // Using 'open' trust config. on our TrustManager, we can get in.
        "https://outlook.matsgfl.usmc.mil/",
        "https://www.marforino.usmc.mil/",
        "https://weather.iwz.usmc.mil/",
            
        // These are signed by DOD.  We can get past the 'unknown certificate'
        // w/ if trust set to 'open' but then we get a 401 (Wants us to do a
        // login).
        "https://hq.mfr.usmc.mil/G1/Adjutant/force%20orders.htm", 
        "https://mail.4meb.usmc.mil/",
            
        // This does EOF both here and in mozilla.
        // https://www.kadena.af.mil/18wg/18ceg/718/house.asp
        
        // This one returns java.net.ConnectException: Connection refused
        // "https://mcsd4.ala.usmc.mil/MCSD/ITEMAPPS/srch.html",
        
        // These just do traditional time out.  Not much 'ssl' about this.
        // Will always fail.
        // "https://kcma.mfr.usmc.mil/web%20site/cherry%20point/p3570-2pwch1.pdf",
        // "https://mcpic.bic.usmc.mil/login.aspx?ReturnUrl=/Default.aspx"
        // "https://www2.yuma.usmc.mil/",
        // "https://wwwmil.kadena.af.mil/18ceg/718/house.asp",
    };
    
    /**
     * @param test Name of the test to run.
     */
    public ConfigurableX509TrustManagerTest(String test)
    {
        super(test);
    }
    
    /**
     * Select tests to run.
     *
     * @return Test to run for ConfigurableX509TrustManagerTest.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
    
        /*
         * Commented out because going out to net to fetch random urls whose
         * state can change at any time is too flakey to include as part of 
         * general unit test suite.  Comment in if doing 
         * ConfigurableX509TrustManagerTest development/testing.
         *
         *
            suite.addTest(new ConfigurableX509TrustManagerTest(
                "testOpenConfigurableX509TrustManagerTest"));
            suite.addTest(new ConfigurableX509TrustManagerTest(
               "testLooseConfigurableX509TrustManagerTest"));
            suite.addTest(new ConfigurableX509TrustManagerTest(
               "testNormalConfigurableX509TrustManagerTest"));
               
            // Strict setting not yet implemented.
         * 
         */
        
        
        /* This is a tough test to do.  See comment at head of method.
         *
         *
            suite.addTest(
                new ConfigurableX509TrustSSLProtocolSocketFactoryTest(
                "testHeritrixTrustStore"));
         */
        
        return suite;
    }
    
    /**
     * Test the configurable trust manager set to OPEN.
     * 
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public void testOpenConfigurableX509TrustManagerTest()
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        HttpClient client = new HttpClient();
        Protocol.registerProtocol("https", 
            new Protocol("https", new SSLProtocolSocketFactory(), 443));
        runURLs(client, OPEN_URLS, true);
            
        Protocol.registerProtocol("https", 
            new Protocol("https", 
                new TestTrustSecureProtocolSocketFactory(
                    ConfigurableX509TrustManager.OPEN),
				443));
        runURLs(client, OPEN_URLS, false);
    }
 
    /**
     * Test the configurable trust manager set to LOOSE.
     * 
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public void testLooseConfigurableX509TrustManagerTest()
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        HttpClient client = new HttpClient();
        Protocol.registerProtocol("https", 
            new Protocol("https", new SSLProtocolSocketFactory(), 443));
        runURLs(client, LOOSE_URLS, true);
            
        Protocol.registerProtocol("https", 
           new Protocol("https", 
           new TestTrustSecureProtocolSocketFactory(
           		    ConfigurableX509TrustManager.LOOSE),
                443));
        runURLs(client, LOOSE_URLS, false);
    }
        
    /**
     * Test configurable trust manager set to NORMAL.  
     * 
     * The trust manager should get same results as case were our trust manager
     * is not installed.  Run the OPEN_URLS w/o the configurable trust manager
     * installed and expect an exception every time.  Rerun the OPEN_URLS 
     * w/ the trust manager installed and expect to get an exception
     * for every one when trust level is set to NORMAL.
     * 
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public void testNormalConfigurableX509TrustManagerTest()
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        HttpClient client = new HttpClient();
        Protocol.registerProtocol("https", 
            new Protocol("https", new SSLProtocolSocketFactory(), 443));
        runURLs(client, OPEN_URLS, true);
            
        Protocol.registerProtocol("https", 
           new Protocol("https", 
           new TestTrustSecureProtocolSocketFactory(
           		    ConfigurableX509TrustManager.NORMAL),
                443));
        runURLs(client, OPEN_URLS, true);
    }
    
    /**
     * Test heritrix trust store.  
     * 
     * Test first w/o our trust manager and we should fail because we don't have
     * the CA for the verisign site when running on IBM JVM 1.4.1.  Then test 
     * w/ our test manager set to NORMAL AND load the heritrix truststore
     * which does have the verisign CA
     * (See {@link org.archive.crawler.Heritrix#configureTrustStore()}).
     * 
     * <p>THIS TEST ONLY WORKS IF IBM JVM 1.4.1.
     * 
     * <p>TODO: This test needs to have setting of truststore implemented.
     * 
     * <p>TODO: Move this test to heritrix.
     * 
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public void testHeritrixTrustStore()
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        String [] caNotInIBMTrustStore = {CA_NOT_IN_IBM_TRUSTSTORE};
        HttpClient client = new HttpClient();
        Protocol.registerProtocol("https", 
            new Protocol("https", new SSLProtocolSocketFactory(), 443));
        runURLs(client, caNotInIBMTrustStore, true);
            
        Protocol.registerProtocol("https", 
           new Protocol("https", 
           new TestTrustSecureProtocolSocketFactory(
           		    ConfigurableX509TrustManager.NORMAL),
                443));
        // TODO: Set the javax.net.ssl.trustStore before calling next method.
        // See org.archive.crawler.Heritrix#configureTrustStore().
        runURLs(client, caNotInIBMTrustStore, false);
    }
    
    /**
     * Get passed urls using passed client.
     * 
     * @param client Client to exercise.
     * @param URLS URLs to run.
     * @param expectExceptions True if we expect every URL to throw an
     * exception.
     */
    private void runURLs(HttpClient client, String [] URLS,
        boolean expectExceptions)
    {
        for (int i = 0; i < URLS.length; i++)
        {
            boolean exception = false;
            try
            {
                getPage(client, URLS[i]);
            }
            catch (Exception e)
            {
                exception = true;
                logger.info(URLS[i] + ": " + e.toString());
            }
            
            if (expectExceptions)
            {
                assertTrue("No exception: " + URLS[i], exception);
            }       
        }
    }
    
    private void getPage(HttpClient client, String url)
        throws HttpException, IOException
    {
        GetMethod httpget = new GetMethod(url);
        client.executeMethod(httpget);
        logger.info(url + ": " + httpget.getStatusLine().toString());
        httpget.releaseConnection();
    }
    
    /**
     * Implementation of the commons-httpclient SecureProtocolSocketFactory so we 
     * can insert our own configurable trust manager.
     * 
     * Used testing configurable trustmanager. Based on suggestions found up on
     * commons-httpclient: 
     * <a href="http://jakarta.apache.org/commons/httpclient/sslguide.html">SSL
     * Guide</a>.
     * 
     * <p>Should be only one instance of this factory per JVM.
     * 
     * @author stack
     * @version $Id$
     */
    public class TestTrustSecureProtocolSocketFactory
        implements SecureProtocolSocketFactory
    {   
        /**
         * Socket factory that has the configurable trust manager installed.
         * 
         * TODO: Is this thread safe?  Is this the way factories are supposed
         * to be used -- one instance out of which all sockets are given or
         * do we get a factory each time we need sockets?
         */
        private SSLSocketFactory factory = null;
        
        
        public TestTrustSecureProtocolSocketFactory()
            throws KeyManagementException, KeyStoreException,
			    NoSuchAlgorithmException
        {
            this(ConfigurableX509TrustManager.DEFAULT);
        }
        
        /**
         * Constructor.
         * 
         * @param level Level of trust to effect.
         * 
         * @throws NoSuchAlgorithmException
         * @throws KeyStoreException
         * @see ConfigurableX509TrustManager
         */
        public TestTrustSecureProtocolSocketFactory(String level)
            throws KeyManagementException, KeyStoreException,
			    NoSuchAlgorithmException
        {
            super();
            
            // Get an SSL context and initialize it.
            SSLContext context = SSLContext.getInstance("SSL");
            // I tried to get the default KeyManagers but below doesn't work, at 
            // least on IBM JVM.  Passing in null seems to do the right thing so 
            // we'll go w/ that.
            //  KeyManagerFactory kmf = KeyManagerFactory.
            //      getInstance(KeyManagerFactory.getDefaultAlgorithm());
            //  // Get default KeyStore.  Assume empty string password.
            // kmf.init(KeyStore.getInstance(KeyStore.getDefaultType()), 
            //      "".toCharArray());
            context.init(null,
                    new TrustManager[] {new ConfigurableX509TrustManager(level)}, null);
            this.factory = context.getSocketFactory();
        }

        public Socket createSocket(String host, int port, InetAddress clientHost,
                int clientPort)
            throws IOException, UnknownHostException
        {
            return this.factory.createSocket(host, port, clientHost, clientPort);
        }

        public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException
        {
            return this.factory.createSocket(host, port);
        }

        public Socket createSocket(Socket socket, String host, int port,
                boolean autoClose)
            throws IOException, UnknownHostException
        {
            return this.factory.createSocket(socket, host, port, autoClose);
        }
    }
}
