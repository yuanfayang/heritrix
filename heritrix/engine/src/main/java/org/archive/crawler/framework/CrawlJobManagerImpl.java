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
 * CrawlJobManagerImpl.java
 *
 * Created on Jan 24, 2007
 *
 * $Id:$
 */
package org.archive.crawler.framework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.archive.crawler.Heritrix;
import org.archive.crawler.event.CrawlStatusListener;
import static org.archive.crawler.framework.JobStage.*;
import org.archive.crawler.util.LogRemoteAccessImpl;
import org.archive.io.RandomAccessInputStream;
import org.archive.openmbeans.annotations.Bean;
import org.archive.openmbeans.annotations.BeanProxy;
import org.archive.settings.DefaultCheckpointRecovery;
import org.archive.settings.ListModuleListener;
import org.archive.settings.ModuleListener;
import org.archive.settings.SheetManager;
import org.archive.settings.file.FileSheetManager;
import org.archive.settings.jmx.LoggingDynamicMBean;
import org.archive.settings.jmx.JMXModuleListener;
import org.archive.settings.jmx.JMXSheetManager;
import org.archive.settings.jmx.JMXSheetManagerImpl;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.JmxUtils;

import com.sleepycat.je.DatabaseException;

/**
 * Implementation for CrawlJobManager.  Jobs and profiles are stored in a 
 * directory called the jobsDir.  The jobs are contained as subdirectories of
 * jobDir.  These subdirectories have strange names:  The are composed of 
 * <i>both</i> the job's stage and the job's name.  Eg, a profile named "basic"
 * is stored as <code>profile-basic</code>.
 * 
 * <p>Any operation that requires a "job" parameter requires a string that 
 * represents the both stage and name; the parameter input should be the same
 * as the directory names.  See {@link JobStage.encode}.
 * 
 * @author pjack
 */
public class CrawlJobManagerImpl extends Bean implements CrawlJobManager {
    
    private static final long serialVersionUID = 3L;

    final public static String NAME = "CrawlJobManager";
    final public static String TYPE = "CrawlJobManager";
    
    
    final private static String CONTROLLER_PATH = "root:controller";

    
    final private static String CHECKPOINT_DIR_PATH = 
        CONTROLLER_PATH + ":" + CrawlController.CHECKPOINTS_DIR.getFieldName();
    
    final public static String LOGS_DIR_PATH =
        CONTROLLER_PATH + ":logger-module:dir";

    final private static Logger LOGGER = 
        Logger.getLogger(CrawlJobManagerImpl.class.getName()); 
    
    final public static String DOMAIN = "org.archive.crawler";
    
    final public static String BOOTSTRAP = "config.txt";
    
    // If null, no JMX registrations occur
    private MBeanServer server;
    
    final private File jobsDir;

    final private ObjectName oname;
    
    final private Thread heritrixThread;
    
    private HashMap<String, LogRemoteAccessImpl> logRemoteAccess;

    
    public CrawlJobManagerImpl(CrawlJobManagerConfig config) {
        super(CrawlJobManager.class);
        this.server = config.getServer();
        if (server == null) {
            throw new IllegalArgumentException("MBeanServer must not be null.");
        }
        this.jobsDir = new File(config.getJobsDirectory());
        this.jobsDir.mkdirs();
        this.oname = JMXModuleListener.nameOf(DOMAIN, NAME, this);
        
        // Any jobs that were previously running should now be marked as 
        // complete.
        for (String s: jobsDir.list()) {
            if (s.startsWith(JobStage.ACTIVE.getPrefix())) {
                this.changeState(s, JobStage.COMPLETED);
            }
        }
        
        logRemoteAccess = new HashMap<String, LogRemoteAccessImpl>();
        
        register(this, oname);
        this.heritrixThread = config.getHeritrixThread();
    }
    
    
    private String changeState(String job, JobStage newState) {
        String name = getJobName(job);
        String newJob = newState.getPrefix() + name;
        
        File existing = new File(getJobsDir(), job);
        File target = new File(getJobsDir(), newJob);
        
        if (!existing.exists()) {
            throw new IllegalStateException("Can't change " + job + " to " +
                    newJob + ", no such dir: " + existing.getAbsolutePath());
        }
        if (!existing.renameTo(target)) {
            throw new IllegalStateException("Rename of " + job + " to " +
                    newJob + " failed, reason unknown.");
        }
        
        return newJob;
    }

    
    private boolean isValidJob(String job) {
        return job.startsWith(PROFILE.getPrefix())
            || job.startsWith(READY.getPrefix()) 
            || job.startsWith(ACTIVE.getPrefix())
            || job.startsWith(COMPLETED.getPrefix());
    }
    
    
    private void validateJobName(String job) {
        if (isValidJob(job)) {
            return;
        }
        throw new IllegalArgumentException(job + " is not a valid state-name name.");
    }
    
    
    public static String getJobName(String pair) {
        int p = pair.indexOf(DELIMITER);
        if (p < 0) {
            throw new IllegalArgumentException(pair + 
                    " is not a valid state-name pair.");
        }
        return pair.substring(p + 1);
    }
    
    
    private void verifyUnique(String jobName) {
        for (String s: getJobsDir().list()) {
            if ((s.indexOf(JobStage.DELIMITER) >= 0) 
                    && JobStage.getJobName(s).equals(jobName)) {
                throw new IllegalArgumentException("Job already exists: " + s);
            }
        }        
    }

    public synchronized void copy(String origName, String copiedName) 
    throws IOException {
        validateJobName(origName);
        validateJobName(copiedName);
        if (!copiedName.startsWith(JobStage.PROFILE.getPrefix()) &&
                !copiedName.startsWith(JobStage.READY.getPrefix())) {
            throw new IllegalArgumentException("Can only copy to PROFILE or READY.");
        }
        File src = new File(getJobsDir(), origName);
        File dest = new File(getJobsDir(), copiedName);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such job/profile: " + origName);
        }

        closeSheetManagerStub(origName);

        String destName = JobStage.getJobName(copiedName);
        verifyUnique(destName);

        copy(src, dest);
    }
    
    
    private void copy(File src, File dest) throws IOException {       
        dest.mkdirs();
        
        // FIXME: Add option for only copying history DB
        // FIXME: Don't hardcode these names
        
        File srcConfig = new File(src, "config.txt");
        if (srcConfig.exists()) {
            FileUtils.copyFile(srcConfig, new File(dest, "config.txt"));
        }

        File srcSeeds = new File(src, "seeds.txt");
        if (srcSeeds.exists()) {
            FileUtils.copyFile(srcSeeds, new File(dest, "seeds.txt"));
        }
        
        File srcSheets = new File(src, "sheets");
        if (srcSheets.isDirectory()) {
            FileUtils.copyFiles(srcSheets, new File(dest, "sheets"));
        }
        
        File srcState = new File(src, "state");
        if (srcState.isDirectory()) {
            FilenameFilter ff = new FilenameFilter() {
                public boolean accept(File parent, String name) {
                    return !name.equals("je.lck");
                }
            };
            FileUtils.copyFiles(srcState, ff, new File(dest, "state"), 
                    false, true);
        }
    }

    
    private Set<ObjectName> getSheetManagers(String name) {
        String query = DOMAIN + ":*,name=" + name + ",type=" 
        + JMXSheetManager.class.getName();
        Set<ObjectName> set = JmxUtils.find(server, query);
        return set;
    }


    public synchronized ObjectName getSheetManagerStub(String job) 
    throws IOException {
        validateJobName(job);
        if (job.startsWith(ACTIVE.getPrefix())) {
            throw new IllegalArgumentException("Can't get stub for active job: "
                    + job);
        }

        String name = getJobName(job);

        Set<ObjectName> set = getSheetManagers(name);
        if (set.size() == 1) {
            return set.iterator().next();
        }
        if (set.size() > 1) {
            throw new IllegalStateException("Found more than one " +
                        "JMXSheetManager for " + job);
        }
        
        // Not already open. Open it and return object name.
        File src = new File(getJobsDir(), job);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such profile: " + job);
        }

        File bootstrap = new File(src, BOOTSTRAP);
        FileSheetManager fsm;
        try {
            fsm = new FileSheetManager(bootstrap, name, false);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }

        JMXSheetManagerImpl jmx = new JMXSheetManagerImpl(server, name, DOMAIN, fsm);
        return jmx.getObjectName();
    }

    public synchronized ObjectName getLogs(String job) throws IOException {
        if(logRemoteAccess.containsKey(job)){
            return logRemoteAccess.get(job).getObjectName();
        }
        File src = new File(getJobsDir(), job);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such job: " + job);
        }

        String logsPath = this.getFilePath(job, LOGS_DIR_PATH);
        LogRemoteAccessImpl lra = new LogRemoteAccessImpl(
                job, DOMAIN, logsPath);
        register(lra, lra.getObjectName());
        logRemoteAccess.put(job, lra);
        return lra.getObjectName();
    }

    
    public synchronized void closeSheetManagerStub(String job) {
        validateJobName(job);
        if (job.startsWith(ACTIVE.getPrefix())) {
            throw new IllegalArgumentException("Can't close SheetManager for active job: " + job);
        }
        Set<ObjectName> set = getSheetManagers(getJobName(job));
        for (ObjectName oname: set) {
            try {
                JMXSheetManager jsm = BeanProxy.proxy(server, oname, 
                        JMXSheetManager.class);
                jsm.offlineCleanup();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing stub SheetManager", e);
            }
            this.unregister(oname);
        }
    }

    public void launchJob(final String j) throws Exception {
        JobLauncher jl = new JobLauncher(j);
        jl.start();
        jl.join();
        if (jl.exception != null) {
            throw jl.exception;
        }
    }

    
    private ObjectName createJMXSheetManager(String name, SheetManager fsm) {
        JMXSheetManagerImpl jmx = new JMXSheetManagerImpl(server, 
                name, DOMAIN, fsm);
        return jmx.getObjectName();
    }
    
    
    private ObjectName findCrawlController(String name) {
        String query = DOMAIN + ":*,type=" + JobController.class.getName() 
            + ",name=" + name;
        return JmxUtils.findUnique(server, query);
    }
    
    
    private void addFinishedCallback(
            final String job, 
            final ObjectName ccName, 
            final ObjectName smName, 
            final JMXModuleListener jmxListener) {
        try {
            server.addNotificationListener(
                ccName, 
                new NotificationListener() {
                    public void handleNotification(Notification n, Object o) {
                        unregister(smName);
                        for (Object m: jmxListener.getModules()) {
                            unregister(jmxListener.nameOf(m));
                        }
                        changeState(job, COMPLETED);
                    }
                }, new NotificationFilter() {
                    private static final long serialVersionUID = 1L;
                    public boolean isNotificationEnabled(Notification n) {
                        return n.getType().startsWith("FINISHED");
                    }
                }, null);
        } catch (InstanceNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    
    
    private ObjectName register(Object o, ObjectName oname) {
        LoggingDynamicMBean.register(server, o, oname);
        return oname;
    }
    
    
    private void unregister(ObjectName oname) {
        try {
            server.unregisterMBean(oname);  
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public synchronized ObjectName getObjectName() {
        return this.oname;
    }

    
    private File getJobsDir() {
        return jobsDir;
    }
    
    
    public synchronized String[] listCheckpoints(String job) {
        String path = getFilePath(job, "root:controller:checkpoints-dir");
        File checkpointsDir = new File(path);
        File[] files = checkpointsDir.listFiles();
        if (files == null) {
            return new String[0];
        }
        ArrayList<String> checkpoints = new ArrayList<String>();
        for (File f: checkpointsDir.listFiles()) {
            if (f.isDirectory()) {
                checkpoints.add(f.getName());
            }
        }
        String[] arr = checkpoints.toArray(new String[checkpoints.size()]);
        return arr;
    }
    
    
    public synchronized void recoverCheckpoint(
            String oldJob,
            String newJob,
            String checkpointName,
            String[] oldPaths, 
            String[] newPaths) {
        CheckpointLauncher cl = new CheckpointLauncher(
                oldJob,
                newJob,
                checkpointName,
                oldPaths,
                newPaths);
        cl.start();
        try {
            cl.join();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (cl.exception != null) {
            throw cl.exception;
        }
    }

    public synchronized void close() {
        String query = DOMAIN + ":*,type=" + JobController.class.getName();
        Set<ObjectName> jobs = JmxUtils.find(server, query);
        if (!jobs.isEmpty()) {
            throw new IllegalStateException("Cannot close CrawlJobManager " + 
                    "when jobs are still active.");
        }
        unregister(oname);
        // Clean up LogRemoteAccessors that may have been opened
        for(String key : logRemoteAccess.keySet()){
            LogRemoteAccessImpl lra = logRemoteAccess.get(key);
            unregister(lra.getObjectName());
            logRemoteAccess.remove(key);
        }
        if (heritrixThread != null) {
            heritrixThread.interrupt();
        }
    }


    public synchronized void systemExit() {
        System.exit(1);
    }

    
    public String[] listJobs() {
        String[] r = jobsDir.list();
        ArrayList<String> result = new ArrayList<String>(r.length);
        for (String f: r) {
            if (isValidJob(f)) {
                result.add(f);
            }
        }
        return result.toArray(new String[result.size()]);
    }


    
    public synchronized String readFile(String job, String settingsPath, 
            String fileName, long startPos, int length) throws IOException {
        String path = getFilePath(job, settingsPath);
        File f = new File(path);
        byte[] buf;
        RandomAccessInputStream raf = null;
        try {
            raf = new RandomAccessInputStream(f);
            raf.position(startPos);
            length = Math.min(length, (int)(f.length() - startPos));
            buf = new byte[length];
            IoUtils.readFully(raf, buf);
        } finally {
            IoUtils.close(raf);
        }
        
        ByteArrayInputStream binp = new ByteArrayInputStream(buf);
        InputStreamReader isr = new InputStreamReader(binp);
        return IoUtils.readFullyAsString(isr);
    }

    public synchronized void writeLines(String job, 
            String settingsPath,
            String fileName,
            int startLine, int lineCount, String lines) throws IOException {
        String path = getFilePath(job, settingsPath);
        File f = new File(path);
        if (fileName != null) {
            f = new File(f, fileName);
        }
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be positive.");
        }

        File tempFile = new File(f.getAbsolutePath() + ".temp");
        
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            BufferedReader linesBr = new BufferedReader(new StringReader(lines));
            br = new BufferedReader(new FileReader(f));
            bw = new BufferedWriter(new FileWriter(tempFile));
            for (int i = 0; i < startLine; i++) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                bw.write(line);
                bw.write('\n');
            }
            
            int count = 0;
            for (String s = linesBr.readLine(); s != null; s = linesBr.readLine()) {
                count++;
                br.readLine();
                bw.write(s);
                bw.write('\n');
            }
            
            for (int i = count; i < lineCount; i++) {
                br.readLine();
            }
            
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                bw.write(s);
                bw.write('\n');
            }            
        } finally {
            IoUtils.close(br);
            IoUtils.close(bw);
        }
        tempFile.renameTo(f);
    }
    
    public String getHeritrixVersion(){
        return Heritrix.getVersion();
    }


    public String help() {
        return 
            "Any operation that takes a 'job' parameter requires that you\n" +
            "specify *both* the job stage *and* the job name.  Eg, to\n" +
            "operate on a profile named 'basic', you specify 'profile-basic'.\n" +
            "To copy a profile named basic to a ready job named foo,\n" +
            "You would invoke copy(\"profile-basic\", \"ready-foo\").";
    }

    
    public synchronized String getFilePath(String job, String settingPath) {
        String jobName = JobStage.getJobName(job);
        JobStage stage = JobStage.getJobStage(job);

        ObjectName oname;
        if (stage == JobStage.ACTIVE) {
            String query = DOMAIN + ":*,type=" + JMXSheetManager.class.getName() 
                + ",name=" + jobName;
            oname = JmxUtils.findUnique(server, query);
        } else {
            try {
                oname = getSheetManagerStub(job);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        
        try {
            JMXSheetManager mgr = BeanProxy.proxy(server, oname, JMXSheetManager.class);
            return mgr.getFilePath(settingPath);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    
    public synchronized String[] listFiles(String job, 
            String settingsPath, 
            String regex) {
        Pattern pattern = Pattern.compile(regex);
        String path = getFilePath(job, settingsPath);
        File dir = new File(path);
        List<String> result = new ArrayList<String>();
        for (String s: dir.list()) {
            if (pattern.matcher(s).find()) {
                result.add(s);
            }
        }
        return result.toArray(new String[result.size()]);
    }


    public synchronized void deleteJob(String job) {
        validateJobName(job);
        File f = new File(this.jobsDir, job);
        if (f.exists()) {
            if (!FileUtils.deleteDir(f)) {
                throw new IllegalStateException("Could not delete job.");
            }
        } else {
            throw new IllegalStateException("No such job: " + job);
        }
    }

    
    public synchronized long getFileSize(String job, String settingsPath) {
        String filePath = getFilePath(job, settingsPath);
        return new File(filePath).length();
    }


    class JobLauncher extends Thread {
        
        
        final private String j;
        public Exception exception;
        
        public JobLauncher(String j) {
            super(new AlertThreadGroup(j), j);
            this.j = j;
        }
    
    
        private void doLaunch() throws Exception {
            if (!j.startsWith(READY.getPrefix())) {
                throw new IllegalArgumentException("Can't launch " + j);
            }
            
            closeSheetManagerStub(j);
    
            final String job = changeState(j, ACTIVE);
            File dest = new File(getJobsDir(), job);
            File bootstrap = new File(dest, CrawlJobManagerImpl.BOOTSTRAP);
            FileSheetManager fsm;
            final String name = getJobName(job);
            final JMXModuleListener jmxListener = new JMXModuleListener(
                    CrawlJobManagerImpl.DOMAIN, name, server);

            List<ModuleListener> list = new ArrayList<ModuleListener>();
            list.add(jmxListener);
            list.add(ListModuleListener.make(CrawlStatusListener.class));
            fsm = new FileSheetManager(bootstrap, name, true, list);

            final ObjectName smName = createJMXSheetManager(name, fsm);
            final ObjectName ccName = findCrawlController(name);
            addFinishedCallback(job, ccName, smName, jmxListener);           
        }


        public void run() {
            synchronized (CrawlJobManagerImpl.this) {
                try {
                    doLaunch();
                } catch (Exception e) {
                    this.exception = e;
                }
            }
        }
    
    }

    
    class CheckpointLauncher extends Thread {
        
        
        private String oldJob;
        private String newJob;
        private String[] oldPaths;
        private String[] newPaths;
        private String checkpointName;
        public RuntimeException exception;
        
        
        public CheckpointLauncher(
                String oldJob,
                String newJob,
                String checkpointName,
                String[] oldPaths, 
                String[] newPaths) {
            super(new AlertThreadGroup(newJob), newJob);
            this.oldJob = oldJob;
            this.newJob = newJob;
            this.checkpointName = checkpointName;
            this.oldPaths = oldPaths;
            this.newPaths = newPaths;
        }


        private void doCheckpointRecover() {
            validateJobName(oldJob);
            validateJobName(newJob);
            File src = new File(getJobsDir(), oldJob);
            if (!src.exists()) {
                throw new IllegalArgumentException("No such job: " + oldJob);
            }

            if (!newJob.startsWith(JobStage.ACTIVE.getPrefix())) {
                throw  new IllegalArgumentException(
                        "Must specify active-name for recovered job.");
            }

            String newName = JobStage.getJobName(newJob);
            verifyUnique(JobStage.getJobName(newName));

            File dest = new File(getJobsDir(), newJob);
            dest.mkdir();
            
            String cpPath = getFilePath(oldJob, CHECKPOINT_DIR_PATH);
            File checkpointDir = new File(new File(cpPath), checkpointName);
            if (!checkpointDir.isDirectory()) {
                throw new IllegalArgumentException("Not a dir: " + cpPath);
            }

            if (oldPaths.length != newPaths.length) {
                throw new IllegalArgumentException(
                        "oldPaths and newPaths must be parallel.");
            }

            // Old job was "active" when it was checkpointed, so we need to 
            // translate its old "active" path to the new job's directory.
            String oldActive = JobStage.encode(JobStage.ACTIVE, 
                    JobStage.getJobName(oldJob));
            
            DefaultCheckpointRecovery cr = new DefaultCheckpointRecovery();
            cr.getFileTranslations().put(new File(getJobsDir(), oldActive).getAbsolutePath(), 
                    dest.getAbsolutePath());
            for (int i = 0; i < oldPaths.length; i++) {
                cr.getFileTranslations().put(oldPaths[i], newPaths[i]);
                new File(newPaths[i]).mkdirs();
            }

            SheetManager mgr;
            try {
                mgr = org.archive.settings.Checkpointer.recover(checkpointDir, cr);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            JMXModuleListener jml = JMXModuleListener.get(mgr);
            jml.setServer(server, JobStage.getJobName(newJob));

            ObjectName smName = createJMXSheetManager(newName, mgr);
            ObjectName ccName = findCrawlController(newName);
            addFinishedCallback(newJob, ccName, smName, jml);
        }

        
        public void run() {
            synchronized (CrawlJobManagerImpl.this) {
                try {
                    doCheckpointRecover();
                } catch (RuntimeException e) {
                    this.exception = e;
                }
            }
        }
        
    }
}