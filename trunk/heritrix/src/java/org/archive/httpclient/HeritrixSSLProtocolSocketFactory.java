/* HeritrixSSLProtocolSocketFactory
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;


/**
 * Implementation of the commons-httpclient SecureProtocolSocketFactory so we 
 * can insert our own trust manager. 
 * 
 * Based on suggestions found up on commons-httpclient: 
 * <a href="http://jakarta.apache.org/commons/httpclient/sslguide.html">SSL
 * Guide</a>.
 * 
 * <p>Should be only one instance of this factory per running heritrix instance.
 * 
 * @author stack
 * @version $Id$
 */
public class HeritrixSSLProtocolSocketFactory
    implements SecureProtocolSocketFactory
{   
    /**
     * Socket factory that has the heritrix trust manager instaled.
     */
    private SSLSocketFactory factory = null;
    
    
    public HeritrixSSLProtocolSocketFactory()
        throws KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException
    {
        this(HeritrixX509TrustManager.DEFAULT);
    }
    
    /**
     * Constructor.
     * 
     * @param level Level of trust to effect.  Values can be 'strict',
     * 'normal', 'loose', and 'open'.  The first confirms certs have servers DN. 
     * 'normal' is what JSSE does w/o modification.  'loose' is 'normal plus
     * trusting valid self-signed certificates.  'open' trusts any
     * certificate offered.
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public HeritrixSSLProtocolSocketFactory(String level)
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
            new TrustManager[] {new HeritrixX509TrustManager(level)}, null);
        this.factory = context.getSocketFactory();
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
            int clientPort)
        throws IOException, UnknownHostException
    {
        return this.factory.createSocket(host, port,
            clientHost, clientPort);
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
