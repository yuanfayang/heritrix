/* HttpRecorderGetMethod
 * 
 * Created on Feb 24, 2004
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

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.util.HttpRecorder;


/**
 * Override of GetMethod that marks the passed HttpRecorder w/ the transition
 * from HTTP head to body and that forces a close on the responseConnection.
 * 
 * The actions done in this subclass used to be done by copying 
 * org.apache.commons.HttpMethodBase, overlaying our version in place of the 
 * one that came w/ httpclient.  Here is the patch of the difference between
 * shipped httpclient code and our mods:
 * <pre>
 *    @@ -1338,6 +1346,12 @@
 *         
 *        public void releaseConnection() {
 *                                                                                   
 *   +        // HERITRIX always ants the streams closed.
 *   +        if (responseConnection != null)
 *   +        {
 *   +            responseConnection.close();
 *   +        }
 *   +
 *            if (responseStream != null) {
 *                try {
 *                    // FYI - this may indirectly invoke responseBodyConsumed.
 *   @@ -1959,6 +1973,11 @@
 *                        this.statusLine = null;
 *                    }
 *                }
 *   +            // HERITRIX mark transition from header to content.
 *   +            if (this.httpRecorder != null)
 *   +            {
 *   +                this.httpRecorder.markContentBegin();
 *   +            }
 *                readResponseBody(state, conn);
 *                processResponseBody(state, conn);
 *            } catch (IOException e) {
 * </pre>
 * 
 * @author stack
 * @version $Id$
 */
public class HttpRecorderGetMethod extends GetMethod 
{   
    /**
     * Instance of http recorder we're using recording this http get.
     */
    private HttpRecorder httpRecorder = null;
    
    /**
     * Save around so can force close.
     * 
     * See [ 922080 ] IllegalArgumentException (size is wrong).
     * https://sourceforge.net/tracker/?func=detail&aid=922080&group_id=73833&atid=539099
     */
    private HttpConnection connection = null;
    
    
	public HttpRecorderGetMethod(String uri, HttpRecorder recorder)
    {
		super(uri);
        this.httpRecorder = recorder;
	}
    
	protected void readResponseBody(HttpState state, HttpConnection connection)
		throws IOException, HttpException
    {
        // We're about to read the body.  Mark http recorder.
		this.httpRecorder.markContentBegin();
		super.readResponseBody(state, connection);
	}
    
    
    protected boolean shouldCloseConnection(HttpConnection conn)
    {
        // Save off the connection so we can close it on our way out in case
        // httpclient fails to (We're not supposed to have access to the 
        // underlying connection object; am only violating contract because
        // see cases where httpclient is skipping out w/o cleaning up 
        // after itself). This is second attempt at catching the connection used
        // fetching.  First is above in the execute method override.  
        // 
        // If there's been a shortcircuit of the connection close, this method
        // most likely won't be called and I won't get a connection to close.
        // Means this bit of code is of little use but leaving it here anyways.
        if (conn != this.connection) {
            this.connection = conn;
        }
        
        // Always close connection after each request. As best I can tell, this
        // is superfluous -- we've set our client to be HTTP/1.0.  Doing this
        // out of paranoia.
        return true;
    }
    
    public void releaseConnection()
    {
        try {
            super.releaseConnection();
        }
        
        finally {
            // Calling isOpen, makes httpclient do a lookup on the connection.
            // If something bad happened during the releaseConnection above,
            /// it will usually call close itself inside in the isOpen -- 
            // the close() won't get called but the wished-for effect will 
            // have occurred.
            if (this.connection != null) {
                if (this.connection.isOpen()) {
                    this.connection.close();
                }
                this.connection = null;
            }
        }
    }

    public int execute(HttpState state, HttpConnection conn)
            throws HttpException, HttpRecoverableException, IOException
    {
        // Save off the connection so we can close it on our way out in case
        // httpclient fails to (We're not supposed to have access to the 
        // underlying connection object; am only violating contract because
        // see cases where httpclient is skipping out w/o cleaning up 
        // after itself).
        this.connection = conn;
        return super.execute(state, conn);
    }
}
