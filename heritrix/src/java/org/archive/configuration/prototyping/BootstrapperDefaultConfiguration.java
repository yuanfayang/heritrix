package org.archive.configuration.prototyping;

import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;

public class BootstrapperDefaultConfiguration extends Configuration {

    public BootstrapperDefaultConfiguration() throws ConfigurationException {
        super();
    }

    protected java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> addAttributes(java.util.List<javax.management.openmbean.OpenMBeanAttributeInfo> attributes)
    throws javax.management.openmbean.OpenDataException {
        attributes = super.addAttributes(attributes);
        attributes.add(new OpenMBeanAttributeInfoSupport("XXX",
                "Blah if true", SimpleType.BOOLEAN,
                true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
        return attributes;
    }
}
