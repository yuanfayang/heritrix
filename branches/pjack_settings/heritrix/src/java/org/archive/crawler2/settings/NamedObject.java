package org.archive.crawler2.settings;

import java.util.List;


/**
 * An object with a name.  
 * 
 * @author pjack
 */
public class NamedObject<T> {


    /** The name. */
    private String name;
    
    /** The object. */
    private T object;


    /**
     * Constructor.
     * 
     * @param name    the name for the object
     * @param object  the object being named
     */
    public NamedObject(String name, T object) {
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
    public T getObject() {
        return object;
    }


    public static Object getByName(List<NamedObject> list, String name) {
        for (NamedObject no: list) {
            if (no.getName().equals(name)) {
                return no.getObject();
            }
        }
        return null;
    }
}
