/*
 * Created on Jul 29, 2003
 *
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.datamodel.InitializationException;

/** ConfigurationExceptions should be thrown when a configuration file
 *   is missing data, or contains uninterpretable data, at runtime.  Fatal
 *   errors (that should cause the program to exit) should be thrown as
 *   FatalConfigurationExceptions.
 * 
 *   You may optionally note the 
 * 
 * @author Parker Thompson
 *
 */
public class ConfigurationException extends InitializationException {

	// optionally store the file name and element so the catcher
	// can report the information and/or take other actions based on it
	protected String file = null;	
	protected String element = null;

	public ConfigurationException() {
		super();
	}

	/** Create a ConfigurationException
	 * @param message
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/** Create a ConfigurationException
	 * @param cause
	 */
	public ConfigurationException(Throwable cause) {
		super(cause);
	}
	
	/** Create ConfigurationException
	 * @param message
	 * @param filename
	 * @param elementname
	 */
	public ConfigurationException(String message, String filename, String elementname){
		super(message);
		file = filename;
		element = elementname;
	}
	
	/**  Create ConfigurationException
	 * @param message
	 * @param cause
	 * @param filename
	 * @param elementname
	 */
	public ConfigurationException(String message, Throwable cause, String filename, String elementname){
		super(message, cause);
		file = filename;
		element = elementname;
	}
	
	/** Create ConfigurationException
	 * @param cause
	 * @param filename
	 * @param elementname
	 */
	public ConfigurationException(Throwable cause, String filename, String elementname){
		super(cause);
		file = filename;
		element = elementname;
	}
	
	/** Store the name of the configuration file that was being parsed
	 *  when this error occured. 
	 * @param filename
	 */
	public void setFile(String name){
		file = name;
	}
	public String getFile(){
		return file;
	}
	
	/** Set the name of the element that was being parsed 
	 *   when this error occured.
	 * @param target
	 */
	public void setElement(String target){
		element = target;
	}
	public String getElement(){
		return element;
	}

}
