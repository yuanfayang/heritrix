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
package org.archive.crawler.event;

/**
 * An Adapter class for the CrawlStatusListener interface. 
 * 
 * @author Kristinn Sigurdsson
 */
public class CrawlStatusAdapter implements CrawlStatusListener
{

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlEnded(java.lang.String)
	 */
	public void crawlEnded(String sExitMessage) {
	}
	
}
