package org.archive.configuration.registry;

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;

public class P implements Configurable {
    private final String name;
    
    public P(final String n) {
        super();
        this.name = n;
    }

    public String getName() {
        return this.name;
    }

    public Configurable initialize(Registry r) throws ConfigurationException {
        return this;
    }
    
    public Configuration getConfiguration() throws ConfigurationException {
        return new Configuration("Test P Processor") {
            private static final long serialVersionUID = 4340724584065204781L;
        };
    }
}
