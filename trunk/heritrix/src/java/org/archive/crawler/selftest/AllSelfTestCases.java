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
package org.archive.crawler.selftest;

import java.io.File;

import org.archive.crawler.admin.CrawlJob;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All registered heritrix selftests. 
 * 
 * @author stack
 * @version $Id$
 */
public class AllSelfTestCases
{
    /**
     * Run all of the selftest suite.
     * 
     * Each unit test to run as part of selftest needs to be added here.
     * 
     * @param selftestURL Base url to selftest webapp.
     * @param job The completed selftest job.
     * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.  
     * @param htdocs Expanded webapp directory location.
     * 
     * @return Suite of all selftests.
     */
    public static Test suite(final String selftestURL, final CrawlJob job,
            final File jobDir, final File htdocs)
    {
        TestSuite suite =
            new TestSuite("Test for org.archive.crawler.selftest");
        //$JUnit-BEGIN$
        // Add mention of self tests here for them to be run as part of the
        // general integration self test.
        suite.addTestSuite(BackgroundImageExtractionSelfTestCase.class);
        suite.addTestSuite(FramesSelfTestCase.class);
        //$JUnit-END$
        
        // Return an anonymous instance of TestSetup that does the one-time
        // set up of SelfTestCase base class installing required test
        // parameters.
        return new TestSetup(suite)
            {
                protected void setUp() throws Exception
                {
                    SelfTestCase.initialize(selftestURL, job, jobDir, htdocs);
                }
            };
    }
}
