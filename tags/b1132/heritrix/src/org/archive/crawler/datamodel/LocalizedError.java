/*
 * LocalizedError.java
 * Created on Oct 28, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * @author gojomo
 *
 */
public class LocalizedError {

	private String message;
	public Exception exception;
	private String processorName;

	/**
	 * @param processorName
	 * @param ex
	 * @param message
	 */
	public LocalizedError(String processorName, Exception ex, String message) {
		this.processorName = processorName;
		this.exception = ex;
		this.message = message;
	}

}
