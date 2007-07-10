/* FetchHistoryProcessor
 * 
 * Created on Feb 12, 2005
 *
 * Copyright (C) 2007 Internet Archive.
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
package org.archive.crawler.recrawl;

import java.util.HashMap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.state.Expert;
import org.archive.state.Key;
import org.archive.state.StateProvider;

import static org.archive.modules.recrawl.RecrawlAttributeConstants.*;

/**
 * Maintain a history of fetch information inside the CrawlURI's attributes. 
 * 
 * @author gojomo
 * @version $Date: 2006-09-25 20:19:54 +0000 (Mon, 25 Sep 2006) $, $Revision: 4654 $
 */
public class FetchHistoryProcessor extends Processor 
implements CoreAttributeConstants {
    private static final long serialVersionUID = 8476621038669163983L;
    
    /** Desired history array length. */
    @Expert
    final public static Key<Integer> HISTORY_LENGTH = Key.make(2);
//    key description: "Number of previous fetch entries to retain in the URI " +
//    "history. The current fetch becomes a history entry at " +
//    "this Processor step, so the smallest useful value is " +
//    "'2' (including the current fetch). Default is '2'."
    
    // class description: "FetchHistoryProcessor. Maintain a history of fetch " +
    // "information inside the CrawlURI's attributes.."
    
    public FetchHistoryProcessor(String name) {
    }

    @Override
    protected void innerProcess(ProcessorURI curi) throws InterruptedException {
        HashMap<String, Object> latestFetch = new HashMap<String,Object>();

        // save status
        latestFetch.put(A_STATUS,curi.getFetchStatus());
        // save fetch start time
        latestFetch.put(A_FETCH_BEGAN_TIME,curi.getData().get(A_FETCH_BEGAN_TIME));
        // save digest
        String digest = curi.getContentDigestSchemeString();
        if(digest!=null) {
            latestFetch.put(A_CONTENT_DIGEST,digest);
        }
        // save relevant HTTP headers, if available
        if(curi.containsDataKey(A_HTTP_TRANSACTION)) {
            HttpMethodBase method = 
                (HttpMethodBase) curi.getData().get(A_HTTP_TRANSACTION);
            saveHeader(A_ETAG_HEADER,method,latestFetch);
            saveHeader(A_LAST_MODIFIED_HEADER,method,latestFetch);
            // save reference length (real or virtual)
            long referenceLength; 
            if(curi.containsDataKey(A_REFERENCE_LENGTH) ) {
                // reuse previous length if available (see FetchHTTP#setSizes). 
                referenceLength = (Long) curi.getData().get(A_REFERENCE_LENGTH);
            } else {
                // normally, use content-length
                referenceLength = curi.getContentLength();
            }
            latestFetch.put(A_REFERENCE_LENGTH,referenceLength);
        }
        
        // get or create proper-sized history array
        int targetHistoryLength = curi.get(this, HISTORY_LENGTH);
        HashMap[] history = 
            (HashMap[]) (curi.containsDataKey(A_FETCH_HISTORY) 
		    ? curi.getData().get(A_FETCH_HISTORY) 
		    : new HashMap[targetHistoryLength]);
        if(history.length != targetHistoryLength) {
            HashMap[] newHistory = new HashMap[targetHistoryLength];
            System.arraycopy(
                    history,0,
                    newHistory,0,
                    Math.min(history.length,newHistory.length));
            history = newHistory; 
        }
        
        // rotate all history entries up one slot, insert new at [0]
        for(int i = history.length-1; i >0; i--) {
            history[i] = history[i-1];
        }
        history[0]=latestFetch;
        
        curi.getData().put(A_FETCH_HISTORY,history);
    }

    /**
     * Save a header from the given HTTP operation into the AList.
     * 
     * @param name header name to save into history AList
     * @param method http operation containing headers
     * @param latestFetch AList to get header
     */
    protected void saveHeader(String name, HttpMethodBase method, 
    		HashMap<String, Object> latestFetch) {
        Header header = method.getResponseHeader(name);
        if(header!=null) {
            latestFetch.put(name, header.getValue());
        }
    }

    @Override
    public void initialTasks(StateProvider defaults) {
        // ensure history info persists across enqueues and recrawls
        CrawlURI.addDataPersistentMember(A_FETCH_HISTORY);
    }

	@Override
	protected boolean shouldProcess(ProcessorURI uri) {
		// TODO evaluate if any pre-eligibility testing should occur
		return true;
	}
    
    
}