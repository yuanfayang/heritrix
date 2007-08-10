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
 * WebUITest.java
 *
 * Created on May 29, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.framework.CrawlJobManagerConfig;
import org.archive.crawler.framework.CrawlJobManagerImpl;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.TmpDirTestCase;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Unit test for the web ui. Starts a crawler, starts the webui via Jetty, and
 * uses {@link java.net.URL} to connect to various pages in the webui.
 * 
 * <p>
 * The test doesn't really examine the output much. The test extracts the URL
 * for the next page from the previously fetched page, and in some cases will
 * perform a regex on a fetched page to see if desired content is present, but
 * this is not comprehensive. This unit test is mostly for testing the action
 * code, not the presentation.
 * 
 * <p>
 * By specifying any value for the org.archive.crawler.webui.wait system
 * property, the unit test will never complete: This can be useful if you want
 * to drive the UI to a certain state and then manually click around in your
 * browser.
 * 
 * <p>
 * This is named WebUIJunit and not WebUITest because, alas, it will not run
 * under maven2 due to classpath issues. (Jetty requires the system class loader
 * to be able to compile JSPs; but if we use the system class loader, then we're
 * stuck with maven2's version of commons.lang, and Heritrix requires a later
 * version.)
 * 
 * @author pjack
 */
public class WebUIJUnit extends TmpDirTestCase {
    

    /** Jetty server. */
    private Server server;
    
    /** Crawler instance. */
    private CrawlJobManagerImpl manager;
    
    /** Result of System.identityHashCode(manager), used all over place. */
    private int managerId;

    /** The full text of the most recently fetched page. */
    private String lastFetched;

    private URL lastUrl;

    private String host;
    private String urlSheets;
    
    /**
     * Starts Jetty, starts Heritrix.
     */
    public void setUp() throws Exception {
        // Start Jetty.
        
        server = new Server();
        
        SocketConnector sc = new SocketConnector();
        sc.setHost("localhost");
        sc.setPort(7777);
        server.addConnector(sc);

        String webAppPath = WebUITestMain.getWebAppDir().getAbsolutePath();
        WebAppContext webapp = new WebAppContext(webAppPath, "/heritrix");
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { 
                webapp,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        server.start();
        
        // Start Heritrix.
        setupDirs();
        CrawlJobManagerConfig config = new CrawlJobManagerConfig();
        config.setJobsDirectory(getJobsDir().getAbsolutePath());
        this.manager = new CrawlJobManagerImpl(config);
        this.managerId = System.identityHashCode(manager);
    }
    

    /**
     * Stops Jetty, stops Heritrix.
     */
    public void tearDown() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        manager.close();        
    }
    
    
    private static String extract(String url, String key) {
        int p = url.indexOf(key);
        if (p < 0) {
            throw new IllegalStateException();
        }
        int p2 = url.indexOf('&', p);
        if (p2 < 0) {
            p2 = url.length();
        }
        return url.substring(p + key.length() + 1, p2);
    }
    
    
    /**
     * Tests the webui.
     */
    public void testWebui() throws Exception  {
        doGet("/heritrix");
        doGet("/heritrix/home/do_show_add_crawler.jsp");
        
        doPost("/heritrix/home/do_add_crawler.jsp",
                "host", "localhost",
                "port", "-1",
                "username", "local",
                "password", "local");

        String url = findHref("do_show_crawler.jsp", managerId);
        this.host = extract(url, "host");
        doGet(url);
        
        this.urlSheets = findHref("do_show_sheets.jsp", managerId, "basic");
        doGet(urlSheets);
        
        addSingleAndOverride(
                "foo", 
                "root:controller:processors:HTTP:sotimeout-ms", 
                "20000", 
                "3333", 
                "http://www.foo.org");
        
        addSingleAndOverride(
                "bar",
                "root:controller:processors:HTTP:timeout-seconds",
                "1200",
                "9999",
                "http://www.bar.org/1/2/3");
        
        addSingleAndOverride(
                "baz",
                "root:scope:rules:2:max-hops",
                "20",
                "123456",
                "http://www.baz.org/1.html");

        doGet(urlSheets);

        if (System.getProperty("org.archive.crawler.webui.wait") != null) {
            Object eternity = new Object();
            synchronized (eternity) {
                eternity.wait();
            }
        }
    }

    
    /**
     * Creates a new single sheet, overrides a setting on that sheet, commits
     * the sheet, and associates an URL with the sheet.
     * 
     * The new sheet will be created in the default profile, and the setting
     * to override must be of type int.
     * 
     * @param sheet   the name of the new sheet
     * @param path    the path to the setting to override
     * @param oldValue   the old (default) value of that setting
     * @param newValue   the new value for that setting
     * @param assoc      the URL to associate with the new sheet
     * @throws Exception
     */
    private void addSingleAndOverride(
            String sheet, 
            String path, 
            String oldValue,
            String newValue,
            String assoc) 
    throws Exception {
        // Start on the sheets page for the default profile.
        doGet(urlSheets);

        // FIXME: Make these parameters, repeat test on ready & active job,
        // not just profiles.
        String stage =  "PROFILE";
        String job = "basic";
        
        // Create a new single sheet with the specified name.
        String url = findHref("do_show_add_single_sheet.jsp", managerId, "basic");
        doGet(url);
        
        doPost("do_add_single_sheet.jsp",
                "host", host,
                "port", "-1",
                "id", Integer.toString(managerId),
                "stage", stage,
                "job", job,
                "sheet", sheet);

        // Override the specified setting to be newValue instead of oldValue. 
        url = findHref("do_override_path.jsp", path, oldValue);
        doGet(url);
        
        url = findHref("do_show_path_detail.jsp", path, oldValue);
        doGet(url);
        
        doPost("do_save_path.jsp",
                "host", host,
                "port", "-1",
                "id", Integer.toString(managerId),
                "stage", stage,
                "job", job,
                "sheet", sheet, 
                "path", path, 
                "type-" + path, "int",
                "value-" + path, newValue
        );
        
        doGet(urlSheets);

        // Commit the override.
        url = findHref("do_commit_sheet.jsp", managerId, sheet);
        doGet(url);

        // Associate the URL prefix "http://www.foo.org" with the "foo" sheet.
        url = findHref("do_show_associate.jsp", managerId, sheet);
        doGet(url);
        
        doPost("do_associate.jsp",
                "host", host,
                "port", "-1",
                "id", Integer.toString(managerId),
                "stage", stage,
                "job", job,
                "sheet", sheet, 
                "add", "Y",
                "surts", "# Comment line to ignore\n\n" + assoc);
        
        // View the configuration for url assoc + "/a/b/c/d",
        // which should contain our newValue override.
        doPost("do_show_config.jsp",
                "host", host,
                "port", "-1",
                "id", Integer.toString(managerId),
                "job", job,
                "stage", stage,
                "button", "Settings",
                "url", assoc + "/a/b/c/d");

        int p = this.lastFetched.indexOf(newValue);
        assertTrue("Configuration for " + assoc 
                + " did not contain override value of " + newValue, p > 0);
    }
    
    
    
    private URL toURL(String urlString) throws Exception {
        if (urlString.startsWith("http://")) {
            return new URL(urlString);
        }

        if (urlString.startsWith("/")) {
            return new URL("http://localhost:7777" + urlString);
        }
        
        if (lastUrl == null) {
            return new URL("http://localhost:7777/heritrix/" + urlString);
        }
        
        // Make new url relative to last fetched url.
        String last = lastUrl.toString();

        // Chop off query string
        int p = last.indexOf('&');
        if (p >= 0) {
            last = last.substring(0, p);
        }
        
        // Chop off filename
        p = last.lastIndexOf('/');
        if (p >= 0) {
            last = last.substring(0, p + 1);
        }
        
        return new URL(last + urlString);
    }
    
    
    private void doGet(String urlString) throws Exception {
        URL url = toURL(urlString);
        HttpURLConnection conn = (HttpURLConnection)(url.openConnection());
        InputStream input = null;
        try {
            input = conn.getInputStream();
            lastUrl = url;
            lastFetched = IoUtils.readFullyAsString(conn.getInputStream());
            System.out.println("Fetched: " + urlString);
        } finally {
            IoUtils.close(input);
        }
    }
    
    

    private void doPost(String urlString, String... pairs) throws Exception {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Pairs must come in pairs.");
        }
        URL url = toURL(urlString);
        HttpURLConnection conn = (HttpURLConnection)(url.openConnection());
        conn.setDoOutput(true);
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            if (pairs.length > 0) {
                wr.write(URLEncoder.encode(pairs[0], "UTF-8"));
                wr.write('=');
                wr.write(URLEncoder.encode(pairs[1], "UTF-8"));
                for (int i = 2; i < pairs.length; i += 2) {
                    wr.write('&');
                    wr.write(URLEncoder.encode(pairs[i], "UTF-8"));
                    wr.write('=');
                    wr.write(URLEncoder.encode(pairs[i + 1], "UTF-8"));
                }
            }
            wr.flush();
        } finally {
            IoUtils.close(wr);
        }

        InputStream input = null;
        try {
            input = conn.getInputStream();
            lastUrl = url;
            lastFetched = IoUtils.readFullyAsString(conn.getInputStream());
        } finally {
            IoUtils.close(input);
        }
    }

    
    private String findHref(Object... tokens) {
        StringBuilder regex = new StringBuilder();
        regex.append("href=\"(.*?");
        for (Object t: tokens) {
            regex.append(t).append(".*?");
        }
        regex.append(")\"");
        return find(regex.toString());
    }
    
    
    private String find(String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.lastFetched);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("Didn't find expected pattern: " + regex);
    }
    
    
    public static File getCrawlerDir() {
        try {
            File tmp = TmpDirTestCase.tmpDir();
            return new File(tmp, "webuitest");        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public static File getJobsDir() {
        return new File(getCrawlerDir(), "jobs");
    }
    
    


    public void setupDirs() throws Exception {
        File crawler = getCrawlerDir();
        FileUtils.deleteDir(crawler);
        crawler.mkdirs();
        File jobs = getJobsDir();
        jobs.mkdirs();
        File defProf = new File(jobs, "profile-basic");
        defProf.mkdirs();
        File defProfSheets = new File(defProf, "sheets");
        defProfSheets.mkdirs();
        new FileOutputStream(new File(defProf, "config.txt")).close();
        new FileOutputStream(new File(defProf, "seeds.txt")).close();
        File defProfGlobal = new File(defProfSheets, "global.sheet");
        copyResource("/org/archive/crawler/webui/global.sheet", defProfGlobal);
    }


    private void copyResource(String resource, File file) throws IOException {
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = getClass().getResourceAsStream(resource);
            if (input == null) {
                throw new IllegalArgumentException("Not found: " + resource);
            }
            output = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            for (int l = input.read(buf); l > 0; l = input.read(buf)) {
                output.write(buf, 0, l);
            }
        } finally {
            IoUtils.close(input);
            IoUtils.close(output);
        }
    }

}
