/*
 * $Header$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages a set of HttpConnections for various HostConfigurations.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author Eric Johnson
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Carl A. Dunham
 *
 * @since 2.0
 */
public class MultiThreadedHttpConnectionManager implements HttpConnectionManager {

    // -------------------------------------------------------- Class Variables
    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(MultiThreadedHttpConnectionManager.class);

    /** The default maximum number of connections allowed per host */
    public static final int DEFAULT_MAX_HOST_CONNECTIONS = 2;   // Per RFC 2616 sec 8.1.4

    /** The default maximum number of connections allowed overall */
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 20;

    // ----------------------------------------------------- Instance Variables
    /** Maximum number of connections allowed per host */
    private int maxHostConnections = DEFAULT_MAX_HOST_CONNECTIONS;

    /** Maximum number of connections allowed overall */
    private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;

    /** The value to set when calling setStaleCheckingEnabled() on each connection */
    private boolean connectionStaleCheckingEnabled = true;

    /** Connection Pool */
    private ConnectionPool connectionPool;

    /** mapping from reference to hostConfiguration */
    private Map referenceToHostConfig;

    /**
     * the reference queue used to track when HttpConnections are lost to the
     * garbage collector
     */
    private ReferenceQueue referenceQueue;

    /**
     * No-args constructor
     */
    public MultiThreadedHttpConnectionManager() {

        this.referenceToHostConfig = Collections.synchronizedMap(new HashMap());
        this.connectionPool = new ConnectionPool();
        
        this.referenceQueue = new ReferenceQueue();

        new ReferenceQueueThread().start();
    }

    /**
     * Gets the staleCheckingEnabled value to be set on HttpConnections that are created.
     * 
     * @return <code>true</code> if stale checking will be enabled on HttpConections
     * 
     * @see HttpConnection#isStaleCheckingEnabled()
     */
    public boolean isConnectionStaleCheckingEnabled() {
        return connectionStaleCheckingEnabled;
    }

    /**
     * Sets the staleCheckingEnabled value to be set on HttpConnections that are created.
     * 
     * @param connectionStaleCheckingEnabled <code>true</code> if stale checking will be enabled 
     * on HttpConections
     * 
     * @see HttpConnection#setStaleCheckingEnabled(boolean)
     */
    public void setConnectionStaleCheckingEnabled(boolean connectionStaleCheckingEnabled) {
        this.connectionStaleCheckingEnabled = connectionStaleCheckingEnabled;
    }

    /**
     * Sets the maximum number of connections allowed for a given
     * HostConfiguration. Per RFC 2616 section 8.1.4, this value defaults to 2.
     *
     * @param maxHostConnections the number of connections allowed for each
     * hostConfiguration
     */
    public void setMaxConnectionsPerHost(int maxHostConnections) {
        this.maxHostConnections = maxHostConnections;
    }

    /**
     * Gets the maximum number of connections allowed for a given
     * hostConfiguration.
     *
     * @return The maximum number of connections allowed for a given
     * hostConfiguration.
     */
    public int getMaxConnectionsPerHost() {
        return maxHostConnections;
    }

    /**
     * Sets the maximum number of connections allowed in the system.
     *
     * @param maxTotalConnections the maximum number of connections allowed
     */
    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    /**
     * Gets the maximum number of connections allowed in the system.
     *
     * @return The maximum number of connections allowed
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    /**
     * @see HttpConnectionManager#getConnection(HostConfiguration)
     */
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {

        while (true) {
            try {
                return getConnectionWithTimeout(hostConfiguration, 0);
            } catch (ConnectTimeoutException e) {
                // we'll go ahead and log this, but it should never happen. HttpExceptions
                // are only thrown when the timeout occurs and since we have no timeout
                // it should never happen.
                LOG.debug(
                    "Unexpected exception while waiting for connection",
                    e
                );
            };
        }
    }

    /**
     * @see HttpConnectionManager#getConnectionWithTimeout(HostConfiguration, long)
     */
    public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, 
        long timeout) throws ConnectTimeoutException {

        LOG.trace("enter HttpConnectionManager.getConnectionWithTimeout(HostConfiguration, long)");

        if (hostConfiguration == null) {
            throw new IllegalArgumentException("hostConfiguration is null");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("HttpConnectionManager.getConnection:  config = "
                + hostConfiguration + ", timeout = " + timeout);
        }

        final HttpConnection conn = doGetConnection(hostConfiguration, timeout);

        // wrap the connection in an adapter so we can ensure it is used 
        // only once
        return new HttpConnectionAdapter(conn);
    }

	/**
	 * @see HttpConnectionManager#getConnection(HostConfiguration, long)
	 * 
	 * @deprecated Use #getConnectionWithTimeout(HostConfiguration, long)
	 */
	public HttpConnection getConnection(HostConfiguration hostConfiguration, 
		long timeout) throws HttpException {

		LOG.trace("enter HttpConnectionManager.getConnection(HostConfiguration, long)");
		try {
			return getConnectionWithTimeout(hostConfiguration, timeout);
		} catch(ConnectTimeoutException e) {
			throw new HttpException(e.getMessage());
		}
	}

    /**
     * Gets a connection or waits if one is not available.  A connection is
     * available if one exists that is not being used or if fewer than
     * maxHostConnections have been created in the connectionPool, and fewer
     * than maxTotalConnections have been created in all connectionPools.
     *
     * @param hostConfiguration The host configuration.
     * @param timeout the number of milliseconds to wait for a connection, 0 to
     * wait indefinitely
     *
     * @return HttpConnection an available connection
     *
     * @throws ConnectTimeoutException if a connection does not become available in
     * 'timeout' milliseconds
     */
    private HttpConnection doGetConnection(HostConfiguration hostConfiguration, 
        long timeout) throws ConnectTimeoutException {

        HttpConnection connection = null;

        synchronized (connectionPool) {

            // we clone the hostConfiguration
            // so that it cannot be changed once the connection has been retrieved
            hostConfiguration = new HostConfiguration(hostConfiguration);
            HostConnectionPool hostPool = connectionPool.getHostPool(hostConfiguration);
            WaitingThread waitingThread = null;

            boolean useTimeout = (timeout > 0);
            long timeToWait = timeout;
            long startWait = 0;
            long endWait = 0;

            while (connection == null) {

                // happen to have a free connection with the right specs
                //
                if (hostPool.freeConnections.size() > 0) {
                    connection = connectionPool.getFreeConnection(hostConfiguration);

                // have room to make more
                //
                } else if ((hostPool.numConnections < maxHostConnections) 
                    && (connectionPool.numConnections < maxTotalConnections)) {

                    connection = connectionPool.createConnection(hostConfiguration);

                // have room to add host connection, and there is at least one free
                // connection that can be liberated to make overall room
                //
                } else if ((hostPool.numConnections < maxHostConnections) 
                    && (connectionPool.freeConnections.size() > 0)) {

                    connectionPool.deleteLeastUsedConnection();
                    connection = connectionPool.createConnection(hostConfiguration);

                // otherwise, we have to wait for one of the above conditions to
                // become true
                //
                } else {
                    // TODO: keep track of which hostConfigurations have waiting
                    // threads, so they avoid being sacrificed before necessary

                    try {
                        
                        if (useTimeout && timeToWait <= 0) {
                            throw new ConnectTimeoutException("Timeout waiting for connection");
                        }
                        
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Unable to get a connection, waiting..., hostConfig=" + hostConfiguration);
                        }
                        
                        if (waitingThread == null) {
                            waitingThread = new WaitingThread();
                            waitingThread.hostConnectionPool = hostPool;
                            waitingThread.thread = Thread.currentThread();
                        }
                                    
                        if (useTimeout) {
                            startWait = System.currentTimeMillis();
                        }
                        
                        hostPool.waitingThreads.addLast(waitingThread);
                        connectionPool.waitingThreads.addLast(waitingThread);
                        connectionPool.wait(timeToWait);
                        
                        // we have not been interrupted so we need to remove ourselves from the 
                        // wait queue
                        hostPool.waitingThreads.remove(waitingThread);
                        connectionPool.waitingThreads.remove(waitingThread);
                    } catch (InterruptedException e) {
                        // do nothing
                    } finally {
                        if (useTimeout) {
                            endWait = System.currentTimeMillis();
                            timeToWait -= (endWait - startWait);
                        }
                    }
                }
            }
        }
        return connection;
    }

    /**
     * Gets the number of connections in use for this configuration.
     *
     * @param hostConfiguration the key that connections are tracked on
     * @return the number of connections in use
     */
    public int getConnectionsInUse(HostConfiguration hostConfiguration) {
        synchronized (connectionPool) {
            HostConnectionPool hostPool = connectionPool.getHostPool(hostConfiguration);
            return hostPool.numConnections;
        }
    }

    /**
     * Gets the total number of connections in use.
     * 
     * @return the total number of connections in use
     */
    public int getConnectionsInUse() {
        synchronized (connectionPool) {
            return connectionPool.numConnections;
        }
    }

    /**
     * Make the given HttpConnection available for use by other requests.
     * If another thread is blocked in getConnection() that could use this
     * connection, it will be woken up.
     *
     * @param conn the HttpConnection to make available.
     */
    public void releaseConnection(HttpConnection conn) {
        LOG.trace("enter HttpConnectionManager.releaseConnection(HttpConnection)");

        if (conn instanceof HttpConnectionAdapter) {
            // connections given out are wrapped in an HttpConnectionAdapter
            conn = ((HttpConnectionAdapter) conn).getWrappedConnection();
        } else {
            // this is okay, when an HttpConnectionAdapter is released
            // is releases the real connection
        }

        // make sure that the response has been read.
        SimpleHttpConnectionManager.finishLastResponse(conn);

        connectionPool.freeConnection(conn);
    }

    /**
     * Gets the host configuration for a connection.
     * @param conn the connection to get the configuration of
     * @return a new HostConfiguration
     */
    private HostConfiguration configurationForConnection(HttpConnection conn) {

        HostConfiguration connectionConfiguration = new HostConfiguration();
        
        connectionConfiguration.setHost(
            conn.getHost(), 
            conn.getVirtualHost(), 
            conn.getPort(), 
            conn.getProtocol()
        );
        if (conn.getLocalAddress() != null) {
            connectionConfiguration.setLocalAddress(conn.getLocalAddress());
        }
        if (conn.getProxyHost() != null) {
            connectionConfiguration.setProxy(conn.getProxyHost(), conn.getProxyPort());
        }

        return connectionConfiguration;
    }
    

    /**
     * Global Connection Pool, including per-host pools
     */
    private class ConnectionPool {
        
        /** The list of free connections */
        private LinkedList freeConnections = new LinkedList();

        /** The list of WaitingThreads waiting for a connection */
        private LinkedList waitingThreads = new LinkedList();

        /**
         * Map where keys are {@link HostConfiguration}s and values are {@link
         * HostConnectionPool}s
         */
        private final Map mapHosts = new HashMap();

        /** The number of created connections */
        private int numConnections = 0;

        /**
         * Creates a new connection and returns is for use of the calling method.
         *
         * @param hostConfiguration the configuration for the connection
         * @return a new connection or <code>null</code> if none are available
         */
        public synchronized HttpConnection createConnection(HostConfiguration hostConfiguration) {
            HttpConnection connection = null;

            HostConnectionPool hostPool = getHostPool(hostConfiguration);

            if ((hostPool.numConnections < getMaxConnectionsPerHost()) 
                && (numConnections < getMaxTotalConnections())) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Allocating new connection, hostConfig=" + hostConfiguration);
                }
                connection = new HttpConnection(hostConfiguration);
                connection.setStaleCheckingEnabled(connectionStaleCheckingEnabled);
                connection.setHttpConnectionManager(MultiThreadedHttpConnectionManager.this);
                numConnections++;
                hostPool.numConnections++;
        
                // add a weak reference to this connection
                referenceToHostConfig.put(new WeakReference(connection, referenceQueue),
                                          hostConfiguration);
            } else if (LOG.isDebugEnabled()) {
                if (hostPool.numConnections >= getMaxConnectionsPerHost()) {
                    LOG.debug("No connection allocated, host pool has already reached "
                        + "maxConnectionsPerHost, hostConfig=" + hostConfiguration
                        + ", maxConnectionsPerhost=" + getMaxConnectionsPerHost());
                } else {
                    LOG.debug("No connection allocated, maxTotalConnections reached, "
                        + "maxTotalConnections=" + getMaxTotalConnections());
                }
            }
            
            return connection;
        }
    
        /**
         * Get the pool (list) of connections available for the given hostConfig.
         *
         * @param hostConfiguration the configuraton for the connection pool
         * @return a pool (list) of connections available for the given config
         */
        public synchronized HostConnectionPool getHostPool(HostConfiguration hostConfiguration) {
            LOG.trace("enter HttpConnectionManager.ConnectionPool.getHostPool(HostConfiguration)");

            // Look for a list of connections for the given config
            HostConnectionPool listConnections = (HostConnectionPool) 
                mapHosts.get(hostConfiguration);
            if (listConnections == null) {
                // First time for this config
                listConnections = new HostConnectionPool();
                listConnections.hostConfiguration = hostConfiguration;
                mapHosts.put(hostConfiguration, listConnections);
            }
            
            return listConnections;
        }

        /**
         * If available, get a free connection for this host
         *
         * @param hostConfiguration the configuraton for the connection pool
         * @return an available connection for the given config
         */
        public synchronized HttpConnection getFreeConnection(HostConfiguration hostConfiguration) {

            HttpConnection connection = null;
            
            HostConnectionPool hostPool = getHostPool(hostConfiguration);

            if (hostPool.freeConnections.size() > 0) {
                connection = (HttpConnection) hostPool.freeConnections.removeFirst();
                freeConnections.remove(connection);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Getting free connection, hostConfig=" + hostConfiguration);
                }
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("There were no free connections to get, hostConfig=" 
                    + hostConfiguration);
            }
            return connection;
        }

        /**
         * Close and delete an old, unused connection to make room for a new one.
         */
        public synchronized void deleteLeastUsedConnection() {

            HttpConnection connection = (HttpConnection) freeConnections.removeFirst();

            if (connection != null) {
                HostConfiguration connectionConfiguration = configurationForConnection(connection);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reclaiming unused connection, hostConfig=" 
                        + connectionConfiguration);
                }

                connection.close();

                // make sure this connection will not be cleaned up again when garbage 
                // collected
                for (Iterator iter = referenceToHostConfig.keySet().iterator(); iter.hasNext();) {
                    WeakReference connectionRef = (WeakReference) iter.next();
                    if (connectionRef.get() == connection) {
                        iter.remove();
                        connectionRef.enqueue();
                        break;
                    }
                }
                
                HostConnectionPool hostPool = getHostPool(connectionConfiguration);
                
                hostPool.freeConnections.remove(connection);
                hostPool.numConnections--;
                numConnections--;
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Attempted to reclaim an unused connection but there were none.");
            }
        }

        /**
         * Notifies a waiting thread that a connection for the given configuration is 
         * available.
         * @param configuration the host config to use for notifying
         */
        public synchronized void notifyWaitingThread(HostConfiguration configuration) {
            notifyWaitingThread(getHostPool(configuration));
        }

        /**
         * Notifies a waiting thread that a connection for the given configuration is 
         * available.  This will wake a thread witing in tis hostPool or if there is not
         * one a thread in the ConnectionPool will be notified.
         * 
         * @param hostPool the host pool to use for notifying
         */
        public synchronized void notifyWaitingThread(HostConnectionPool hostPool) {

            // find the thread we are going to notify, we want to ensure that each
            // waiting thread is only interrupted once so we will remove it from 
            // all wait queues before interrupting it
            WaitingThread waitingThread = null;
                
            if (hostPool.waitingThreads.size() > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notifying thread waiting on host pool, hostConfig=" 
                        + hostPool.hostConfiguration);
                }                
                waitingThread = (WaitingThread) hostPool.waitingThreads.removeFirst();
                waitingThreads.remove(waitingThread);
            } else if (waitingThreads.size() > 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No-one waiting on host pool, notifying next waiting thread.");
                }
                waitingThread = (WaitingThread) waitingThreads.removeFirst();
                waitingThread.hostConnectionPool.waitingThreads.remove(waitingThread);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Notifying no-one, there are no waiting threads");
            }
                
            if (waitingThread != null) {
                waitingThread.thread.interrupt();
            }
        }

        /**
         * Marks the given connection as free.
         * @param conn a connection that is no longer being used
         */
        public void freeConnection(HttpConnection conn) {

            HostConfiguration connectionConfiguration = configurationForConnection(conn);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Freeing connection, hostConfig=" + connectionConfiguration);
            }

            synchronized (this) {
                HostConnectionPool hostPool = getHostPool(connectionConfiguration);

                // Put the connect back in the available list and notify a waiter
                hostPool.freeConnections.add(conn);
                if (hostPool.numConnections == 0) {
                    // for some reason this connection pool didn't already exist
                    LOG.error("Host connection pool not found, hostConfig=" 
                              + connectionConfiguration);
                    hostPool.numConnections = 1;
                }

                freeConnections.add(conn);
                if (numConnections == 0) {
                    // for some reason this connection pool didn't already exist
                    LOG.error("Host connection pool not found, hostConfig=" 
                              + connectionConfiguration);
                    numConnections = 1;
                }
                
                notifyWaitingThread(hostPool);
            }
        }
    }

    /**
     * A simple struct-like class to combine the connection list and the count
     * of created connections.
     */
    private class HostConnectionPool {
        /** The hostConfig this pool is for */
        public HostConfiguration hostConfiguration;
        
        /** The list of free connections */
        public LinkedList freeConnections = new LinkedList();
        
        /** The list of WaitingThreads for this host */
        public LinkedList waitingThreads = new LinkedList();

        /** The number of created connections */
        public int numConnections = 0;
    }
    
    /**
     * A simple struct-like class to combine the waiting thread and the connection 
     * pool it is waiting on.
     */
    private class WaitingThread {
        /** The thread that is waiting for a connection */
        public Thread thread;
        
        /** The connection pool the thread is waiting for */
        public HostConnectionPool hostConnectionPool;
    }

    /**
     * A thread for listening for HttpConnections reclaimed by the garbage
     * collector.
     */
    private class ReferenceQueueThread extends Thread {

        /**
         * Create an instance and make this a daemon thread.
         */
        public ReferenceQueueThread() {
            setDaemon(true);
        }

        /**
         * Handles cleaning up for the given reference.  Decrements any connection counts
         * and notifies waiting threads, if appropriate.
         * 
         * @param ref the reference to clean up
         */
        private void handleReference(Reference ref) {
            synchronized (connectionPool) {
                // only clean up for this reference if it is still associated with 
                // a HostConfiguration
                if (referenceToHostConfig.containsKey(ref)) {
                    HostConfiguration config = (HostConfiguration) referenceToHostConfig.get(ref);
                    referenceToHostConfig.remove(ref);
                    
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(
                            "Connection reclaimed by garbage collector, hostConfig=" + config);
                    }
                    
                    HostConnectionPool hostPool = connectionPool.getHostPool(config);
                    hostPool.numConnections--;

                    connectionPool.numConnections--;
                    connectionPool.notifyWaitingThread(config);
                }
            }            
        }

        /**
         * Start execution.
         */
        public void run() {
            while (true) {
                try {
                    Reference ref = referenceQueue.remove();
                    if (ref != null) {
                        handleReference(ref);
                    }
                } catch (InterruptedException e) {
                    LOG.debug("ReferenceQueueThread interrupted", e);
                }
            }
        }

    }

    /**
     * An HttpConnection wrapper that ensures a connection cannot be used
     * once released.
     */
    private static class HttpConnectionAdapter extends HttpConnection {

        // the wrapped connection
        private HttpConnection wrappedConnection;

        /**
         * Creates a new HttpConnectionAdapter.
         * @param connection the connection to be wrapped
         */
        public HttpConnectionAdapter(HttpConnection connection) {
            super(connection.getHost(), connection.getPort(), connection.getProtocol());
            this.wrappedConnection = connection;
        }

        /**
         * Tests if the wrapped connection is still available.
         * @return boolean
         */
        protected boolean hasConnection() {
            return wrappedConnection != null;
        }

        /**
         * @return HttpConnection
         */
        HttpConnection getWrappedConnection() {
            return wrappedConnection;
        }
        
        public void close() {
            if (hasConnection()) {
                wrappedConnection.close();
            } else {
                // do nothing
            }
        }

        public InetAddress getLocalAddress() {
            if (hasConnection()) {
                return wrappedConnection.getLocalAddress();
            } else {
                return null;
            }
        }

        public boolean isStaleCheckingEnabled() {
            if (hasConnection()) {
                return wrappedConnection.isStaleCheckingEnabled();
            } else {
                return false;
            }
        }

        public void setLocalAddress(InetAddress localAddress) {
            if (hasConnection()) {
                wrappedConnection.setLocalAddress(localAddress);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setStaleCheckingEnabled(boolean staleCheckEnabled) {
            if (hasConnection()) {
                wrappedConnection.setStaleCheckingEnabled(staleCheckEnabled);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String getHost() {
            if (hasConnection()) {
                return wrappedConnection.getHost();
            } else {
                return null;
            }
        }

        public HttpConnectionManager getHttpConnectionManager() {
            if (hasConnection()) {
                return wrappedConnection.getHttpConnectionManager();
            } else {
                return null;
            }
        }

        public InputStream getLastResponseInputStream() {
            if (hasConnection()) {
                return wrappedConnection.getLastResponseInputStream();
            } else {
                return null;
            }
        }

        public int getPort() {
            if (hasConnection()) {
                return wrappedConnection.getPort();
            } else {
                return -1;
            }
        }

        public Protocol getProtocol() {
            if (hasConnection()) {
                return wrappedConnection.getProtocol();
            } else {
                return null;
            }
        }

        public String getProxyHost() {
            if (hasConnection()) {
                return wrappedConnection.getProxyHost();
            } else {
                return null;
            }
        }

        public int getProxyPort() {
            if (hasConnection()) {
                return wrappedConnection.getProxyPort();
            } else {
                return -1;
            }
        }

        public OutputStream getRequestOutputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getRequestOutputStream();
            } else {
                return null;
            }
        }

        public InputStream getResponseInputStream()
            throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.getResponseInputStream();
            } else {
                return null;
            }
        }

        public boolean isOpen() {
            if (hasConnection()) {
                return wrappedConnection.isOpen();
            } else {
                return false;
            }
        }

        public boolean isProxied() {
            if (hasConnection()) {
                return wrappedConnection.isProxied();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable() throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable();
            } else {
                return false;
            }
        }

        public boolean isResponseAvailable(int timeout) throws IOException {
            if (hasConnection()) {
                return  wrappedConnection.isResponseAvailable(timeout);
            } else {
                return false;
            }
        }

        public boolean isSecure() {
            if (hasConnection()) {
                return wrappedConnection.isSecure();
            } else {
                return false;
            }
        }

        public boolean isTransparent() {
            if (hasConnection()) {
                return wrappedConnection.isTransparent();
            } else {
                return false;
            }
        }

        public void open() throws IOException {
            if (hasConnection()) {
				if (recorder!=null) {
					wrappedConnection.setHttpRecorder(recorder);
				}
                wrappedConnection.open();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void print(String data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.print(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void printLine()
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.printLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void printLine(String data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.printLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String readLine() throws IOException, IllegalStateException {
            if (hasConnection()) {
                return wrappedConnection.readLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void releaseConnection() {
            if (!isLocked() && hasConnection()) {
                HttpConnection wrappedConnection = this.wrappedConnection;
                this.wrappedConnection = null;
                wrappedConnection.releaseConnection();
            } else {
                // do nothing
            }
        }

        public void setConnectionTimeout(int timeout) {
            if (hasConnection()) {
                wrappedConnection.setConnectionTimeout(timeout);
            } else {
                // do nothing
            }
        }

        public void setHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setHost(host);
            } else {
                // do nothing
            }
        }

        public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
            if (hasConnection()) {
                wrappedConnection.setHttpConnectionManager(httpConnectionManager);
            } else {
                // do nothing
            }
        }

        public void setLastResponseInputStream(InputStream inStream) {
            if (hasConnection()) {
                wrappedConnection.setLastResponseInputStream(inStream);
            } else {
                // do nothing
            }
        }

        public void setPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setPort(port);
            } else {
                // do nothing
            }
        }

        public void setProtocol(Protocol protocol) {
            if (hasConnection()) {
                wrappedConnection.setProtocol(protocol);
            } else {
                // do nothing
            }
        }

        public void setProxyHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyHost(host);
            } else {
                // do nothing
            }
        }

        public void setProxyPort(int port) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setProxyPort(port);
            } else {
                // do nothing
            }
        }

        public void setSoTimeout(int timeout)
            throws SocketException, IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setSoTimeout(timeout);
            } else {
                // do nothing
            }
        }

        public void shutdownOutput() {
            if (hasConnection()) {
                wrappedConnection.shutdownOutput();
            } else {
                // do nothing
            }
        }

        public void tunnelCreated() throws IllegalStateException, IOException {
            if (hasConnection()) {
                wrappedConnection.tunnelCreated();
            } else {
                // do nothing
            }
        }

        public void write(byte[] data, int offset, int length)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.write(data, offset, length);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void write(byte[] data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.write(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine()
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.writeLine();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void writeLine(byte[] data)
            throws IOException, IllegalStateException, HttpRecoverableException {
            if (hasConnection()) {
                wrappedConnection.writeLine(data);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void flushRequestOutputStream() throws IOException {
            if (hasConnection()) {
                wrappedConnection.flushRequestOutputStream();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public int getSoTimeout() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSoTimeout();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public String getVirtualHost() {
            if (hasConnection()) {
                return wrappedConnection.getVirtualHost();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setVirtualHost(String host) throws IllegalStateException {
            if (hasConnection()) {
                wrappedConnection.setVirtualHost(host);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public int getSendBufferSize() throws SocketException {
            if (hasConnection()) {
                return wrappedConnection.getSendBufferSize();
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

        public void setSendBufferSize(int sendBufferSize) throws SocketException {
            if (hasConnection()) {
                wrappedConnection.setSendBufferSize(sendBufferSize);
            } else {
                throw new IllegalStateException("Connection has been released");
            }
        }

    }

}

