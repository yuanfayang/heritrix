package org.archive.hcc.client;

import java.util.Collection;
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

}
