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
 * LongSet.java
 * Created on Oct 19, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Set for holding primitive long fingerprints. 
 * 
 * @author Gordon Mohr
 */
public interface LongFPSet {
	boolean add(long l);
	boolean contains(long l);
	boolean remove(long l);
	long count();
	
	/**
	 * Do a contains() check that doesn't require laggy
	 * activity (eg disk IO). If this returns true, 
	 * fp is definitely contained; if this returns 
	 * false, fp  *MAY* still be contained -- must use
	 * full-cost contains() to be sure. 
	 * 
	 * @param fp
	 * @return
	 */
	boolean quickContains(long fp);
}
