/* PatchedHttpClient
 * 
 * Created on Mar 19, 2004
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

import org.apache.commons.httpclient.ConnectMethod;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Override of HttpClient to customize executeMethod setting timeout after
 * the connection open.
 * 
 * Had to override setter methods to catch the setting of private values used
 * in executeMethod so could effect the ovveride.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class PatchedHttpClient extends HttpClient
{
    /** Log object for this class.
     * 
     * HERITRIX: Let the name be that of the parent class so loggings from here
     * show when httpclient logging is enabled.
     */
    private static final Log LOG = LogFactory.getLog(HttpClient.class);
    
    /** 
     * The timout in milliseconds when waiting for a connection from the 
     * {@link HttpConnectionManager connection manager} 
     *  
     * HERITRIX Duplicated because super is private.  Below we override setters
     * so we can get a copy of the value to use.  This data member is only used
     * by code that has been brought into there -- by setters and the execute
     * method.  Means should be safe having local copy; there won't be
     * disagreement w/ another variable kept up in the parent.
     */
    private long httpConnectionTimeout = 0;

    /** 
     * The socket timeout in milliseconds.
     * 
     * HERITRIX: Duplicated because super is private.  Below we override setters
     * so we can get a copy of the value to use.  This data member is only used
     * by code that has been brought into there -- by setters and the execute
     * method.  Means should be safe having local copy; there won't be
     * disagreement w/ another variable kept up in the parent.
     */
    private int timeoutInMilliseconds = 0;

    /** 
     * The connection timeout in milliseconds. 
     * 
     * HERITRIX: Duplicated because super is private.  Below we override setters
     * so we can get a copy of the value to use. This data member is only used
     * by code that has been brought into there -- by setters and the execute
     * method.  Means should be safe having local copy; there won't be
     * disagreement w/ another variable kept up in the parent.
     */
    private int connectionTimeout = 0;
    
    
    public PatchedHttpClient()
    {
        super();
    }

    public PatchedHttpClient(HttpConnectionManager httpConnectionManager)
    {
        super(httpConnectionManager);
    }
    
    /**
     * Sets the timeout in milliseconds used when retrieving an 
     * {@link HttpConnection HTTP connection} from the
     * {@link HttpConnectionManager HTTP connection manager}.
     * 
     * HERITRIX: Copied from super class so we can get copy of what timeout has
     * been set to.
     * 
     * @param timeout the timeout in milliseconds
     * 
     * @see HttpConnectionManager#getConnection(HostConfiguration, long)
     */
    public synchronized void setHttpConnectionFactoryTimeout(long timeout) {
        super.setHttpConnectionFactoryTimeout(timeout);
        this.httpConnectionTimeout = timeout;
    }
    
    /**
     * Sets the timeout until a connection is etablished. A timeout value of 
     * zero means the timeout is not used. The default value is zero.
     * 
     * HERITRIX: Copied from super class so we can get copy of what timeout has
     * been set to.
     * 
     * @param newTimeoutInMilliseconds Timeout in milliseconds.
     * 
     * @see HttpConnection#setConnectionTimeout(int)
     */
    public synchronized void setConnectionTimeout(int newTimeoutInMilliseconds) {
       super.setConnectionTimeout(newTimeoutInMilliseconds);
       this.connectionTimeout = newTimeoutInMilliseconds;
    }
    
    /**
     * Sets the socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds which is the
     * timeout for waiting for data. A timeout value of zero is interpreted as an
     * infinite timeout.
     *
     * HERITRIX: Copied from super class so we can get copy of what timeout has
     * been set to.
     * 
     * @param newTimeoutInMilliseconds Timeout in milliseconds
     */
    public synchronized void setTimeout(int newTimeoutInMilliseconds) {
        super.setTimeout(newTimeoutInMilliseconds);
        this.timeoutInMilliseconds = newTimeoutInMilliseconds;
    }


    /**
     * Executes the given {@link HttpMethod HTTP method} using the given custom 
     * {@link HostConfiguration host configuration} with the given custom 
     * {@link HttpState HTTP state}.
     * 
     * HERITRIX: This is a duplication of whats in the superclass excepting the 
     * lines marked HERITRIX in the below and changes using accessors rather 
     * than private data members directly.
     *
     * @param hostConfiguration The {@link HostConfiguration host configuration}
     * to use.
     * @param method the {@link HttpMethod HTTP method} to execute.
     * @param state the {@link HttpState HTTP state} to use when executing the
     * method.
     * If <code>null</code>, the state returned by {@link #getState()} will be
     * used instead.
     *
     * @return the method's response code
     *
     * @throws IOException If an I/O (transport) error occurs. Some transport
     * exceptions can be recovered from.
     * @throws HttpException  If a protocol exception occurs. Usually protocol
     * exceptions cannot be recovered from.
     * @since 2.0
     */
    public int executeMethod(HostConfiguration hostConfiguration, 
            HttpMethod method, HttpState state)
        throws IOException, HttpException  {
            
        LOG.trace("enter HttpClient.executeMethod(HostConfiguration,HttpMethod,HttpState)");

        if (method == null) {
            throw new IllegalArgumentException("HttpMethod parameter may not be null");
        }

        int soTimeout = 0;
        boolean strictMode = false;
        int connectionTimeout = 0;
        long httpConnectionTimeout = 0;
        HostConfiguration defaultHostConfiguration = null;

        /* access all synchronized data in a single block, this will keeps us
         * from accessing data asynchronously as well having to regain the lock
         * for each item.
         */
        synchronized (this) {
            // HERITRIX: Use accessors and data members from this class rather
            // than those of the parent (they are private there).
            soTimeout = this.timeoutInMilliseconds;
            strictMode = isStrictMode()? true: false;
            connectionTimeout = this.connectionTimeout;
            httpConnectionTimeout = this.httpConnectionTimeout;
            if (state == null) {
                state = getState();
            }
            defaultHostConfiguration = getHostConfiguration();
        }

        HostConfiguration methodConfiguration 
            = new HostConfiguration(hostConfiguration);
        
        if (hostConfiguration != defaultHostConfiguration) {
            // we may need to apply some defaults
            if (!methodConfiguration.isHostSet()) {
                methodConfiguration.setHost(
                    defaultHostConfiguration.getHost(),
                    defaultHostConfiguration.getVirtualHost(),
                    defaultHostConfiguration.getPort(),
                    defaultHostConfiguration.getProtocol()
                );
            }
            if (!methodConfiguration.isProxySet() 
                && defaultHostConfiguration.isProxySet()) {
                    
                methodConfiguration.setProxy(
                    defaultHostConfiguration.getProxyHost(),
                    defaultHostConfiguration.getProxyPort() 
                );   
            }
            if (methodConfiguration.getLocalAddress() == null
                && defaultHostConfiguration.getLocalAddress() != null) {
                    
                methodConfiguration.setLocalAddress(defaultHostConfiguration.getLocalAddress());
            }
        }
        
        HttpConnectionManager connmanager = getHttpConnectionManager();
        if (state.getHttpConnectionManager() != null) {
            connmanager = state.getHttpConnectionManager();
        }

        HttpConnection connection = connmanager.getConnection(
            methodConfiguration,
            httpConnectionTimeout
        );

        try {
            // Catch all possible exceptions to make sure to release the 
            // connection, as although the user may call 
            // Method->releaseConnection(), the method doesn't know about the
            // connection until HttpMethod.execute() is called.
            
            method.setStrictMode(strictMode);
            
            // HERITRIX: Moved this timeout to after connection.open.
            // connection.setSoTimeout(soTimeout);
            
            if (!connection.isOpen()) {
                connection.setConnectionTimeout(connectionTimeout);
                connection.open();
                // HERITRIX: Move socket timeout here.  It used to be done 
                // before connection.open.
                connection.setSoTimeout(soTimeout);
                if (connection.isProxied() && connection.isSecure()) {
                    method = new ConnectMethod(method);
                }
            }
        } catch (IOException e) {
            connection.releaseConnection();
            throw e;
        } catch (RuntimeException e) {
            connection.releaseConnection();
            throw e;
        }
        
        return method.execute(state, connection);
    }
}