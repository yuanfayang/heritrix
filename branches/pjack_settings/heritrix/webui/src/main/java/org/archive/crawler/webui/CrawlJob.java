package org.archive.crawler.webui;

import java.util.Set;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJobManager;
import org.archive.crawler.framework.JobController;
import org.archive.crawler.util.LogRemoteAccess;

/**
 * Represents a crawl job (a profile, an active job or a completed
 * job) on a crawler.
 * 
 * @author Kristinn
 */
public class CrawlJob {
    public enum State{
        // TODO: Consider pending jobs
        PROFILE,
        PENDING, // An active job whose crawl status is 'Prepared'
        ACTIVE,
        COMPLETED
    }
    
    String name;
    Crawler crawler; // TODO: Do we need to keep this?
    State state;
    String crawlstatus;
    
    public CrawlJob(String name, Crawler crawler){
        this.name = name;
        this.crawler = crawler;
        JMXConnector jmxc = crawler.connect();
        CrawlJobManager manager = Remote.make(
                jmxc, 
                crawler.getObjectName(), 
                CrawlJobManager.class).getObject();

        // Determine job state 
        // TODO: This might be better handled on the server side
        String[] profiles = manager.listProfiles();
        for(String p : profiles){
            if(p.equals(name)){
                state = State.PROFILE;
            }
        }
        if(state == null){
            String[] jobs = manager.listActiveJobs();
            for(String j : jobs){
                if(j.equals(name)){
                    state = State.ACTIVE;
                }
            }
        }
        if(state == null){
            String[] jobs = manager.listCompletedJobs();
            for(String j : jobs){
                if(j.equals(name)){
                    state = State.COMPLETED;
                }
            }
        }
        if(state==State.ACTIVE){
            determineCrawlStatus(jmxc);
        }
        
        Misc.close(jmxc);
    }
    
    public CrawlJob(String name, Crawler crawler, State state){
        this.name = name;
        this.crawler = crawler;
        this.state = state;
        
        if(state==State.ACTIVE){
            // Try to access the JobController and StatisticsTracking beans
            // on the crawler to populate more detailed info.
            JMXConnector jmxc = crawler.connect();
            determineCrawlStatus(jmxc);
            Misc.close(jmxc);
        }
    }
    
    private void determineCrawlStatus(JMXConnector jmxc){
        JobController controller = findJobController(jmxc,name);
        if(controller == null){
            throw new IllegalStateException("Failed to find " +
                    "JobController for job " + name);
        }
        crawlstatus = controller.getCrawlStatusString();
    }
    
    /**
     * Finds an appropriate LogRemoteAccess object.
     * @param jmxc JMXConnector to the remote machine
     * @param job The name of the job whose logs we are interested in
     * @return The LogRemoteAccess object or null if none is found.
     * @throws IllegalStateException If there are multiple 
     *      {@link LogRemoteAccess} objects found. 
     */
    private static JobController findJobController(JMXConnector jmxc, String job){
        String query = "org.archive.crawler:*," + 
        "name=" + job + 
        ",type=" + JobController.class.getName();

        Set<ObjectName> set = Misc.find(jmxc, query);
        if (set.size() == 1) {
            ObjectName name = set.iterator().next();
            return Remote.make(jmxc, name, JobController.class).getObject();
        } else if(set.size() > 1) {
            // Error
            throw new IllegalStateException("Found multiple JobControllers for job " + job);
        }
        return null;
    }
    
    public String getName(){
        return name;
    }

    public State getState(){
        if(getCrawlStatus().equals(CrawlController.State.PREPARED.toString())){
            // Handle this border case. 'Prepared' jobs may not have all
            // reports ready etc. so it is important to distingush them.
            return State.PENDING;
        }
        return state;
    }

    
    public String getCrawlStatus(){
        // TODO: Completed jobs also have more specific crawl state (i.e. how they ended).
        if(state==State.ACTIVE){
            return crawlstatus;
        } else {
            return state.toString();
        }
    }
    
}
