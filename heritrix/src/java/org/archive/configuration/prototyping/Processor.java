package org.archive.configuration.prototyping;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;

public class Processor implements Configurable {
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    }

    public Configurable initialize(Registry r)
    throws ConfigurationException {
    	return this;
    }

	public Configuration getInitialConfiguration() throws ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}
}
