/* $Id$
 *
 * Created Aug 15, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
 */
package org.archive.crawler.selftest;


import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestResult;

import org.archive.crawler.framework.CrawlStatus;
import org.archive.util.FileUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;


/**
 * Assumes checkpoint was run during the SelfTest.
 * @author stack
 * @version $Date$ $Version$
 */
public class CheckpointSelfTest extends SelfTestBase {

    final private static String HOST = "127.0.0.1";
    
    final private static int MIN_PORT = 7000;
    
    final private static int MAX_PORT = 7010;
    
    final private static int MAX_HOPS = 1;
    

    private Server[] servers;
    
    
    public CheckpointSelfTest() {
    }

    
    @Override
    protected void stopHttpServer() {
        boolean fail = false;
        for (int i = 0; i < servers.length; i++) try {
            servers[i].stop();
        } catch (Exception e) {
            fail = true;
            e.printStackTrace();
        }
        
        if (fail) {
            throw new AssertionError();
        }
    }
    
    @Override
    protected void startHttpServer() throws Exception {
        this.servers = new Server[MAX_PORT - MIN_PORT];
        for (int i = 0; i < servers.length; i++) {
            servers[i] = makeHttpServer(i + MIN_PORT);
            servers[i].start();
        }
    }
    
    
    private Server makeHttpServer(int port) throws Exception {
        Server server = new Server();
        SocketConnector sc = new SocketConnector();
        sc.setHost(HOST);
        sc.setPort(port);
        server.addConnector(sc);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        RandomServlet random = new RandomServlet();
        random.setHost(HOST);
        random.setMinPort(MIN_PORT);
        random.setMaxPort(MAX_PORT);
        random.setMaxHops(MAX_HOPS);
        random.setPathRoot("random");

        ServletHolder holder = new ServletHolder(random);
        servletHandler.addServletWithMapping(holder, "/random/*");
        server.start();
        return server;
    }


    @Override
    protected void waitForCrawlFinish() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        
        // Start the crawl; wait for two seconds; pause the crawl so we
        // can checkpoint; checkpoint; and abort the crawl.
        
        // superclass startHeritrix()'s launchJob already did a requestCrawlStart
        // this second invocation was causing two sets of ToeThreads to be 
        // launched, one set of which lingered
        // invokeAndWait("basic", "requestCrawlStart", CrawlStatus.RUNNING);
        Thread.sleep(2000);
        invokeAndWait("basic", "requestCrawlPause", CrawlStatus.PAUSED);
        invokeAndWait("basic", "requestCrawlCheckpoint", CrawlStatus.PAUSED);
        invokeAndWait("basic", "requestCrawlStop", CrawlStatus.FINISHED);
        waitFor("org.archive.crawler:*,name=basic,type=org.archive.crawler.framework.CrawlController", false);
        stopHeritrix();
        Set<ObjectName> set = dumpMBeanServer();
        if (!set.isEmpty()) {
            throw new Exception("Mbeans lived on after stopHeritrix: " + set);
        }
        this.heritrixThread = new HeritrixThread(new String[] {
            "-j", getCrawlDir().getAbsolutePath() + "/jobs", "-n"
        });
        this.heritrixThread.start();
        
        ObjectName cjm = getEngine();
        String[] checkpoints = (String[])server.invoke(
                cjm,
                "listCheckpoints", 
                new Object[] { "completed-basic" },
                new String[] { "java.lang.String" });

        assertEquals(1, checkpoints.length);
        File recoverLoc = new File(getCompletedJobDir().getParentFile(), "recovered");
        FileUtils.deleteDir(recoverLoc);
        String[] oldPath = new String[] { getCompletedJobDir().getAbsolutePath() };
        String[] newPath = new String[] { recoverLoc.getAbsolutePath() };
        server.invoke(
                cjm,
                "recoverCheckpoint", 
                new Object[] {
                        "completed-basic",
                        "active-recovered",
                        checkpoints[0], 
                        oldPath, 
                        newPath
                },
                new String[] { 
                        String.class.getName(),
                        String.class.getName(),
                        String.class.getName(),
                        "java.lang.String[]",
                        "java.lang.String[]"
                        });
        ObjectName cc = getCrawlController("recovered");
        waitFor(cc);
        invokeAndWait("recovered", "requestCrawlResume", CrawlStatus.FINISHED);
        
        server.invoke(
                cjm, 
                "closeSheetManagerStub", 
                new Object[] { "completed-basic" },
                new String[] { "java.lang.String" });
    }


    

    @Override
    protected void verifyCommon() throws IOException {
        // checkpointing rotated the logs so default behavior won't work here
        // FIXME: Make this work :)
    }


    protected void verify() throws Exception {
        // FIXME: Complete test.
    }

    /**
     * Repeat core testSomething 100 times. Rename to JUNit convention
     * to enable. 
     */
    public void xestSomething100() {
        for(int i = 0; i < 100; i++) {
            try {
                testSomething();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }
//    @Override
//    public void testSomething() throws Exception {
//
//    }

    
    
}
