/* HeritrixSSLProtocolSocketFactory
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
}
