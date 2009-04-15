package org.archive.crawler.framework;

import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;


/**
 * Interface for controlling a crawl job.  The implementation is 
 * {@link CrawlControllerImpl}, which is an open MBean.  
 * 
 * @author pjack
 */
public interface CrawlController {

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

    @Attribute(desc = "Returns the toe threads report", def="")
    String getToeThreadReport();

    @Attribute(desc = "Returns a summarized toe threads report", def="")
    String getToeThreadReportShort();

    @Attribute(desc = "Returns the frontier report", def="")
    String getFrontierReport();

    @Attribute(desc = "Returns a summarized frontier report", def="")
    String getFrontierReportShort();

    @Attribute(desc = "Returns the processors report", def="")
    String getProcessorsReport();

    @Operation(desc = "Kill a specific ToeThread.", impact=Bean.ACTION)
    void killThread(
            @Parameter(name = "threadNumber", desc = "Serial number of the ToeThread to kill")
            int threadNumber,
            @Parameter(name = "replace", desc = "If the killed thread should be replaced")
            boolean replace);
}
