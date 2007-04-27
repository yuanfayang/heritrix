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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlJobManagerImpl;
import org.archive.crawler.framework.CrawlStatus;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
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

    final private Logger LOGGER = 
        Logger.getLogger(SelfTestBase.class.getName());
    
//    protected HeritrixThread heritrixThread;
    protected Server httpServer;
    
    protected HeritrixThread heritrixThread;

    
    protected void open() throws Exception {
        // We expect to be run from the project directory.
        // (Both eclipse and maven run junit tests from there).
        String name = getSelfTestName();
        
        // Make sure the project directory contains a selftest profile 
        // and content for the self test.
        File src = getTestDataDir();
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
        startHttpServer();
        
        // Copy configuration for eg Logging over
        File tmpConfDir = new File(tmpTestDir, "conf");
        tmpConfDir.mkdirs();
        FileUtils.copyFiles(new File("testdata/selftest/conf"), tmpConfDir);
        
        startHeritrix(tmpTestDir.getAbsolutePath());
        this.waitForCrawlFinish();
    }
    
    
    

    protected void close() throws Exception {
        stopHttpServer();
        stopHeritrix();
        Set<ObjectName> set = dumpMBeanServer();
        if (!set.isEmpty()) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            for (ObjectName name: set) {
                server.unregisterMBean(name);
            }
            throw new IllegalStateException("MBeans lived on after test: " + set);
        }
    }

    
    protected Set<ObjectName> dumpMBeanServer() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        @SuppressWarnings("unchecked")
        Set<ObjectName> set = 
            server.queryNames(null, new ObjectName("org.archive.crawler:*"));
        System.out.println(set);
        return set;
    }
    

    public void testSomething() throws Exception {
        boolean fail = false;
        try {
            open();
            verifyCommon();
            verify();
        } finally {
            try {
                close();
            } catch (Exception e) {
                e.printStackTrace();
                fail = true;
            }
        }
        assertFalse(fail);
    }
    
    
    protected abstract void verify() throws Exception;
    
    
    protected void stopHttpServer() throws Exception {
        try {
            httpServer.stop();  
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    protected void startHttpServer() throws Exception {
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
        
        this.httpServer = server;
        server.start();
    }
    
    
    protected void startHeritrix(String path) throws Exception {
        // Launch heritrix in its own thread.
        // By interrupting the thread, we can gracefully clean up the test.
        String[] args = { "-j", path + "/jobs", "-p", path + "/profiles" };
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
        
        waitFor("org.archive.crawler:*,name=the_job,type=org.archive.crawler.framework.CrawlController", true);
    }
    
    
    protected void stopHeritrix() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName cjm = getCrawlJobManager();
        server.invoke(cjm, "close", new Object[0], new String[0]);
        heritrixThread.interrupt();
    }

    protected void waitForCrawlFinish() throws Exception {
        invokeAndWait("requestCrawlStart", CrawlStatus.FINISHED);
    }
    
    protected File getSrcHtdocs() {
        return new File(getTestDataDir(), "htdocs");
    }
    
    
    protected File getTestDataDir() {
        File r = new File("testdata");
        if (!r.exists()) {
            r = new File("heritrix");
            r = new File(r, "testdata");
            if (!r.exists()) {
                throw new IllegalStateException(
                        "Can't find selfest testdata " +
                        "(tried testdata/selftest and " +
                        "heritrix/testdata/selftest)");
            }
        }
        r = new File(r, "selftest");
        r = new File(r, getSelfTestName());
        if (!r.exists()) {
            throw new IllegalStateException("No testdata directory: " 
                    + r.getAbsolutePath());
        }
        return r;
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
    
    
    protected File getLogsDir() {
        return new File(getJobDir(), "logs");
    }



    private String getSelfTestName() {
        String full = getClass().getName();
        int i = full.lastIndexOf('.');
        return full.substring(i + 1);
    }
    
    
    
    protected static void waitFor(ObjectName name) throws Exception{
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        int count = 0;
        while (server.queryNames(name, null).isEmpty()) {
            count++;
            if (count > 40) {
                throw new IllegalStateException("Could not find " + 
                        name + " after 20 seconds.");
            }
            Thread.sleep(500);
        }
    }

    
    protected static void waitFor(String query, boolean exist) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        int count = 0;
        ObjectName name = new ObjectName(query);
        Set set = server.queryNames(null, name);
        while (set.isEmpty() == exist) {
            count++;
            if (count > 40) {
                throw new IllegalStateException("Could not find " + 
                        name + " after 20 seconds.");
            }
            Thread.sleep(500);
            set = server.queryNames(null, name);
            if (set.size() > 1) {
                throw new IllegalStateException(set.size() + " matches for " + query);
            }
        }        
    }
    
    
    protected static ObjectName getCrawlController() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        @SuppressWarnings("unchecked")
        Set<ObjectName> set = server.queryNames(null, null);
        for (ObjectName name: set) {
            if (name.getDomain().equals("org.archive.crawler")
                    && name.getKeyProperty("name").equals("the_job")
                    && name.getKeyProperty("type").equals("org.archive.crawler.framework.CrawlController")) {
                return name;
            }
        }
        return null;
    }
    
    
    protected static ObjectName getCrawlJobManager() throws Exception {
        ObjectName cjm = JmxUtils.makeObjectName(
                CrawlJobManagerImpl.DOMAIN,
                CrawlJobManagerImpl.NAME, 
                CrawlJobManagerImpl.TYPE);
        waitFor(cjm);
        return cjm;
    }
    
    
    protected static void invokeAndWait(String operation, CrawlStatus status) 
    throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        
        // Above invocation should have created a new SheetManager and a new
        // CrawlController for the job.  Find the CrawlController.
        ObjectName crawlController = getCrawlController();
        waitFor(crawlController);
        
        // Set up utility to wait for signal from crawler.
        JmxWaiter waiter = new JmxWaiter(server, crawlController, 
                status.toString());
        
        // Tell the CrawlController to start the crawl.
        server.invoke(
                crawlController,
                operation,
                new Object[0],
                new String[0]             
                );
        
        // Wait for the FINISHED signal for up to 30 seconds.
        waiter.waitUntilNotification(0);
    }

    
    protected void verifyArcsClosed() {
        File arcsDir = getArcDir();
        if (!arcsDir.exists()) {
            throw new IllegalStateException("Missing arc dir " + 
                    arcsDir.getAbsolutePath());
        }
        for (File f: arcsDir.listFiles()) {
            String fn = f.getName();
            if (fn.endsWith(".open")) {
                throw new IllegalStateException(
                        "Arc file not closed at end of crawl: " + f.getName());
            }
        }
    }
    
    
    protected void verifyLogFileEmpty(String logFileName) {
        File logsDir = getLogsDir();
        File log = new File(logsDir, logFileName);
        if (log.length() != 0) {
            throw new IllegalStateException("Log " + logFileName + 
                    " isn't empty.");
        }
    }
    
    
    protected void verifyCommon() throws Exception {
        verifyLogFileEmpty("uri-errors.log");
        verifyLogFileEmpty("runtime-errors.log");
        verifyLogFileEmpty("local-errors.log");
        verifyProgressStatistics();
        verifyArcsClosed();
    }
    
    
    protected void verifyProgressStatistics() throws IOException {
        File logs = new File(getJobDir(), "logs");
        File statsFile = new File(logs, "progress-statistics.log");
        String stats = IoUtils.readFullyAsString(new FileInputStream(statsFile));
        if (!stats.contains("CRAWL RESUMED - Preparing")) {
            fail("progress-statistics.log has no Preparing line.");
        }
        if (!stats.contains("CRAWL RESUMED - Running")) {
            fail("progress-statistics.log has no Running line.");
        }
        if (!stats.contains("CRAWL ENDING - Finished")) {
            fail("progress-statistics.log has missing/wrong Finished line.");
        }
        if (!stats.contains("doc/s(avg)")) {
            fail("progress-statistics.log has no legend.");
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
            if (arh.getUrl().startsWith("http:")) {
                result.add(path);
            }
        }
        LOGGER.finest(result.toString());
        return result;
    }


    static class HeritrixThread extends Thread{
        
        String[] args;
        Exception exception;

        public HeritrixThread(String args[]) {
            this.args = args;
        }
        
        
        public void run() {
            try {
                Heritrix.main(args);
            } catch (Exception e) {
                e.printStackTrace();
                this.exception = e;
            }
        }
        
        public Exception getStartUpException() {
            return exception;
        }
    }

}
