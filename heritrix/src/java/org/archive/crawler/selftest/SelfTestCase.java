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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.settings.ComplexType;
import org.archive.crawler.writer.ARCWriterProcessor;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCRecordMetaData;
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

    private static CrawlJob crawlJob = null;
    private static File crawlJobDir = null;
    private static File arcFile = null;
    private static String selftestURL = null;

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
     * Test passed object is non-null.
     *
     * @param obj Object to test.
     * @return Passed object.
     * @throws NullPointerException Thrown if passed object is null.
     */
    private static Object testNonNull(Object obj)
        throws NullPointerException
    {
        if (obj == null)
        {
            throw new NullPointerException(obj.toString());
        }
        return obj;
    }

    /**
     * Test non null and not empty.
     *
     * @param str String to test.
     * @return The passed string.
     * @throws IllegalArgumentException if null or empty string.
     */
    private static String testNonNullNonEmpty(String str)
        throws IllegalArgumentException, NullPointerException
    {
        if (((String)testNonNull(str)).length() <= 0)
        {
            throw new IllegalArgumentException("Passed string " + str +
                " is empty.");
        }
        return str;
    }

    /**
     * Test nonull and exits.
     *
     * @param file File to test.
     * @return Passed file.
     * @throws FileNotFoundException passed file doesn't exist.
     */
    private static File testNonNullExists(File file)
        throws FileNotFoundException
    {
        if (!((File)testNonNull(file)).exists())
        {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        return file;
    }

    /**
     * Static initializer.
     *
     * Must be called before instantiation of any tests based off this class.
     *
     * @param url URL to selftest webapp.
     * @param job The selftest crawl job.
     * @param jobDir Job output directory.  Has the seed file, the order file
     * and logs.
     * @param docs Expanded webapp directory location.
     *
     * @throws IOException if nonexistent directories passed.
     */
    public static synchronized void initialize(final String url,
            final CrawlJob job, final File jobDir, final File docs)
        throws IOException, AttributeNotFoundException, MBeanException,
            ReflectionException
    {
        testNonNullNonEmpty(url);
        SelfTestCase.selftestURL = url.endsWith("/")? url: url + "/";
        SelfTestCase.crawlJob = (CrawlJob)testNonNull(job);
        SelfTestCase.crawlJobDir = testNonNullExists(jobDir);
        SelfTestCase.htdocs = testNonNullExists(docs);
        // Calculate the logs directory.  If diskPath is not absolute, then logs
        // are in the jobs directory under the diskPath subdirectory.  Guard
        // against case where diskPath is empty.
        CrawlOrder crawlOrder =
            (CrawlOrder)testNonNull(job.getSettingsHandler().getOrder());
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
        testNonNullExists(SelfTestCase.logsDir);
        // Calculate the arcfile name.  Find it in the arcDir.  Should only be
        // one. Then make an instance of ARCReader and call the validate on it.
        ComplexType arcWriterProcessor = 
            crawlOrder.getSettingsHandler().getModule("Archiver");
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
        testNonNullExists(arcDir);
        String prefix = testNonNullNonEmpty((String)arcWriterProcessor.
            getAttribute(ARCWriterProcessor.ATTR_PREFIX));
        File [] arcs = FileUtils.getFilesWithPrefix(arcDir, prefix);
        if (arcs.length != 1)
        {
            throw new IOException("Expected one only arc file.  Found" +
                " instead " + Integer.toString(arcs.length) + " files.");
        }
        SelfTestCase.arcFile = arcs[0];
        SelfTestCase.readReader = new ARCReader(arcFile);
        SelfTestCase.readReader.validate();

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
    protected static File getCrawlJobDir()
    {
        return SelfTestCase.crawlJobDir;
    }

    /**
     * @return Return the directory w/ logs in it.
     */
    protected static File getLogsDir()
    {
        return SelfTestCase.logsDir;
    }

    /**
     * Returns the selftest read ARCReader.
     *
     * The returned ARCReader has been validated.  Use it to get at metadata.
     *
     * @return Returns the readReader, an ARCReader that has been validated.
     */
    protected static ARCReader getReadReader()
    {
        return SelfTestCase.readReader;
    }

    /**
     * @return Returns the selftestURL.
     */
    public static String getSelftestURL()
    {
        return SelfTestCase.selftestURL;
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

    /**
     * @return Returns the crawlJob.
     */
    public static CrawlJob getCrawlJob()
    {
        return crawlJob;
    }

    /**
     * Confirm passed files exist on disk under the test directory.
     *
     * @param files Files to test for existence under the test's directory.
     * @return true if all files exist on disk.
     */
    public boolean filesExist(List files)
    {
        boolean result = true;
        for (Iterator i = files.iterator(); i.hasNext();)
        {
            if (!fileExists((String)i.next()))
            {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Confirm passed file exists on disk under the test directory.
     *
     * This method takes care of building up the file path under the selftest
     * webapp.  Just pass the file name.
     *
     * @param fileName Name of file to look for.
     * @return True if file exists.
     */
    public boolean fileExists(String fileName)
    {
        File testDir = new File(getHtdocs(), getTestName());
        File fileOnDisk = new File(testDir, fileName);
        return fileOnDisk.exists();
    }

    /**
     * Test passed list were all found in the arc.
     * 
     * If more or less found, test fails.
     *
     * @param files List of files to find in the arc.  No other files but these
     * should be found in the arc.
     */
    public void testFilesInArc(List files)
    {
        List foundFiles = filesFoundInArc();
        assertTrue("All files are on disk", filesExist(files));
        assertTrue("All found", foundFiles.containsAll(files));
        assertTrue("Same size", foundFiles.size() == files.size());
    }

    /**
     * Find all files that belong to this test that are mentioned in the arc.
     * @return List of found files.
     */
    private List filesFoundInArc()
    {
        String baseURL = getSelftestURLWithTrailingSlash();
        if (baseURL.endsWith(getTestName() + '/')) {
            // URL may already end in the test name for case where we're
            // running one test only.  If so, strip back the trailing '/'.
            baseURL = baseURL.substring(0, baseURL.length() - 1);
        } else {
            baseURL += getTestName();
        }
        List metaDatas = getReadReader().getMetaDatas();
        ARCRecordMetaData metaData = null;
        List filesFound = new ArrayList();
        for (Iterator i = metaDatas.iterator(); i.hasNext();)
        {
            metaData = (ARCRecordMetaData)i.next();
            String url = metaData.getUrl();
            if (url.startsWith(baseURL) &&
                metaData.getMimetype().equalsIgnoreCase("text/html"))
            {
                String name = url.substring(url.lastIndexOf("/") + 1);
                if (name != null && name.length() > 0)
                {
                    filesFound.add(name);
                }
            }
        }
        return filesFound;
    }
}