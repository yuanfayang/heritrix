/* SelftestCrawlJobHandler
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
package org.archive.crawler.selftest;

import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestResult;

import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;


/**
 * An override to gain access to end-of-crawljob message.
 *
 *
 * @author stack
 * @version $Id$
 */

public class SelfTestCrawlJobHandler
    extends CrawlJobHandler
{
    /**
     * Name of the selftest webapp.
     */
    private static final String SELFTEST_WEBAPP = "selftest";



    private static Logger logger =
        Logger.getLogger("org.archive.crawler.admin.SelftestCrawlJobHandler");

    public SelfTestCrawlJobHandler()
    {
        super();
    }

    public void crawlEnded(String sExitMessage)
    {
        TestResult result = null;

        try
        {
            super.crawlEnded(sExitMessage);

            // At crawlEnded time, there is no current job.  Get the selftest
            // job by pulling from the completedCrawlJobs queue.
            Vector completedCrawlJobs = getCompletedJobs();
            if (completedCrawlJobs == null || completedCrawlJobs.size() <= 0)
            {
                logger.severe("Selftest job did not complete.");
            }
            else
            {
                CrawlJob job = (CrawlJob)completedCrawlJobs.lastElement();
                Test test = AllSelfTestCases.suite(Heritrix.getSelftestURL(),
                        job, getJobdir(job),
                        Heritrix.getHttpServer().
                            getWebappPath(SELFTEST_WEBAPP));
                result = junit.textui.TestRunner.run(test);
            }
        }

        catch (Exception e)
        {
            logger.info("Failed running selftest analysis: " + e.getMessage());
        }

        finally
        {
            logger.info((new Date()).toString() + " Selftest " +
                (result != null && result.wasSuccessful()? "PASSED": "FAILED"));
            Heritrix.shutdown(result.wasSuccessful()? 0: 1);
        }
    }
}

