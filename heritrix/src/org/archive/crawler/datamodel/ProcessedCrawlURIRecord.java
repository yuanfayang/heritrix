/*
 * Created on Jul 31, 2003
 *
 */
package org.archive.crawler.datamodel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.archive.crawler.basic.FetcherDNS;
import org.archive.crawler.basic.URIStoreable;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;

import st.ata.util.AList;
import st.ata.util.HashtableAList;

/** When a CrawlURI has been processed a ProcessedCrawlURIRecord
 *  can be created to store meta-data about the URI that can be extracted
 *  from the CrawlURI before it is 
 * @author Parker Thompson
 */
public class ProcessedCrawlURIRecord implements CoreAttributeConstants{

	private int fetchStatus = 0;	// default to unattempted
	private int deferrals = 0;
	private int fetchAttempts = 0;	// the number of fetch attempts that have been made
	private int contentSize = -1;
	protected long fetchStartTime = -1;
	protected long fetchEndTime = -1;
	

	public ProcessedCrawlURIRecord() {
		super();
	}
	
	public ProcessedCrawlURIRecord(CrawlURI curi){
		contentSize = curi.getContentSize();
		
		fetchStartTime = curi.getAList().getLong(A_FETCH_BEGAN_TIME);
		fetchEndTime = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
		
	}
	
	public long getStartTime(){
		return fetchStartTime;
	}
	
	public long getEndTime(){
		return fetchEndTime;
	}
	
	public int getSize(){
		return contentSize;
	}

}
