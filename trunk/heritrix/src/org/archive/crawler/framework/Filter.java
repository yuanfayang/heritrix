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
	boolean inverter = false;
	
	public  void setName(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}
	
	public boolean accepts(Object o) {
		return inverter ^ innerAccepts(o);
	}
	
	/**
	 * @param o
	 * @return
	 */
	protected abstract boolean innerAccepts(Object o);
	
	public void initialize() {
		setName(getStringAt("@name"));
		if("not".equals(getStringAt("@modifier"))) {
			inverter = true;
		}
	}
}
