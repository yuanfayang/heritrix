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
