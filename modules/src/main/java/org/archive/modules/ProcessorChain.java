package org.archive.modules;

import java.util.Iterator;
import java.util.List;

import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;

public class ProcessorChain implements Iterable<Processor>, HasKeyedProperties {
    
    KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    public int size() {
        // TODO Auto-generated method stub
        return -1;
    }

    public Iterator<Processor> iterator() {
        return getProcessors().iterator();
    }

    @SuppressWarnings("unchecked")
    public List<Processor> getProcessors() {
        return (List<Processor>) kp.get("processors");
    }
    public void setProcessors(List<Processor> processors) {
        kp.put("processors",processors);
    }
}
