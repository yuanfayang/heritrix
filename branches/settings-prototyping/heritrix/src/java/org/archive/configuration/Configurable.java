package org.archive.configuration;


/**
 * Implemented by code that wants to be configured.
 * 
 * Usually Configurables have a constructor that takes a single String
 * argument, the name of the Configurable instance. Configurables need
 * the name so they can find their own {@link Configuration} in
 * a {@link Registry}.
 * 
 * @author stack
 */
public interface Configurable {
	/**
	 * Called soon after construction.
	 * @param r Registry of configurations.  Look up this components'
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
     * When called on a newly instantiated Configurable, has INITIAL
     * Configuration with descriptions, defaults, value ranges, etc.
     * <p>
     * Note: Any Configuration gotten other than by going via the
     * Registry is dead Configuration: Always go via Registry to
     * read values.
     * 
     * @return Configuration of this Configurable.  
     */
    public Configuration getConfiguration()
    throws ConfigurationException;
    
    /**
     * @return Name of this Configurable.  Every Configurable must have
     * a name.  It needs it at least for finding its own
     * {@link Configuration} in a {@link Registry}.  Name is immutable.
     * Must be passed on Construction of the Configurable or else,
     * if Configurable is a Singleton, Configurable has a hardcoded
     * name.
     */
    public String getName();
}
