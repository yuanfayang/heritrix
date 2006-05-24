package org.archive.configuration;

import javax.management.DynamicMBean;


public interface Configurable {
    public void configure(final Registry r) throws ConfigurationException;
}
