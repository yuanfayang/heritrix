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

	public void markContentBegin() {
		this.httpRecorder.markContentBegin();
	}
    
    public void closeConnection() {
        // Calling isOpen, makes httpclient do a lookup on the connection.
        // If something bad happened during connection use,
        // it will usually call close itself inside in the isOpen --
        // the close() won't get called but the wished-for effect will
        // have occurred.
        if (this.connection != null && this.connection.isOpen()) {
            this.connection.close();
            this.connection = null;
        }
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
