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
 * SelfTestBase.java
 *
 * Created on Feb 21, 2007
 *
 * $Id:$
 */

package org.archive.crawler.selftest;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.crawler.framework.CrawlJobManagerImpl;
import org.archive.crawler.framework.CrawlStatus;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.FileUtils;
import org.archive.util.JmxUtils;
import org.archive.util.JmxWaiter;
import org.archive.util.TmpDirTestCase;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

/**
 * @author pjack
 *
 */
public abstract class SelfTestBase extends TmpDirTestCase {

    
    private HeritrixThread heritrixThread;
    private Server httpServer;

    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // We expect to be run from the project directory.
        // (Both eclipse and maven run junit tests from there).
        String name = getSelfTestName();
        
        // Make sure the project directory contains a selftest profile 
        // and content for the self test.
        File src = new File("testdata/selftest/" + name);
        if (!src.exists()) {
            throw new Exception("No selftest directory for " + name);
        }
        
        // Create temporary directories for Heritrix to run in.
        File tmpDir = new File(getTmpDir(), "selftest");
        File tmpTestDir = new File(tmpDir, name);
        
        File tmpProfiles = new File(tmpTestDir, "profiles");
        tmpProfiles.mkdirs();
        
        // If we have an old job lying around from a previous run, delete it.
        File tmpJobs = new File(tmpTestDir, "jobs");
        if (tmpJobs.exists()) {
            FileUtils.deleteDir(tmpJobs);
        }
        
        // Copy the selftest's profile in the project directory to the
        // default profile in the temporary Heritrix directory.
        File tmpDefProfile = new File(tmpProfiles, "default");
        FileUtils.copyFiles(new File(src, "profile"), tmpDefProfile);
        
        // Start up a Jetty that servers the selftest's content directory.
        this.httpServer = configureHttpServer();
        httpServer.start();
        
        startHeritrix(tmpTestDir.getAbsolutePath());
    }
    
    
    @Override
    public void tearDown() throws Exception{
        super.tearDown();

        heritrixThread.interrupt();
        httpServer.stop();
    }
    
    
    
    protected Server configureHttpServer() {
        Server server = new Server();
        SocketConnector sc = new SocketConnector();
        sc.setHost("localhost");
        sc.setPort(7777);
        server.addConnector(sc);
        ResourceHandler rhandler = new ResourceHandler();
        rhandler.setResourceBase(getSrcHtdocs().getAbsolutePath());
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { rhandler, new DefaultHandler() });
        server.setHandler(handlers);
        
        return server;
    }
    
    
    protected void startHeritrix(String path) throws Exception {
        // Launch heritrix in its own thread.
        // By interrupting the thread, we can gracefully clean up the test.
        String[] args = { path };
        heritrixThread = new HeritrixThread(args);
        
        heritrixThread.start();

        // Wait up to 20 seconds for the main OpenMBean to appear.
        ObjectName cjm = JmxUtils.makeObjectName(
                CrawlJobManagerImpl.DOMAIN,
                CrawlJobManagerImpl.NAME, 
                CrawlJobManagerImpl.TYPE);
        waitFor(cjm);

        // Tell the CrawlJobManager to launch a new job based on the 
        // default profile.
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        server.invoke(
                cjm, 
                "launchProfile", 
                new Object[] { "default", "the_job" },
                new String[] { "java.lang.String", "java.lang.String" });
        
        // Above invocation should have created a new SheetManager and a new
        // CrawlController for the job.  Find the CrawlController.
        ObjectName crawlController = JmxUtils.makeObjectName(
                    CrawlJobManagerImpl.DOMAIN,
                    "the_job",
                    "CrawlController"                
                );
        waitFor(crawlController);
        
        // Set up utility to wait for FINISHED signal from crawler.
        JmxWaiter waiter = new JmxWaiter(server, crawlController, 
                CrawlStatus.FINISHED.toString());
        
        // Tell the CrawlController to start the crawl.
        server.invoke(
                crawlController,
                "requestCrawlStart",
                new Object[0],
                new String[0]             
                );
        
        // Wait for the FINISHED signal for up to 30 seconds.
        waiter.waitUntilNotification(0);
    }
    
    
    protected File getSrcHtdocs() {
        return new File("testdata/selftest/" + getSelfTestName() + "/htdocs");
    }
    
    
    protected File getCrawlDir() {
        File tmp = getTmpDir();
        File selftest = new File(tmp, "selftest");
        File crawl = new File(selftest, getSelfTestName());
        return crawl;
    }
    
    
    protected File getJobDir() {
        File crawl = getCrawlDir();
        File jobs = new File(crawl, "jobs");
        File theJob = new File(jobs, "the_job");
        return theJob;
    }
    
    
    protected File getArcDir() {
        return new File(getJobDir(), "arcs");
    }



    private String getSelfTestName() {
        String full = getClass().getName();
        int i = full.lastIndexOf('.');
        return full.substring(i + 1);
    }
    
    
    
    private static void waitFor(ObjectName name) throws Exception{
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        int count = 0;
        while (!server.isRegistered(name)) {
            count++;
            if (count > 40) {
                throw new IllegalStateException("Could not find " + 
                        name + " after 20 seconds.");
            }
            Thread.sleep(500);
        }
    }

    
    protected List<ArchiveRecordHeader> headersInArcs() throws IOException {
        List<ArchiveRecordHeader> result = new ArrayList<ArchiveRecordHeader>();
        File arcsDir = getArcDir();
        if (!arcsDir.exists()) {
            throw new IllegalStateException("Missing arc dir " + 
                    arcsDir.getAbsolutePath());
        }
        File[] files = arcsDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        for (File f: files) {
            result.addAll(ARCReaderFactory.get(f).validate());
        }
        return result;
    }
    
    
    protected Set<String> filesInArcs() throws IOException {
        List<ArchiveRecordHeader> headers = headersInArcs();
        HashSet<String> result = new HashSet<String>();
        for (ArchiveRecordHeader arh: headers) {
            UURI uuri = UURIFactory.getInstance(arh.getUrl());
            String path = uuri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            result.add(path);
        }
        return result;
    }

}
