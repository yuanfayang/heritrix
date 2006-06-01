package org.archive.configuration;


/**
 * Implemented by code that wants to be configured.
 * 
 * Configurables must have a constructor that takes a String, the
 * name of the instance.
 * 
 * @author stack
 */
public interface Configurable {
	/**
	 * Called soon after construction.
	 * @param r Registry of configurations.  Look up this components
	 * configuration via the registry or the configuration of other
	 * components.  Its ok to keep a reference to the registry for
	 * life of the configured code or to just use here in the configure
	 * method and then let it go.
	 * @throws ConfigurationException
	 */
    public void initialize(final Registry r) throws ConfigurationException;
    
    /**
     * Return a Configuration instance set with defaults and initial
     * values.  Used to learn what in Configurable is settable as well
     * as defaults and value ranges.
     * @return Initial configuration of the Configurable.  
     */
    public Configuration getInitialConfiguration() throws ConfigurationException;
}
