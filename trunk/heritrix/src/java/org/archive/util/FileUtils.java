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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;


/** Utility methods for manipulating files and directories.
 *
 * @author John Erik Halse
 */
public class FileUtils
{
    /** Constructor made private because all methods of this class are static.
     *
     */
    private FileUtils() {
        super();
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
            copyFile(src, dest);
        }
    }

    /**
     * Copy the src file to the destination.
     * 
     * @param src
     * @param dest
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void copyFile(File src, File dest) throws FileNotFoundException, IOException {
		copyFile(src, dest, -1);
	}

	/**
     * Copy up to extent bytes of the source file to the destination
     * 
	 * @param src
	 * @param dest
	 * @param extent
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(File src, File dest, long extent) throws FileNotFoundException, IOException {
		if(dest.exists()) {
            dest.delete();
        } 
        // get channels
		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(dest);
		FileChannel fcin = fis.getChannel();
		FileChannel fcout = fos.getChannel();
		
        if (extent<0) {
         extent = fcin.size();   
        }
        
		// do the file copy
		long trans = fcin.transferTo(0, extent, fcout);
        if(trans < extent ) {
            System.err.println("FileUtils.copyFile() only transferred "+trans+" of "+extent);
        }
        
		// finish up
		fcin.close();
		fcout.close();
		fis.close();
		fos.close();
	}

	/** Deletes all files and subdirectories under dir.
     * @param dir
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

    

    /**
     * Utility method to read an entire file as a String.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public static String readFileAsString(File file) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;
        BufferedReader br
            = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        line = br.readLine();
        while(line!=null) {
            sb.append(line);
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }
    /**
     * Get a list of all files in directory that have passed prefix.
     *
     * @param dir Dir to look in.
     * @param prefix Basename of files to look for. Compare is case insensitive.
     *
     * @return List of files in dir that start w/ passed basename.
     */
    public static File [] getFilesWithPrefix(File dir, final String prefix)
    {
        FileFilter prefixFilter = new FileFilter()
            {
                public boolean accept(File pathname)
                {
                    return pathname.getName().toLowerCase().
                        startsWith(prefix.toLowerCase());
                }
            };
        return dir.listFiles(prefixFilter);
    }

    /** Get a @link java.io.FileFilter that filters files based on a regular
     * expression.
     *
     * @param regexp the regular expression the files must match.
     * @return the newly created filter.
     */
    public static FileFilter getRegexpFileFilter(String regexp) {
        // Inner class defining the RegexpFileFilter
        class RegexpFileFilter implements FileFilter {
            Pattern pattern;

            protected RegexpFileFilter(String regexp) {
                pattern = Pattern.compile(regexp);
            }

            public boolean accept(File pathname) {
                return pattern.matcher(pathname.getName()).matches();
            }
        }

        return new RegexpFileFilter(regexp);
    }
}
