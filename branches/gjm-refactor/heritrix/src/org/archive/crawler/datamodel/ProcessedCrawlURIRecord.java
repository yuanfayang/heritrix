/*
 * Created on Jul 31, 2003
 *
 */
package org.archive.crawler.datamodel;

import org.archive.util.TimedFixedSizeList;

/** When a CrawlURI has been processed a ProcessedCrawlURIRecord
 *  can be created to store meta-data about the URI that can be extracted
 *  from the CrawlURI before it is 
 * @author Parker Thompson
 */
public class ProcessedCrawlURIRecord implements CoreAttributeConstants, TimedFixedSizeList.TimedItem {

	private int fetchStatus = 0;	// default to unattempted
	private int deferrals = 0;
	private int fetchAttempts = 0;	// the number of fetch attempts that have been made
	private long contentSize = -1;
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
	
	public long getSize(){
		return contentSize;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.TimedFixedSizeList.TimedItem#getTime()
	 */
	public long getTime() {
		return getEndTime();
	}

}
