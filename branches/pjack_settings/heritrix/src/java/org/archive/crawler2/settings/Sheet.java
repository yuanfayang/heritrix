package org.archive.crawler2.settings;


import org.archive.state.Key;


/**
 * A sheet of settings.  Sheets must be created via a sheet manager.
 * Concrete implementations are safe for use by concurrent threads.
 * 
 * @author pjack
 */
public abstract class Sheet {

    
    /**
     * The manager that created this sheet.
     */
    final private SheetManager manager;


    private String name;

    
    /**
     * Constructor.  Package-protected to ensure that only two subclasses
     * exist, SingleSheet and SheetBundle.
     * 
     * @param manager   the manager who created this sheet
     */
    Sheet(SheetManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException();
        }
        this.manager = manager;
    }
    
    
    /**
     * Returns the sheet manager who created this sheet.
     * 
     * @return   the sheet manager
     */
    public SheetManager getSheetManager() {
        return manager;
    }

    
    /**
     * Returns the unique name of this sheet.  The name may be changed via
     * the SheetManager.
     * 
     * @return  the unique name of this sheet
     */
    public String getName() {
        return name;
    }
    
    
    void setName(String name) {
        this.name = name;
    }

    /**
     * Looks up a value for a property.  
     * 
     * @param <T>
     * @param processor   the processor who needs the property value
     * @param key         a key (declared by the processor) that defines the
     *                    property to return
     * @return  either the value for that processor/Key combination, or null
     *    if this sheet does not include such a value
     */
    public abstract <T> T get(Object processor, Key<T> key);

    
    public abstract <T> Resolved<T> resolve(Object processor, Key<T> key);


    public <T> Resolved<T> resolveDefault(Object processor, Key<T> key) {
        SingleSheet defaults = getSheetManager().getDefault();
        T result = defaults.get(processor, key);
        if (result == null) {
            result = key.getDefaultValue();
        }
        return new Resolved<T>(defaults, processor, key, key.getDefaultValue());
    }
}
