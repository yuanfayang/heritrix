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


import org.archive.crawler.framework.CrawlStatus;
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
    
    final private static int MAX_HOPS = 3;
    

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

        ServletHolder holder = new ServletHolder(new RandomServlet());
        servletHandler.addServletWithMapping(holder, "/random/*");
        server.start();
        return server;
    }


    @Override
    protected void waitForCrawlFinish() throws Exception {
        // Start the crawl; wait for two seconds; pause the crawl so we
        // can checkpoint; checkpoint; and abort the crawl.
        invokeAndWait("requestCrawlStart", CrawlStatus.RUNNING);
        Thread.sleep(2000);
        invokeAndWait("requestCrawlPause", CrawlStatus.PAUSED);
        invokeAndWait("requestCrawlCheckpoint", CrawlStatus.PAUSED);
        invokeAndWait("requestCrawlStop", CrawlStatus.ABORTED);
        
        // Get rid of the old crawler thread.
        heritrixThread.interrupt();
    }

    

    public void testCheckpointRecover() throws Exception {
    }
	
}
