/* 
 * UURIFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * 
 * @author Gordon Mohr
 */
public interface Filter {
	public void setName(String name);
	public String getName();
	public boolean accepts(Object o); 
}
