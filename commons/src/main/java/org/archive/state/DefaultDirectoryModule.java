/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * DirectoryModule.java
 *
 * Created on Mar 16, 2007
 *
 * $Id:$
 */
package org.archive.state;


import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.archive.settings.CheckpointRecovery;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;


/**
 * @author pjack
 *
 */
public class DefaultDirectoryModule
implements DirectoryModule, Serializable, Initializable {


    private static final long serialVersionUID = 1L;

    
    @Immutable
    final public static Key<DirectoryModule> PARENT = 
        Key.make(DirectoryModule.class, null);

    @Immutable
    final public static Key<String> DIRECTORY = Key.make(".");
    
    
    static {
        KeyManager.addKeys(DefaultDirectoryModule.class);
    }
    
    private File directory;
    private DirectoryModule parent;
    
    
    public DefaultDirectoryModule() {
    }
    
    
    public File getDirectory() {
        return directory;
    }
    
    
    public String toRelativePath(String path) {
        return toRelativePath(directory, path);
    }
    
    
    public String toAbsolutePath(String path) {
        return toAbsolutePath(directory, path);
    }
    

    public void initialTasks(StateProvider provider) {
        parent = provider.get(this, PARENT);
        String path = provider.get(this, DIRECTORY);
        if (parent != null) {
            path = parent.toAbsolutePath(path);
        }
        directory = new File(path);
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream input) 
    throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (input instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)input;
            String abs = directory.getAbsolutePath();
            abs = cr.translatePath(abs);
            this.directory = new File(abs);
            if (parent != null) {
                abs = parent.toRelativePath(abs);
            }
            cr.setState(this, DIRECTORY, abs);
        }
    }

    
    public static String toAbsolutePath(File dir, String path) {
        File test = new File(path);
        if (!test.isAbsolute()) {
            return new File(dir, path).getAbsolutePath();
        }
        return path;
    }


    public static String toRelativePath(File dir, String path) {
        File test = new File(path);
        if (!test.isAbsolute()) {
            return path;
        }
        String dirAbs = dir.getAbsolutePath();
        if (path.startsWith(dirAbs)) {
            return path.substring(dirAbs.length() + 1);
        }
        return path;
    }

}
