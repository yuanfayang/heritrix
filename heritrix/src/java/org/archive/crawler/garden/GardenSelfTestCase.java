/* GardenTestCase
 * 
 * Created on Feb 4, 2004
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
package org.archive.crawler.garden;

import java.io.File;
import java.io.IOException;

import org.archive.io.arc.ARCReader;
import org.archive.util.FileUtils;

import junit.framework.TestCase;


/**
 * Base class for integrated selftest unit tests.
 * 
 * Has utility for integrated selftest such as location of selftest generated
 * arc file.
 * 
 * @author stack
 * @version $Id$
 */
public class GardenSelfTestCase extends TestCase
{
    /**
     * Suffix for selftest classes.
     */
    protected static final String SELFTEST = "SelfTest";
    
    private static File jobDir = null;
    private static String jobName = null;
    private static File arcFile = null;
    private static boolean initialized = false;
    private static String prefix = null;
    private static String selftestURL = null;
    
    /**
     * The selftest webapp directory.
     */
    private static File webappDir = null;
    
    /**
     * A reference to an ARCReader on which the validate method has been called.
     * Can be used to walk the metadata.
     * 
     * @see org.archive.io.arc.ARCReader#validate()
     */
    private static ARCReader readReader = null;


    public GardenSelfTestCase()
    {
        super();
    }

    public GardenSelfTestCase(String testName)
    {
        super(testName);
    }
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        if (!initialized)
        {
            throw new Exception("GardenSelfTestCase.initialize() not called" +
                " before running of first test.");
        }
        super.setUp();
    }
    
    /**
     * Static initializer.
     * 
     * Must be called before instantiation of any tests based off this class.
     * 
     * @param selftestURL URL to selftest webapp.
     * @param webappDir Expanded webapp directory location.
     * @param jobDir Directory where selftest resides.
     * @param jobName Name of the selftest job.
     * @param arcDir Directory wherein to find selftest arc.
     * @param prefix Arc file prefix.
     * 
     * @throws IOException if nonexistent directories passed.
     */
    public static synchronized void initialize(String selftestURL,
        File webappDir, File jobDir, String jobName, File arcDir, String prefix)
        throws IOException
    {
        if (selftestURL == null || selftestURL.length() <= 0)
        {
            throw new IOException("SelftestURL not set");
        }    
        GardenSelfTestCase.selftestURL =
            selftestURL.endsWith("/")? selftestURL: selftestURL + "/";
        
        if (webappDir == null || !webappDir.exists())
        {
            throw new IOException("WebappDir not set");
        }
        GardenSelfTestCase.webappDir = webappDir;
            
        if (jobDir == null || !jobDir.exists())
        {
            throw new IOException("Jobdir not set");
        }
        GardenSelfTestCase.jobDir = jobDir;
        
        if (arcDir == null || !arcDir.exists())
        {
            throw new IOException("ArcDir not set");
        }
        
        if (jobName == null || jobName.length() <= 0)
        {
            throw new IOException("JobName not set");
        }    
        GardenSelfTestCase.jobName = jobName;
        
        if (prefix == null || prefix.length() <= 0)
        {
            throw new IOException("Prefix not set");
        }    
        GardenSelfTestCase.prefix = prefix;
        
        // Find the arc file in the arcDir.  Should only be one.  Then make
        // an instance of ARCReader and call the validate on it.
        File [] arcs = FileUtils.getFilesWithPrefix(arcDir, prefix);
        if (arcs.length != 1)
        {
            throw new IOException("Expected one only arc file.  Found" +
                " instead " + Integer.toString(arcs.length) + " files.");
        }
        arcFile = arcs[0];
        readReader = new ARCReader(arcFile);
        readReader.validate();
        
        GardenSelfTestCase.initialized = true;
    }
    
    /**
     * @return Returns the arcDir.
     */
    protected static File getArcFile()
    {
        return arcFile;
    }

    /**
     * @return Returns the jobDir.
     */
    protected static File getJobDir()
    {
        return jobDir;
    }

    /**
     * @return Returns the jobName.
     */
    protected static String getJobName()
    {
        return jobName;
    }
    
    /**
     * Returns the selftest ARCReader.
     * 
     * The returned ARCReader has been validated.  Use it to get at metadata.
     * 
     * @return Returns the readReader, an ARCReader that has been validated.
     */
    protected static ARCReader getReadReader()
    {
        return readReader;
    }
    
    /**
     * @return Returns the selftestURL.  URL returned is guaranteed to have
     * a trailing '/'.
     */
    public static String getSelftestURL()
    {
        return selftestURL;
    }
    
    /**
     * Calculates test name by stripping SelfTest from current class name.
     * 
     * @return The name of the test.
     */
    public String getTestName()
    {
        String classname = getClass().getName();
        int selftestIndex = classname.indexOf(SELFTEST);
        assertTrue("Class name ends with SelfTest", selftestIndex > 0);
        int lastDotIndex = classname.lastIndexOf('.');
        assertTrue("Package dot in unexpected location",
            lastDotIndex + 1 < classname.length() && lastDotIndex > 0);
        return classname.substring(lastDotIndex + 1, selftestIndex);
    }

    /**
     * @return Returns the selftest webappDir.
     */
    public static File getWebappDir()
    {
        return webappDir;
    }
}
