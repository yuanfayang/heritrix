package org.archive.hcc.client;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.archive.hcc.util.OrderJarFactory;

public class CrawlerImplSelfTest
        extends
            ClusterControllerClientSelfTest {
    private CrawlerImpl c;

    protected void setUp() throws Exception {
        super.setUp();
        c = (CrawlerImpl) cc.createCrawler();
        c.startPendingJobQueue();

    }

    protected void tearDown() throws Exception {
        c.destroy();
        super.tearDown();
        c = null;
    }

    public void testGetVersion() {
        assertNotNull(this.c.getVersion());
        System.out.println(c.getVersion());
    }

    public void testIsPendingJobQueueRunning() {
        c.startPendingJobQueue();
        assertTrue(c.isPendingJobQueueRunning());
        c.stopPendingJobQueue();
        assertFalse(c.isPendingJobQueueRunning());
    }

    public void testIsCrawling() {
        assertFalse(c.isCrawling());
    }

    public void testCreateJobHearJobFindJobParentStopJob() {
        AllPurposeTestListener listener = new AllPurposeTestListener();

        try {
            cc.addCrawlJobListener(listener);
            String uid = c.addJob(new JobOrder("test", getTestJar()));

            try {
                assertTrue(listener.crawlJobStartedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertEquals(uid, listener.j.getUid());

            listener.j.pause();

            try {
                assertTrue(listener.crawlJobPausedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            listener.j.resume();

            try {
                assertTrue(listener.crawlJobResumedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                assertTrue(listener.crawlJobStatisticsChangedLatch.await(
                        30 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertNotNull(listener.statistics);

            Crawler foundC = cc.findCrawlJobParent(uid, listener.j
                    .getRemoteAddress());
            assertEquals(c, foundC);

            try {
                // TODO follow up with Stack about why this pause is required.
                // if you take it away, sometimes terminateCurrentJob() hangs.
                Thread.sleep(1);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            foundC.terminateCurrentJob();

            try {
                assertTrue(listener.crawlJobCompletedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
                assertNotNull(listener.completedj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        } finally {
            cc.removeCrawlJobListener(listener);
        }
    }

    public static File getTestJar() {
        Map map = new HashMap();
        map.put("name", "test");
        List<String> seeds = new LinkedList<String>();
        seeds.add("http://crawler.archive.org");
        map.put("seeds", seeds);
        return OrderJarFactory.createOrderJar(map);
    }

}
