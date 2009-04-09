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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.archive.crawler.framework.EngineImpl;
import org.archive.crawler.restlet.EngineApplication;
import org.archive.util.ArchiveUtils;
import org.restlet.Component;
import org.restlet.data.Protocol;


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

    
    /**
     * Name of configuration directory.
     */
    private static final String CONF = "conf";
    
    /**
     * Name of the heritrix properties file.
     */
    private static final String PROPERTIES = "logging.properties";


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

    
    private static void usage(PrintStream out) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("Heritrix", options());
    }
    
    
    private static Options options() {
        Options options = new Options();
        options.addOption("h", "help", true, "Usage information." );
        options.addOption("j", "jobs-dir", true, "The jobs directory.  " +
                        "Defaults to ./jobs");
        options.addOption("l", "logging-properties", true, 
                "The full path to the logging properties file " + 
                "(eg, conf/logging.properties).  If present, this file " +
                "will be used to configure Java logging.  Defaults to " +
                "./conf/logging.properties");
        options.addOption("a", "webui-admin", true,  "Specifies the " +
        		"authorization password which must be supplied to " +
        		"access the webui. Required if launching the webui.");
        options.addOption("b", "webui-bind-hosts", true, 
                "A comma-separated list of hostnames for the " +
                "webui to bind to.");
        options.addOption("p", "webui-port", true, "The port the webui " +
                "should listen on.");
        options.addOption("w", "webui-war-path", true, "The path to the " +
                "Heritrix webui WAR.");
        options.addOption("n", "no-webui", false, "Do not run the admin web " +
                "user interface; only run the crawl engine.  If set, the " +
        	"crawl engine will need to be controlled via JMX or a remote " +
        	"web UI.");
        options.addOption("u", "no-engine", false, "Do not run the crawl " +
        	"engine; only run the admin web UI."); 
        options.addOption("r", "run-job", true,  "Specify a ready job or a " +
        	"profile name to launch at launch.  If you specify a profile " +
        	"name, the profile will first be copied to a new ready job, " +
        	"and that ready job will be launched.");
        return options;
    }
    
    
    private static File getDefaultPropertiesFile() {
        File confDir = new File(CONF);
        File props = new File(confDir, PROPERTIES);
        return props;
    }
    
    
    private static CommandLine getCommandLine(PrintStream out, String[] args) {
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(options(), args);
        } catch (ParseException e) {
            usage(out);
            return null;
        }
        
        if (cl.getArgList().size() != 0) {
            usage(out);
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
        
        PrintStream out;
        if (isDevelopment()) {
            out = System.out;
            out.println("heritrix.development mode");
        } else {
            File startLog = new File(getHeritrixHome(), STARTLOG);
            out = new PrintStream(
                    new BufferedOutputStream(
                            new FileOutputStream(startLog),16384));
        }


        CommandLine cl = getCommandLine(out, args);
        if (cl == null) return;

        if (cl.hasOption('h')) {
          usage(out);
          return ;
        }

        if (cl.hasOption('n') && cl.hasOption('u')) {
            out.println("Only one of -n or -u may be specified.");
            usage(out);
            System.exit(1);
        }

        WebUIConfig webConfig = new WebUIConfig();
        File properties = getDefaultPropertiesFile();

        if (!cl.hasOption('n')) {
            if (cl.hasOption('a')) {
                webConfig.setUiPassword(cl.getOptionValue('a'));
            } else {
                System.err.println("Unless -n is specified, you must specify " +
                	"an admin password for the web UI using -a.");
                System.exit(1);
            }
        }
        
        File jobsDir = null; 
        if (cl.hasOption('j')) {
            jobsDir = new File(cl.getOptionValue('j'));
        }
                
        if (cl.hasOption('l')) {
            properties = new File(cl.getOptionValue('l'));
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
            finp.close();
        }
        
        
        // Set timezone here.  Would be problematic doing it if we're running
        // inside in a container.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Start Heritrix.
        WebUI webui = null;
        try {
            if (cl.hasOption('u')) {
                out.println("Not running crawl engine.");
            } else {
                EngineImpl engine = new EngineImpl(jobsDir);
                
                Component component = new Component();
                // TODO: update, make port configurable
                component.getServers().add(Protocol.HTTP,8888);
                component.getClients().add(Protocol.FILE);
                component.getDefaultHost().attach(new EngineApplication(engine));
                component.start();
                out.println("engine listening at port 8888");
                if (cl.hasOption('r')) {
                    engine.requestLaunch(cl.getOptionValue('r'));
                }
            }
            
//            // Start WebUI, if desired.
//            if (cl.hasOption('n')) {
//                out.println("Not running web UI.");
//            } else {
//                webui = new WebUI(webConfig);
//                webui.start();
//                out.println("Web UI listening on " 
//                        + webConfig.hostAndPort() + ".");
//            }
        } catch (Exception e) {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(out);
            if (webui != null) {
                webui.stop();
            }
            throw e;
        } finally {
            // If not development, close the file that signals the wrapper
            // script that we've started.  Otherwise, just flush it; if in
            // development, the output is probably a console.
            if (!isDevelopment()) {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                System.out.println("Heritrix version: " +
                        ArchiveUtils.VERSION);
            } else {
                if (out != null) {
                    out.flush();
                }
            }
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
    
    protected static boolean isDevelopment() {
        return System.getProperty("heritrix.development") != null;
    }    
}