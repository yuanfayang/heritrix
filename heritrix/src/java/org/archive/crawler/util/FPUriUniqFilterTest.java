/* FPUriUniqFilterTest
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUri;
import org.archive.util.MemLongFPSet;


/**
 * Test FPUriUniqFilter.
 * @author stack
 */
public class FPUriUniqFilterTest extends TestCase
implements UriUniqFilter.HasUriReceiver, UriUniqFilter.HasUri {
    private Logger logger =
        Logger.getLogger(FPUriUniqFilterTest.class.getName());

    private UriUniqFilter filter = null;
    
    /**
     * Set to true if we visited received.
     */
    private boolean received = false;
    
	protected void setUp() throws Exception {
		super.setUp();
        // 17 makes a MemLongFPSet of one meg of longs (64megs).
		this.filter = new FPUriUniqFilter(new MemLongFPSet(10, 0.75f));
		this.filter.setDestination(this);
    }
    
    public void testAdding() {
    	this.filter.add(this);
        this.filter.addNow(this);
        this.filter.addForce(this);
        // Should only have add 'this' once.
        assertTrue("Count is off", this.filter.count() == 1);
    }
    
    /**
     * Test inserting and removing.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void testWriting() throws FileNotFoundException, IOException {
        long start = System.currentTimeMillis();
        ArrayList list = new ArrayList(1000);
        int count = 0;
        final int MAX_COUNT = 1000;
        for (; count < MAX_COUNT; count++) {
        	UURI u = UURIFactory.getInstance("http://www" +
        			count + ".archive.org/" + count + "/index.html");
        	this.filter.add(u);
        	if (count > 0 && ((count % 100) == 0)) {
        		list.add(u);
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
            this.filter.count() == MAX_COUNT);
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
}
