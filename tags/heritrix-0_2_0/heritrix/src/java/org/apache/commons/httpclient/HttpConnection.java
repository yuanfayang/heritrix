/*
 * $Header$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

package org.apache.commons.httpclient;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.util.TimeoutController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.util.HttpRecorder;

/**
 * An abstraction of an HTTP {@link InputStream} and {@link OutputStream}
 * pair, together with the relevant attributes.
 * <p>
 * The following options are set on the socket before getting the input/output 
 * streams in the {@link #open()} method:
 * <table border=1><tr>
 *    <th>Socket Method
 *    <th>Sockets Option
 *    <th>Configuration
 * </tr><tr>
 *    <td>{@link java.net.Socket#setTcpNoDelay(boolean)}
 *    <td>SO_NODELAY
 *    <td>None
 * </tr><tr>
 *    <td>{@link java.net.Socket#setSoTimeout(int)}
 *    <td>SO_TIMEOUT
 *    <td>{@link #setConnectionTimeout(int)}
 * </tr></table>
 *
 * @author Rod Waldhoff
 * @author Sean C. Sullivan
 * @author Ortwin Glück
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Michael Becke
 * @author Eric E Johnson
 * @author Laura Werner
 * 
 * @version   $Revision$ $Date$
 */
public class HttpConnection {

    // ----------------------------------------------------------- Constructors

    /**
     * Constructor.
     *
     * @param host the host I should connect to
     * @param port the port I should connect to
     */
    public HttpConnection(String host, int port) {
        this(null, -1, host, null, port, Protocol.getProtocol("http"));
    }

    /**
     * Constructor.
     *
     * @param host the host I should connect to
     * @param port the port I should connect to
     * @param protocol the protocol to use
     */
    public HttpConnection(String host, int port, Protocol protocol) {
        this(null, -1, host, null, port, protocol);
    }

    /**
     * Constructor.
     *
     * @param host the host I should connect to
     * @param virtualHost the virtual host I will be sending requests to
     * @param port the port I should connect to
     * @param protocol the protocol to use
     */
    public HttpConnection(String host, String virtualHost, int port, Protocol protocol) {
        this(null, -1, host, virtualHost, port, protocol);
    }

    /**
     * Constructor.
     *
     * @param proxyHost the host I should proxy via
     * @param proxyPort the port I should proxy via
     * @param host the host I should connect to
     * @param port the port I should connect to
     */
    public HttpConnection(
        String proxyHost,
        int proxyPort,
        String host,
        int port) {
        this(proxyHost, proxyPort, host, null, port, Protocol.getProtocol("http"));
    }

    /**
     * Creates a new HttpConnection.
     * 
     * @param hostConfiguration the host/proxy/protocol to use
     */
    public HttpConnection(HostConfiguration hostConfiguration) {
        this(hostConfiguration.getProxyHost(),
             hostConfiguration.getProxyPort(),
             hostConfiguration.getHost(),
             hostConfiguration.getVirtualHost(),
             hostConfiguration.getPort(),
             hostConfiguration.getProtocol());
        this.localAddress = hostConfiguration.getLocalAddress();
    }

    /**
     * Create an instance
     * 
     * @param proxyHost the host I should proxy via
     * @param proxyPort the port I should proxy via
     * @param host the host I should connect to. Parameter value must be non-null.
     * @param virtualHost the virtual host I will be sending requests to
     * @param port the port I should connect to
     * @param protocol The protocol to use. Parameter value must be non-null.
     */
    public HttpConnection(
        String proxyHost,
        int proxyPort,
        String host,
        String virtualHost,
        int port,
        Protocol protocol) {

        if (host == null) {
            throw new IllegalArgumentException("host parameter is null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }

        proxyHostName = proxyHost;
        proxyPortNumber = proxyPort;
        hostName = host;
        virtualName = virtualHost;
        portNumber = protocol.resolvePort(port);
        protocolInUse = protocol;
    }

    // ------------------------------------------ Attribute Setters and Getters

    /**
     * Return my host name.
     *
     * @return my host.
     */
    public String getHost() {
        return hostName;
    }

    /**
     * Set my host name.
     *
     * @param host the host I should connect to. Parameter value must be non-null.
     * @throws IllegalStateException if I am already connected
     */
    public void setHost(String host) throws IllegalStateException {
        if (host == null) {
            throw new IllegalArgumentException("host parameter is null");
        }
        assertNotOpen();
        hostName = host;
    }

    /**
     * Return my virtual host name.
     *
     * @return my virtual host.
     */
    public String getVirtualHost() {
        return virtualName;
    }

    /**
     * Set my virtual host name.
     *
     * @param host the virtual host name that should be used instead of 
     *        physical host name when sending HTTP requests. Virtual host 
     *        name can be set to <tt> null</tt> if virtual host name is not
     *        to be used
     * @throws IllegalStateException if I am already connected
     */
    public void setVirtualHost(String host) throws IllegalStateException {
        assertNotOpen();
        virtualName = host;
    }

    /**
     * Return my port.
     *
     * If the port is -1 (or less than 0) the default port for
     * the current protocol is returned.
     *
     * @return my port.
     */
    public int getPort() {
        if (portNumber < 0) {
            return isSecure() ? 443 : 80;
        } else {
            return portNumber;
        }
    }

    /**
     * Set my port.
     *
     * @param port the port I should connect to
     * @throws IllegalStateException if I am already connected
     */
    public void setPort(int port) throws IllegalStateException {
        assertNotOpen();
        portNumber = port;
    }

    /**
     * Return my proxy host.
     *
     * @return my proxy host.
     */
    public String getProxyHost() {
        return proxyHostName;
    }

    /**
     * Set the host I should proxy through.
     *
     * @param host the host I should proxy through.
     * @throws IllegalStateException if I am already connected
     */
    public void setProxyHost(String host) throws IllegalStateException {
        assertNotOpen();
        proxyHostName = host;
    }

    /**
     * Return my proxy port.
     *
     * @return my proxy port.
     */
    public int getProxyPort() {
        return proxyPortNumber;
    }

    /**
     * Set the port I should proxy through.
     *
     * @param port the host I should proxy through.
     * @throws IllegalStateException if I am already connected
     */
    public void setProxyPort(int port) throws IllegalStateException {
        assertNotOpen();
        proxyPortNumber = port;
    }

    /**
     * Return <tt>true</tt> if I will (or I am) connected over a
     * secure (HTTPS/SSL) protocol.
     *
     * @return <tt>true</tt> if I will (or I am) connected over a
     *         secure (HTTPS/SSL) protocol.
     */
    public boolean isSecure() {
        return protocolInUse.isSecure();
    }

    /**
     * Get the protocol.
     * @return HTTPS if secure, HTTP otherwise
     */
    public Protocol getProtocol() {
        return protocolInUse;
    }

    /**
     * Sets the protocol used by this connection.
     * 
     * @param protocol The new protocol.
     */
    public void setProtocol(Protocol protocol) {
        assertNotOpen();

        if (protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }

        protocolInUse = protocol;

    }

    /**
     * Return the local address used when creating the connection.
     * If <tt>null</tt>, the default address is used.
     * 
     * @return InetAddress the local address to be used when creating Sockets
     */
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }
    
    /**
     * Set the local address used when creating the connection.
     * If unset or <tt>null</tt>, the default address is used.
     * 
     * @param localAddress the local address to use
     */
    public void setLocalAddress(InetAddress localAddress) {
        assertNotOpen();
        this.localAddress = localAddress;
    }

    /**
     * Return <tt>true</tt> if I am connected,
     * <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if I am connected
     */
    public boolean isOpen() {
        if (used && isStaleCheckingEnabled() && isStale()) {
            LOG.debug("Connection is stale, closing...");
            close();
        }
        return isOpen;
    }

    /**
     * Tests if stale checking is enabled.
     * 
     * @return <code>true</code> if enabled
     * 
     * @see #isStale()
     */
    public boolean isStaleCheckingEnabled() {
        return staleCheckingEnabled;
    }

    /**
     * Sets whether or not isStale() will be called when testing if this connection is open.
     * 
     * @param staleCheckEnabled <code>true</code> to enable isStale()
     * 
     * @see #isStale()
     * @see #isOpen()
     */
    public void setStaleCheckingEnabled(boolean staleCheckEnabled) {
        this.staleCheckingEnabled = staleCheckEnabled;
    }

    /**
     * Determines whether this connection is "stale", which is to say that either
     * it is no longer open, or an attempt to read the connection would fail.
     *
     * <p>Unfortunately, due to the limitations of the JREs prior to 1.4, it is
     * not possible to test a connection to see if both the read and write channels
     * are open - except by reading and writing.  This leads to a difficulty when
     * some connections leave the "write" channel open, but close the read channel
     * and ignore the request.  This function attempts to ameliorate that
     * problem by doing a test read, assuming that the caller will be doing a
     * write followed by a read, rather than the other way around.
     * </p>
     *
     * <p>To avoid side-effects, the underlying connection is wrapped by a
     * {@link PushbackInputStream}, so although data might be read, what is visible
     * to clients of the connection will not change with this call.</p.
     *
     * @return <tt>true</tt> if the connection is already closed, or a read would
     * fail.
     */
    protected boolean isStale() {
        boolean isStale = true;
        if (isOpen) {
            // the connection is open, but now we have to see if we can read it
            // assume the connection is not stale.
            isStale = false;
            try {
                if (inputStream.available() == 0) {
                    try {
                        socket.setSoTimeout(1);
                        int byteRead = inputStream.read();
                        if (byteRead == -1) {
                            // again - if the socket is reporting all data read,
                            // probably stale
                            isStale = true;
                        } else {
                            inputStream.unread(byteRead);
                        }
                    } finally {
                        socket.setSoTimeout(soTimeout);
                    }
                }
            } catch (InterruptedIOException e) {
                // aha - the connection is NOT stale - continue on!
            } catch (IOException e) {
                // oops - the connection is stale, the read or soTimeout failed.
                LOG.debug(
                    "An error occurred while reading from the socket, is appears to be stale",
                    e
                );
                isStale = true;
            }
        }

        return isStale;
    }

    /**
     * Return <tt>true</tt> if I am (or I will be)
     * connected via a proxy, <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if I am (or I will be)
     *         connected via a proxy, <tt>false</tt> otherwise.
     */
    public boolean isProxied() {
        return (!(null == proxyHostName || 0 >= proxyPortNumber));
    }

    /**
     * Set the state to keep track of the last response for the last request.
     *
     * <p>The connection managers use this to ensure that previous requests are
     * properly closed before a new request is attempted.  That way, a GET
     * request need not be read in its entirety before a new request is issued.
     * Instead, this stream can be closed as appropriate.</p>
     *
     * @param inStream  The stream associated with an HttpMethod.
     */
    public void setLastResponseInputStream(InputStream inStream) {
        lastResponseInputStream = inStream;
    }

    /**
     * Returns the stream used to read the last response's body.
     *
     * <p>Clients will generally not need to call this function unless
     * using HttpConnection directly, instead of calling {@link HttpClient#executeMethod}.
     * For those clients, call this function, and if it returns a non-null stream,
     * close the stream before attempting to execute a method.  Note that
     * calling "close" on the stream returned by this function <i>may</i> close
     * the connection if the previous response contained a "Connection: close" header. </p>
     *
     * @return An {@link InputStream} corresponding to the body of the last
     *  response.
     */
    public InputStream getLastResponseInputStream() {
        return lastResponseInputStream;
    }

    // --------------------------------------------------- Other Public Methods

    /**
     * Set my {@link Socket}'s timeout, via {@link Socket#setSoTimeout}.  If the
     * connection is already open, the SO_TIMEOUT is changed.  If no connection
     * is open, then subsequent connections will use the timeout value.
     * <p>
     * Note: This is not a connection timeout but a timeout on network traffic!
     *
     * @param timeout the timeout value
     * @throws SocketException - if there is an error in the underlying
     * protocol, such as a TCP error.
     * @throws IllegalStateException if I am not connected
     */
    public void setSoTimeout(int timeout)
        throws SocketException, IllegalStateException {
        LOG.debug("HttpConnection.setSoTimeout(" + timeout + ")");
        soTimeout = timeout;
        if (socket != null) {
            socket.setSoTimeout(timeout);
        }
    }

    /**
     * Return my {@link Socket}'s timeout, via {@link Socket#setSoTimeout}, if the
     * connection is already open. If no connection is open, return the value subsequent 
     * connection will use.
     * <p>
     * Note: This is not a connection timeout but a timeout on network traffic!
     *
     * @return the timeout value
     * 
     * @exception SocketException
     */
    public int getSoTimeout() throws SocketException {
        LOG.debug("HttpConnection.getSoTimeout()");
        if (this.socket != null) {
            return this.socket.getSoTimeout();
        } else {
            return this.soTimeout;
        }
    }

    /**
     * Sets the connection timeout. This is the maximum time that may be spent
     * until a connection is established. The connection will fail after this
     * amount of time.
     * @param timeout The timeout in milliseconds. 0 means timeout is not used.
     */
    public void setConnectionTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    /**
     * Open this connection to the current host and port
     * (via a proxy if so configured).
     * The underlying socket is created from the {@link ProtocolSocketFactory}.
     *
     * @throws IOException when there are errors opening the connection
     */
    public void open() throws IOException {
        LOG.trace("enter HttpConnection.open()");

        assertNotOpen(); // ??? is this worth doing?
        try {
            if (null == socket) {

                final String host = (null == proxyHostName) ? hostName : proxyHostName;
                final int port = (null == proxyHostName) ? portNumber : proxyPortNumber;

                usingSecureSocket = isSecure() && !isProxied();

                // use the protocol's socket factory unless this is a secure
                // proxied connection
                final ProtocolSocketFactory socketFactory =
                    (isSecure() && isProxied()
                            ? new DefaultProtocolSocketFactory()
                            : protocolInUse.getSocketFactory());

                if (connectTimeout == 0) {
                    if (localAddress != null) {
                        socket = socketFactory.createSocket(host, port, localAddress, 0);
                    } else {
                        socket = socketFactory.createSocket(host, port);
                    }
                } else {
                    SocketTask task = new SocketTask() {
                        public void doit() throws IOException {
                            if (localAddress != null) {
                                setSocket(socketFactory.createSocket(host, port, localAddress, 0));
                            } else {
                                setSocket(socketFactory.createSocket(host, port));
                            }
                        }
                    };
                    TimeoutController.execute(task, connectTimeout);
                    socket = task.getSocket();
                    if (task.exception != null) {
                        throw task.exception;
                    }
                }

            }

            /*
            "Nagling has been broadly implemented across networks, 
            including the Internet, and is generally performed by default 
            - although it is sometimes considered to be undesirable in 
            highly interactive environments, such as some client/server 
            situations. In such cases, nagling may be turned off through 
            use of the TCP_NODELAY sockets option." */

            socket.setTcpNoDelay(soNodelay);
            socket.setSoTimeout(soTimeout);
            if (sendBufferSize != -1) {
                socket.setSendBufferSize(sendBufferSize);
            }
            InputStream is = socket.getInputStream();
			if(recorder!=null) {
				is = recorder.inputWrap(is);
			}
            inputStream = new PushbackInputStream(is);
            outputStream = new BufferedOutputStream(
                new WrappedOutputStream(socket.getOutputStream()),
                socket.getSendBufferSize()
            );
			if(recorder!=null) {
				outputStream = recorder.outputWrap(outputStream);
			}

            isOpen = true;
            used = false;
        } catch (IOException e) {
            // Connection wasn't opened properly
            // so close everything out
            closeSocketAndStreams();
            throw e;
        } catch (TimeoutController.TimeoutException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("The host " + hostName + ":" + portNumber
                        + " (or proxy " + proxyHostName + ":" + proxyPortNumber
                        + ") did not accept the connection within timeout of "
                        + connectTimeout + " milliseconds");
            }
            throw new ConnectionTimeoutException();
        }
    }

    /**
     * Calling this method indicates that the proxy has successfully created the
     * tunnel to the host. The socket will be switched to the secure socket.
     * Subsequent communication is done via the secure socket. The method can
     * only be called once on a proxied secure connection.
     *
     * @throws IllegalStateException if connection is not secure and proxied or
     * if the socket is already secure.
     * @throws IOException if an error occured creating the secure socket
     */
    public void tunnelCreated() throws IllegalStateException, IOException {
        LOG.trace("enter HttpConnection.tunnelCreated()");

        if (!isSecure() || !isProxied()) {
            throw new IllegalStateException(
                "Connection must be secure "
                    + "and proxied to use this feature");
        }

        if (usingSecureSocket) {
            throw new IllegalStateException("Already using a secure socket");
        }

        SecureProtocolSocketFactory socketFactory =
            (SecureProtocolSocketFactory) protocolInUse.getSocketFactory();

        socket = socketFactory.createSocket(socket, hostName, portNumber, true);
        if (sendBufferSize != -1) {
            socket.setSendBufferSize(sendBufferSize);
        }        
        inputStream = new PushbackInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(
            new WrappedOutputStream(socket.getOutputStream()),
            socket.getSendBufferSize()
        );
        usingSecureSocket = true;
        tunnelEstablished = true;
        LOG.debug("Secure tunnel created");
    }

    /**
     * Indicates if the connection is completely transparent from end to end.
     *
     * @return true if conncetion is not proxied or tunneled through a transparent
     * proxy; false otherwise.
     */
    public boolean isTransparent() {
        return !isProxied() || tunnelEstablished;
    }

    /**
     * Flushes the output request stream.  This method should be called to 
     * ensure that data written to the request OutputStream is sent to the server.
     * 
     * @throws IOException if an I/O problem occurs
     */
    public void flushRequestOutputStream() throws IOException {
        LOG.trace("enter HttpConnection.flushRequestOutputStream()");
        assertOpen();
        outputStream.flush();
    }

    /**
     * Return a {@link OutputStream} suitable for writing (possibly
     * chunked) bytes to my {@link OutputStream}.
     *
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     * @return a stream to write the request to
     */
    public OutputStream getRequestOutputStream()
        throws IOException, IllegalStateException {
        LOG.trace("enter HttpConnection.getRequestOutputStream()");
        assertOpen();
        OutputStream out = this.outputStream;
        if (Wire.enabled()) {
            out = new WireLogOutputStream(out);
        }
        return out;
    }

    /**
     * Return the response input stream
     * @return InputStream The response input stream.
     * @throws IOException If an IO problem occurs
     * @throws IllegalStateException If the connection isn't open.
     */
    public InputStream getResponseInputStream()
        throws IOException, IllegalStateException {
        LOG.trace("enter HttpConnection.getResponseInputStream()");
        assertOpen();
        return inputStream;
    }

    /**
     * Tests if input data avaialble. This method returns immediately
     * and does not perform any read operations on the input socket
     * 
     * @return boolean <tt>true</tt> if input data is availble, 
     *                 <tt>false</tt> otherwise.
     * 
     * @throws IOException If an IO problem occurs
     * @throws IllegalStateException If the connection isn't open.
     */
    public boolean isResponseAvailable() 
        throws IOException {
        LOG.trace("enter HttpConnection.isResponseAvailable()");
        assertOpen();
        return this.inputStream.available() > 0;
    }

    /**
     * Tests if input data becomes available within the given period time in milliseconds.
     * 
     * @param timeout The number milliseconds to wait for input data to become available 
     * @return boolean <tt>true</tt> if input data is availble, 
     *                 <tt>false</tt> otherwise.
     * 
     * @throws IOException If an IO problem occurs
     * @throws IllegalStateException If the connection isn't open.
     */
    public boolean isResponseAvailable(int timeout) 
        throws IOException {
        LOG.trace("enter HttpConnection.isResponseAvailable(int)");
        assertOpen();
        boolean result = false;
        if (this.inputStream.available() > 0) {
            result = true;
        } else {
            try {
                this.socket.setSoTimeout(timeout);
                int byteRead = inputStream.read();
                if (byteRead != -1) {
                    inputStream.unread(byteRead);
                    LOG.debug("Input data available");
                    result = true;
                } else {
                    LOG.debug("Input data not available");
                }
            } catch (InterruptedIOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Input data not available after " + timeout + " ms");
                }
            } finally {
                try {
                    socket.setSoTimeout(soTimeout);
                } catch (IOException ioe) {
                    LOG.debug("An error ocurred while resetting soTimeout, we will assume that"
                        + " no response is available.",
                        ioe);
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * Write the specified bytes to my output stream.
     *
     * @param data the data to be written
     * @throws HttpRecoverableException if a SocketException occurs
     * @throws IllegalStateException if not connected
     * @throws IOException if an I/O problem occurs
     * @see #write(byte[],int,int)
     */
    public void write(byte[] data)
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.write(byte[])");
        this.write(data, 0, data.length);
    }

    /**
     * Write <i>length</i> bytes in <i>data</i> starting at
     * <i>offset</i> to my output stream.
     *
     * The general contract for
     * write(b, off, len) is that some of the bytes in the array b are written
     * to the output stream in order; element b[off] is the first byte written
     * and b[off+len-1] is the last byte written by this operation.
     *
     * @param data array containing the data to be written.
     * @param offset the start offset in the data.
     * @param length the number of bytes to write.
     * @throws HttpRecoverableException if a SocketException occurs
     * @throws IllegalStateException if not connected
     * @throws IOException if an I/O problem occurs
     */
    public void write(byte[] data, int offset, int length)
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.write(byte[], int, int)");

        if (offset + length > data.length) {
            throw new HttpRecoverableException("Unable to write:"
                    + " offset=" + offset + " length=" + length
                    + " data.length=" + data.length);
        } else if (data.length <= 0) {
            throw new HttpRecoverableException(
                "Unable to write:" + " data.length=" + data.length);
        }

        assertOpen();

        try {
            outputStream.write(data, offset, length);
        } catch (HttpRecoverableException hre) {
            throw hre;
        } catch (SocketException se) {
            LOG.debug(
                "HttpConnection: Socket exception while writing data",
                se);
            throw new HttpRecoverableException(se.toString());
        } catch (IOException ioe) {
            LOG.debug("HttpConnection: Exception while writing data", ioe);
            throw ioe;
        }
    }

    /**
     * Write the specified bytes, followed by <tt>"\r\n".getBytes()</tt> to my
     * output stream.
     *
     * @param data the bytes to be written
     * @throws HttpRecoverableException when socket exceptions occur writing data
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     */
    public void writeLine(byte[] data)
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.writeLine(byte[])");
        write(data);
        writeLine();
    }

    /**
     * Write <tt>"\r\n".getBytes()</tt> to my output stream.
     *
     * @throws HttpRecoverableException when socket exceptions occur writing
     *      data
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     */
    public void writeLine()
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.writeLine()");
        write(CRLF);
    }

    /**
     * Write the specified String (as bytes) to my output stream.
     *
     * @param data the string to be written
     * @throws HttpRecoverableException when socket exceptions occur writing
     *      data
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     */
    public void print(String data)
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.print(String)");
        write(HttpConstants.getBytes(data));
    }

    /**
     * Write the specified String (as bytes), followed by
     * <tt>"\r\n".getBytes()</tt> to my output stream.
     *
     * @param data the data to be written
     * @throws HttpRecoverableException when socket exceptions occur writing
     *      data
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     */
    public void printLine(String data)
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.printLine(String)");
        writeLine(HttpConstants.getBytes(data));
    }

    /**
     * Write <tt>"\r\n".getBytes()</tt> to my output stream.
     *
     * @throws HttpRecoverableException when socket exceptions occur writing
     *      data
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     */
    public void printLine()
        throws IOException, IllegalStateException, HttpRecoverableException {
        LOG.trace("enter HttpConnection.printLine()");
        writeLine();
    }

    /**
     * Read up to <tt>"\n"</tt> from my (unchunked) input stream.
     * If the stream ends before the line terminator is found,
     * the last part of the string will still be returned.
     *
     * @throws IllegalStateException if I am not connected
     * @throws IOException if an I/O problem occurs
     * @return a line from the response
     */
    public String readLine() throws IOException, IllegalStateException {
        LOG.trace("enter HttpConnection.readLine()");

        assertOpen();
        return HttpParser.readLine(inputStream);
    }

    /**
     * Shutdown my {@link Socket}'s output, via Socket.shutdownOutput()
     * when running on JVM 1.3 or higher.
     */
    public void shutdownOutput() {
        LOG.trace("enter HttpConnection.shutdownOutput()");

        try {
            // Socket.shutdownOutput is a JDK 1.3
            // method. We'll use reflection in case
            // we're running in an older VM
            Class[] paramsClasses = new Class[0];
            Method shutdownOutput =
                socket.getClass().getMethod("shutdownOutput", paramsClasses);
            Object[] params = new Object[0];
            shutdownOutput.invoke(socket, params);
        } catch (Exception ex) {
            LOG.debug("Unexpected Exception caught", ex);
            // Ignore, and hope everything goes right
        }
        // close output stream?
    }

    /**
     * Close my socket and streams.
     */
    public void close() {
        LOG.trace("enter HttpConnection.close()");
        closeSocketAndStreams();
    }

    /**
     * Returns the httpConnectionManager.
     * @return HttpConnectionManager
     */
    public HttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    /**
     * Sets the httpConnectionManager.
     * @param httpConnectionManager The httpConnectionManager to set
     */
    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
    }

    /**
     * Release the connection.
     */
    public void releaseConnection() {
        LOG.trace("enter HttpConnection.releaseConnection()");

        // we are assuming that the connection will only be released once used
        used = true;
        
        if (locked) {
            LOG.debug("Connection is locked.  Call to releaseConnection() ignored.");
        } else if (httpConnectionManager != null) {
            LOG.debug("Releasing connection back to connection manager.");
            httpConnectionManager.releaseConnection(this);
        } else {
            LOG.warn("HttpConnectionManager is null.  Connection cannot be released.");
        }
    }

    /**
     * @return
     */
    boolean isLocked() {
        return locked;
    }

    /**
     * @param locked
     */
    void setLocked(boolean locked) {
        this.locked = locked;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Close everything out.
     */
    protected void closeSocketAndStreams() {
        LOG.trace("enter HttpConnection.closeSockedAndStreams()");

        // no longer care about previous responses...
        lastResponseInputStream = null;

        if (null != outputStream) {
            OutputStream temp = outputStream;
            outputStream = null;
            try {
                temp.close();
            } catch (Exception ex) {
                LOG.debug("Exception caught when closing output", ex);
                // ignored
            }
        }

        if (null != inputStream) {
            InputStream temp = inputStream;
            inputStream = null;
            try {
                temp.close();
            } catch (Exception ex) {
                LOG.debug("Exception caught when closing input", ex);
                // ignored
            }
        }

        if (null != socket) {
            Socket temp = socket;
            socket = null;
            try {
                temp.close();
            } catch (Exception ex) {
                LOG.debug("Exception caught when closing socket", ex);
                // ignored
            }
        }
        isOpen = false;
        used = false;
        tunnelEstablished = false;
        usingSecureSocket = false;
    }

    /**
     * Throw an {@link IllegalStateException} if I am connected.
     *
     * @throws IllegalStateException if connected
     */
    protected void assertNotOpen() throws IllegalStateException {
        if (isOpen) {
            throw new IllegalStateException("Connection is open");
        }
    }

    /**
     * Throw an {@link IllegalStateException} if I am not connected.
     *
     * @throws IllegalStateException if not connected
     */
    protected void assertOpen() throws IllegalStateException {
        if (!isOpen) {
            throw new IllegalStateException("Connection is not open");
        }
    }

    /**
     * Gets the socket's sendBufferSize.
     * 
     * @return the size of the buffer for the socket OutputStream, -1 if the value
     * has not been set and the socket has not been opened
     * 
     * @throws SocketException if an error occurs while getting the socket value
     * 
     * @see Socket#getSendBufferSize()
     */
    public int getSendBufferSize() throws SocketException {
        if (socket == null) {
            return -1;
        } else {
            return socket.getSendBufferSize();
        }
    }

    /**
     * Sets the socket's sendBufferSize.
     * 
     * @param sendBufferSize the size to set for the socket OutputStream
     * 
     * @throws SocketException if an error occurs while setting the socket value
     * 
     * @see Socket#setSendBufferSize(int)
     */
    public void setSendBufferSize(int sendBufferSize) throws SocketException {
        this.sendBufferSize = sendBufferSize;
        if (socket != null) {
            socket.setSendBufferSize(sendBufferSize);
        }
    }

    // -- Timeout Exception
    /**
     * Signals that a timeout occured while opening the socket.
     */
    public class ConnectionTimeoutException extends IOException {
        /** Create an instance */
        public ConnectionTimeoutException() {
        }
    }


    /**
     * Helper class for wrapping socket based tasks.
     */
    private abstract class SocketTask implements Runnable {

        /** The socket */
        private Socket socket;
        /** The exception */
        private IOException exception;

        /**
         * Set the socket.
         * @param newSocket The new socket.
         */
        protected void setSocket(final Socket newSocket) {
            socket = newSocket;
        }

        /**
         * Return the socket.
         * @return Socket The socket.
         */
        protected Socket getSocket() {
            return socket;
        }
        /**
         * Perform the logic.
         * @throws IOException If an IO problem occurs
         */
        public abstract void doit() throws IOException;

        /** Execute the logic in this object and keep track of any exceptions. */
        public void run() {
            try {
                doit();
            } catch (IOException e) {
                exception = e;
            }
        }
    }

    /**
     * A wrapper for output streams that closes the connection and converts to recoverable
     * exceptions as appropriable when an IOException occurs.
     */
    private class WrappedOutputStream extends OutputStream {

        /** the actual output stream */
        private OutputStream out;

        /**
         * @param out the output stream to wrap
         */
        public WrappedOutputStream(OutputStream out) {
            this.out = out;
        }

        /**
         * Closes the connection and conditionally converts exception to recoverable.
         * @param ioe the exception that occurred
         * @return the exception to be thrown
         */
        private IOException handleException(IOException ioe) {
            // keep the original value of used, as it will be set to false by close().
            boolean tempUsed = used;
            HttpConnection.this.close();
            if (ioe instanceof InterruptedIOException) {
                return new IOTimeoutException(ioe.getMessage()); 
            } else if (tempUsed) {
                LOG.debug(
                    "Output exception occurred on a used connection.  Will treat as recoverable.", 
                    ioe
                );
                return new HttpRecoverableException(ioe.getMessage(), ioe);                
            } else {
                return ioe;
            }            
        }

        public void write(int b) throws IOException {
            try {
                out.write(b);            
            } catch (IOException ioe) {
                throw handleException(ioe);
            }
        }
        
        public void flush() throws IOException {
            try {
                out.flush();            
            } catch (IOException ioe) {
                throw handleException(ioe);
            }
        }

        public void close() throws IOException {
            try {
                out.close();            
            } catch (IOException ioe) {
                throw handleException(ioe);
            }
        }

        public void write(byte[] b, int off, int len) throws IOException {
            try {
                out.write(b, off, len);
            } catch (IOException ioe) {
                throw handleException(ioe);
            }
        }

        public void write(byte[] b) throws IOException {
            try {
                out.write(b);            
            } catch (IOException ioe) {
                throw handleException(ioe);
            }
        }

    }

    // ------------------------------------------------------- Static Variable

    /** <tt>"\r\n"</tt>, as bytes. */
    private static final byte[] CRLF = new byte[] {(byte) 13, (byte) 10};

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpConnection.class);
    
    // ----------------------------------------------------- Instance Variables
    
    /** A flag indicating if this connection has been used since being opened */
    private boolean used = false;
    
    /** My host. */
    private String hostName = null;
    
    /** My virtual host. */
    private String virtualName = null;
    
    /** My port. */
    private int portNumber = -1;
    
    /** My proxy host. */
    private String proxyHostName = null;
    
    /** My proxy port. */
    private int proxyPortNumber = -1;
    
    /** My client Socket. */
    private Socket socket = null;
    
    /** My InputStream. */
    private PushbackInputStream inputStream = null;

    /** My OutputStream. */
    private OutputStream outputStream = null;
    
    /** the size of the buffer to use for the socket OutputStream */
    private int sendBufferSize = -1;
    
    /** An {@link InputStream} for the response to an individual request. */
    private InputStream lastResponseInputStream = null;
    
    /** Whether or not I am connected. */
    protected boolean isOpen = false;
    
    /** the protocol being used */
    private Protocol protocolInUse;
    
    /** SO_TIMEOUT socket value */
    private int soTimeout = 0;
    
    /** TCP_NODELAY socket value */
    private boolean soNodelay = true;
    
    /** flag to indicate if this connection can be released, if locked the connection cannot be 
     * released */
    private boolean locked = false;
    
    /** Whether or not the socket is a secure one. */
    private boolean usingSecureSocket = false;
    
    /** Whether I am tunneling a proxy or not */
    private boolean tunnelEstablished = false;
    
    /** Whether or not isStale() is used by isOpen() */
    private boolean staleCheckingEnabled = true;    

    /** Timeout until connection established (Socket created). 0 means no timeout. */
    private int connectTimeout = 0;
    
    /** the connection manager that created this connection or null */
    private HttpConnectionManager httpConnectionManager;
    
    /** The local interface on which the connection is created, or null for the default */
    private InetAddress localAddress;
    
	/** Optional recorder. */
	protected HttpRecorder recorder;

	/* (non-Javadoc)
	 * @see org.apache.commons.httpclient.HttpMethod#setHttpRecorder(org.archive.util.HttpRecorder)
	 */
	public void setHttpRecorder(HttpRecorder recorder) {
		this.recorder = recorder;
	}

}
