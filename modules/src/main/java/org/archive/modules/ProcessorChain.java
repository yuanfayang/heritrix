package org.archive.modules;

import java.util.Iterator;
import java.util.List;

import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;
import org.springframework.context.Lifecycle;

/**
 * Collection of Processors to run.
 * 
 * Not just a list on another bean so that:
 *  - chain is a prominent standalone part of configuration
 *  - Lifecycle events may be propagated to members defined 
 *  as inner beans
 *  - future override capability may allow inserts at any place in
 *  order, not just end (assuming TBD specialized iterator)
 *  
 */
public class ProcessorChain 
implements Iterable<Processor>, HasKeyedProperties, Lifecycle {
    
    KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    public int size() {
        return getProcessors().size();
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

    boolean isRunning = false; 
    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        for(Processor p : getProcessors()) {
            // relies on each Processor's start() being ok to call if 
            // already running, which is part of the Lifecycle contract
            p.start(); 
        }
        isRunning = true; 
    }

    public void stop() {
        for(Processor p : getProcessors()) {
            // relies on each Processor's stop() being ok to call if 
            // not running, which is part of the Lifecycle contract
            p.stop(); 
        }
        isRunning = false; 
    }
}
