/* SelfTestCase
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
import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.basic.ARCWriterProcessor;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.settings.ComplexType;
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
public class SelfTestCase extends TestCase
{
    /**
     * Suffix for selftest classes.
     */
    protected static final String SELFTEST = "SelfTest";
    
    private static CrawlJob job = null;
    private static File jobDir = null;
    private static File arcFile = null;
    private static String selftestURL = null;
    private static CrawlOrder crawlOrder = null;
    
    /**
     * Directory logs are kept in.
     */
    private static File logsDir = null;
    
    /**
     * Has the static initializer for this class been run.
     */
    private static boolean initialized = false;
    
    /**
     * The selftest webapp htdocs directory.
     */
    private static File htdocs = null;
    
    /**
     * A reference to an ARCReader on which the validate method has been called.
     * Can be used to walk the metadata.
     * 
     * @see org.archive.io.arc.ARCReader#validate()
     */
    private static ARCReader readReader = null;


    public SelfTestCase()
    {
        super();
    }

    public SelfTestCase(String testName)
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
            throw new Exception("SelfTestCase.initialize() not called" +
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
     * @param job The selftest crawl job.
      * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.  
     * @param htdocs Expanded webapp directory location.
     * 
     * @throws IOException if nonexistent directories passed.
     */
    public static synchronized void initialize(final String selftestURL,
            final CrawlJob job, final File jobDir, final File htdocs)
        throws IOException, AttributeNotFoundException, MBeanException,
            ReflectionException
    {
        if (selftestURL == null || selftestURL.length() <= 0)
        {
            throw new IOException("SelftestURL not set");
        }    
        SelfTestCase.selftestURL =
            selftestURL.endsWith("/")? selftestURL: selftestURL + "/";
        
        if (job == null)
        {
        	throw new NullPointerException("Passed job is null");
        }
        SelfTestCase.job = job;
        
        if (jobDir == null || !jobDir.exists())
        {
            throw new IOException("Jobdir null or does not exist");
        }
        SelfTestCase.jobDir = jobDir;
        
        SelfTestCase.crawlOrder = job.getSettingsHandler().getOrder();
        if (crawlOrder != null)
        {
            throw new NullPointerException("CrawlOrder is null");
        }
        
        // Calculate the logs directory.  If diskPath is not absolute, then logs
        // are in the jobs dir under the diskPath subdirectory.
        String diskPath = (String)crawlOrder.
            getAttribute(null, CrawlOrder.ATTR_DISK_PATH);
        if (diskPath != null && diskPath.length() > 0 &&
                diskPath.startsWith(File.separator))
        {
            SelfTestCase.logsDir = new File(diskPath);
        }
        else
        {
            SelfTestCase.logsDir =
                (diskPath != null && diskPath.length() > 0)?
                    new File(jobDir, diskPath): jobDir;
        }

        if (SelfTestCase.logsDir == null || !SelfTestCase.logsDir.exists())
        {
            throw new IOException("Logs directory not found");
        }
        
        // Calculate the arcfile name.  Find it in the arcDir.  Should only be one.
        // Then make an instance of ARCReader and call the validate on it.
        ComplexType arcWriterProcessor =
            (ComplexType)crawlOrder.getProcessors().
                getAttribute("Archiver");
        String arcDirStr = (String)arcWriterProcessor.
            getAttribute(ARCWriterProcessor.ATTR_PATH);
        File arcDir = null;
        if (arcDirStr != null && arcDirStr.length() > 0 &&
                arcDirStr.startsWith(File.separator))
        {
        	arcDir = new File(arcDirStr);
        }
        else
        {
            arcDir = (arcDirStr != null && arcDirStr.length() > 0)?
                new File(SelfTestCase.logsDir, arcDirStr): SelfTestCase.logsDir;
        }
 
        if (arcDir == null || !arcDir.exists())
        {
            throw new IOException("ArcDir not found");
        }
        
        String prefix = (String)arcWriterProcessor.
            getAttribute(ARCWriterProcessor.ATTR_PREFIX);
        if (prefix == null || prefix.length() <= 0)
        {
            throw new IOException("Prefix not set");
        }    
        
        File [] arcs = FileUtils.getFilesWithPrefix(arcDir, prefix);
        if (arcs.length != 1)
        {
            throw new IOException("Expected one only arc file.  Found" +
                " instead " + Integer.toString(arcs.length) + " files.");
        }
        SelfTestCase.arcFile = arcs[0];
        SelfTestCase.readReader = new ARCReader(arcFile);
        SelfTestCase.readReader.validate();
        
        if (htdocs == null || !htdocs.exists())
        {
            throw new IOException("WebappDir htdocs not set");
        }
        SelfTestCase.htdocs = htdocs;
        
        SelfTestCase.initialized = true;
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
     * @return Return the directory w/ logs in it.
     */
    protected static File getLogsDir()
    {
        return SelfTestCase.logsDir;
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
     * @return Returns the selftestURL.
     */
    public static String getSelftestURL()
    {
        return selftestURL;
    }
    
    /**
     * @return Returns the selftestURL.  URL returned is guaranteed to have
     * a trailing '/'.
     */
    public static String getSelftestURLWithTrailingSlash()
    {
        return selftestURL.endsWith("/")? selftestURL: selftestURL + "/";
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
    public static File getHtdocs()
    {
        return SelfTestCase.htdocs;
    }
}
