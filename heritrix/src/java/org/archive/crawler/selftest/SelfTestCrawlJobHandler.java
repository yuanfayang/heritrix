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

import java.io.File;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestResult;

import org.archive.crawler.Heritrix;
import org.archive.crawler.SimpleHttpServer;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.basic.ARCWriterProcessor;
import org.archive.crawler.datamodel.settings.ComplexType;

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
    

    /* (non-Javadoc)
     * @see org.archive.crawler.admin.CrawlJobHandler#startNextJob()
     */
    protected void startNextJob()
    {
        super.startNextJob();
        logger.info("Selftest started.");
    }

    public void crawlEnded(String sExitMessage)
    {
        // Output test results to stdout.
        logger.info("Running selftest postcrawl analysis unit tests.");  
        TestResult result = null;
        
        try
        {
            super.crawlEnded(sExitMessage);
           
            // At crawlEnded time, there is no current job.  Get the selftest
            // job by pulling from the completedCrawlJobs queue.
            Vector completedCrawlJobs = getCompletedJobs();
            if (completedCrawlJobs != null && completedCrawlJobs.size() > 0)
            {
                CrawlJob job = (CrawlJob)completedCrawlJobs.lastElement();
                File jobDir = getJobdir(job);
                String jobName = job.getJobName();
                ComplexType arcWriterProcessor =
                    (ComplexType)job.getSettingsHandler().getOrder().
                        getProcessors().getAttribute("Archiver");
                String arcDirStr = (String)arcWriterProcessor.
                    getAttribute(ARCWriterProcessor.ATTR_PATH);
                String prefix = (String)arcWriterProcessor.
                    getAttribute(ARCWriterProcessor.ATTR_PREFIX);
                // If the path does not begin w/ File.separator, assume arcs
                // are in the job directory.
                File arcDir = (arcDirStr.startsWith(File.separator))?
                    new File(arcDirStr):
                    new File(new File(jobDir, jobName), arcDirStr);
                // Get the expanded webapp dir from the server.
                File webappDir =
                    Heritrix.getHttpServer().getWebappPath(SELFTEST_WEBAPP);
                Test test =
                    AllSelfTestCases.suite(Heritrix.getSelftestURL(),
                        webappDir, jobDir, jobName, arcDir, prefix);
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
            SimpleHttpServer server = Heritrix.getHttpServer();
            if (server != null)
            {
                try
                {
                    server.stopServer();
                }
                catch(Exception e)
                {
                    logger.info("Failed to shutdown web server: " +
                        e.getMessage());
                }
            }
        }
    }
}
