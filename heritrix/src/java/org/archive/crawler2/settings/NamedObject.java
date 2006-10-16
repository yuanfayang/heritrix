package org.archive.crawler2.settings;


/**
 * An object with a name.  
 * 
 * @author pjack
 */
public class NamedObject {


    /** The name. */
    private String name;
    
    /** The object. */
    private Object object;


    /**
     * Constructor.
     * 
     * @param name    the name for the object
     * @param object  the object being named
     */
    public NamedObject(String name, Object object) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        if (object == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.object = object;
    }
    
    
    /**
     * Returns the name.
     * 
     * @return  the name
     */
    public String getName() {
        return name;
    }
    
    
    /**
     * Returns the object.
     * 
     * @return  the object
     */
    public Object getObject() {
        return object;
    }


}
