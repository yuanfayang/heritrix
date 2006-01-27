/* HeritixSSLProtocolSocketFactory
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
package org.archive.crawler.fetcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.httpclient.ConfigurableX509TrustManager;


/**
 * Implementation of the commons-httpclient SSLProtocolSocketFactory so we
 * can return SSLSockets whose trust manager is
 * {@link org.archive.httpclient.ConfigurableX509TrustManager}.
 * 
 * We also go to the heritrix cache to get IPs to use making connection.
 * To this, we have dependency on {@link HeritrixProtocolSocketFactory};
 * its assumed this class and it are used together.
 * See {@link HeritrixProtocolSocketFactory#getHostAddress(ServerCache,String)}.
 *
 * @author stack
 * @version $Id$
 * @see org.archive.httpclient.ConfigurableX509TrustManager
 */
public class HeritrixSSLProtocolSocketFactory
implements SecureProtocolSocketFactory {
    /***
     * Socket factory that has the configurable trust manager installed.
     */
    private SSLSocketFactory sslfactory = null;
    
    private final ServerCache cache;
    
    /**
     * Shutdown constructor.
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    private HeritrixSSLProtocolSocketFactory()
    throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException{
        this(null, ConfigurableX509TrustManager.DEFAULT);
    }
    
    public HeritrixSSLProtocolSocketFactory(final ServerCache cache)
    throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException {
        this(cache, ConfigurableX509TrustManager.DEFAULT);
    }

    /**
     * Constructor.
     * @param cache Cache to get server instance from.
     *
     * @param level Level of trust to effect.
     * @throws KeyManagementException 
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @see ConfigurableX509TrustManager
     */
    public HeritrixSSLProtocolSocketFactory(final ServerCache cache,
            final String level)
    throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException {
        super();
        this.cache = cache;

        // Get an SSL context and initialize it.
        SSLContext context = SSLContext.getInstance("SSL");

        // I tried to get the default KeyManagers but doesn't work unless you
        // point at a physical keystore. Passing null seems to do the right
        // thing so we'll go w/ that.
        context.init(null,
            new TrustManager[] {new ConfigurableX509TrustManager(level)}, null);
        this.sslfactory = context.getSocketFactory();
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
        int clientPort)
    throws IOException, UnknownHostException {
        InetAddress hostAddress =
            HeritrixProtocolSocketFactory.getHostAddress(this.cache, host);
        return (hostAddress == null)?
            this.sslfactory.createSocket(host, port, clientHost, clientPort):
            this.sslfactory.createSocket(hostAddress, port, clientHost,
                clientPort);    
    }

    public Socket createSocket(String host, int port)
    throws IOException, UnknownHostException {
        InetAddress hostAddress =
            HeritrixProtocolSocketFactory.getHostAddress(this.cache, host);
        return (hostAddress == null)?
            this.sslfactory.createSocket(host, port):
            this.sslfactory.createSocket(hostAddress, port);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress,
        int localPort, HttpConnectionParams params)
    throws IOException, UnknownHostException {
        // Below code is from the DefaultSSLProtocolSocketFactory#createSocket
        // method only it has workarounds to deal with pre-1.4 JVMs.  I've
        // cut these out.
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        Socket socket = null;
        int timeout = params.getConnectionTimeout();
        if (timeout == 0) {
            socket = createSocket(host, port, localAddress, localPort);
        } else {
            socket = this.sslfactory.createSocket();
            InetAddress hostAddress =
                HeritrixProtocolSocketFactory.getHostAddress(this.cache, host);
            InetSocketAddress address = (hostAddress != null)?
                    new InetSocketAddress(hostAddress, port):
                    new InetSocketAddress(host, port);
            try {
                socket.connect(address, timeout);
            } catch (SocketTimeoutException e) {
                // Add timeout info. to the exception.
                throw new SocketTimeoutException(e.getMessage() +
                    ": timeout set at " + Integer.toString(timeout) + "ms.");
            }
            assert socket.isConnected(): "Socket not connected " + host;
        }
        return socket;
    }
    
	public Socket createSocket(Socket socket, String host, int port,
        boolean autoClose)
    throws IOException, UnknownHostException {
        return this.sslfactory.createSocket(socket, host, port, autoClose);
	}
    
    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().
            equals(HeritrixSSLProtocolSocketFactory.class));
    }

    public int hashCode() {
        return HeritrixSSLProtocolSocketFactory.class.hashCode();
    }
}
