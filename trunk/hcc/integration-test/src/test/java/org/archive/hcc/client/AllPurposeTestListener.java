package org.archive.hcc.client;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AllPurposeTestListener implements
        CrawlerLifecycleListener,
        CurrentCrawlJobListener {
    CountDownLatch crawlerCreatedLatch = new CountDownLatch(1);

    CountDownLatch crawlerDestroyedLatch = new CountDownLatch(1);

    CountDownLatch crawlJobStartedLatch = new CountDownLatch(1);

    CountDownLatch crawlJobCompletedLatch = new CountDownLatch(1);

    CountDownLatch crawlJobPausedLatch = new CountDownLatch(1);

    CountDownLatch crawlJobResumedLatch = new CountDownLatch(1);

    CountDownLatch crawlJobStatisticsChangedLatch = new CountDownLatch(1);

    Crawler c;

    CurrentCrawlJob j;

    CompletedCrawlJob completedj;

    Map<String,Object> statistics;

    public void crawlerCreated(Crawler c) {
        this.c = c;
        crawlerCreatedLatch.countDown();
    }

    public void crawlerDestroyed(Crawler c) {
        this.c = c;
        crawlerDestroyedLatch.countDown();
    }

    public void crawlJobCompleted(CompletedCrawlJob job) {
        this.completedj = job;
        crawlJobCompletedLatch.countDown();
    }

    public void crawlJobPaused(CurrentCrawlJob job) {
        this.crawlJobPausedLatch.countDown();
        this.j = job;

    }

    public void crawlJobResumed(CurrentCrawlJob job) {
        this.crawlJobResumedLatch.countDown();
        this.j = job;

    }

    public void crawlJobStarted(CurrentCrawlJob job) {
        this.j = job;
        crawlJobStartedLatch.countDown();
    }

    public void crawlJobStopping(CurrentCrawlJob job) {
        // TODO Auto-generated method stub

    }

    public void statisticsChanged(CurrentCrawlJob job, Map<String,Object> statistics) {
        this.j = job;
        this.statistics = statistics;
        crawlJobStatisticsChangedLatch.countDown();

    }

}
