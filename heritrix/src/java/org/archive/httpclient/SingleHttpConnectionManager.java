/* SingleHttpConnectionManager
*
* $Id$
*
* Created on Mar 8, 2004
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
import java.io.InputStream;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * An HttpClient-compatible HttpConnection "manager" that  gives out a
 * new connection each time skipping the overhead of connection management,
 * since we already throttle our crawler with external mechanisms.
 * 
 * <p>We used to subclass SimpleHttpConnectionManager but it keeps a reference
 * to a single httpconnection internally.  In a threaded environment we were
 * working on other method's connections using 3.0 httpclient (It worked fine
 * in 2.0 httpclient).  Below is copy and paste of SimpleHttpConnectionManager
 * from httpclient with all no reference to any connection kept locally.
 * 
 * <p>We can't use the MultiThreadedHttpConnectionManager because things get
 * confused closing at times of high concurrency (250 ToeThreads).  You'll see
 * exceptions like the below (if assertions are enabled):
 * <pre>
 * java.lang.AssertionError: Inputstream is null: ToeThread #149
 *  at org.archive.io.RecordingInputStream.read(RecordingInputStream.java:105)
 *  at java.io.FilterInputStream.read(FilterInputStream.java:111)
 *  at org.apache.commons.httpclient.ContentLengthInputStream.read(ContentLengthInputStream.java:148)
 *  at org.apache.commons.httpclient.ContentLengthInputStream.read(ContentLengthInputStream.java:162)
 *  at org.apache.commons.httpclient.ChunkedInputStream.exhaustInputStream(ChunkedInputStream.java:351)
 *  at org.apache.commons.httpclient.ContentLengthInputStream.close(ContentLengthInputStream.java:96)
 *  at java.io.FilterInputStream.close(FilterInputStream.java:159)
 *  at org.apache.commons.httpclient.AutoCloseInputStream.notifyWatcher(AutoCloseInputStream.java:176)
 *  at org.apache.commons.httpclient.AutoCloseInputStream.close(AutoCloseInputStream.java:140)
 *  at org.apache.commons.httpclient.HttpMethodBase.releaseConnection(HttpMethodBase.java:1093)
 *  at org.archive.crawler.fetcher.FetchHTTP.innerProcess(FetchHTTP.java:283)
 *  at org.archive.crawler.framework.Processor.process(Processor.java:106)
 *  at org.archive.crawler.framework.ToeThread.processCrawlUri(ToeThread.java:251)
 *  at org.archive.crawler.framework.ToeThread.run(ToeThread.java:127)
 * </pre>
 * Its probably harmless (We use null stream as indication that stream has
 * already been closed).  Means we're doing things in a different order when
 * there is alot going on. Could mean lost data.  Probably not (Attempts at
 * verification of lost data is difficult because is.available() on a closed
 * socket throws a hidden IOException that makes things look like its working
 * but it ain't; could see if socket is open before doing the available test
 * but getting at socket from inside in HttpRecorder would be perverse).
 * Because its worrisome, will use this dumb new HttpConnection to go with
 * each new HttpMethod.
 * 
 * @author gojomo
 */
public class SingleHttpConnectionManager implements HttpConnectionManager {
    private HttpConnectionManagerParams params =
        new HttpConnectionManagerParams(); 
    
    public SingleHttpConnectionManager() {
        super();
    }
    
    public HttpConnection getConnection(HostConfiguration hostConfiguration) {
        return getConnection(hostConfiguration, 0);
    }

    public boolean isConnectionStaleCheckingEnabled() {
        return this.params.isStaleCheckingEnabled();
    }

    public void setConnectionStaleCheckingEnabled(
            boolean connectionStaleCheckingEnabled) {
        this.params.setStaleCheckingEnabled(connectionStaleCheckingEnabled);
    }
    
    public HttpConnection getConnectionWithTimeout(
    		HostConfiguration hostConfiguration, long timeout) {
        // timeout is unused.
        HttpConnection connection = new HttpConnection(hostConfiguration);
        connection.setHttpConnectionManager(this);
        connection.getParams().setDefaults(this.params);
        return connection;
    }

    public HttpConnection getConnection(
    		HostConfiguration hostConfiguration, long timeout) {
        return getConnectionWithTimeout(hostConfiguration, timeout);
    }

    public void releaseConnection(HttpConnection conn) {
    	finishLast(conn);
    }

    public HttpConnectionManagerParams getParams() {
        return this.params;
    }

    public void setParams(final HttpConnectionManagerParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }
    
    public void closeIdleConnections(long idleTimeout) {
    	throw new RuntimeException("Unimplemented");
    }
    
    private static void finishLast(HttpConnection conn) {
        InputStream lastResponse = conn.getLastResponseInputStream();
        if (lastResponse != null) {
            conn.setLastResponseInputStream(null);
            try {
                lastResponse.close();
            } catch (IOException ioe) {
                //FIXME: badness - close to force reconnect.
                conn.close();
            }
        }
    }
}