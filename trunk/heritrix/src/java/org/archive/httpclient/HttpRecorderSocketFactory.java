/* HttpRecorderSocketFactory
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
package org.archive.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import org.archive.util.HttpRecorder;


/**
 * A Socket Factory that gives out sockets that record the http traffic 
 * read and written over them.
 * 
 * Assumption is that the socket can find HttpRecorder instance to use
 * in the current thread context.
 * 
 * <p>Assumption is that the HttpRecorder state is being managed external to
 * this factory: i.e. somewhere else, the transition between header and 
 * body gets marked into HttpRecorder and somewhere knows when the request
 * is finished and is managing the HttpRecorder close.
 * 
 * <p>Assumption is a single fetching (recording) thread has one recorder
 * and only uses one socket to read and write the HTTP request/response.
 * 
 * <p>Only one instance of this socket factory per jvm.
 *
 * @author stack
 * @version $Id$
 */
public class HttpRecorderSocketFactory extends SocketFactory
{
    /**
     * Singleton instance of HttpRecorderSocketFactory.
     */
    private static HttpRecorderSocketFactory instance =
        new HttpRecorderSocketFactory();

    /**
     * Constructor.
     *
     * Protected so users go via <code>getDefault()</code> to get an instance.
     */
    private HttpRecorderSocketFactory()
    {
    	super();
    }
   
    public static SocketFactory getDefault()
    {
        return HttpRecorderSocketFactory.instance;
    }

    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return new HttpRecorderSocket(host, port);
    }

    public Socket createSocket(InetAddress host, int port)
        throws IOException
    {
        return new HttpRecorderSocket(host, port);
    }

    public Socket createSocket(String host, int port, InetAddress localHost,
            int localPort)
        throws IOException, UnknownHostException
    {
        return new HttpRecorderSocket(host, port, localHost, localPort);
    }

    public Socket createSocket(InetAddress address, int port, InetAddress
            localAddress, int localPort)
        throws IOException
    {
        return new HttpRecorderSocket(address, port, localAddress, localPort);
    }

    

    /**
     * A socket that records all read (and written) via HttpRecorders.
     *
     * HttpRecorderSocket tees all read and written via HttpRecorders.
     * HttpRecorderSocket can only be used reading and writing the HTTP
     * protocol.  Assumes another process is maintaining state on the
     * HttpRecorder: i.e. it knows when to close the recorder, etc.
     *
     * @author stack
     */
    public class HttpRecorderSocket
        extends Socket
    {
        public HttpRecorderSocket(String host, int port)
            throws IOException, UnknownHostException
        {
            super(host, port);
        }

        public HttpRecorderSocket(InetAddress host, int port)
            throws IOException
        {
            super(host, port);
        }

        public HttpRecorderSocket(String host, int port, InetAddress localHost,
                int localPort)
            throws IOException, UnknownHostException
        {
            super(host, port, localHost, localPort);
        }

        public HttpRecorderSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort)
            throws IOException
        {
            super(address, port, localAddress, localPort);
        }

//        /* (non-Javadoc)
//         * @see java.net.Socket#getOutputStream()
//         */
//        public OutputStream getOutputStream()
//            throws IOException
//        {
//            HttpRecorder recorder = HttpRecorder.getHttpRecorder();
//            recorder.outputWrap(super.getOutputStream());
//            return recorder.getRecordedOutput();
//        }
//
//        /* (non-Javadoc)
//         * @see java.net.Socket#getInputStream()
//         */
//        public InputStream getInputStream()
//            throws IOException
//        {
//            HttpRecorder recorder = HttpRecorder.getHttpRecorder();
//            recorder.inputWrap(super.getInputStream());
//            return recorder.getRecordedInput();
//        }
//
//        /**
//         * Close socket and write the data read to ARC file.
//         *
//         * @see java.net.Socket#close()
//         */
//        public synchronized void close()
//            throws IOException
//        {
//            super.close();
//            HttpRecorder.getHttpRecorder().closeRecorders();
//        }
//
//		/* (non-Javadoc)
//         * @see java.lang.Object#finalize()
//         */
//        protected void finalize() throws Throwable
//        {
//            try
//            {
//                if (HttpRecorder.hasHttpRecorder())
//                {
//                	close();
//                }
//            }
//            
//            catch(Exception e)
//            {
//                // No point letting it out when we're in finalize.
//                e.printStackTrace(System.out);
//            }
//            super.finalize();
//        }
    }
}
