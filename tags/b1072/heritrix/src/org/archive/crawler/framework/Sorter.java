/*
 * Sorter.java
 * Created on May 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * @author gojomo
 *
 */
public interface Sorter {
	public void setName(String name);
	public String getName();
	public String categorize(Object o); 
}
