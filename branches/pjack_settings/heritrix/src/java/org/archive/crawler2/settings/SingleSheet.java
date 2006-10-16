package org.archive.crawler2.settings;

import java.util.HashMap;
import java.util.Map;

import org.archive.state.Key;


/**
 * A single sheet of settings.
 * 
 * @author pjack
 */
public class SingleSheet extends Sheet {
    

    /**
     * Maps processor/Key combo to value for that combo.
     */
    private Map<ObjectAndKey,Object> settings;


    /**
     * Constructor.
     * 
     * @param manager   the manager who created this sheet
     */
    SingleSheet(SheetManager manager) {
        super(manager);
        this.settings = new HashMap<ObjectAndKey,Object>();
    }
        
    
    @Override
    public <T> T get(Object target, Key<T> key) {
        ObjectAndKey<T> nk = new ObjectAndKey<T>(target, key);
        return key.getType().cast(settings.get(nk));
    }

    
    /**
     * Sets a property.
     * 
     * @param <T>         the type of the property to set
     * @param processor   the processor to set the property on
     * @param key         the property to set
     * @param value       the new value for that property, or null to remove
     *     the property from this sheet
     */
    public <T> void set(Object processor, Key<T> key, T value) {
        ObjectAndKey<T> nk = new ObjectAndKey<T>(processor, key);
        if (value == null) {
            settings.remove(nk);
        }
        settings.put(nk, value);
    }


}
