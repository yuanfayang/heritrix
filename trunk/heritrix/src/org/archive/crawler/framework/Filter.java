/* 
 * Filter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * 
 * @author Gordon Mohr
 */
public abstract class Filter extends XMLConfig {
	String name;
	
	public  void setName(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}
	
	public abstract boolean accepts(Object o); 
}
