/**
 * 
 */
package org.archive.state;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.archive.settings.CheckpointRecovery;

/**
 * @author pjack
 *
 */
public class DefaultPathContext implements PathContext, Serializable {
    
    private static final long serialVersionUID = 1L;

    private File baseDir;
    private Map<String,String> variables;
    
    public DefaultPathContext(File baseDir) {
        this.baseDir = baseDir;
        this.variables = new HashMap<String,String>();
    }

    public File getBaseDir() {
        return baseDir;
    }

    public Map<String, String> getPathVariables() {
        return variables;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream inp) 
    throws IOException, ClassNotFoundException {
        inp.defaultReadObject();
        if (inp instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)inp;
            this.baseDir = new File(cr.translatePath(baseDir.getAbsolutePath()));
        }
    }

}
