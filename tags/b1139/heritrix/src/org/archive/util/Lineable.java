/*
 * Lineable.java
 * Created on Nov 12, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Interface for objects which offer their own single-line
 * summary text.
 * 
 * @author gojomo
 *
 */
public interface Lineable {
	/**
	 * Report the object's 1-line fielded description, usually
	 * with the class name in the first position. 
	 * 
	 * @return String
	 */
	public String getLine();
}
