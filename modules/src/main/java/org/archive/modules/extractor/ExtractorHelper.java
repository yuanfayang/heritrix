package org.archive.modules.extractor;

import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;

public class ExtractorHelper implements HasKeyedProperties {
    KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    {
        setMaxOutlinks(6000);
    }
    public int getMaxOutlinks() {
        return (Integer) kp.get("maxOutlinks");
    }
    public void setMaxOutlinks(int max) {
        kp.put("maxOutlinks", max);
    }
}
