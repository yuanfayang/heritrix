/**
 * 
 */
package org.archive.state;


import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;


/**
 * An abstract pathname.  This is similar to {@link java.io.File}, but is
 * needed to provide custom serialization for checkpointing purposes.  Also
 * this class provides variable interpolation in paths.
 * 
 * @author pjack
 */
public class Path implements Serializable {


    /**
     * An empty path.
     */
    final public static Path EMPTY = new Path("");
    

    /** 
     * For serialization.
     */
    private static final long serialVersionUID = 1L;


    /**
     * The path.  May contain variables in the form ${x}.
     */
    private String path;
    
    /**
     * The file resolved from the path.  Will never contain variables.  May
     * be relative or absolute depending on whether the path string was.
     */
    private File file;

    /**
     * The context used to determine the base directory that paths are 
     * relative to.  Also provides values for variable interpolation.
     */
    private PathContext context;


    /**
     * Constructs a new path.
     * 
     * @param path   The path.
     */
    public Path(String path) {
        this.path = path;
        this.file = new File(path);
    }

    
    /**
     * Constructs a new path that can contain variables.
     * 
     * @param context  the path context that provides the base directory for
     *    relative paths and variable values
     * @param path   the path string; may contain variables in the form ${x}
     */
    public Path(PathContext context, String path) {
        this.context = context;
        this.path = path;
        setFromContext(path);        
    }

    
    private void setFromContext(String p) {
        if (context != null) {
            for (Map.Entry<String,String> var: context.getPathVariables().entrySet()) {
                String token = "${" + var.getKey() + "}";
                p = p.replace(token, var.getValue());
            }
            File f = new File(p);
            if (!f.isAbsolute()) {
                f = new File(context.getBaseDir(), p);
            }
            this.file = f;        
        } else {
            this.file = new File(p);
        }
    }

    
    /**
     * Returns true if the path is empty.
     * 
     * @return  true if the path is empty
     */
    public boolean isEmpty() {
        return path.equals("");
    }


    /**
     * Returns the string used to construct the path.  May contain variable
     * references in the form of ${x}.
     */
    public String toString() {
        return path;
    }
    
    
    /**
     * Returns the resolved file for the path.  Will not contain any variable
     * references.
     * 
     * @return   the resolved file.
     */
    public File toFile() {
        return file;
    }

    
    private void readObject(ObjectInputStream input) 
    throws IOException,  ClassNotFoundException {
        input.defaultReadObject();
        setFromContext(path);
    }
}
