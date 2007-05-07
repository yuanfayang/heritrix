package org.archive.crawler.webui;

import java.io.File;

import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.TmpDirTestCase;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import junit.framework.TestCase;

public class WebUITest extends TmpDirTestCase {

    
    
    public void testWebUI() throws Exception {
        /*File root = new File(this.getTmpDir(), "webuitest");
        File webinf = new File(root, "WEB-INF");
        FileUtils.copyFiles(getWebappDir(), root);
        FileUtils.copyFiles(getClassesDir(), new File(webinf, "classes"));
        
        File lib = new File(webinf, "lib");
        lib.mkdir();
        
        for (String s: System.getProperty("java.class.path").split(":")) {
            if (s.endsWith(".jar")) {
                int p = s.lastIndexOf(File.separatorChar);
                String baseName = s.substring(p + 1);
                FileUtils.copyFile(new File(s), new File(lib, baseName));
            }
        }*/
        
        Server server = new Server();
        
        SocketConnector sc = new SocketConnector();
        sc.setHost("localhost");
        sc.setPort(7777);
        server.addConnector(sc);

        String webAppPath = getWebAppDir().getAbsolutePath();
        WebAppContext webapp = new WebAppContext(webAppPath, "/heritrix");
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { 
                webapp,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        server.start();
        Object eternity = new Object();
        synchronized (eternity) {
            eternity.wait();
        }
    }
    
    
    private File getWebAppDir() {
        File r = new File("src/main/webapp");
        if (r.isDirectory()) {
            return r;
        }
        r = new File("webui/src/main/webapp");
        if (r.isDirectory()) {
            return r;
        }
        throw new IllegalStateException("Can't find src/main/webapps");
    }
    
    
    private File getClassesDir() {
        File r = new File("bin");
        if (r.isDirectory()) {
            return r;
        }
        r = new File("target/classes");
        if (r.isDirectory()) {
            return r;
        }
        r = new File("webui/target/classes");
        if (r.isDirectory()) {
            return r;
        }
        throw new IllegalStateException("Cannot find Eclipse or maven generated classes.");
    }
    
}
