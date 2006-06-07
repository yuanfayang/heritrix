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
	 * @return This configurable.
	 * @throws ConfigurationException
	 */
    public Configurable initialize(final Registry r)
    throws ConfigurationException;
    
    /**
     * Return a Configuration.
     * When called on a newly instantiated Configurable, has initial
     * Configuration with defaults, value ranges, etc. Note
     * that any Configuration gotten other than by going via the
     * Registry is dead Configuration: Always go via Registry to
     * read values.
     * @return Configuration of the Configurable.  
     */
    public Configuration getConfiguration()
    throws ConfigurationException;
}
