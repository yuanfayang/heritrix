/*
 * Created on Jul 31, 2003
 *
 */
package org.archive.crawler.datamodel;


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
