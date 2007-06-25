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
 * Created on Mar 20, 2007
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

/**
 * @author pjack
 *
 */
public class FileModule implements Initializable, Serializable, Module {


    private static final long serialVersionUID = 1L;


    @Immutable
    final public static Key<FileModule> PARENT = 
        Key.make(FileModule.class, null);

    
    @Immutable
    final public static Key<String> PATH = Key.make(".");

    
    private FileModule parent;
    private File file;

    
    static {
        KeyManager.addKeys(FileModule.class);
    }

    /**
     * Constructor.
     */
    public FileModule() {        
    }
    
    
    public void initialTasks(StateProvider sp) {
        parent = sp.get(this, PARENT);
        String path = sp.get(this, PATH);
        if (parent != null) {
            path = parent.toAbsolutePath(path);
        }
        this.file = new File(path);
    }
    
    
    
    public File getFile() {
        return file;
    }
        
    
    /**
     * Makes a possibly relative path absolute.  If the given path is 
     * relative, then the returned string will be an absolute path 
     * relative to getDirectory().
     * 
     * <p>If the given path is already absolute, then it is returned unchanged.
     * 
     * @param path   the path to make absolute
     * @return   the absolute path
     */
    public String toAbsolutePath(String path) {
        return toAbsolutePath(file, path);
    }

    
    /**
     * Makes a possibly absolute path relative.  If the given path is 
     * relative, then it is returned unchanged.  Otherwise, if the given 
     * absolute path represents a subdirectory of getDirectory, then 
     * the subdirectory path is returned as a relative path.
     * 
     * Eg, if getDirectory() returns <code>/foo/bar</code>, then:
     * 
     * <ul>
     * <li><code>toRelativePath("/foo/bar/baz/snafu")</code> should return
     * <codeE>baz/snafu</code>.</li>
     * <li><code>toRelativePath("/fnord/x")</code> should return 
     * <code>/fnord/x</code>.</li>
     * <li><code>toRelativePath("a/b/c")</code> should return <code>a/b/c</code>.
     * </ul>
     * 
     * @param path
     * @return
     */
    String toRelativePath(String path) {
        return toRelativePath(file, path);
    }

    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream input) 
    throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (input instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)input;
            String abs = file.getAbsolutePath();
            abs = cr.translatePath(abs);
            this.file = new File(abs);
            if (parent != null) {
                abs = parent.toRelativePath(abs);
            }
            cr.setState(this, PATH, abs);
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
