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
import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.crawler.Heritrix;
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

    final private static String HOST = "localhost";
    
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
        invokeAndWait("requestCrawlStart", CrawlStatus.RUNNING);
        Thread.sleep(2000);
        invokeAndWait("requestCrawlPause", CrawlStatus.PAUSED);
        invokeAndWait("requestCrawlCheckpoint", CrawlStatus.PAUSED);
        invokeAndWait("requestCrawlStop", CrawlStatus.FINISHED);
        waitFor("org.archive.crawler:*,name=the_job,type=org.archive.crawler.framework.CrawlController", false);
        dumpMBeanServer();
        stopHeritrix();
        dumpMBeanServer();
        Heritrix.main(new String[] { getCrawlDir().getAbsolutePath() });
        
        ObjectName cjm = getCrawlJobManager();
        String[] checkpoints = (String[])server.invoke(
                cjm,
                "listCheckpoints", 
                new Object[0], 
                new String[0]);

        assertEquals(1, checkpoints.length);
        File recoverLoc = new File(getJobDir().getParentFile(), "recovered");
        FileUtils.deleteDir(recoverLoc);
        String[] oldPath = new String[] { getJobDir().getAbsolutePath() };
        String[] newPath = new String[] { recoverLoc.getAbsolutePath() };
        server.invoke(
                cjm,
                "recoverCheckpoint", 
                new Object[] { 
                        checkpoints[0], 
                        oldPath, 
                        newPath },
                new String[] { 
                        String.class.getName(), 
                        oldPath.getClass().getName(), 
                        newPath.getClass().getName()
                        });
        ObjectName cc = getCrawlController();
        waitFor(cc);
        invokeAndWait("requestCrawlResume", CrawlStatus.FINISHED);
    }

    

    protected void verify() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    }

}
