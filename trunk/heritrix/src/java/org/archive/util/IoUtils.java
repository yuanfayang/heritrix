/* IoUtils
 * 
 * Created on Dec 8, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * I/O Utility methods.
 * @author stack
 * @version $Date$, $Revision$
 */
public class IoUtils {
    /**
     * @param file File to operate on.
     * @return Path suitable for use getting resources off the CLASSPATH
     * (CLASSPATH resources always use '/' as path separator, even on
     * windows).
     */
    public static String getClasspathPath(File file) {
        String path = file.getPath();
        if (File.separatorChar != '/') {
            // OK.  We're probably on a windows system. Strip
            // drive if its present and convert '\' to '/'.
            path = path.replace(File.separatorChar, '/');
            int index = path.indexOf(':');
            if (index > 0 && index < 3) {
                path.substring(index + 1);
            }
        }
        return path;
    }
    
    /**
     * Ensure writeable directory.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dir Directory to test for exitence and is writeable.
     *
     * @return The passed <code>dir</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(String dir)
    throws IOException {
        return ensureWriteableDirectory(new File(dir));
    }
    
    /**
     * Ensure writeable directories.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dirs List of Files to test.
     *
     * @return The passed <code>dirs</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static List ensureWriteableDirectory(List dirs)
    throws IOException {
        for (Iterator i = dirs.iterator(); i.hasNext();) {
             ensureWriteableDirectory((File)i.next());
        }
        return dirs;
    }

    /**
     * Ensure writeable directory.
     *
     * If doesn't exist, we attempt creation.
     *
     * @param dir Directory to test for exitence and is writeable.
     *
     * @return The passed <code>dir</code>.
     *
     * @exception IOException If passed directory does not exist and is not
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(File dir)
    throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            if (!dir.canWrite()) {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " not writeable.");
            } else if (!dir.isDirectory()) {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " is not a directory.");
            }
        }

        return dir;
    }
}
