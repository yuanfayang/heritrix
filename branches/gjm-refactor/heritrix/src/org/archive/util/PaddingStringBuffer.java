/* 
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
	 * @param i
	 */
	public PaddingStringBuffer padTo(int i) {
		while(buffer.length()<i) {
			buffer += " ";
		}
		return this;
	}

	/**
	 * @param discoveredPages
	 */
	public PaddingStringBuffer append(int i) {
		buffer += i;
		return this; 
	}

	/**
	 * @param discoveredPages
	 */
	public PaddingStringBuffer append(long lo) {
		buffer += lo;
		return this; 
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return buffer;
	}

}
