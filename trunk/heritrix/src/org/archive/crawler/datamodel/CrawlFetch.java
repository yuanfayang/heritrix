/* 
 * CrawlFetch.java
 * Created on Apr 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.io.ReadWriteVirtualBuffer;

/**
 * URI/protocol independent single "fetch" episode. 
 * 
 * Could have protocol-specific subclasses (HTTPFetch, etc.)
 * 
 * Unsure if this should be folded into CrawlURI or is valuable
 * as separate object.
 * 
 * @author Gordon Mohr
 */
public abstract class CrawlFetch {
	UURI uuri;
	long startTime;
	long endTime;
	
	public abstract ReadWriteVirtualBuffer getRequestBuffer();
	public abstract ReadWriteVirtualBuffer getResponseBuffer();
	
}
