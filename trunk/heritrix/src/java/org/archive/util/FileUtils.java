/* FileUtils
 * 
 * $Id$
 * 
 * Created on Feb 2, 2004
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/** Utility methods for manipulating files and directories.
 * 
 * @author John Erik Halse
 */
public class FileUtils {

    /** Constructor made private because all methods of this class are static.
     * 
     */
    private FileUtils() {
    }

    /** Recursively copy all files from one directory to another.
     * 
     * @param src file or directory to copy from.
     * @param dest file or directory to copy to.
     * @throws IOException
     */
    public static void copyFiles(File src, File dest) throws IOException {
        if (!src.exists()) {
            return;
        }

        if (src.isDirectory()) {
            // Create destination directory
            dest.mkdirs();
            
            // Go trough the contents of the directory
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                File src1 = new File(src, list[i]);
                File dest1 = new File(dest, list[i]);
                copyFiles(src1 , dest1);
            }
            
        } else {

            // get channels
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dest);
            FileChannel fcin = fis.getChannel();
            FileChannel fcout = fos.getChannel();

            // do the file copy
            fcin.transferTo(0, fcin.size(), fcout);

            // finish up
            fcin.close();
            fcout.close();
            fis.close();
            fos.close();
        }
    }
    
    /** Deletes all files and subdirectories under dir.
     *  @param dir
     * @return true if all deletions were successful. If a deletion fails, the 
     *          method stops attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

}
