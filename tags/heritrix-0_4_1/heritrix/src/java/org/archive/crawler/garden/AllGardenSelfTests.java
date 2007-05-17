/* AllTests
 * 
 * Created on Jan 29, 2004
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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All registered heritrix selftests. 
 * 
 * @author stack
 * @version $Id$
 */
public class AllGardenSelfTests
{
    /**
     * Run all of the selftest suite.
     * 
     * Each unit test to run as part of selftest needs to be added here.
     * 
     * @param selftestURL Base url to selftest garden.
     * @param webappDir Expanded webapp directory location.
     * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.  
     * @param jobName Name of job.  The logs are kept in jobdir/jobname.
     * @param arcDir Directory in which to find arc files.
     * @param prefix ARC file prefix.
     * 
     * @return Suite of all selftests.
     */
    public static Test suite(final String selftestURL, final File webappDir,
            final File jobDir, final String jobName, final File arcDir,
            final String prefix)
    {
        TestSuite suite = new TestSuite("Test for org.archive.crawler.garden");
        //$JUnit-BEGIN$
        // Add mention of self tests here for them to be run as part of the
        // general integration self test.
        suite.addTestSuite(BackgroundImageExtractionSelfTest.class);
        suite.addTestSuite(FramesSelfTest.class);
        //$JUnit-END$
        
        // Return an anonymous instance of TestSetup that does the one-time
        // set up of GardenSelfTestCase base class installing required test
        // parameters.
        return new TestSetup(suite)
            {
                protected void setUp() throws Exception
                {
                    GardenSelfTestCase.initialize(selftestURL, webappDir, 
                        jobDir, jobName, arcDir, prefix);
                }
            };
    }
 
    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.
            run(AllGardenSelfTests.suite(args[0], new File(args[1]),
                new File(args[2]), args[3], new File(args[4]), "IAH"));
    }
}