package org.archive.hcc.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.archive.hcc.util.JmxUtils;

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
                        20 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertEquals(c, l.c);

            c.destroy();
            try {
                assertTrue(l.crawlerDestroyedLatch.await(
                		20 * 1000,
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
        int max = 5;
    	setMaxCrawlersTo(max);
    	
    	final List<Crawler> crawlers = new LinkedList<Crawler>();
        
        final CountDownLatch createdLatch = new CountDownLatch(max);
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
            int currentCount = cc.listCrawlers().size();
            int count = (int)createdLatch.getCount();
            for(int i = 0; i < count; i++){
                Crawler c = cc.createCrawler();
            }

            try {
                assertTrue(createdLatch.await(
                        60 * 1000,
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
            
            assertEquals(currentCount, cc.listCrawlers().size());

            
        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        } finally {
            cc.removeCrawlerLifecycleListener(l);
        }
    }



    
    
    public void testCreatePauseAllResumeAllMultipleJobsInRapidSuccession() {
        int max = 5;
    	setMaxCrawlersTo(5);
    	
    	final List<Crawler> crawlers = new LinkedList<Crawler>();
        
        final CountDownLatch createdLatch = new CountDownLatch(max);
        final CountDownLatch destroyedLatch = new CountDownLatch((int)createdLatch.getCount());
        final CountDownLatch crawlJobStarted  = new CountDownLatch((int)createdLatch.getCount());
        final CountDownLatch crawlJobCompleted  = new CountDownLatch((int)createdLatch.getCount());
        final CountDownLatch crawlJobPaused = new CountDownLatch((int)createdLatch.getCount());
        final CountDownLatch crawlJobResumed = new CountDownLatch((int)createdLatch.getCount());
               
        class MyListener implements CrawlerLifecycleListener,CurrentCrawlJobListener{
            
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
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#crawlJobCompleted(org.archive.hcc.client.CompletedCrawlJob)
             */
            public void crawlJobCompleted(CompletedCrawlJob job) {
                crawlJobCompleted.countDown();
                try {
                    assertTrue(job.getCrawlReport().contains("Ended by operator"));
                } catch (ClusterException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#crawlJobPaused(org.archive.hcc.client.CurrentCrawlJob)
             */
            public void crawlJobPaused(CurrentCrawlJob job) {
                crawlJobPaused.countDown();
            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#crawlJobResumed(org.archive.hcc.client.CurrentCrawlJob)
             */
            public void crawlJobResumed(CurrentCrawlJob job) {
                crawlJobResumed.countDown();
                
            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#crawlJobStarted(org.archive.hcc.client.CurrentCrawlJob)
             */
            public void crawlJobStarted(CurrentCrawlJob job) {
            	assertTrue(job.getCrawlStatus().equals("RUNNING") 
                        || job.getCrawlStatus().equals("PREPARING"));

                crawlJobStarted.countDown();

            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#crawlJobStopping(org.archive.hcc.client.CurrentCrawlJob)
             */
            public void crawlJobStopping(CurrentCrawlJob job) {
                
            }
            
            /* (non-Javadoc)
             * @see org.archive.hcc.client.CurrentCrawlJobListener#statisticsChanged(org.archive.hcc.client.CurrentCrawlJob, java.util.Map)
             */
            public void statisticsChanged(CurrentCrawlJob job, Map statistics) {

                
            }
        };
        
        MyListener l = new MyListener();
        try {
            cc.addCrawlerLifecycleListener(l);
            cc.addCrawlJobListener(l);
            int count = (int)createdLatch.getCount();
            for(int i = 0; i < count; i++){
                Crawler c = cc.createCrawler();
                c.addJob(new JobOrder("test"+i, "", getTestJar()));
                c.startPendingJobQueue();
            }
            
           try {
                assertTrue(createdLatch.await(
                        30 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            assertEquals(count, crawlers.size());

            
            try {
                assertTrue(crawlJobStarted.await(
                        40 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            assertTrue(cc.pauseAllJobs());

            try {
                assertTrue(crawlJobPaused.await(
                        40 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            assertTrue(cc.resumeAllPausedJobs());

            try {
                assertTrue(crawlJobResumed.await(
                        40 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            
            for(Crawler c : new LinkedList<Crawler>(crawlers)){
                c.terminateCurrentJob();
            }
            try {
                assertTrue(crawlJobCompleted.await(
                        80 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            
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
            cc.removeCrawlJobListener(l);
            
            try {
				for(Crawler c :cc.listCrawlers()){
				    c.destroy();
				}
			} catch (ClusterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    public void testGetCurrentCrawlJob() {
        AllPurposeTestListener l = new AllPurposeTestListener();
        try {
            cc.addCrawlerLifecycleListener(l);
            cc.addCrawlJobListener(l);
            cc.createCrawler();

            try {
                assertTrue(l.crawlerCreatedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            l.c.addJob(new JobOrder("test1", "",getTestJar()));
            l.c.startPendingJobQueue();

            try {
                assertTrue(l.crawlJobStartedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            CurrentCrawlJob ccj = cc.getCurrentCrawlJob(l.c);
            
            assertTrue(ccj.equals(l.j));
            

            
            l.c.destroy();

            try {
                assertTrue(l.crawlerDestroyedLatch.await(
                        10 * 1000,
                        TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (ClusterException e) {
            e.printStackTrace();
            assertFalse(true);
        } finally {
            cc.removeCrawlerLifecycleListener(l);
            cc.removeCrawlJobListener(l);
        }
    }
    
    public void testGetSetMaxInstances(){
    	try{
    		Crawler c = cc.createCrawler();
    		
    		InetSocketAddress a  = JmxUtils.extractRemoteAddress(c.getName());
    		int max = cc.getMaxInstances(a.getHostName(), a.getPort());
    		assertTrue(max > 0);
    		cc.setMaxInstances(a.getHostName(), a.getPort(), ++max);
    		
    		int newMax = cc.getMaxInstances(a.getHostName(), a.getPort());
    		assertEquals(max, newMax);
    		c.destroy();
    	}catch(Exception e){
            e.printStackTrace();
            assertFalse(true);
    	}
    }
     
}
