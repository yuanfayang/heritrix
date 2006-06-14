package org.archive.configuration;

import javax.management.ObjectName;

public class StoreElement {
    private final Configuration configuration;
    private final ObjectName objectName;
    
    private StoreElement() {
        this(null, null);
    }
    
    public StoreElement(final Configuration c,
            final ObjectName on) {
        this.configuration = c;
        this.objectName = on;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ObjectName getObjectName() {
        return objectName;
    }
}
