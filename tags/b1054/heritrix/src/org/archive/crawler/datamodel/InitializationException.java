/*
 * Created on Jul 29, 2003
 *
 */
package org.archive.crawler.datamodel;

/** InitializationExceptions should be thrown when there is a problem with 
 *   the crawl's initialization, such as file creation problems, etc.  In the event
 *   that a more specific exception can be thrown (such as a ConfigurationException
 *   in the event that there is a configuration-specific problem) it should be.  
 * 
 * @author Parker Thompson
 *
 */
public class InitializationException extends Exception {

	public InitializationException() {
		super();
	}

	/**
	 * @param message
	 */
	public InitializationException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public InitializationException(Throwable cause) {
		super(cause);
	}

}
