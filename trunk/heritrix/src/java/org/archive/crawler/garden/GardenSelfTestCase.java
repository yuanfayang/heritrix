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
    private static File jobDir = null;
    private static String jobName = null;
    private static File arcDir = null;
    private static boolean initialized = false;
    private static String prefix = null;
    
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
     * @param jobDir Directory where selftest resides.
     * @param jobName Name of the selftest job.
     * @param arcDir Directory wherein to find selftest arc.
     * @param prefix Arc file prefix.
     * 
     * @throws IOException if nonexistent directories passed.
     */
    public static synchronized void initialize(File jobDir, String jobName, 
            File arcDir, String prefix)
        throws IOException
    {
        if (jobDir == null || !jobDir.exists())
        {
            throw new IOException("Jobdir not set");
        }
        GardenSelfTestCase.jobDir = jobDir;
        
        if (arcDir == null || !arcDir.exists())
        {
            throw new IOException("ArcDir not set");
        }
        GardenSelfTestCase.arcDir = arcDir;
        
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
        
        GardenSelfTestCase.initialized = true;
    }
    
    /**
     * @return Returns the arcDir.
     */
    protected static File getArcDir()
    {
        return arcDir;
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
}
