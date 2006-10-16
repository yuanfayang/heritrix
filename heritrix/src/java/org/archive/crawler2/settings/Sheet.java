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

}
