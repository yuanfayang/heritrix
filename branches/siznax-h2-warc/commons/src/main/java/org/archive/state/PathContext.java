/**
 * 
 */
package org.archive.state;

import java.io.File;
import java.util.Map;

/**
 * A context for a path.  Provides the base directory for relative paths,
 * and variable values.
 * 
 * @author pjack
 */
public interface PathContext {


    /**
     * The base directory.
     */
    File getBaseDir();

    /**
     * Maps variable name to variable value.
     */
    Map<String,String> getPathVariables();
    
}
