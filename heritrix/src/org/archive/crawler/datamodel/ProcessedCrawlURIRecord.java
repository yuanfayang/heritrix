/* Copyright (C) 2003 Internet Archive.
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
 *
 * Created on Jul 31, 2003
 *
 */
package org.archive.crawler.datamodel;

import org.archive.util.TimedFixedSizeList;

/** When a CrawlURI has been processed a ProcessedCrawlURIRecord
 *  can be created to store meta-data about the URI that can be extracted
 *  from the CrawlURI before it is 
 * @author Parker Thompson
 * 
 * @deprecated Was originally intended for use by statistics trackers.  
 * Due to memory constraints they should only store aggregate data!
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
