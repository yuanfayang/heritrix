/* HttpRecorderSSLProtocolSocketFactory
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
package org.archive.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.archive.util.ConfigurableX509TrustManager;


/**
 * Implementation of the commons-httpclient SSLProtocolSocketFactory so we 
 * can return SSLSockets whose streams are wrapped in HttpRecorders and
 * whose trust manager is {@link org.archive.util.ConfigurableX509TrustManager}.
 * 
 * Implementation is done by getting insecure sockets from a 
 * {@link org.archive.httpclient.HttpRecorderSocketFactory} and then wrapping
 * these in ssl using the SSLSocketFactory#createSocket method that takes 
 * an insecure socket.
 * 
 * <p>Alternate implementations were awkward and different from the pattern used
 * for insecure sockets because SSLSockets and SSLSocketFactories are not
 * overrideable.  SSLSocket is an abstract type which means
 * need to keep around an instance of SSLSocket so have means for
 * realizing the abstract methods.  SSLSocketFactory is also awkward for same
 * reason in that it has abstract methods that need implementations.
 * 
 * <p>In an alternate implementation we first made our own SSLContext w/ an
 * amended TrustManger installed.  From here we then got a SSLSocketFactory.
 * Then, on each socket returned out of this factory, we wrapped it w/ a new
 * socket class wherein we had to implement every method adapting all calls to
 * our new socket so they made it through to the wrapped socket just so we could 
 * intercept the getting of socket input/output streams.  Putting this
 * implementation under httpclient generated complaints from its socket
 * pooling on close; it was saying the socket had never been opened by the pool.
 * 
 * @author stack
 * @version $Id$
 * @see org.archive.httpclient.HttpRecorderSocketFactory
 */
public class HttpRecorderSSLProtocolSocketFactory
    implements SecureProtocolSocketFactory
{   
    /**
     * Socket factory that has the configurable trust manager installed.
     * 
     * TODO: Is this thread safe?  Is this the way factories are supposed
     * to be used -- one instance out of which all sockets are given or
     * do we get a factory each time we need sockets?
     */
    private SSLSocketFactory sslfactory = null;
    
    /**
     * An insecure socket factory that gives out sockets that records all 
     * reads and writes via a HttpRecorder.
     */
    private SocketFactory factory = HttpRecorderSocketFactory.getDefault();
    
    /**
     * Autoclose setting.
     * 
     * Set it to true for now.
     */
    private static final boolean AUTOCLOSE = true;
    
    
    public HttpRecorderSSLProtocolSocketFactory()
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
    public HttpRecorderSSLProtocolSocketFactory(String level)
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        super();
        
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
        throws IOException, UnknownHostException
    {
        return createSocket(
            this.factory.createSocket(host, port, clientHost, clientPort),
                host, port, AUTOCLOSE);
    }

    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return createSocket(this.factory.createSocket(host, port), host, port,
            AUTOCLOSE);
    }

    public Socket createSocket(Socket socket, String host, int port,
            boolean autoClose)
        throws IOException, UnknownHostException
    {
        return this.sslfactory.createSocket(socket, host, port, autoClose);
    }
}
