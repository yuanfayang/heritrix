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
 */
package org.archive.crawler.framework;

/**
 * An interface for objects that want to collect statistics on 
 * running crawls. An implementation of this is referenced in the
 * crawl order and loaded when the crawl begins.
 * <p>
 * It will be given a reference to the relevant CrawlController. 
 * The CrawlController will contain any additional configuration
 * information needed.
 * <p>
 * Any class that implements this interface can be specified as a 
 * statistics tracker in a crawl order.  The CrawlController will
 * then create and initialize a copy of it and call it's start() 
 * method.
 * <p>
 * This interface also specifies several methods to access data that
 * the CrawlController or the URIFrontier may be interested in at
 * run time but do not want to have keep track of for themselves.
 * {@link org.archive.crawler.framework.AbstractTracker AbstractTracker}
 * implements these. If there are more then one StatisticsTracking
 * classes defined in the crawl order only the first one will be
 * used to access this data.
 * <p>
 * It is recommended that it register for 
 * {@link org.archive.crawler.event.CrawlStatusListener CrawlStatus} events and
 * {@link org.archive.crawler.event.CrawlURIDispositionListener CrawlURIDisposition} 
 * events to be able to properly monitor a crawl. Both are registered with the 
 * CrawlController.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see AbstractTracker
 * @see org.archive.crawler.event.CrawlStatusListener
 * @see org.archive.crawler.event.CrawlURIDispositionListener
 * @see org.archive.crawler.framework.CrawlController
 */
public interface StatisticsTracking extends Runnable
{
	/**
	 * Do initialization. 
	 * The CrawlController will call this method before calling the start() method.
	 * 
	 * @param c The {@link CrawlController CrawlController} running the crawl that this class is to gather statistics on.
	 */
	public void initalize(CrawlController c);
	
	/**
	 * Returns how long the current crawl has been running (excluding any time spent 
	 * paused/suspended/stopped) since it was begin.
	 * 
	 * @return The length of time - in msec - that this crawl has been running.
	 */
	public long crawlDuration();
}