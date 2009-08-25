package org.archive.hcc.client;

import java.util.concurrent.TimeUnit;

public class CrawlerImplSelfTest
        extends
            ClusterControllerClientSelfTestBase {
    private CrawlerImpl c;
    
    protected void setUp() throws Exception {
    
        super.setUp();
        c = (CrawlerImpl) cc.createCrawler();
        c.startPendingJobQueue();
    }

    protected void tearDown() throws Exception {
        c.destroy();
        Thread.sleep(5*1000);
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
        AllPurposeTestListener listener;
        listener = new AllPurposeTestListener();
        cc.addCrawlerLifecycleListener(listener);
        cc.addCrawlJobListener(listener);


        try {
            String uid = c.addJob(new JobOrder("test", "", getTestJar()));

            try {
                assertTrue(listener.crawlJobStartedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertEquals(new Long(uid), listener.j.getUid());

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
                Thread.sleep(2000);
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
            cc.removeCrawlerLifecycleListener(listener);
            cc.removeCrawlJobListener(listener);
        }
    }

    public void testCheckCrawlJobStatus(){
        AllPurposeTestListener listener;
        listener = new AllPurposeTestListener();
        cc.addCrawlerLifecycleListener(listener);
        cc.addCrawlJobListener(listener);
        //start a new job
        c.addJob(new JobOrder("test","", getTestJar()));

        try {
            assertTrue(listener.crawlJobStartedLatch.await(
                    10 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        //check status of job once it has started.
        assertNotNull(listener.j.getCrawlStatus());
        assertEquals("test", listener.j.getJobName());
        assertEquals(listener.j.getMother().getName(), c.getName());
        
    }
    
    public void testCheckMotherNotNull(){
        AllPurposeTestListener listener;
        listener = new AllPurposeTestListener();
        cc.addCrawlerLifecycleListener(listener);
        cc.addCrawlJobListener(listener);
        //start a new job
        c.addJob(new JobOrder("test", "", getTestJar()));

        try {
            assertTrue(listener.crawlJobStartedLatch.await(
                    10 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        //check status of job once it has started.
        assertEquals(listener.j.getMother().getName(), c.getName());
        
    }

    
    public void testListCompletedCrawlJobs(){
        AllPurposeTestListener listener;
        listener = new AllPurposeTestListener();
        cc.addCrawlerLifecycleListener(listener);
        cc.addCrawlJobListener(listener);
        //start a new job
        c.addJob(new JobOrder("test", "",getTestJar()));


        int completedJobCount = c.listCompletedCrawlJobs().size();
        try {
            assertTrue(listener.crawlJobStatisticsChangedLatch.await(
                    30 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        c.terminateCurrentJob();
        
        try {
            assertTrue(listener.crawlJobCompletedLatch.await(
                    10 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
      
        assertEquals(completedJobCount+1, c.listCompletedCrawlJobs().size());

    }
    
    public void testDeleteCrawlJob(){
        AllPurposeTestListener listener;
        listener = new AllPurposeTestListener();
        cc.addCrawlerLifecycleListener(listener);
        cc.addCrawlJobListener(listener);
        //start a new job
        c.addJob(new JobOrder("test", "",getTestJar()));

        try {
            assertTrue(listener.crawlJobStatisticsChangedLatch.await(
                    30 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        c.terminateCurrentJob();
        
        try {
            assertTrue(listener.crawlJobCompletedLatch.await(
                    10 * 1000,
                    TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        int completedJobCount = c.listCompletedCrawlJobs().size();

        try {
            c.deleteCompletedCrawlJob(listener.completedj);
        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        }
        
        assertEquals(completedJobCount-1, c.listCompletedCrawlJobs().size());

    }


}
