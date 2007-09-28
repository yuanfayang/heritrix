/* Heritrix
 *
 * $Id$
 *
 * Created on May 15, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.archive.crawler.framework.CrawlJobManagerConfig;
import org.archive.crawler.framework.CrawlJobManagerImpl;
import org.archive.util.IoUtils;
import org.archive.util.JndiUtils;


/**
 * Main class for Heritrix crawler.
 *
 * Heritrix is usually launched by a shell script that backgrounds heritrix
 * that redirects all stdout and stderr emitted by heritrix to a log file.  So
 * that startup messages emitted subsequent to the redirection of stdout and
 * stderr show on the console, this class prints usage or startup output
 * such as where the web UI can be found, etc., to a STARTLOG that the shell
 * script is waiting on.  As soon as the shell script sees output in this file,
 * it prints its content and breaks out of its wait.
 * See ${HERITRIX_HOME}/bin/heritrix.
 * 
 * <p>Heritrix can also be embedded or launched by webapp initialization or
 * by JMX bootstrapping.  So far I count 4 methods of instantiation:
 * <ol>
 * <li>From this classes main -- the method usually used;</li>
 * <li>From the Heritrix UI (The local-instances.jsp) page;</li>
 * <li>A creation by a JMX agent at the behest of a remote JMX client; and</li>
 * <li>A container such as tomcat or jboss.</li>
 * </ol>
 *
 * @author gojomo
 * @author Kristinn Sigurdsson
 * @author Stack
 */
public class Heritrix {

    final private static String VERSION = loadVersion();
    
    /**
     * Name of configuration directory.
     */
    private static final String CONF = "conf";
    
    /**
     * Name of the heritrix properties file.
     */
    private static final String PROPERTIES = "heritrix.properties";


    /**
     * Heritrix logging instance.
     */
    private static final Logger logger =
        Logger.getLogger(Heritrix.class.getName());

    
    /**
     * Heritrix start log file.
     *
     * This file contains standard out produced by this main class for startup
     * only.  Used by heritrix shell script.  Name here MUST match that in the
     * <code>bin/heritrix</code> shell script.  This is a DEPENDENCY the shell
     * wrapper has on this here java heritrix.
     */
    private static final String STARTLOG = "heritrix_dmesg.log";

    
    private static void usage() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("Heritrix", options());
    }
    
    
    private static Options options() {
        Options options = new Options();
        options.addOption("j", "jobs-dir", true, "The jobs directory.  " +
                        "Defaults to ./jobs");
        options.addOption("h", "heritrix-properties", true, 
                "The full path to the heritrix properties file " + 
                "(eg, conf/heritrix.properties).  If present, this file " +
                "will be used to configure Java logging.  Defaults to " +
                "./conf/heritrix.properties");
        options.addOption("b", "webui-bind-hosts", true, 
                "A comma-separated list of hostnames for the " +
                "webui to bind to.  Ignored if -r is not specified.");
        options.addOption("p", "webui-port", true, "The port the webui " +
                "should listen on.  Ignored if -r is not specified.");
        options.addOption("w", "webui-war-path", true, "The path to the " +
                "Heritrix webui WAR.  Ignored if -r is not specified.");
        options.addOption("r", "run-webui", false,  "If set, launches " +
        		 "a local web server and crawler webui. If not set, " +
        		 "the launched crawl engine will need to be controlled " +
        		 "via JMX (and possibly a remote webui).");
        options.addOption("a", "webui-admin", true,  "Specifies the " +
        		"authorization password which must be supplied to " +
        		"access the webui. Required if launching the webui.");
        return options;
    }
    
    
    private static File getDefaultPropertiesFile() {
        File confDir = new File(CONF);
        File props = new File(confDir, PROPERTIES);
        return props;
    }
    
    
    private static CommandLine getCommandLine(String[] args) {
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(options(), args);
        } catch (ParseException e) {
            usage();
            return null;
        }
        
        if (cl.getArgList().size() != 0) {
            usage();
            return null;
        }

        return cl;
    }

    /**
     * Launch program.
     * Will also register Heritrix MBean with platform MBeanServer.
     * 
     * @param args Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args)
    throws Exception {
        // Set some system properties early.
        // Can't use class names here without loading them.
        String ignoredSchemes = "org.archive.net.UURIFactory.ignored-schemes";
        if (System.getProperty(ignoredSchemes) == null) {
            System.setProperty(ignoredSchemes,
                    "mailto, clsid, res, file, rtsp, about");
        }

        String maxFormSize = "org.mortbay.jetty.Request.maxFormContentSize";
        if (System.getProperty(maxFormSize) == null) {
            System.setProperty(maxFormSize, "52428800");
        }
        
        CrawlJobManagerConfig config = new CrawlJobManagerConfig();
        WebUIConfig webConfig = new WebUIConfig();
        File properties = getDefaultPropertiesFile();
        
        CommandLine cl = getCommandLine(args);
        if (cl == null) return;

        if (cl.hasOption('r')) {
            if (cl.hasOption('a')) {
                webConfig.setUiPassword(cl.getOptionValue('a'));
            } else {
                System.err.println("If -r is specified, -a must be specified too.");
                System.exit(1);
            }
        }
        
        if (cl.hasOption('j')) {
            config.setJobsDirectory(cl.getOptionValue('j'));
        }
                
        if (cl.hasOption('h')) {
            properties = new File(cl.getOptionValue('h'));
        }

        if (cl.hasOption('b')) {
            String hosts = cl.getOptionValue('b');
            List<String> list;
            if("/".equals(hosts)) {
                list = new ArrayList<String>(); 
            } else {
                list = Arrays.asList(hosts.split(","));
            }
            webConfig.getHosts().addAll(list);
        } else {
            // default: only localhost
            webConfig.getHosts().add("localhost");
        }
        if (cl.hasOption('p')) {
            int port = Integer.parseInt(cl.getOptionValue('p'));
            webConfig.setPort(port);
        }
        if (cl.hasOption('w')) {
            webConfig.setPathToWAR(cl.getOptionValue('w'));
        }

        if (properties.exists()) {
            FileInputStream finp = new FileInputStream(properties);
            LogManager.getLogManager().readConfiguration(finp);
        }
        
        PrintStream out;
        if (isDevelopment()) {
            out = System.out;
        } else {
            File startLog = new File(getHeritrixHome(), STARTLOG);
            out = new PrintStream(
                    new BufferedOutputStream(
                            new FileOutputStream(startLog),16384));
        }
        
        // Set timezone here.  Would be problematic doing it if we're running
        // inside in a container.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Start Heritrix.
        try {
            CrawlJobManagerImpl cjm = new CrawlJobManagerImpl(config);
            registerJndi(cjm.getObjectName(), out);
            out.println("CrawlJobManager registered at " + cjm.getObjectName());
            
            // Start WebUI, if desired.
            if (cl.hasOption('r')) {
                new WebUI(webConfig).start();
                out.println("Web UI listening on " 
                        + webConfig.hostAndPort() + ".");
            }
        } catch (Exception e) {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(out);
            throw e;
        } finally {
            // If not development, close the file that signals the wrapper
            // script that we've started.  Otherwise, just flush it; if in
            // development, the output is probably a console.
            if (!isDevelopment()) {
                if (out != null) {
                    out.close();
                }
                System.out.println("Heritrix version: " +
                        Heritrix.getVersion());
            } else {
                if (out != null) {
                    out.flush();
                }
            }
        }
        
        
        try {
            Object eternity = new Object();
            synchronized (eternity) {
                eternity.wait();
            }
        } catch (InterruptedException e) {
            
        }
    }
    
    
    /**
     * Exploit <code>-Dheritrix.home</code> if available to us.
     * Is current working dir if no heritrix.home property supplied.
     * @return Heritrix home directory.
     * @throws IOException
     */
    protected static File getHeritrixHome()
    throws IOException {
        File heritrixHome = null;
        String home = System.getProperty("heritrix.home");
        if (home != null && home.length() > 0) {
            heritrixHome = new File(home);
            if (!heritrixHome.exists()) {
                throw new IOException("HERITRIX_HOME <" + home +
                    "> does not exist.");
            }
        } else {
            heritrixHome = new File(new File("").getAbsolutePath());
        }
        return heritrixHome;
    }


    

    protected static void registerJndi(final ObjectName name, PrintStream out)
    throws NullPointerException, NamingException {
    	Context c = getJndiContext();
    	if (c == null) {
    	    out.println("No JNDI context.");
            return;
    	}
        CompoundName key = JndiUtils.bindObjectName(c, name);
        out.println("Bound '"  + key + "' to '" + JndiUtils.
               getCompoundName(c.getNameInNamespace()).toString()
               + "' jndi context");
    }
    
    protected static void deregisterJndi(final ObjectName name, PrintStream out)
    throws NullPointerException, NamingException {
    	Context c = getJndiContext();
    	if (c == null) {
            return;
    	}
        CompoundName key = JndiUtils.unbindObjectName(c, name);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Unbound '" + key + "' from '" +
                JndiUtils.getCompoundName(c.getNameInNamespace()).toString() +
                	"' jndi context");
        }
    }
    
    /**
     * @return Jndi context for the crawler or null if none found.
     * @throws NamingException 
     */
    protected static Context getJndiContext() throws NamingException {
    	Context c = null;
    	try {
    	    c = JndiUtils.getSubContext(CrawlJobManagerImpl.DOMAIN);
    	} catch (NoInitialContextException e) {
    	    logger.fine("No JNDI Context: " + e.toString());
    	}
    	return c;
    }

    
    /**
     * Get the heritrix version.
     *
     * @return The heritrix version.  May be null.
     */
    public static String getVersion() {
        return VERSION;
    }

    
    protected static boolean isDevelopment() {
        return System.getProperty("heritrix.development") != null;
    }    


    private static String loadVersion() {
        InputStream input = Heritrix.class.getResourceAsStream(
                "/org/archive/crawler/version.txt");
        if (input == null) {
            return "UNKNOWN";
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(input));
            return br.readLine();
        } catch (IOException e) {
            return e.getMessage();
        } finally {
            IoUtils.close(br);
        }
    }
}
