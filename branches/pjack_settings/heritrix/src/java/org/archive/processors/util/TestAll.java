/* 
 * Copyright (C) 2007 Internet Archive.
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
 *
 * TestAll.java
 *
 * Created on Feb 5, 2007
 *
 * $Id:$
 */

package org.archive.processors.util;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author pjack
 *
 */
public class TestAll {

    
    public static Test suite() {
        String cname = TestAll.class.getName();
        int p = cname.lastIndexOf('.');
        String pname = cname.substring(0, p);
        TestSuite result = new TestSuite(pname);
        result.addTestSuite(CrawlHostTest.class);
        result.addTestSuite(CrawlServerTest.class);
        result.addTestSuite(RobotsHonoringPolicyTest.class);
        result.addTestSuite(ServerCacheTest.class);        
        return result;
    }
}
