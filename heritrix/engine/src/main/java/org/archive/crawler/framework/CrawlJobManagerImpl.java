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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.archive.crawler.Heritrix;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.util.LogRemoteAccessImpl;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.DefaultCheckpointRecovery;
import org.archive.settings.ListModuleListener;
import org.archive.settings.ModuleListener;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.file.FileSheetManager;
import org.archive.settings.jmx.JMXModuleListener;
import org.archive.settings.jmx.JMXSheetManagerImpl;
import org.archive.settings.path.PathValidator;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;

import com.sleepycat.je.DatabaseException;

/**
 * @author pjack
 *
 */
public class CrawlJobManagerImpl extends Bean implements CrawlJobManager {
    
    final public static String NAME = "CrawlJobManager";
    final public static String TYPE = "CrawlJobManager";
    
    
    final private static Logger LOGGER = 
        Logger.getLogger(CrawlJobManagerImpl.class.getName()); 
    
    final public static String DOMAIN = "org.archive.crawler";
    
    final public static String BOOTSTRAP = "config.txt";
    
    // If null, no JMX registrations occur
    private MBeanServer server;
    
    final private File jobsDir;
    
    final private File profilesDir;
    
    final private Map<String,CrawlController> jobs;

    final private ObjectName oname;
    
    final private Thread heritrixThread;
    
    private HashMap<String, LogRemoteAccessImpl> logRemoteAccess;
    
    public CrawlJobManagerImpl(CrawlJobManagerConfig config) {
        super(CrawlJobManager.class);
        this.server = config.getServer();
        if (server == null) {
            throw new IllegalArgumentException("MBeanServer must not be null.");
        }
        this.profilesDir = new File(config.getProfilesDirectory());
        if (!profilesDir.isDirectory()) {
            throw new IllegalArgumentException("Profiles directory unreadable: " 
                    + profilesDir.getAbsolutePath());
        }
        this.jobsDir = new File(config.getJobsDirectory());
        this.jobs = new HashMap<String,CrawlController>();
        this.oname = JMXModuleListener.nameOf(DOMAIN, NAME, this);
        
        logRemoteAccess = new HashMap<String, LogRemoteAccessImpl>();
        
        register(this, oname);
        this.heritrixThread = config.getHeritrixThread();
    }
    

    public void copyProfile(String origName, String copiedName) 
    throws IOException {
        File src = new File(getProfilesDir(), origName);
        File dest = new File(getProfilesDir(), copiedName);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such profile: " + origName);
        }
        
        if (dest.exists()) {
            throw new IllegalArgumentException("Profile already exists: " + 
                    copiedName);
        }
        
        FileUtils.copyFiles(src, dest);
    }

    
    public void openProfile(String profile) throws IOException {
        File src = new File(getProfilesDir(), profile);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such profile: " + profile);
        }

        File bootstrap = new File(src, BOOTSTRAP);
        FileSheetManager fsm;
        try {
            fsm = new FileSheetManager(bootstrap, false);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }

        JMXSheetManagerImpl jmx = new JMXSheetManagerImpl(fsm);
        ObjectName name = JMXModuleListener.nameOf(DOMAIN, profile, jmx);
        register(jmx, name);
    }

    public String getLogs(String job) throws IOException {
        if(logRemoteAccess.containsKey(job)){
            return logRemoteAccess.get(job).getObjectName().getCanonicalName();
        }
        File src = new File(getJobsDir(), job);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such job: " + job);
        }

        LogRemoteAccessImpl lra = new LogRemoteAccessImpl(
                job, DOMAIN,
                src.getAbsolutePath() + File.separator + "logs"); // FIXME: Stop assuming location of logs
        register(lra, lra.getObjectName());
        logRemoteAccess.put(job, lra);
        return lra.getObjectName().getCanonicalName();
    }

    
    public void closeProfile(String profile) {
        File src = new File(getProfilesDir(), profile);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such profile: " + profile);
        }
        
        ObjectName sm = name(profile, "SheetManager");
        unregister(sm);
    }
    
    
    public void launchProfile(String profile, final String job) 
    throws IOException {
        File src = new File(getProfilesDir(), profile);
        File dest = new File(getJobsDir(), job);
        
        if (!src.exists()) {
            throw new IllegalArgumentException("No such profile: " + profile);
        }
        
        if (dest.exists()) {
            throw new IllegalArgumentException("Job already exists: " + job);
        }
        
        FileUtils.copyFiles(src, dest);
        
        File bootstrap = new File(dest, BOOTSTRAP);
        FileSheetManager fsm;
        final JMXModuleListener jmxListener = new JMXModuleListener(DOMAIN, job, server);
        try {
            List<ModuleListener> list = new ArrayList<ModuleListener>();
            list.add(jmxListener);
            list.add(ListModuleListener.make(CrawlStatusListener.class));
            fsm = new FileSheetManager(bootstrap, true, list);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }

        JMXSheetManagerImpl jmx = new JMXSheetManagerImpl(fsm);
        final ObjectName smName = jmxListener.nameOf(jmx); // register(job, "SheetManager", jmx);
        try {
            server.registerMBean(jmx, smName);
        } catch (Exception e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
        
        // Find the crawlcontroller.
        // FIXME: Do a JMX query here instead (that way we don't have to 
        // worry about something being at a particular path)
        Sheet sheet = fsm.getDefault();
        Object o = PathValidator.validate(sheet, "root:controller");
        if (!(o instanceof CrawlController)) {
            LOGGER.warning("Could not find CrawlController in job named " 
                    + job + " at expected path (root.controller).");
            return;
        }
        
        CrawlController cc = (CrawlController)o;
        
        final ObjectName ccName = jmxListener.nameOf(cc);

        try {
            server.addNotificationListener(
                ccName, 
                new NotificationListener() {
                    public void handleNotification(Notification n, Object o) {
                        unregister(smName);
                        for (Object m: jmxListener.getModules()) {
                            unregister(jmxListener.nameOf(m));
                        }
                        jobs.remove(job);
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
        
        jobs.put(job, cc);
    }
    
    
    private ObjectName name(String name, String type) {
        try {
            Hashtable<String,String> ht = new Hashtable<String,String>();
            ht.put("name", name);
            ht.put("type", type);
            return new ObjectName(DOMAIN, ht);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    
    private ObjectName register(Object o, ObjectName oname) {
        try {
            server.registerMBean(o, oname);
            return oname;
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalStateException(e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    private void unregister(ObjectName oname) {
        try {
            server.unregisterMBean(oname);  
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    
    public String[] listAllJobs() {
        File jobsDir = getJobsDir();
        return jobsDir.list();
    }


    public String[] listProfiles() {
        File profDir = getProfilesDir();
        return profDir.list();
    }

    
    public ObjectName getObjectName() {
        return this.oname;
    }

    private File getProfilesDir() {
        return profilesDir;
    }
    
    
    private File getJobsDir() {
        return jobsDir;
    }


    public String[] listCheckpoints() {
        ArrayList<String> checkpoints = new ArrayList<String>();
        for (File f: getJobsDir().listFiles()) {
            if (f.isDirectory()) {
                File cp = new File(f, "checkpoints");
                if (cp.exists()) {
                    for (File d: cp.listFiles()) {
                        if (d.isDirectory()) {
                            checkpoints.add(d.getAbsolutePath());
                        }
                    }
                }
            }
        }
        String[] arr = checkpoints.toArray(new String[checkpoints.size()]);
        return arr;
    }
    
    
    public void recoverCheckpoint(String cpPath, 
            String[] oldPaths, 
            String[] newPaths) 
    throws IOException {
        if (cpPath.startsWith(".")) {
            throw new IllegalArgumentException("Illegal checkpoint: " + cpPath);
        }
        File checkpointDir = new File(cpPath);
        if (!checkpointDir.isDirectory()) {
            throw new IllegalArgumentException("Not a dir: " + cpPath);
        }
        
        if (oldPaths.length != newPaths.length) {
            throw new IllegalArgumentException(
                    "oldPaths and newPaths must be parallel.");
        }
        
        DefaultCheckpointRecovery cr = new DefaultCheckpointRecovery();
        for (int i = 0; i < oldPaths.length; i++) {
            cr.getFileTranslations().put(oldPaths[i], newPaths[i]);
            new File(newPaths[i]).mkdirs();
        }

        SheetManager mgr = org.archive.settings.Checkpointer.recover(checkpointDir, cr);
        JMXModuleListener jml = JMXModuleListener.get(mgr);
        jml.setServer(server);
    }

    public void close() {
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


    public void systemExit() {
        System.exit(1);
    }


    public String[] listActiveJobs() {
        return jobs.keySet().toArray(new String[0]);
    }


    public String[] listCompletedJobs() {
        Set<String> result = new HashSet<String>();
        result.addAll(Arrays.asList(jobsDir.list()));
        result.removeAll(jobs.keySet());
        return result.toArray(new String[0]);
    }

    
    public synchronized String readLines(String fileName, int startLine, int lineCount) 
    throws IOException {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be positive.");
        }
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount must be positive.");
        }
        if (lineCount > 10000) {
            throw new IllegalArgumentException("lineCount exceeds max (10000)");
        }

        BufferedReader br = null;
        StringBuilder result = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(fileName));
            for (int i = 0; i < startLine; i++) {
                String line = br.readLine();
                if (line == null) {
                    return "";
                }
            }
            for (int i = 0; i < lineCount; i++) {
                String line = br.readLine();
                if (line == null) {
                    return result.toString();
                }
                result.append(line).append('\n');
            }
            return result.toString();
        } finally {
            IoUtils.close(br);
        }
    }

    public synchronized void writeLines(String fileName, int startLine, 
            int lineCount, String lines)
    throws IOException {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be positive.");
        }

        String tempFilename = fileName + ".temp";
        
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            BufferedReader linesBr = new BufferedReader(new StringReader(lines));
            br = new BufferedReader(new FileReader(fileName));
            bw = new BufferedWriter(new FileWriter(tempFilename));
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
        new File(tempFilename).renameTo(new File(fileName));
    }
    
    public String getHeritrixVersion(){
        return Heritrix.getVersion();
    }
}
