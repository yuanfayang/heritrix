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
 * Filter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.filter.OrFilter;

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
	 * @return If it accepts.
	 */
	protected abstract boolean innerAccepts(Object o);
	
	public void initialize(CrawlController controller) {
		if(xNode!=null) {
			setName(getStringAt("@name"));
			if("not".equals(getStringAt("@modifier"))) {
				inverter = true;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Filter<"+name+">";
	}

	/**
	 * @param other
	 * @return Filter.
	 */
	public Filter orWith(Filter other) {
		OrFilter orF = new OrFilter();
		orF.addFilter(this);
		orF.addFilter(other);
		return orF;
	}

}
