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
 * PaddingStringBuffer.java
 * Created on Oct 23, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * StringBuffer-like utility which can add spaces to reach a certain column.
 * 
 * Current use of String concatenation is awfully inefficient, should be
 * changed at some point. 
 * 
 * @author Gordon Mohr
 */
public class PaddingStringBuffer {
	// TODO: be more efficient
	String buffer = "";
	
	/**
	 * 
	 */
	public PaddingStringBuffer() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param string
	 */
	public PaddingStringBuffer append(String string) {
		buffer += string;
		return this; 
	}
	
	/**
	 * Append, right-aligned to the given column
	 * 
	 * @param string
	 */
	public PaddingStringBuffer raAppend(int col, String string) {
		padTo(col-string.length());
		buffer += string;
		return this; 
	}

	/**
	 * @param i
	 */
	public PaddingStringBuffer padTo(int i) {
		while(buffer.length()<i) {
			buffer += " ";
		}
		return this;
	}

	/**
	 * @param i
	 */
	public PaddingStringBuffer append(int i) {
		buffer += i;
		return this; 
	}
	

	/**
	 * @param col
	 * @param i
	 * @return
	 */
	public PaddingStringBuffer raAppend(int col, int i) {
		return raAppend(col,Integer.toString(i)); 
	}

	/**
	 * @param i
	 */
	public PaddingStringBuffer append(long lo) {
		buffer += lo;
		return this; 
	}
	
	/**
	 * @param col
	 * @param lo
	 * @return
	 */
	public PaddingStringBuffer raAppend(int col, long lo) {
		return raAppend(col,Long.toString(lo)); 
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return buffer;
	}

}
