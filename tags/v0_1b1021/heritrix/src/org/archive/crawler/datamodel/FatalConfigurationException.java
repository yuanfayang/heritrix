/*
 * Created on Jul 29, 2003
 *
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.datamodel.ConfigurationException;;

/**
 * @author Parker Thompson
 *
 */
public class FatalConfigurationException extends ConfigurationException {

	public FatalConfigurationException(String explanation) {
		super(explanation);
	}

	public FatalConfigurationException() {
		super();
	}
	
	public FatalConfigurationException(String message, String file, String element){
		super(message,file,element);
	}
}
