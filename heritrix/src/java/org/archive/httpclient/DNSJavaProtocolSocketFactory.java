/* DNSJavaProtocolSocketFactory
 * 
 * Created on Oct 8, 2004
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.archive.util.DNSJavaUtil;


/**
 * Version of protocol socket factory that uses dnsjava to do dns lookups
 * (The address should already be in the dnsjava cache by the time we
 * get here).
 * 
 * Copied the guts of DefaultProtocolSocketFactory.
 * 
 * @author stack
 * @version $Date$, $Revision$
 */
public class DNSJavaProtocolSocketFactory
implements ProtocolSocketFactory {
    /**
     * The factory singleton.
     */
    private static final DNSJavaProtocolSocketFactory factory =
        new DNSJavaProtocolSocketFactory();

    /**
     * @return a ProtocolSocketFactory
     */
    public static ProtocolSocketFactory getSocketFactory() {
        return factory;
    }
    
    /**
     * Constructor for DNSJavaProtocolSocketFactory.
     * Private so only can be used internally creating singleton.
     */
    private DNSJavaProtocolSocketFactory() {
        super();
    }

    /**
     * @see #createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(
        String host,
        int port,
        InetAddress localAddress,
        int localPort
    ) throws IOException, UnknownHostException {
        InetAddress hostAddress = DNSJavaUtil.getHostAddress(host);
        // If we didn't get a remoteHost, fall back on the old manner
        // of obtaining a socket.
        return (hostAddress == null)?
            new Socket(host, port, localAddress, localPort):
            new Socket(hostAddress, port, localAddress, localPort);
    }

    /**
     * Attempts to get a new socket connection to the given host within the
     * given time limit.
     * <p>
     * This method employs several techniques to circumvent the limitations
     * of older JREs that do not support connect timeout. When running in
     * JRE 1.4 or above reflection is used to call
     * Socket#connect(SocketAddress endpoint, int timeout) method. When
     * executing in older JREs a controller thread is executed. The
     * controller thread attempts to create a new socket within the given
     * limit of time. If socket constructor does not return until the
     * timeout expires, the controller terminates and throws an
     * {@link ConnectTimeoutException}
     * </p>
     *
     * @param host the host name/IP
     * @param port the port on the host
     * @param localAddress the local host name/IP to bind the socket to
     * @param localPort the port on the local machine
     * @param params {@link HttpConnectionParams Http connection parameters}
     *
     * @return Socket a new socket
     *
     * @throws IOException if an I/O error occurs while creating the socket
     * @throws UnknownHostException if the IP address of the host cannot be
     * @throws IOException if an I/O error occurs while creating the socket
     * @throws UnknownHostException if the IP address of the host cannot be
     * determined
     * @throws ConnectTimeoutException if socket cannot be connected within the
     *  given time limit
     *
     * @since 3.0
     */
    public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params)
    throws IOException, UnknownHostException, ConnectTimeoutException {
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
            socket = new Socket();
            InetAddress hostAddress = DNSJavaUtil.getHostAddress(host);
            InetSocketAddress address = (hostAddress != null)?
                    new InetSocketAddress(hostAddress, port):
                    new InetSocketAddress(host, port);
            socket.connect(address, timeout);
            assert socket.isConnected(): "Socket not connected " + host;
        }
        return socket;
    }

    /**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException {
        InetAddress hostAddress = DNSJavaUtil.getHostAddress(host);
        // If we didn't get a remoteHost, fall back on the old manner
        // of obtaining a socket.
        return (hostAddress == null)?
            new Socket(host, port): new Socket(hostAddress, port);
    }

    /**
     * All instances of DefaultProtocolSocketFactory are the same.
     */
    public boolean equals(Object obj) {
        return ((obj != null) &&
            obj.getClass().equals(DNSJavaProtocolSocketFactory.class));
    }

    /**
     * All instances of DefaultProtocolSocketFactory have the same hash code.
     */
    public int hashCode() {
        return DNSJavaProtocolSocketFactory.class.hashCode();
    }
}
