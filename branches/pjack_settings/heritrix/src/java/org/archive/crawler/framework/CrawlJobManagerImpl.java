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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.archive.crawler.event.CrawlStatusAdapter;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.ListModuleListener;
import org.archive.settings.ModuleListener;
import org.archive.settings.Sheet;
import org.archive.settings.file.Checkpointable;
import org.archive.settings.file.FileSheetManager;
import org.archive.settings.jmx.JMXModuleListener;
import org.archive.settings.jmx.JMXSheetManager;
import org.archive.settings.path.PathValidator;
import org.archive.util.FileUtils;

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
    
    final private File rootDir;
    
    final private Map<String,CrawlController> jobs;

    final private ObjectName oname;
    
    public CrawlJobManagerImpl(File rootDir, MBeanServer server) {
        super(CrawlJobManager.class);
        this.rootDir = rootDir;
        this.jobs = new HashMap<String,CrawlController>();
        this.server = server;
        this.oname = register(NAME, TYPE, this);
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

        JMXSheetManager jmx = new JMXSheetManager(fsm);
        register(profile, "SheetManager", jmx);
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
        JMXModuleListener jmxListener = new JMXModuleListener(DOMAIN, job, server);
        try {
            List<ModuleListener> list = new ArrayList<ModuleListener>();
            list.add(jmxListener);
            list.add(ListModuleListener.make(Checkpointable.class));
            fsm = new FileSheetManager(bootstrap, true, list);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }

        JMXSheetManager jmx = new JMXSheetManager(fsm);
        final ObjectName smName = register(job, "SheetManager", jmx);

        // Find the crawlcontroller.
        Sheet sheet = fsm.getDefault();
        Object o = PathValidator.validate(sheet, "root.controller");
        if (!(o instanceof CrawlController)) {
            LOGGER.warning("Could not find CrawlController in job named " 
                    + job + " at expected path (root.controller).");
            return;
        }
        
        CrawlController cc = (CrawlController)o;
        
        final ObjectName ccName = jmxListener.nameOf(cc);

        // TODO: Use JMX notification for this instead.
        cc.addCrawlStatusListener(new CrawlStatusAdapter() {
            public void crawlEnded(String msg) {
                unregister(ccName);
                unregister(smName);
                jobs.remove(job);
            }
        });
        
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

    
    private ObjectName register(String name, String type, Object o) {
        try {
            ObjectName oname = name(name, type);
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
        return new File(rootDir, "profiles");
    }
    
    
    private File getJobsDir() {
        return new File(rootDir, "jobs");
    }

    
    public static void main(String args[]) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        System.out.println(server);
        File f = new File("/Users/pjack/Desktop/crawl");
        new CrawlJobManagerImpl(f, server);
        Object eternity = new Object();
        synchronized (eternity) {
            eternity.wait();
        }
    }

}
