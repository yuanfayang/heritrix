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
import java.io.Serializable;

import org.archive.settings.CheckpointRecovery;

/**
 * Represents a file in a filesystem.  This module is analogous to the
 * {@link File} provided by the JDK, but it can be configured using the 
 * {@link org.archive.settings} system.  This class allows other modules to 
 * advertise configurable files and directories.
 * 
 * @author pjack
 */
public class FileModule 
implements Initializable, Serializable, Module {


    /** For serialization. */
    private static final long serialVersionUID = 1L;


    /**
     * The parent directory of this FileModule.  If {@link #PATH} is absolute,
     * then this setting is ignored.  Otherwise, {@link #PATH} specifies a 
     * path relative to {@link #PARENT}.
     * 
     * <p>May be null, in which case {@link #PATH} will be considered relative
     * to the JVM's current working directory.
     * 
     * <p>This Key has custom constraints that only allow 
     * <i>existing directories</i> as valid values.
     */
    @Immutable
    final public static Key<FileModule> PARENT = makeParent();


    /**
     * The path of this FileModule.  If relative, it will be relative to 
     * {@link #PARENT}, or relative to the JVM's current working directory
     * if {@link #PARENT} is null.
     */
    @Immutable
    final public static Key<String> PATH = Key.make(".");

    
    /**
     * The parent file module.
     */
    private FileModule parent;
    
    
    /**
     * The file represented by this FileModule.
     */
    private File file;

    
    /**
     * All modules must manually register their keys.
     */
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
    
    
    /**
     * Returns the file represented by this FileModule.  Will return null
     * unless {@link #initialTasks} has been invoked.
     * 
     * @return  the file represented by this FileModule
     */
    public File getFile() {
        return file;
    }
        
    
    /**
     * Makes a possibly relative path absolute.  If the given path is 
     * relative, then the returned string will be an absolute path 
     * relative to {@link #getFile}.
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
     * absolute path represents a subdirectory of {@link #getFile}, then 
     * the subdirectory path is returned as a relative path.
     * 
     * Eg, if {@link #getFile} is <code>/foo/bar</code>, then:
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
     * @return  the relative path
     */
    String toRelativePath(String path) {
        return toRelativePath(file, path);
    }

    

    /**
     * Custom deserialization for checkpointing purposes.  If the given 
     * input stream is {@link CheckpointRecovery}, then the deserialized
     * {@link #file} value is translated using that CheckpointRecovery,
     * and the (possibly modified) path is saved instead.  
     * 
     * <p>This allows an end user to checkpoint an application on one machine,
     * and to recover the checkpoint on a new machine with a possibly different
     * directory structure.  See {@link CheckpointRecovery} for more 
     * information. 
     * 
     * <p>If the given input stream is not a CheckpointRecovery, then nothing
     * special happens, and this class is deserialized as normal.
     * 
     * @param input   the input stream to restore this object's state from
     * @throws IOException   if an IO error occurs while reading
     * @throws ClassNotFoundException  if a class cannot be found (should
     *   never happen; the only required classes are part of the JDK)
     */
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

    

    /**
     * Makes a possibly relative path absolute.  If the given path is 
     * relative, then the returned string will be an absolute path 
     * relative to <code>dir</code>.
     * 
     * <p>If the given path is already absolute, then it is returned unchanged.
     * 
     * <p>For instance, if <code>dir</code> is <code>/foo/bar</code>, then:
     * 
     * <ul>
     * <li><code>toAbsolutePath(dir, "baz/snafu")</code> will return 
     * <code>/foo/bar/baz/snafu</code>.
     * <li><code>toAbsolutePath(dir, "/fnord/x")</code> will return
     * <code>/fnord/x</code>.
     * </ul>
     * 
     * @param dir    the parent directory for relative paths
     * @param path   the path to make absolute
     * @return   the absolute path
     */
    public static String toAbsolutePath(File dir, String path) {
        File test = new File(path);
        if (!test.isAbsolute()) {
            return new File(dir, path).getAbsolutePath();
        }
        return path;
    }


    /**
     * Makes a possibly absolute path relative.  If the given path is 
     * relative, then it is returned unchanged.  Otherwise, if the given 
     * absolute path represents a subdirectory of <code>dir</code>, then 
     * the subdirectory path is returned as a relative path.
     * 
     * Eg, if <code>dir</code> is <code>/foo/bar</code>, then:
     * 
     * <ul>
     * <li><code>toRelativePath("/foo/bar/baz/snafu")</code> should return
     * <codeE>baz/snafu</code>.</li>
     * <li><code>toRelativePath("/fnord/x")</code> should return 
     * <code>/fnord/x</code>.</li>
     * <li><code>toRelativePath("a/b/c")</code> should return <code>a/b/c</code>.
     * </ul>
     * 
     * @param dir    the parent directory of relative paths
     * @param path   the path to make relative
     * @return  the relative path
     */
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


    /**
     * Constructs the Key for the {@link #PARENT} field.  This method is
     * necessary because {@link #PARENT} has special constraints.
     * 
     * @return  the key for the {@link #PARENT} field.
     */
    private static Key<FileModule> makeParent() {
        KeyMaker<FileModule> r = KeyMaker.makeNull(FileModule.class);
        r.constraints.add(new FileModuleParentConstraint());
        return r.toKey();
    }

    
    /**
     * Constraint for the {@link #PARENT}.  Only allows null values, or 
     * FileModules that actually represent a directory and not an ordinary
     * file.
     * 
     * @author pjack
     */
    private static class FileModuleParentConstraint 
    implements Constraint<FileModule> {

        /** For serialization. */
        private static final long serialVersionUID = 1L;

        
        public boolean allowed(FileModule module) {
            // FIXME: This was never going to work, delete this class and
            // use standard key.
            return true;
        }
        
        public String description() {
            return "parent must be a directory"; // ???
        }

    }
    
}
