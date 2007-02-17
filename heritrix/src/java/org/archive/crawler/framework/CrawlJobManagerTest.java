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
 * CrawlJobManagerTest.java
 *
 * Created on Feb 13, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.archive.settings.file.FileSheetManager;
import org.archive.util.FileUtils;

/**
 * @author pjack
 *
 */
public class CrawlJobManagerTest extends TestCase {

    
    public void test() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        System.out.println(server);
        File f = new File("/Users/pjack/Desktop/crawl");
        File jobsDir = new File(f, "jobs");
        FileUtils.deleteDir(jobsDir);
        jobsDir.mkdir();

        CrawlJobManagerImpl cjmi = new CrawlJobManagerImpl(f, server);
        cjmi.launchProfile("default", "test1");
    }
    
}
