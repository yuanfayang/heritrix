/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
 
package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.archive.spring.PathSharingContext;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.validation.Errors;

/**
 * CrawlJob represents a crawl configuration, including its 
 * configuration files, instantiated/running ApplicationContext, and 
 * disk output, potentially across multiple runs.
 * 
 * CrawlJob provides convenience methods for an administrative 
 * interface to assemble, launch, monitor, and manage crawls. 
 * 
 * @contributor gojomo
 */
public class CrawlJob implements Comparable<CrawlJob>{
    File primaryConfig; 
    PathSharingContext ac; 
    int launchCount; 
    DateTime lastLaunch;
    
    DateTime xmlOkAt = new DateTime(0L);
    Logger jobLogger;
    
    public CrawlJob(File cxml) {
        primaryConfig = cxml; 
        scanJobLog(); 
    }
    
    public File getPrimaryConfig() {
        return primaryConfig;
    }
    public File getJobDir() {
        return getPrimaryConfig().getParentFile();
    }
    public String getShortName() {
        return getJobDir().getName();
    }
    public File getJobLog() {
        return new File(getJobDir(),"job.log");
    }
    
    public PathSharingContext getJobContext() {
        if(ac==null) {
            instantiateContainer();
        }
        return ac; 
    }

    
    /**
     * Get a logger to a distinguished file, job.log in the job's
     * directory, into which job-specific events may be reported.
     * 
     * @return Logger writing to the job-specific log
     */
    public Logger getJobLogger() {
        if(jobLogger == null) {
            jobLogger = Logger.getLogger(getShortName());
            try {
                Handler h = new FileHandler(getJobLog().getAbsolutePath(),true);
                h.setFormatter(new JobLogFormatter());
                jobLogger.addHandler(h);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return jobLogger;
    }
    
    public DateTime getLastLaunch() {
        return lastLaunch;
    }
    public int getLaunchCount() {
        return launchCount;
    }
    /**
     * Refresh knowledge of total launched and last launch by scanning
     * the job.log. 
     */
    protected void scanJobLog() {
        File jobLog = getJobLog();
        launchCount = 0; 
        if(!jobLog.exists()) return;
        
        try {
            LineIterator lines = FileUtils.lineIterator(jobLog);
            Pattern launchLine = Pattern.compile("(\\S+) (\\S+) LAUNCHED");
            while(lines.hasNext()) {
                String line = lines.nextLine();
                Matcher m = launchLine.matcher(line);
                if(m.matches()) {
                    launchCount++;
                    lastLaunch = new DateTime(m.group(1));
                }
            }
            LineIterator.closeQuietly(lines);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
    /**
     * Is this job a 'profile' (or template), meaning it may be editted
     * or copied to another jobs, but should not be launched. Profiles
     * are marked with the convention that their short name 
     * (job directory name) begins "profile-".
     * 
     * @return true if this job is a 'profile'
     */
    public boolean isProfile() {
        return primaryConfig.getName().startsWith("profile-");
    }

//
//
//

    public void writeHtmlTo(PrintWriter pw) {
        writeHtmlTo(pw,"./");
    }
    public void writeHtmlTo(PrintWriter pw, String uriPrefix) {
        pw.println("<span class='job'>");
        pw.println("<a href='"+uriPrefix+getShortName()+"'>"+getShortName()+"</a>");
        if(isProfile()) {
            pw.println("(profile)");
        }
        pw.println(" " + getLaunchCount() + " launches");
        pw.println("<br/><span style='color:#666'>");
        pw.println(getPrimaryConfig());
        pw.println("</span><br/>");
        if(lastLaunch!=null) {
            pw.println("(last at "+lastLaunch+")");
        }
        pw.println("</span>");
    }

    //
    // Is the primary XML config minimally well-formed? 
    //

    public void checkXML() {
        // TODO: suppress check if XML unchanged? job.log when XML changed? 
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            DateTime testTime = new DateTime(getPrimaryConfig().lastModified());
            docBuilder.parse(getPrimaryConfig());
            // TODO: check for other minimal requirements, like
            // presence of a few key components (CrawlController etc.)? 
            xmlOkAt = testTime; 
        } catch (Exception e) {
            xmlOkAt = new DateTime(0L);
        }
    }
    
    public boolean isXmlOk() {
        return xmlOkAt.getMillis() >= getPrimaryConfig().lastModified();
    }
    
    //
    // Can the configuration yield an assembled ApplicationContext? 
    //
    
    public void instantiateContainer() {
        checkXML(); 
        if(ac==null) {
            try {
                ac = new PathSharingContext(primaryConfig.getAbsolutePath());
            } catch (BeansException be) {
                getJobLogger().log(Level.SEVERE,be.getMessage(),be);
            }
        }
    }
    
    public boolean isContainerOk() {
        return ac!=null;
    }
    
    //
    // Does the assembled ApplicationContext self-validate? 
    //
    
    public void validateConfiguration() {
        instantiateContainer();
        // TODO: collect, report errors
        ac.validate();
    }

    public boolean isContainerValidated() {
        if(ac==null) {
            return false;
        }
        Errors errs = ac.getErrors(); 
        return errs != null && errs.getErrorCount() == 0;
    }
    
    //
    // Valid job lifecycle operations
    //
    
    /**
     * Launch a crawl into 'running' status, assembling if necessary. 
     * 
     * (Note the crawl may have been configured to start in a 'paused'
     * state.) 
     */
    public void launch() {
        validateConfiguration();
        if (isProfile()) {
            throw new IllegalArgumentException("Can't launch " + this);
        }
        //final String job = changeState(j, ACTIVE);
        
        // this temporary thread ensures all crawl-created threads
        // land in the AlertThreadGroup, to assist crawl-wide 
        // logging/alerting
        Thread launcher = new Thread(new AlertThreadGroup(getShortName()), getShortName()) {
            public void run() {
                ac.start();
            }
        };
        launcher.start();
        
        try {
            launcher.join();
        } catch (InterruptedException e) {
            // do nothing
        }
        
        getCrawlController().requestCrawlStart();
        getJobLogger().log(Level.INFO,"LAUNCHED");
        scanJobLog();
    }

    public int compareTo(CrawlJob o) {
        // prefer reverse-chronological ordering
        return -((Long)getLastActivityTime()).compareTo(o.getLastActivityTime());
    }
    
    public long getLastActivityTime() {
        return Math.max(getPrimaryConfig().lastModified(), getJobLog().lastModified());
    }
    
    public boolean isRunning() {
        return this.ac != null && this.ac.isRunning();
    }

    public CrawlControllerImpl getCrawlController() {
        if(ac==null) {
            return null;
        }
        return (CrawlControllerImpl) ac.getBean("crawlController");
    }

    public boolean isPausable() {
        CrawlControllerImpl cc = getCrawlController(); 
        if(cc==null) {
            return false;
        }
        return !cc.isPaused(); 
    }
    
    public boolean isUnpausable() {
        CrawlControllerImpl cc = getCrawlController(); 
        if(cc==null) {
            return false;
        }
        return cc.isPaused() || cc.isPausing();
    }
    
    /**
     * Ensure a fresh start for any configuration changes or relaunches,
     * by stopping and discarding an existing ApplicationContext.
     */
    public void reset() {
        if(ac!=null) {
            if(ac.isRunning()) {
                ac.stop(); 
            }
            ac = null;
        }
        xmlOkAt = new DateTime(0); 
    }

    /**
     * Formatter for job.log
     */
    public class JobLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb
              .append(new DateTime(record.getMillis()))
              .append(" ")
              .append(record.getLevel())
              .append(" ")
              .append(record.getMessage())
              .append("\n");
            return  sb.toString();
        }
    }
}