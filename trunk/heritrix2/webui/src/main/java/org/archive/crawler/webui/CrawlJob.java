package org.archive.crawler.webui;

import javax.management.remote.JMXConnector;
import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.framework.AlertTracker;
import org.archive.crawler.framework.CrawlJobManager;
import org.archive.crawler.framework.JobController;
import org.archive.crawler.framework.JobStage;

/**
 * Represents a crawl job (a profile, an active job or a completed
 * job) on a crawler.
 * 
 * @author Kristinn
 */
public class CrawlJob {

    
    String name;
    JobStage stage;
    String crawlstatus;
    int alerts = -1;
    // TODO: Completed jobs also have more specific crawl state (i.e. how they ended).
    
    public CrawlJob(String name, JobStage stage, String crawlStatus) {
        this(name, stage, crawlStatus, 0);
    }

    public CrawlJob(String name, JobStage stage, String crawlStatus, int alerts) {
        this.name = name;
        this.stage = stage;
        this.crawlstatus = crawlStatus;
        this.alerts = alerts;
    }

    
    public int hashCode() {
        return name.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof CrawlJob)) {
            return false;
        }
        CrawlJob cj = (CrawlJob)o;
        return cj.name.equals(name);
    }
    
    public String getName() {
        return name;
    }

    public JobStage getJobStage() {
        return stage;
    }

    public String getCrawlStatus() {
        return crawlstatus;
    }
    
    public int getAlertCount() {
        return alerts;
    }
    
    
    public String encode() {
        return JobStage.encode(stage, name);
    }

    
    public static CrawlJob determineCrawlStatus(
            JMXConnector jmxc, 
            String name, 
            JobStage stage) {
        String crawlstatus;
        int alerts;
        if (stage == JobStage.ACTIVE) {
            try {
                JobController jc = Misc.find(jmxc, name, JobController.class);
                crawlstatus = jc.getCrawlStatusString();
            } catch (RuntimeException e) {
                crawlstatus = "UNKNOWN";
            }
            
            try {
                AlertTracker at = Misc.find(jmxc, name, AlertTracker.class);
                alerts = at.getAlertCount();
            } catch (RuntimeException e) {
                alerts = -1;
            }
        } else {
            crawlstatus = null;
            alerts = -1;
        }
        CrawlJob result = new CrawlJob(name, stage, crawlstatus, alerts);
        return result;
    }
    
    
    private static CrawlJob determineCrawlStatus(
            HttpServletRequest request, 
            JMXConnector jmxc, 
            String name, 
            JobStage stage) {
        CrawlJob result = determineCrawlStatus(jmxc, name, stage);
        request.setAttribute("job", result);
        return result;
    }

    
    /**
     * Constructs a new CrawlJob based on information in a http request.  The
     * given request object must contain two parameters, "stage" and "job", 
     * that specify the CrawlJob's stage and job.  If the stage is ACTIVE,
     * then the given JMXConnector will be used to look up the job's 
     * crawl status.
     * 
     * <p>Use this when a link requires that a job be in a certain stage in
     * order to work correctly, eg editing sheets.
     * 
     * @param request  the request containing the job stage and name
     * @param jmxc   the connector to use to lookup an active job's status
     * @return   the CrawlJob
     */
    public static CrawlJob fromRequest(HttpServletRequest request, 
            JMXConnector jmxc) {
        String name = request.getParameter("job");
        if (name == null) {
            throw new IllegalStateException("Missing required parameter: job");
        }
        String stageString = request.getParameter("stage");
        if (stageString == null) {
            throw new IllegalStateException("Missing required parameter: stage");
        }
        JobStage stage = Enum.valueOf(JobStage.class, stageString);
        return determineCrawlStatus(request, jmxc, name, stage);
    }
    

    /**
     * Looks up a CrawlJob given just the job's name, not its stage.  The
     * given request object must contain a String parameter named "job".  The
     * given CrawlJobManager will be consulted to find a job with that name,
     * regardless of its stage.  If the job is active, a connection will be
     * made to that job's CrawlController to get the crawl status.
     * 
     * <p>This is useful when you're dealing with a page such as the /logs
     * pages that deal with a job whose stage might have changed.  The logs
     * links should still work when an active job becomes a completed job.
     * 
     * @param request   request containing the job name
     * @param remote    the CrawlJobManager used to look up the job
     * @return   the CrawlJob with that name
     */
    public static CrawlJob lookup(HttpServletRequest request, 
            Remote<CrawlJobManager> remote) {
        String jobName = request.getParameter("job");
        JMXConnector jmxc = remote.getJMXConnector();
        for (String s: remote.getObject().listJobs()) {
            String name = JobStage.getJobName(s);
            JobStage stage = JobStage.getJobStage(s);
            if (name.equals(jobName)) {
                return determineCrawlStatus(request, jmxc, name, stage);
            }
        }
        throw new IllegalStateException("No job named " + jobName);
    }
    
    
    public boolean hasReports() {
        return stage == JobStage.ACTIVE && (!"PREPARED".equals(crawlstatus));
    }

}
