/* ARCTest
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
package org.archive.io.arc;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;


/**
 * Base class for ARC*Test files.
 * 
 * This class sets up where we write temporary files and tests for its 
 * writeableness.
 * 
 * @author stack
 */
public class ARCTest
    extends TestCase
{
    /**
     * Name of the system property that holds pointer to tmp directory into
     * which we can safely write files.
     * 
     * This property is used by unit testing code.
     */
    private static final String TEST_TMP_SYSTEM_PROPERTY_NAME = "testtmpdir";
    
    /**
     * Default test tmp.
     * 
     * We'll write test ARC files in here if we can't find a 'testtmpdir' 
     * system property.
     * 
     * 
     */
    private static final String DEFAULT_TEST_TMP_DIR = File.separator + "tmp";
    
    /**
     * Directory to write temporary files to.
     */
    protected File tmpDir = null;
    
    
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
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testTmpDir()
        throws IOException
    {
        assertTrue(this.tmpDir.exists());
        assertTrue(this.tmpDir.canWrite());
    } 
}
