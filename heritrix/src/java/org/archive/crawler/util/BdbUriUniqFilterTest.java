/* BdbUriUniqFilterTest
 *
 * $Id$
 *
 * Created on Sep 15, 2004.
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
package org.archive.crawler.util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUri;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;

import com.sleepycat.je.DatabaseException;


/**
 * Test BdbUriUniqFilter.
 * @author stack
 */
public class BdbUriUniqFilterTest extends TmpDirTestCase
implements UriUniqFilter.HasUriReceiver, UriUniqFilter.HasUri {
    private Logger logger =
        Logger.getLogger(BdbUriUniqFilterTest.class.getName());
    
    private UriUniqFilter filter = null;
    private File bdbDir = null;
    
    /**
     * Set to true if we visited received.
     */
    private boolean received = false;
    
	protected void setUp() throws Exception {
		super.setUp();
        // Remove any bdb that already exists.
        this.bdbDir = new File(getTmpDir(), this.getClass().getName());
        if (this.bdbDir.exists()) {
        	FileUtils.deleteDir(bdbDir);
        }
		this.filter = new BdbUriUniqFilter(bdbDir, 50);
		this.filter.setDestination(this);
    }
    
	protected void tearDown() throws Exception {
		super.tearDown();
        ((BdbUriUniqFilter)this.filter).close();
        // if (this.bdbDir.exists()) {
        //    FileUtils.deleteDir(bdbDir);
        // }
	}
    
    public void testAdding() {
    	this.filter.add(this);
        this.filter.addNow(this);
        this.filter.addForce(this);
        // Should only have add 'this' once.
        assertTrue("Count is off", this.filter.count() == 1);
    }
    
    public void testCreateKey() {
        String url = "dns:archive.org";
        long fingerprint = ((BdbUriUniqFilter)this.filter).createKey(url);
        assertTrue("Fingerprint wrong " + url,
            fingerprint == 8812918003193194397L);
        url = "http://archive.org/index.html";
        fingerprint = ((BdbUriUniqFilter)this.filter).createKey(url);
        assertTrue("Fingerprint wrong " + url,
            fingerprint == 6613238036839061197L);
    }
    
    /**
     * Time import of recovery log.
     * REMOVE
     * @throws IOException
     * @throws FileNotFoundException
     * @throws DatabaseException
     */
    public void testWriting()
    throws FileNotFoundException, IOException, DatabaseException {
        long maxcount = 1000;
        // Look for a system property to override default max count.
        String key = this.getClass().getName() + ".maxcount";
        String maxcountStr = System.getProperty(key);
        logger.info("Looking for override system property " + key);
        if (maxcountStr != null && maxcountStr.length() > 0) {
        	maxcount = Long.parseLong(maxcountStr);
        }
        runTestWriting(maxcount);
    }
    
    protected void runTestWriting(long max)
    throws DatabaseException, URIException {
        long start = System.currentTimeMillis();
        ArrayList list = new ArrayList(1000);
        int count = 0;
        for (; count < max; count++) {
            UURI u = UURIFactory.getInstance("http://www" +
                count + ".archive.org/" + count + "/index.html");
            this.filter.add(u);
            if (count > 0 && ((count % 100) == 0)) {
                list.add(u);
            }
            if (count > 0 && ((count % 100000) == 0)) {
                this.logger.info("Added " + count + " in " +
                    (System.currentTimeMillis() - start) +
                    " misses " +
                    ((BdbUriUniqFilter)this.filter).getCacheMisses() +
                    " diff of misses " +
                    ((BdbUriUniqFilter)this.filter).getLastCacheMissDiff());
            }
        }
        this.logger.info("Added " + count + " in " +
            (System.currentTimeMillis() - start));
        
        start = System.currentTimeMillis();
        for (Iterator i = list.iterator(); i.hasNext();
        this.filter.add((UURI)i.next())) {
            continue;
        }
        this.logger.info("Added random " + list.size() + " in " +
                (System.currentTimeMillis() - start));
        
        start = System.currentTimeMillis();
        for (Iterator i = list.iterator(); i.hasNext();
        this.filter.add((UURI)i.next())) {
            continue;
        }
        this.logger.info("Deleted random " + list.size() + " in " +
            (System.currentTimeMillis() - start));
        // Looks like delete doesn't work.
        assertTrue("Count is off: " + this.filter.count(),
            this.filter.count() == max);
    }
    
    public void testNote() {
    	this.filter.note(this);
        assertFalse("Receiver was called", this.received);
    }
    
    public void testForget() {
        this.filter.forget(this);
        assertTrue("Didn't forget", this.filter.count() == 0);
    }
    
	public void receive(HasUri item) {
		this.received = true;
	}

	public String getUri() {
		return "http://www.archive.org";
	}
    
    /**
     * return the suite of tests for MemQueueTest
     *
     * @return the suite of test
     */
    public static Test suite() {
        return new TestSuite(BdbUriUniqFilterTest.class);
    }

    public static void main(String[] args) {
    	junit.textui.TestRunner.run(suite());
	}
}
