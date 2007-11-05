
package org.archive.settings.path;


/**
 * Thrown by the PathValidator to indicate an invalid path.  For instance, 
 * this exception will be thrown to indicate a missing root object, an 
 * out-of-bounds index, an invalid key field name, and so on.
 * 
 * @author pjack
 */
public class InvalidPathException extends RuntimeException {

    /**
     * For object serialization.
     */
    private static final long serialVersionUID = 1L;

    
    /**
     * Constructor.
     */
    public InvalidPathException() {
        super();
    }
    

    /**
     * Constructor.
     * 
     * @param msg   a message describing the invalid path
     */
    public InvalidPathException(String msg) {
        super(msg);
    }
    
    
    /**
     * Constructor.
     * 
     * @param e  the cause of this exception
     */
    public InvalidPathException(Throwable e) {
        super(e);
    }
}
