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

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUri;
import org.archive.util.MemLongFPSet;

import junit.framework.TestCase;


/**
 * Test FPUriUniqFilter.
 * @author stack
 */
public class FPUriUniqFilterTest extends TestCase
implements UriUniqFilter.HasUriReceiver, UriUniqFilter.HasUri {

    private FPUriUniqFilter filter = null;
    
    /**
     * Set to true if we visited received.
     */
    private boolean received = false;
    
	protected void setUp() throws Exception {
		super.setUp();
        // 17 makes a MemLongFPSet of one meg in size.
		this.filter = new FPUriUniqFilter(new MemLongFPSet(17, 0.75f));
		this.filter.setDestination(this);
    }
    
    public void testAdding() {
    	this.filter.add(this);
        this.filter.addNow(this);
        this.filter.addForce(this);
        // Should only have add 'this' once.
        assertTrue("Count is off", this.filter.count() == 1);
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
