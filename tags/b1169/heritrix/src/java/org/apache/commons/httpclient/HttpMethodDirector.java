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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.HttpAuthenticator;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the process of executing a method including authentication, redirection and retries.
 */
class HttpMethodDirector {

    /** Maximum number of redirects and authentications that will be followed */
    private static final int MAX_FORWARDS = 100;

    private static final Log LOG = LogFactory.getLog(HttpMethodDirector.class);

	private HttpMethod method;
	
	private HttpState state;
	
	private HostConfiguration hostConfiguration;
    
    private HttpConnectionManager connectionManager;
    
    private HttpConnection connection;
    
    private HttpClientParams params;
    
    /** A flag to indicate if the connection should be released after the method is executed. */
    private boolean releaseConnection = false;

    /** How many times did this transparently handle a recoverable exception? */
    private int recoverableExceptionCount = 0;

    /** Realms that we tried to authenticate to */
    private Set realms = null;

    /** Proxy Realms that we tried to authenticate to */
    private Set proxyRealms = null;

    /** Actual authentication realm */
    private String realm = null;

    /** Actual proxy authentication realm */
    private String proxyRealm = null;
	
    /**
     * Executes the method associated with this method director.
     * 
     * @throws IOException
     * @throws HttpException
     */
    public void executeMethod() throws IOException, HttpException {
        
        method.getParams().setDefaults(this.params);
        
        try {
            int forwardCount = 0; //protect from an infinite loop

            while (forwardCount++ < MAX_FORWARDS) {
                // on every retry, reset this state information.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Execute loop try " + forwardCount);
                }

                executeMethodForHost();

                if (!isRetryNeeded()) {
                    // nope, no retry needed, exit loop.
                    break;
                }

                // retry - close previous stream.  Caution - this causes
                // responseBodyConsumed to be called, which may also close the
                // connection.
                if (method.getResponseBodyAsStream() != null) {
                    method.getResponseBodyAsStream().close();
                }

            } //end of retry loop

            if (forwardCount >= MAX_FORWARDS) {
                LOG.error("Narrowly avoided an infinite loop in execute");
                throw new ProtocolException("Maximum redirects ("
                    + MAX_FORWARDS + ") exceeded");
            }

        } finally {
            if (connection != null) {
                connection.setLocked(false);
            }
            // If the response has been fully processed, return the connection
            // to the pool.  Use this flag, rather than other tests (like
            // responseStream == null), as subclasses, might reset the stream,
            // for example, reading the entire response into a file and then
            // setting the file as the stream.
            if (releaseConnection && connection != null) {
                connection.releaseConnection();
            } else if (method.getResponseBodyAsStream() == null) {
                method.releaseConnection();
            }
        }

    }
    
    /**
     * Adds authentication headers if <code>authenticationPreemtive</code> has been set.
     * 
     * @see HttpState#isAuthenticationPreemptive()
     */
    private void addPreemtiveAuthenticationHeaders() {
        
        //pre-emptively add the authorization header, if required.
        if (this.params.isAuthenticationPreemptive()) {

            LOG.debug("Preemptively sending default basic credentials");

            try {
                if (HttpAuthenticator.authenticateDefault(method, connection, state)) {
                    LOG.debug("Default basic credentials applied");
                }
                if (connection.isProxied()) {
                    if (HttpAuthenticator.authenticateProxyDefault(method, connection, state)) {
                        LOG.debug("Default basic proxy credentials applied");
                    }
                }
            } catch (AuthenticationException e) {
                // Log error and move on
                LOG.error(e.getMessage(), e);
            }
        }
    }
    
    /**
     * Makes sure there is a connection allocated and that it is valid and open.
     * 
     * @return <code>true</code> if a valid connection was established, 
     * <code>false</code> otherwise
     * 
     * @throws IOException
     * @throws HttpException
     */
    private boolean establishValidOpenConnection() throws IOException, HttpException {
        
        // make sure the connection we have is appropriate
        if (connection != null && !hostConfiguration.hostEquals(connection)) {
            connection.setLocked(false);
            connection.releaseConnection();
            connection = null;
        }
        
        // get a connection, if we need one
        if (connection == null) {
            connection = connectionManager.getConnectionWithTimeout(
                hostConfiguration,
                this.params.getConnectionManagerTimeout() 
            );
            connection.setLocked(true);

            realms = new HashSet();
            proxyRealms = new HashSet();
            
            addPreemtiveAuthenticationHeaders();
        }

        try {
            // Catch all possible exceptions to make sure to release the 
            // connection, as although the user may call 
            // Method->releaseConnection(), the method doesn't know about the
            // connection until HttpMethod.execute() is called.
            
            if (!connection.isOpen()) {
                // this connection must be opened before it can be used
                connection.setSoTimeout(this.params.getSoTimeout());
                connection.setConnectionTimeout(this.params.getConnectionTimeout());
                if(method.getHttpRecorder()!=null) {
                	connection.setHttpRecorder(method.getHttpRecorder());
                }
                connection.open();
                if (connection.isProxied() && connection.isSecure()) {
                    // we need to create a secure tunnel before we can execute the real method
                    if (!executeConnect()) {
                        // abort, the connect method failed
                        return false;
                    }
                }
            } else if (
                !(method instanceof ConnectMethod)
                && connection.isProxied() 
                && connection.isSecure() 
                && !connection.isTransparent()
            ) {
                // this connection is open but the secure tunnel has not be created yet,
                // execute the connect again
                if (!executeConnect()) {
                    // abort, the connect method failed
                    return false;
                }
            }
            
        } catch (IOException e) {
            releaseConnection = true;
            throw e;
        } catch (RuntimeException e) {
            releaseConnection = true;
            throw e;
        }
        
        return true;
    }
    
    /**
     * Executes a method with the current hostConfiguration.
     *
     * @throws IOException if an I/O (transport) error occurs. Some transport exceptions 
     * can be recovered from.
     * @throws HttpException  if a protocol exception occurs. Usually protocol exceptions 
     * cannot be recovered from.
     */
    private void executeMethodForHost() throws IOException, HttpException {
        
        int execCount = 0;
        // TODO: how do we get requestSent?
        boolean requestSent = false;
        
        // loop until the method is successfully processed, the retryHandler 
        // returns false or a non-recoverable exception is thrown
        while (true) {
            execCount++;
            requestSent = false;

            if (!establishValidOpenConnection()) {
                return;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Attempt number " + execCount + " to process request");
            }
            try {
                method.execute(state, connection);
                break;
            } catch (HttpRecoverableException httpre) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing the connection.");
                }
                connection.close();
                LOG.info("Recoverable exception caught when processing request");
                // update the recoverable exception count.
                recoverableExceptionCount++;
                
                // test if this method should be retried                
                if (!getMethodRetryHandler().retryMethod(
                        method, 
                        connection, 
                        httpre, 
                        execCount, 
                        requestSent)
                ) {
                    LOG.warn(
                        "Recoverable exception caught but MethodRetryHandler.retryMethod() "
                        + "returned false, rethrowing exception"
                    );
                    throw httpre;
                }
            }
        }
        
    }
    
    private MethodRetryHandler getMethodRetryHandler() {
        
        if (method instanceof HttpMethodBase) {
            return ((HttpMethodBase) method).getMethodRetryHandler();
        } else {
            return new DefaultMethodRetryHandler();
        }
    }
    
    /**
     * Executes a ConnectMethod to establish a tunneled connection.
     * 
     * @return <code>true</code> if the connect was successful
     * 
     * @throws IOException
     * @throws HttpException
     */
    private boolean executeConnect() throws IOException, HttpException {

        ConnectMethod connectMethod = new ConnectMethod();
        
        HttpMethod tempMethod = this.method;        
        this.method = connectMethod;
        
        try {
            executeMethod();
        } catch (HttpException e) {
            this.method = tempMethod;
            throw e;
        } catch (IOException e) {
            this.method = tempMethod;
            throw e;
        }

        int code = method.getStatusCode();
        
        if ((code >= 200) && (code < 300)) {
            this.method = tempMethod;
            return true;
        } else {
            // What is to follow is an ugly hack.
            // I REALLY hate having to resort to such
            // an appalling trick
            // TODO: Connect method must be redesigned.
            // The only feasible solution is to split monolithic
            // HttpMethod into HttpRequest/HttpResponse pair.
            // That would allow to execute CONNECT method 
            // behind the scene and return CONNECT HttpResponse 
            // object in response to the original request that 
            // contains the correct status line, headers & 
            // response body.
    
            LOG.debug("CONNECT failed, fake the response for the original method");
            // Pass the status, headers and response stream to the wrapped
            // method.
            // To ensure that the connection is not released more than once
            // this method is still responsible for releasing the connection. 
            // This will happen when the response body is consumed, or when
            // the wrapped method closes the response connection in 
            // releaseConnection().
            if (tempMethod instanceof HttpMethodBase) {
                ((HttpMethodBase) tempMethod).fakeResponse(
                    connectMethod.getStatusLine(),
                    connectMethod.getResponseHeaderGroup(),
                    connectMethod.getResponseBodyAsStream()
                );
            } else {
                releaseConnection = true;
                LOG.warn(
                    "Unable to fake response on method as it is not derived from HttpMethodBase.");
            }
            this.method = tempMethod;
            return false;
        }
    }
    
	/**
	 * Process the redirect response.
     * 
	 * @return <code>true</code> if the redirect was successful
	 */
	private boolean processRedirectResponse() {

		if (!method.getFollowRedirects()) {
			LOG.info("Redirect requested but followRedirects is "
					+ "disabled");
			return false;
		}

		//get the location header to find out where to redirect to
		Header locationHeader = method.getResponseHeader("location");
		if (locationHeader == null) {
			// got a redirect response, but no location header
			LOG.error("Received redirect response " + method.getStatusCode()
					+ " but no location header");
			return false;
		}
		String location = locationHeader.getValue();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Redirect requested to location '" + location
					+ "'");
		}

		//rfc2616 demands the location value be a complete URI
		//Location       = "Location" ":" absoluteURI
		URI redirectUri = null;
		URI currentUri = null;

		try {
			currentUri = new URI(
				connection.getProtocol().getScheme(),
				null,
                connection.getHost(), 
                connection.getPort(), 
				method.getPath()
			);
			redirectUri = new URI(location, true);
			if (redirectUri.isRelativeURI()) {
				if (method.isStrictMode()) {
					LOG.warn("Redirected location '" + location 
						+ "' is not acceptable in strict mode");
					return false;
				} else { 
					//location is incomplete, use current values for defaults
					LOG.debug("Redirect URI is not absolute - parsing as relative");
					redirectUri = new URI(currentUri, redirectUri);
				}
			}
		} catch (URIException e) {
			LOG.warn("Redirected location '" + location + "' is malformed");
			return false;
		}

		//invalidate the list of authentication attempts
		this.realms.clear();
		//remove exisitng authentication headers
		method.removeRequestHeader(HttpAuthenticator.WWW_AUTH_RESP); 
		//update the current location with the redirect location.
		//avoiding use of URL.getPath() and URL.getQuery() to keep
		//jdk1.2 comliance.
		method.setPath(redirectUri.getEscapedPath());
		method.setQueryString(redirectUri.getEscapedQuery());
        hostConfiguration.setHost(redirectUri);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Redirecting from '" + currentUri.getEscapedURI()
				+ "' to '" + redirectUri.getEscapedURI());
		}

		return true;
	}

	/**
	 * Processes a response that requires authentication
	 *
	 * @param state the current state
	 * @param conn The connection
	 *
	 * @return <code>true</code> if the request has completed processing, <code>false</code> 
     * if more attempts are needed
	 */
	private boolean processAuthenticationResponse(HttpState state, HttpConnection conn) {
		LOG.trace("enter HttpMethodBase.processAuthenticationResponse("
			+ "HttpState, HttpConnection)");

		int statusCode = method.getStatusCode();
		// handle authentication required
		Header[] challenges = null;
		Set realmsUsed = null;
        String host = null;
		switch (statusCode) {
			case HttpStatus.SC_UNAUTHORIZED:
				challenges = method.getResponseHeaders(HttpAuthenticator.WWW_AUTH);
				realmsUsed = realms;
                host = conn.getVirtualHost();
                if (host == null) {
                    host = conn.getHost();
                }
				break;
			case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                challenges = method.getResponseHeaders(HttpAuthenticator.PROXY_AUTH);
				realmsUsed = proxyRealms;
                host = conn.getProxyHost();
				break;
		}
		boolean authenticated = false;
		// if there was a header requesting authentication
		if (challenges.length > 0) {
			AuthScheme authscheme = null;
			try {
				authscheme = HttpAuthenticator.selectAuthScheme(challenges);
			} catch (MalformedChallengeException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error(e.getMessage(), e);
				}
				return true;
			} catch (UnsupportedOperationException e) {
				if (LOG.isErrorEnabled()) {
					LOG.error(e.getMessage(), e);
				}
				return true;
			}
        
            StringBuffer buffer = new StringBuffer();
            buffer.append(host);
            buffer.append('#');
            buffer.append(authscheme.getID());
            String realm = buffer.toString();

            if (realmsUsed.contains(realm)) {
                if (LOG.isInfoEnabled()) {
                    buffer = new StringBuffer();
                    buffer.append("Already tried to authenticate with '");
                    buffer.append(authscheme.getRealm());
                    buffer.append("' authentication realm at ");
                    buffer.append(host);
                    buffer.append(", but still receiving: ");
                    buffer.append(method.getStatusLine().toString());
                    LOG.info(buffer.toString());
                }
                return true;
            } else {
                realmsUsed.add(realm);
            }

			try {
				//remove preemptive header and reauthenticate
				switch (statusCode) {
					case HttpStatus.SC_UNAUTHORIZED:
						method.removeRequestHeader(HttpAuthenticator.WWW_AUTH_RESP);
						authenticated = HttpAuthenticator.authenticate(
							authscheme, method, conn, state);
						this.realm = authscheme.getRealm();
						break;
					case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
						method.removeRequestHeader(HttpAuthenticator.PROXY_AUTH_RESP);
						authenticated = HttpAuthenticator.authenticateProxy(
							authscheme, method, conn, state);
						this.proxyRealm = authscheme.getRealm();
						break;
				}
            } catch (CredentialsNotAvailableException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(e.getMessage());
                }
                return true; // finished request
			} catch (AuthenticationException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                return true; // finished request
			}
			if (!authenticated) {
				// won't be able to authenticate to this challenge
				// without additional information
				LOG.debug("HttpMethodBase.execute(): Server demands "
						  + "authentication credentials, but none are "
						  + "available, so aborting.");
			} else {
				LOG.debug("HttpMethodBase.execute(): Server demanded "
						  + "authentication credentials, will try again.");
				// let's try it again, using the credentials
			}
		}

		return !authenticated; // finished processing if we aren't authenticated
	}

	/**
	 * Returns true if a retry is needed.
     * 
	 * @return boolean <code>true</code> if a retry is needed.
	 */
	private boolean isRetryNeeded() {
		switch (method.getStatusCode()) {
			case HttpStatus.SC_UNAUTHORIZED:
			case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
				LOG.debug("Authorization required");
				if (method.getDoAuthentication()) { //process authentication response
					//if the authentication is successful, return the statusCode
					//otherwise, drop through the switch and try again.
					if (processAuthenticationResponse(state, connection)) {
						return false;
					}
				} else { //let the client handle the authenticaiton
					return false;
				}
				break;

			case HttpStatus.SC_MOVED_TEMPORARILY:
			case HttpStatus.SC_MOVED_PERMANENTLY:
			case HttpStatus.SC_SEE_OTHER:
			case HttpStatus.SC_TEMPORARY_REDIRECT:
				LOG.debug("Redirect required");

				if (!processRedirectResponse()) {
					return false;
				}
				break;

			default:
				// neither an unauthorized nor a redirect response
				return false;
		} //end of switch

		return true;
	}

    /**
     * @return
     */
    public HostConfiguration getHostConfiguration() {
        return hostConfiguration;
    }

    /**
     * @param hostConfiguration
     */
    public void setHostConfiguration(HostConfiguration hostConfiguration) {
        this.hostConfiguration = hostConfiguration;
    }

    /**
     * @return
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @param method
     */
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    /**
     * @return
     */
    public HttpState getState() {
        return state;
    }

    /**
     * @param state
     */
    public void setState(HttpState state) {
        this.state = state;
    }

    /**
     * @return
     */
    public HttpConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * @param connectionManager
     */
    public void setConnectionManager(HttpConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * @return
     */
    public HttpParams getParams() {
        return this.params;
    }

    /**
     * @param params
     */
    public void setParams(final HttpClientParams params) {
        this.params = params;
    }

}
