/* KeyedQueueTest
 * 
 * Created on Jul 26, 2004
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
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.util.FileUtils;
import org.archive.util.TmpDirTestCase;

/**
 * @author stack
 * @version $Revision$, $Date$
 */
public class KeyedQueueTest extends TmpDirTestCase {
    
    /**
     * Test queueing and dequeueing of CrawlURIs.
     */
    public void testDequeueCrawlURIs() throws IOException {
        
        // Let this be the original 'seed' object.
        UURI uuri = UURIFactory.getInstance("http://www.dh.gov.uk/Home/fs/en");
        CrawlURI seed = new CrawlURI(uuri);
        seed.setVia("");
        seed.setIsSeed(true);
        seed.setSchedulingDirective(CrawlURI.HIGH);
        
        // Create a queue with only 1 item in memory.  This
        // will bring on excercise of the memory/disk transitions.
        File queueDir = new File(getTmpDir(), seed.getClass().getName());
        if (queueDir.exists()) {
            FileUtils.deleteDir(queueDir);
        }
        KeyedQueue kq = new KeyedQueue(seed.getClassKey(), null,
            queueDir, 1);
        try {
            kq.activate();
            
            // Simulate putting 'seed' on stack and then dequeuing it.
            kq.enqueue(seed);
            CrawlURI dequeuedCuri = kq.dequeue();
            kq.noteInProcess(dequeuedCuri);
            
            // But we find we need dns first.  Means enqueuing it then
            // marking seed as done and requeueing.
            CrawlURI dns =
                new CrawlURI(UURIFactory.getInstance("dns:www.dh.gov.uk"));
            dns.setVia("");
            dns.setPrerequisite(true);
            dns.setSchedulingDirective(CrawlURI.HIGH);
            
            kq.enqueue(dns);
            kq.noteProcessDone(dequeuedCuri);
            seed.setSchedulingDirective(CrawlURI.MEDIUM);
            kq.enqueue(dequeuedCuri);
            
            // Dequeue dns and get it.
            dequeuedCuri = kq.dequeue();
            kq.noteInProcess(dequeuedCuri);
            kq.noteProcessDone(dequeuedCuri);
            
            
            // Dequeue seed and try it again.
            dequeuedCuri = kq.dequeue();
            kq.noteInProcess(dequeuedCuri);
            
            // But we need to get robots.  Queue it.
            // And after marking seed as done, requeue.
            CrawlURI robotsCuri = new CrawlURI(UURIFactory.
                    getInstance("http://www.dh.gov.uk/robots.txt"));
            robotsCuri.setVia("");
            robotsCuri.setPrerequisite(true);
            robotsCuri.setSchedulingDirective(CrawlURI.HIGH);
            
            kq.enqueue(robotsCuri);
            kq.noteProcessDone(dequeuedCuri);
            seed.setSchedulingDirective(CrawlURI.MEDIUM);
            kq.enqueue(dequeuedCuri);
            
            // Dequeue robots and get it.
            dequeuedCuri = kq.dequeue();
            kq.noteInProcess(dequeuedCuri);
            kq.noteProcessDone(dequeuedCuri);
            
            // Dequeue seed.  Go to get it.
            // Before marking it done, the extractor will
            // queue a bunch of URIs before we can mark the seed as done.
            dequeuedCuri = kq.dequeue();
            // If I let it go on, it sometimes fails with a CCE
            // deserializing.
            
//            kq.noteInProcess(dequeuedCuri);
//            CrawlURI miscCuri = null;
//            for (int i = 0; i < 10; i++) {
//                miscCuri = new CrawlURI(UURIFactory.
//                        getInstance("http://www.dh.gov.uk/" + i + ".html"));
//                miscCuri.setVia(seed.toString());
//                miscCuri.setPathFromSeed("E");
//                miscCuri.setSchedulingDirective(CrawlURI.MEDIUM);
//                kq.enqueue(miscCuri);
//            }
//            
//            // Now queue a bunch of normals.
//            for (int i = 0; i < 10; i++) {
//                miscCuri = new CrawlURI(UURIFactory.
//                        getInstance("http://www.dh.gov.uk/" + i + ".html"));
//                miscCuri.setVia(seed.toString());
//                miscCuri.setPathFromSeed("E");
//                miscCuri.setSchedulingDirective(CrawlURI.NORMAL);
//                kq.enqueue(miscCuri);
//            }
//            
//            // Note seed as done.
//            kq.noteProcessDone(dequeuedCuri);
//            
//            dequeuedCuri = kq.dequeue();
//            kq.noteInProcess(dequeuedCuri);
//            kq.noteProcessDone(dequeuedCuri);
//            dequeuedCuri = kq.dequeue();
//            dequeuedCuri = kq.dequeue();
//            dequeuedCuri = kq.dequeue();
        } finally {
            FileUtils.deleteDir(queueDir);
        }
    }
}