package org.archive.crawler.framework;

import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Operation;


/**
 * Interface for controlling a crawl job.  The implementation is 
 * {@link CrawlController}, which is an open MBean.  Ideally this interface
 * would be named CrawlController, and the implementation would be named
 * CrawlControllerImpl, to follow MBean standard practice.  However the
 * CrawlController class has been with us for a very long time, and is 
 * referenced by practically everything else, so that refactoring would be
 * onerous.
 * 
 * @author pjack
 */
public interface JobController {

    /** 
     * Operator requested crawl begin
     */
    @Operation(desc = "Start the crawl.")
    void requestCrawlStart();

    /**
     * Request a checkpoint.
     * Sets a checkpointing thread running.
     * @throws IllegalStateException Thrown if crawl is not in paused state
     * (Crawl must be first paused before checkpointing).
     */
    @Operation(desc = "Request a checkpoint.")
    void requestCrawlCheckpoint() throws IllegalStateException;

    /**
     * Operator requested for crawl to stop.
     */
    @Operation(desc = "Aborts the crawl.")
    void requestCrawlStop();

    /**
     * Stop the crawl temporarly.
     */
    @Operation(desc = "Stop the crawl temporarily.")
    void requestCrawlPause();

    /**
     * Resume crawl from paused state
     */
    @Operation(desc = "Resume crawl from paused state.")
    void requestCrawlResume();

    @Attribute(desc="The current crawl status.", def="PAUSED")
    String getCrawlStatusString();

}
