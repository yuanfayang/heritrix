package org.archive.hcc.client;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClusterControllerClientImplSelfTest
        extends
            ClusterControllerClientSelfTestBase {

    /*
     * Test method for
     * 'org.archive.hcc.client.ClusterControllerClientImpl.listCrawlers()'
     */
    public void testListCrawlers() {
        try {
            Collection<Crawler> crawlers = cc.listCrawlers();
            assertNotNull(crawlers);
        } catch (ClusterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertFalse(true);
        }
    }

    public void testCrawlerLifecycleListener() {

        AllPurposeTestListener l = new AllPurposeTestListener();

        try {
            cc.addCrawlerLifecycleListener(l);
            Crawler c = cc.createCrawler();

            try {
                assertTrue(l.crawlerCreatedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertEquals(c, l.c);

            c.destroy();
            try {
                assertTrue(l.crawlerDestroyedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertEquals(c, l.c);

        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        } finally {
            cc.removeCrawlerLifecycleListener(l);
        }
    }
    
    public void testCreateMultipleCrawlersInRapidSuccession() {
        final List<Crawler> crawlers = new LinkedList<Crawler>();
        
        final CountDownLatch createdLatch = new CountDownLatch(10);
        final CountDownLatch destroyedLatch = new CountDownLatch((int)createdLatch.getCount());
        
        class MyListener implements CrawlerLifecycleListener{
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CrawlerLifecycleListener#crawlerCreated(org.archive.hcc.client.Crawler)
             */
            public void crawlerCreated(Crawler c) {
                crawlers.add(c);
                createdLatch.countDown();
                
            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CrawlerLifecycleListener#crawlerDestroyed(org.archive.hcc.client.Crawler)
             */
            public void crawlerDestroyed(Crawler c) {
                crawlers.remove(c);
                destroyedLatch.countDown();
            }
        };
        
        MyListener l = new MyListener();
        try {
            cc.addCrawlerLifecycleListener(l);

            int count = (int)createdLatch.getCount();
            for(int i = 0; i < count; i++){
                Crawler c = cc.createCrawler();
            }

            try {
                assertTrue(createdLatch.await(
                        30 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            assertEquals(count, crawlers.size());

            for(Crawler c : new LinkedList<Crawler>(crawlers)){
                c.destroy();
            }

            try {
                assertTrue(destroyedLatch.await(
                        30 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            

            
        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        } finally {
            cc.removeCrawlerLifecycleListener(l);
        }
    }


}
