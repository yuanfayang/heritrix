/* TmpDirTestCase
 * 
 * $Id$
 * 
 * Created on Dec 31, 2003.
 * 
 * Copyright (C) 2003 Internet Archive.
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
import java.io.FileFilter;
import java.io.IOException;

import junit.framework.TestCase;


/**
 * Base class for TestCases that want access to a tmp dir for the writing 
 * of files.
 * 
 * @author stack
 */
public class TmpDirTestCase
    extends TestCase
    implements FileFilter
{
    /**
     * Name of the system property that holds pointer to tmp directory into
     * which we can safely write files.
     */
    private static final String TEST_TMP_SYSTEM_PROPERTY_NAME = "testtmpdir";
    
    /**
     * Default test tmp.
     */
    private static final String DEFAULT_TEST_TMP_DIR = File.separator + "tmp" +
        File.separator + "heritrix-junit-tests";
    
    /**
     * Directory to write temporary files to.
     */
    private File tmpDir = null;
    
    /**
     * Store in here the file prefix for filefiltering done by some of the 
     * unit tests.
     */
    private String fileFilterPrefix = null;
    

    public TmpDirTestCase()
    {
        super();
    }

    public TmpDirTestCase(String testName)
    {
        super(testName);
    }
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        String tmpDirStr = System.getProperty(TEST_TMP_SYSTEM_PROPERTY_NAME);
        tmpDirStr = (tmpDirStr == null)? DEFAULT_TEST_TMP_DIR: tmpDirStr;
        tmpDir = new File(tmpDirStr);
        if (!tmpDir.exists())
        {
            tmpDir.mkdirs();
        }
        
        if (!tmpDir.canWrite())
        {
            throw new IOException(tmpDir.getAbsolutePath() +
                 " is unwriteable.");
        }
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    /**
     * @return Returns the tmpDir.
     */
    public File getTmpDir()
    {
        return tmpDir;
    }
      
    /**
     * Delete any files left over from previous run.
     * 
     * @param basename Base name of files we're to clean up.
     */
    public void cleanUpOldFiles(String basename)
    {
        cleanUpOldFiles(getTmpDir(), basename);
    }
    
    /**
     * Delete any files left over from previous run.
     * 
     * @param basename Base name of files we're to clean up.
     * @param basedir Directory to start cleaning in.
     */
    public void cleanUpOldFiles(File basedir, String basename)
    {
        File [] files = getListOfFiles(basedir, basename);
        for (int i = 0; i < files.length; i++)
        {
            files[i].delete();
        }
    }
    
    /**
     * @param basename Basename to look for.
     * @return List of files in dir that start w/ passed basename.
     */
    protected File [] getListOfFiles(File dir, String basename)
    {
        this.fileFilterPrefix = basename;
        return dir.listFiles(this);
    }
    
    /**
     * Return files that begin w/ fileFilterPrefix.
     * 
     * Implementation of the FileFilter.accept method.
     * 
     * @param pathname File to filter.
     * @return True if we are to include the passed file.
     */
    public boolean accept(File pathname)
    {
        return pathname.getName().startsWith(this.fileFilterPrefix);
    }
}
