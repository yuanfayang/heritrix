
/* 
 * ARCSocketFactoryTest
 * 
 * $Id$
 * 
 * Created on Dec 15, 2003.
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
package org.archive.io.arc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.net.SocketFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.archive.util.TmpDirTestCase;

/**
 * Test the ARCSocketFactory.
 * 
 * Tests put ARCSocketFactory under httpclient and makes requests of google.
 * To do this, it implements the ProtocolSocketFactory interface from
 * httpclient.
 *
 * @author stack
 */
public class ARCSocketFactoryTest 
    extends TmpDirTestCase
    implements ProtocolSocketFactory
{
    /**
     * Factory instance.
     * 
     * Created on setup.
     */
    private SocketFactory factory = null;
    
    /**
     * The socket we're running on.
     * 
     * Socket close is never called by httpclient. For testing purposes, call
     * it myself directly.
     */
    private Socket socket = null;
    
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
        Properties properties = new Properties();
        properties.setProperty(ARCSocketFactory.DUMPDIR_KEY,
            getTmpDir().getAbsolutePath());
        ARCSocketFactory.initialize(properties);
        this.factory = ARCSocketFactory.getInstance();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
    
    /**
     * @param testName
     */
    public ARCSocketFactoryTest(String testName)
    {
        super(testName);
    }

    /**
     * Test of httpclient using ARCSocketFactory.
     * 
     * Gets some pages and verifies produced ARC files.
     * 
     * @exception HttpException
     * @exception IOException
     */
    public void testSimpleHttpclient() 
        throws HttpException, IOException
    {
        Protocol httpProtocol = new Protocol("http", this, 80);
        // Register our version of http protocol in place of the default
        // used by httpclient.
        Protocol.registerProtocol("http", httpProtocol);
        // Clean up arc files.
        cleanUpOldFiles(((ARCSocketFactory)this.factory).getArcDumpDir(),
            ARCSocketFactory.DEFAULT_PREFIX);
        cleanUpOldFiles(((ARCSocketFactory)this.factory).getBackingFileDir(),
            "");
        cleanUpOldFiles(getTmpDir(), "arcsocketfactorytest");
        // Now use httpclient to fetch urls.
        String [] urls = {"http://www.google.com/NO_SUCH_PLACE",
            "http://directory.google.com/Top/Society/",
            "http://www.google.com/images/logo.gif"};
        for (int i = 0; i < urls.length; i++)
        {
            getURLContent(urls[i]);
        }
        File [] f =
            getListOfFiles(((ARCSocketFactory)this.factory).getArcDumpDir(),
                ARCSocketFactory.DEFAULT_PREFIX);
        for (int i = 0; i < f.length; i++)
        {
            (new ARCReader(f[i])).validate(urls.length);
        }
    }

    /**
     * Go GET content via httpclient.
     * 
     * @param url URL of page to get content off.
     *
     * @exception HttpException
     * @exception IOException
     */
    private void getURLContent(String url) 
        throws HttpException, IOException
    {
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        // Setting this flag should force closing of socket but doesn't.
        // Doesn't do what I want and its deprecated so leave it out.
        // ((HttpMethodBase)method).setHttp11(false);
    
        // Retry GET up to 3 times.
        int statusCode = -1;
        final int RETRY_MAX = 3;
        for(int i = 0; statusCode == -1 && i < RETRY_MAX; i++) 
        {
            try 
            {
                statusCode = client.executeMethod(method);
            } 
            catch(HttpRecoverableException e) 
            {
                System.err.println("Recoverable exception: Retrying " + 
                        e.getMessage());
            }
            // Let JUnit catch other (IOExceptions) exceptions.
        }

        if(statusCode == -1) 
        {   
            throw new IOException("Failed after " + Integer.toString(RETRY_MAX)  
                + " attempts");
        }

        byte[] responseBody = method.getResponseBody();
        method.releaseConnection();
        
        // Have to force the close myself.  httpclient doesn't call socket
        // close.  It wants to recycle.  It doesn't even call close on streams
        // because it doesn't expect to get an EOF out of stream; rather it
        // wraps our stream w/ a watching stream that looks for content-length
        // characters and there returns a -1.
        this.socket.close();
    }

    /* (non-Javadoc)
     * @see org.apache.commons.httpclient.protocol.ProtocolSocketFactory#createSocket(java.lang.String, int)
     */
    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        // Wrap whats returned out of ARCSocketFactory w/ a recorder that 
        // will capture all read and written on the socket.  Helps debugging.
        this.socket = new ARCSocketFactoryTestSocket(
                this.factory.createSocket(host, port));
        return this.socket;
    }
    
    /* (non-Javadoc)
     * @see org.apache.commons.httpclient.protocol.ProtocolSocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost,
            int clientPort)
        throws IOException, UnknownHostException
    {   
        // Wrap whats returned out of ARCSocketFactory w/ a recorder that 
        // will capture all read and written on the socket.  Helps debugging.
        this.socket = new ARCSocketFactoryTestSocket(
                this.factory.createSocket(host, port, clientHost, clientPort));
        return this.socket;
    }
    
    /**
     * Select tests to run.
     * 
     * @return Test to run for ARCSocketFactory.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new ARCSocketFactoryTest("testSimpleHttpclient"));
        return suite;
    }
    
    /**
     * A socket wrapper that writes all read and written to files to help 
     * testing/debugging.
     * 
     * @author stack
     */
    private class ARCSocketFactoryTestSocket extends Socket
    {
        /**
         * Name of the file we duplicate all socket writes to.
         */
        private static final String OUT_FILENAME =
            "arcsocketfactorytest_out.txt";
        
        /**
         * Name of the file we duplicate all socket reads to.
         */
        private static final String IN_FILENAME =
            "arcsocketfactorytest_in.txt";
        
        /**
         * The socket output stream.
         * 
         * Implementation will duplicate all socket writes in a file.
         */
        private OutputStream out = null;

        /**
         * The socket input stream.
         * 
         * Implementation will duplicate all socket reads in a file.
         */
        private InputStream in = null;


        /**
         * Constructor.
         * 
         * @param wrappedSocket Socket to wrap.
         * 
         * @throws FileNotFoundException
         */
        private ARCSocketFactoryTestSocket(Socket wrappedSocket)
            throws FileNotFoundException, IOException
        {
            this.out = new TeeOutputStream(
                wrappedSocket.getOutputStream(),
                new File(getTmpDir(), OUT_FILENAME), true);
            this.in = new TeeInputStream(wrappedSocket.getInputStream(),
                new File(getTmpDir(), IN_FILENAME), true);
        }
        
        /* (non-Javadoc)
         * @see java.net.Socket#getInputStream()
         */
        public InputStream getInputStream()
            throws IOException
        {
            return this.in;
        }

        /* (non-Javadoc)
         * @see java.net.Socket#getOutputStream()
         */
        public OutputStream getOutputStream()
            throws IOException
        {
            return this.out;
        }

        /* (non-Javadoc)
         * @see java.net.Socket#close()
         */
        public synchronized void close() throws IOException
        {
            super.close();
            this.in.close();
            this.out.close();
        }

    }
    
    /**
     * An output stream that tees all writes to a file.
     * 
     * @author stack
     */ 
    private class TeeOutputStream
        extends FileOutputStream
    {
        /**
         * Stream whose writes we tee.
         */
        private OutputStream teedStream = null;
        
        
        private TeeOutputStream(OutputStream teedStream, File teeFile,
                boolean append) 
            throws FileNotFoundException
        {
            super(teeFile, append);
            this.teedStream = teedStream;
        }
        
        /* (non-Javadoc)
         * @see java.io.FileOutputStream#write(byte[])
         */
        public void write(byte[] buffer) throws IOException
        {
            super.write(buffer);
            this.teedStream.write(buffer);
        }
        
        /* (non-Javadoc)
         * @see java.io.FileOutputStream#write(byte[], int, int)
         */
        public void write(byte[] buffer, int offset, int length)
        throws IOException
        {
            super.write(buffer, offset, length);
            this.teedStream.write(buffer, offset, length);
        }
        
        /* (non-Javadoc)
         * @see java.io.FileOutputStream#write(int)
         */
        public void write(int c) throws IOException
        {
            super.write(c);
            this.teedStream.write(c);
        }
    }

    /**
     * An inputstream stream that tees all reads to a file.
     * 
     * This class needs to subclass FilterInputStream so method calls other 
     * than the overridden reads go through to the wrapped stream (See how we
     * give the tee'd stream to our superclass -- we can't do that w/ a 
     * straight InputStream).
     * 
     * @author stack
     */ 
    private class TeeInputStream
        extends FilterInputStream
    {
        /**
         * Output stream we tee all reads to.
         */
        private FileOutputStream teeStream = null;
        
        
        private TeeInputStream(InputStream teedStream, File teeFile,
                boolean append) 
        throws FileNotFoundException
        {
            super(teedStream);
            this.teeStream = new FileOutputStream(teeFile, append);
        }
        
        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read()
         */
        public int read()
            throws IOException
        {
            int c = super.read();
            this.teeStream.write(c);
            return c;
        }
        
        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read(byte[], int, int)
         */
        public int read(byte[] buffer, int offset, int length)
            throws IOException
        {
            int result = super.read(buffer, offset, length);
            this.teeStream.write(buffer, offset, length);
            return result;
        }
        
        /* (non-Javadoc)
         * @see java.io.FilterInputStream#read(byte[])
         */
        public int read(byte[] buffer)
            throws IOException
        {
            int result = super.read(buffer);
            this.teeStream.write(buffer);
            return result;
        }
    }
}
