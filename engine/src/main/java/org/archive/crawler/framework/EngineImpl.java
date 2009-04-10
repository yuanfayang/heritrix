/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * EngineImpl.java
 *
 * Created on Jan 24, 2007
 *
 * $Id:$
 */
package org.archive.crawler.framework;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.archive.util.ArchiveUtils;

/**
 * Implementation for Engine.  Jobs and profiles are stored in a 
 * directory called the jobsDir.  The jobs are contained as subdirectories of
 * jobDir.  
 * 
 * @contributor pjack
 * @contributor gojomo
 */
// TODO: rename to Engine; no separate interface necessary
public class EngineImpl {
    private static final long serialVersionUID = 4L;

    final public static String LOGS_DIR_NAME = "logs subdirectory";
    final public static String REPORTS_DIR_NAME = "reports subdirectory";

    final private static Logger LOGGER = 
        Logger.getLogger(EngineImpl.class.getName()); 
        
    /** directory where job directores are expected */
    protected File jobsDir;
    /** map of job short names -> CrawlJob instances */ 
    protected HashMap<String,CrawlJob> jobConfigs = new HashMap<String,CrawlJob>();
       
    public EngineImpl(File jobsDir) {
        this.jobsDir = jobsDir;
        this.jobsDir.mkdirs();
        
        findJobConfigs();
        // TODO: cleanup any cruft from improperly ended jobs 
    }
    
    /**
     * Find all job configurations in the usual place -- subdirectories
     * of the jobs directory with files ending '.cxml'.
     */
    protected void findJobConfigs() {
        // TODO: allow other places/paths to be scanned/added as well
        for (File dir : jobsDir.listFiles(new FileFilter(){
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }})) {
            for (File cxml : dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".cxml");
                }})) {
                try {
                    CrawlJob cj = new CrawlJob(cxml);
                    if(!jobConfigs.containsKey(cj.getShortName())) {
                        jobConfigs.put(cj.getShortName(),cj);
                    }
                } catch (IllegalArgumentException iae) {
                    LOGGER.log(Level.WARNING,"bad cxml: "+cxml,iae);
                }
            }
        }
    }
    
    public Map<String,CrawlJob> getJobConfigs() {
        return jobConfigs;
    }
   
    
    
    /**
     * Copy a job to a new location, possibly making a job
     * a profile or a profile a runnable job. 
     * 
     * @param orig CrawlJob representing source
     * @param destDir File location destination
     * @param asProfile true if destination should become a profile
     * @throws IOException 
     */
    public synchronized void copy(CrawlJob orig, File destDir, boolean asProfile) 
    throws IOException {
        destDir.mkdirs();
        if(destDir.list().length>0) {
            throw new IOException("destination dir not empty");
        }
        File srcDir = orig.getPrimaryConfig().getParentFile();

        // FIXME: Add option for only copying history DB
        // FIXME: Don't hardcode these names
        // FIXME: (?) copy any referenced file (ConfigFile/ConfigPath),
        // even outside the job directory? 
       
        // copy all simple files except the 'job.log' and its '.lck' (if any)
        FileUtils.copyDirectory(srcDir, destDir, 
                FileFilterUtils.andFileFilter(
                        FileFilterUtils.fileFileFilter(),
                        FileFilterUtils.notFileFilter(
                                FileFilterUtils.prefixFileFilter("job.log"))));
        
        // ...and all contents of 'resources' subdir...
        File srcResources = new File(srcDir, "resources");
        if (srcResources.isDirectory()) {
            FileUtils.copyDirectory(srcResources, new File(destDir, "resources"));
        }
        
        File newPrimaryConfig = new File(destDir, orig.getPrimaryConfig().getName());
        if(asProfile) {
            if(!orig.isProfile()) {
                // rename cxml to have 'profile-' prefix
                FileUtils.moveFile(
                        newPrimaryConfig, 
                        new File(destDir, "profile-"+newPrimaryConfig.getName()));
            }
        } else {
            if(orig.isProfile()) {
                // rename cxml to remove 'profile-' prefix
                FileUtils.moveFile(
                        newPrimaryConfig, 
                        new File(destDir, newPrimaryConfig.getName().substring(8)));
            }
        }
        findJobConfigs();
    }
    
    /**
     * Copy a job to a new location, possibly making a job
     * a profile or a profile a runnable job. 
     * 
     * @param cj CrawlJob representing source
     * @param copyTo String location destination; interpreted relative to jobsDir
     * @param asProfile true if destination should become a profile
     * @throws IOException 
     */
    public void copy(CrawlJob cj, String copyTo, boolean asProfile) throws IOException {
        File dest = new File(copyTo);
        if(!dest.isAbsolute()) {
            dest = new File(jobsDir,copyTo);
        }
        copy(cj,dest,asProfile);
    }
    
    public String getHeritrixVersion(){
        return ArchiveUtils.VERSION;
    }
    
    public synchronized void deleteJob(CrawlJob job) throws IOException {
        FileUtils.deleteDirectory(job.getJobDir());
    }

    public void requestLaunch(String jobPath) {
        // TODO: find matching CrawlJob, launch, report errors if any
    }

    public CrawlJob getJob(String shortName) {
        return jobConfigs.get(shortName); 
    }

    public File getJobsDir() {
        return jobsDir;
    }
    
    public String heapReport() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        StringBuilder sb = new StringBuilder(64); 
        sb
         .append((totalMemory-freeMemory)/1024)
         .append(" KiB used; ")
         .append(totalMemory/1024)
         .append(" KiB current heap; ")
         .append(maxMemory/1024)
         .append(" KiB max heap");
         return sb.toString(); 
    }

   
}