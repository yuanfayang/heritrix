/* 
 * ARCSocketFactory
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.net.SocketFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
import org.archive.io.ReplayInputStream;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;


/**
 * A Socket Factory that gives out ARCSockets.
 * 
 * This factory is odd in that it takes configuration.   If you don't call the 
 * {@link #initialize(Properties)} before first use of {@link #getInstance()},
 * you'll get <code>null</code> for a SocketFactory instance.
 *
 *<p>The {@link java.util.Properties} passed must contain at least an 
 * <code>arcsocketfactory.dumpDir</code> property that points at a directory
 * into which arc and scratch files can be written.
 * 
 * <p>I thought of exposing arcsocketfactory configuration in the manner of
 * JSSE where a user who would like to use other than the default
 * SSLSocketFactory first creates an SSLContext w/ desired configuration --
 * essentially a factory to get a factory -- but deceided on giving this class
 * a direct <code>initialize</code> method instead, at least until we've had
 * chance to chat on it w/ Nutch folks (Their SocketFactory will have to do
 * initialize too if they go w/ this pattern).
 * 
 * <p>This factory is also a singleton to ensure only a single instance of an
 * ARCWriter shared by all ARCSockets.
 * 
 * @author stack
 */
public class ARCSocketFactory extends SocketFactory
{
    /**
     * Key to pull the required arc file dump dir from passed properties.
     */
    public static final String DUMPDIR_KEY = "arcsocketfactory.dumpDir";
    
    /**
     * Key to pull optional arc file prefix from passed properties.
     */
    public static final String PREFIX_KEY = "arcsocketfactory.prefix";
    
    /**
     * Default arc file prefix.
     * 
     * Default is <i>ASF</i> (ARC Socket Factory).
     * 
     * Protected so unit tests can get at this value.
     */
    protected static final String DEFAULT_PREFIX = "ASF";
    
    /**
     * Dir name where we dump arc files.
     */
    private static final String ARCDIR_NAME = "arcs";
    
    /**
     * Name of scratch directory into which we put backing files made by
     * RecordingInputStream and RecordingOutputStream.
     */
    private static final String SCRATCH_DIR = "recordingStreamBackingFiles";
    
    /**
     * Directory to dump arc and recording stream backing files.
     */
    private File dumpDir = null;
    
    /**
     * Dir name where we dump backing files used recording input and
     * output streams.
     */
    private File backingFileDir = null;
    
    /**
     * Maximum expected header size.
     * 
     * Used as limit marking stream.
     */
    private static final int MAX_HEADER_SIZE = 1024 * 1024;
    
	/**
     * Singleton instance of ARCSocketFactory.
     */
	private static ARCSocketFactory instance = null;

    /**
     * Pool of ARCWriters.
     */
    private ARCWriterPool pool = null;
    
    /**
     * An ever increasing id.
     * 
     * Used to ensure unique backing filenames.
     * 
     * @see #getUniqueBasename()
     */
    private static int id = 0;  
    
    
	/**
     * Constructor.
     * 
     * Private so users go via {@link getInstance} to get an instance.
     * 
     * @param properties Properties to configure ARCSocketFactory.  There must
     * at least be a 'arcsocketfactory.dumpDir' property that points at a 
     * location on local disk to which arc and scratch files can be written.
	 */
    private ARCSocketFactory(Properties properties)
        throws IOException
    {
        String dumpDirStr = properties.getProperty(DUMPDIR_KEY);
        if (dumpDirStr == null)
        {
            throw new MissingResourceException("Missing expected property",
                "Properties", DUMPDIR_KEY);
        }
        this.dumpDir = ArchiveUtils.ensureWriteableDirectory(dumpDirStr);
        this.backingFileDir = new File(dumpDir, SCRATCH_DIR);
        
        String prefix = properties.getProperty(PREFIX_KEY);
        if (prefix ==  null)
        {
            prefix = DEFAULT_PREFIX;
        }
        
        // Set up the pool of ARCWriters.
        this.pool = new ARCWriterPool(new File(dumpDir, ARCDIR_NAME), prefix);
    }
    
    /**
     * Initialize w/ passed Properties.
     * 
     * The singleton instance of this class is made the first time this method
     * is called.  All subsequent invocations do nothing.
     * 
     * @param properties Properties to configure ARCSocketFactory.  There must
     * at least be a 'arcsocketfactory.dumpDir' property that points at a 
     * location on local disk to which arc and scratch files can be written.
     * 
     * @throws IOException
     */
    public static synchronized void initialize(Properties properties)
        throws IOException
    {
    	if(instance == null)
        {
    		instance = new ARCSocketFactory(properties);
        }
    }
    
	public Socket createSocket(String host, int port)
		throws IOException, UnknownHostException
	{
		return new ARCSocket(host, port);
	}
    
    public Socket createSocket(InetAddress host, int port) 
        throws IOException
    {
        return new ARCSocket(host, port);
    }
	
	public Socket createSocket(String host, int port, InetAddress localHost, 
            int localPort)
		throws IOException, UnknownHostException
	{
		return new ARCSocket(host, port, localHost, localPort);
	}

	public Socket createSocket(InetAddress address, int port, InetAddress 
			localAddress, int localPort)
		throws IOException
	{
		return new ARCSocket(address, port, localAddress, localPort);
	}

	/**
     * Returns singleton instance of ARCSocketFactory.
     * 
     *  @return Singleton instance of ARCSocketFactory or null if
     * {@link #initialize(java.util.Properties)} hasn't first been called.
     * Return type is SocketFactory.  No need for client to know about
     * ARCSocketFactory.
	 */
	public static SocketFactory getInstance()
	{
		return instance;
	}
    
    /**
     * Return a unique basename.
     * 
     * Name is timestamp + an every increasing sequence number.  Used 
     * creating backing files for recording input streams.
     * 
     * @return Unique basename.
     */
    private synchronized String getUniqueBasename()
    {
        return ArchiveUtils.get14DigitDate() + '-' + id++;
    }
    
    /**
     * Get the pool of ARCWriters used by ARCSocketFactory.
     * 
     * @return Returns the ARCWriter pool instance.
     */
    private ARCWriterPool getPool()
    {
        return pool;
    }
    
    /**
     * @return Returns the ARC file dumpDir.
     */
    public File getDumpDir()
    {
        return dumpDir;
    }

    /**
     * A socket that records all read (and written) to Internet Archive ARC
     * files.
     * 
     * Internet Archive ARC files are described here: 
     * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
     * File Format</a>.
     * 
     * <p>This socket does not do KeepAlive (Socket close is our signal for
     * completion of transaction and that we now have a record to add to the 
     * arc file).  It explicitly prevents the setting of the keep alive flag.
     * 
     * <p>Currently assumed we're doing http protocol only.
     * 
     * <p>TODO: Test redirects, test what happens w/ other protocol use.  Test
     * socket resuse handling.
     * 
     * @author stack
     */
    public class ARCSocket
        extends Socket
    {           
        /**
         * Instance of an http recorder.
         * 
         * Records all written and read on wrapped stream.
         */
        private HttpRecorder recorder = null;
        
        /**
         * Date at which socket was connected (fetch of content began).
         */
        private Date connectTime = null;
        
        
        public ARCSocket(String host, int port)
            throws IOException, UnknownHostException
        {
            super(host, port);
            setKeepAlive(false);
            recordingSetup();
        }
        
        public ARCSocket(InetAddress host, int port) 
            throws IOException
        {
            super(host, port);
            setKeepAlive(false);
            recordingSetup();
        }
        
        public ARCSocket(String host, int port, InetAddress localHost,
                int localPort)
            throws IOException, UnknownHostException
        {
            super(host, port, localHost, localPort);
            setKeepAlive(false);
            recordingSetup();
        }

        public ARCSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort)
            throws IOException
        {
            super(address, port, localAddress, localPort);
            setKeepAlive(false);
            recordingSetup();
        }
        
        /* (non-Javadoc)
         * @see java.net.Socket#connect(java.net.SocketAddress, int)
         */
        public void connect(SocketAddress endpoint, int timeout)
            throws IOException
        {
            // This method gets called by #connect(SocketAddress).
            // This method is also called by our super contructor.  This 
            // can be a problem in that the data members utilized herein are
            // initialzed AFTER the super constructor has been set up.  Means
            // that if I set data member values in here, they'll get overwritten
            // by the initialization of this instance.
            super.connect(endpoint, timeout);
        }

        /* (non-Javadoc)
         * @see java.net.Socket#connect(java.net.SocketAddress)
         */
        public void connect(SocketAddress endpoint) throws IOException
        {
            super.connect(endpoint);
        }

        /* (non-Javadoc)
         * @see java.net.Socket#getKeepAlive()
         */
        public boolean getKeepAlive() throws SocketException
        {
            return false;
        }

        /* (non-Javadoc)
         * @see java.net.Socket#setKeepAlive(boolean)
         */
        public void setKeepAlive(boolean on) throws SocketException
        {
            // Override forcing it always to be false.
            super.setKeepAlive(false);
        }

        /* (non-Javadoc)
         * @see java.net.Socket#shutdownInput()
         */
        public void shutdownInput() throws IOException
        {
            super.shutdownInput();
        }

        /* (non-Javadoc)
         * @see java.net.Socket#shutdownOutput()
         */
        public void shutdownOutput() throws IOException
        {
            super.shutdownOutput();
        }
        
        /**
         * Ready socket for recording of all read and written.
         *
         * @exception IOException
         */
        private void recordingSetup()
            throws IOException
        {
            this.recorder =  new HttpRecorder(getBackingFileSubDir(),
                getUniqueBasename());
            this.recorder.inputWrap(super.getInputStream());
            this.recorder.outputWrap(super.getOutputStream());
            this.connectTime = new Date();
        }
        
        /* (non-Javadoc)
         * @see java.net.Socket#getOutputStream()
         */
        public OutputStream getOutputStream()
            throws IOException
        {
            return this.recorder.getRecordedOutput();
        }
        
        /* (non-Javadoc)
         * @see java.net.Socket#getInputStream()
         */
        public InputStream getInputStream() throws IOException
        {
            return this.recorder.getRecordedInput();
        }
        
        /**
         * Close socket and write the data read to ARC file.
         * 
         * @see java.net.Socket#close()
         */
        public synchronized void close()
            throws IOException
        {
            super.close();
            this.recorder.close();
            try
            {
                save();
            }
            finally
            {
                this.recorder.cleanup();
            }
        }
        
        /**
         * Write socket recordings to an ARC file.
         * 
         * First tests there is content to write.
         * 
         * @exception IOException
         */
        private void save()
            throws IOException
        {
            ReplayInputStream response =
                this.recorder.getRecordedInput().getReplayInputStream();
            ReplayInputStream request =
                this.recorder.getRecordedOutput().getReplayInputStream();
            if (response.remaining() > 0)
            {
                // Mark start of stream.  We'll want to reset to here below.
                response.mark(MAX_HEADER_SIZE);
                StatusLine statusLine = getStatusLine(response);
                String contentType = getContentType(response);
                // Reset to start of stream so can write out total repsonse.
                response.reset();
                ARCWriter writer = getPool().borrowARCWriter();
                try
                {
                    writer.write(getURL(request), contentType,
                        this.getInetAddress().getHostAddress(),
                        this.connectTime.getTime(), (int)response.getSize(),
                        response);
                }
                finally
                {
                    getPool().returnARCWriter(writer);
                }
            }
        }
        
        /**
         * @param replayInputStream Stream to read from cue'd up at start of 
         * the response.
         * @return HTTP status line.
         */
        private StatusLine getStatusLine(ReplayInputStream replayInputStream)
            throws IOException
        {
            String s = HttpParser.readLine(replayInputStream);
            // TODO: Upgrade httpclient and use StatusLine.startsWithHTTP().
            for (; s != null && !s.startsWith("HTTP");)
            {
                s = HttpParser.readLine(replayInputStream);
            }
            
            if (s == null)
            {
                throw new IOException("HTTP status line is null.");
            }
            
            return new StatusLine(s);
        }

        /**
         * Assumes that url is always http protocol.
         * 
         * @param request An input stream that holds the request.
         * 
         * @return Compose an URL from all the info I have to hand.
         */
        private String getURL(ReplayInputStream request)
            throws IOException
        {
            String s = HttpParser.readLine(request);
            if (s == null)
            {
                throw new IOException("Failed parse of response");
            }
            StringTokenizer tokenizer = new StringTokenizer(s);
            if (tokenizer.countTokens() < 3)
            {
                throw new IOException("Response first line is odd: " + s);
            }
            // Skip over the GET/POST verb first.
            tokenizer.nextToken();
            // Next token is the path requested.
            String path = tokenizer.nextToken();
            if (!path.startsWith("/"))
            {
                path = "/" + path;
            }
            
            String host = getHost(request);
            if (host == null)
            {
                host = this.getInetAddress().getHostName();
            }
            String port = "";
            if (this.getPort() != 80)
            {
                port = ":" + Integer.toString(this.getPort());
            }
            
            return getProtocol() + host + port + path;
        }
        
        /**
         * @return Protocol for this response.  Currently hardcoded to http.
         */
        private String getProtocol()
        {
            return "http://";
        }

        /**
         * @param request An input stream that holds the request cue'd up 
         * at start of request headers.
         * @return Return Host header value.
         */
        private String getHost(ReplayInputStream request)
            throws IOException
        {
            return getHeaderValue(getHeaderGroup(request), "Host");
        }
        
        /**
         * @param response An input stream that holds the response cue'd up 
         * at start of response headers.
         * @return Return content-type header value.
         */
        private String getContentType(ReplayInputStream response)
            throws IOException
        {
            return getHeaderValue(getHeaderGroup(response), "Content-Type");
        }
        
        /**
         * Parse passed stream for headers. 
         * 
         * @param inputStream Stream to parse cue'd up at headers to parse.
         * @return A httpclient HeaderGroup.
         */
        private HeaderGroup getHeaderGroup(InputStream inputStream)
            throws IOException
        {
            Header [] headers = HttpParser.parseHeaders(inputStream);
            if (headers == null || headers.length <= 0)
            {
                throw new IOException("Failed parse of headers from stream.");
            }
            HeaderGroup group = new HeaderGroup();
            group.setHeaders(headers);    
            return group;
        }
        
        /**
         * Get an HTTP header value.
         * 
         * There is no such convenience method in httpclient.  
         * 
         * @param group HeaderGroup representation of the http headers.
         * @param key Key of header we're to look up.
         * @return Get the key value string from headers.
         */
        private String getHeaderValue(HeaderGroup group, String key)
            throws HttpException
        {
            String value = null;
            Header h = group.getCondensedHeader(key);
            if (h != null)
            {
                value = h.getValue();
            }
            return value;
        }
        
        /**
         * @return Sub directory to write recording backing files into for this 
         * socket named for the ip of the host we're connecting to.
         * @exception IOException
         */
        private File getBackingFileSubDir()
            throws IOException
        {
            InetAddress address = super.getInetAddress();
            File hostDir = new File(backingFileDir, address.getHostName());
            return ArchiveUtils.ensureWriteableDirectory(hostDir);
        }
    }
}
