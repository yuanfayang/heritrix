/* HttpRecorderMethod
 *
 * Created on August 22, 2004
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

import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpConnection;
import org.archive.util.HttpRecorder;


/**
 * This class encapsulates the specializations supplied by the
 * overrides {@link HttpRecorderGetMethod} and {@link HttpRecorderPostMethod}.
 * 
 * It keeps instance of HttpRecorder and HttpConnection.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class HttpRecorderMethod {
    protected static Logger logger =
        Logger.getLogger(HttpRecorderMethod.class.getName());
    
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
    

	public HttpRecorderMethod(HttpRecorder recorder) {
        this.httpRecorder = recorder;
	}

	public void markContentBegin(HttpConnection connection) {
        if (connection != this.connection) {
            // We're checking that we're not being asked to work on
            // a connection that is other than the one we started
            // this method#execute with.
            throw new IllegalArgumentException("Connections differ: " +
                this.connection + " " + connection + " " +
                Thread.currentThread().getName());
        }
		this.httpRecorder.markContentBegin();
	}
    
    /**
     * @return Returns the connection.
     */
    public HttpConnection getConnection() {
        return this.connection;
    }
    
    /**
     * @param connection The connection to set.
     */
    public void setConnection(HttpConnection connection) {
        this.connection = connection;
    }
}