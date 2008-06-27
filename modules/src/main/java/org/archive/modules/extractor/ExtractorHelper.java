package org.archive.modules.extractor;

import java.io.Serializable;

import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;

public class ExtractorHelper implements HasKeyedProperties, Serializable {
    private static final long serialVersionUID = 1L;
    
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
