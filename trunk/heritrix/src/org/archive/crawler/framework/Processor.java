/* 
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * 
 * @author Gordon Mohr
 */
public interface Processor {
	public boolean wants(CrawlURI curi);
	public void process(CrawlURI curi);
	public void init(Config conf);
	
	public void addEntryFilter(Filter f);
}
