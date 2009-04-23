/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.TeeOutputStream;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.restlet.EngineApplication;
import org.archive.util.ArchiveUtils;
import org.restlet.Component;
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
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
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Heritrix.class.getName());
    
    /** Name of configuration directory */
    private static final String CONF = "conf";
    
    /** Name of the heritrix properties file */
    private static final String PROPERTIES = "logging.properties";

    protected Engine engine; 
    protected Component component;
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
     * Launches a local Engine and restfgul web interface given the
     * command-line options or defaults. 
     * 
     * @param args Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) 
    throws Exception {
        new Heritrix().instanceMain(args); 
    }
    
    public void instanceMain(String[] args)
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
        
        
        BufferedOutputStream startupOutStream = 
            new BufferedOutputStream(
                new FileOutputStream(
                    new File(getHeritrixHome(), STARTLOG)),16384);
        PrintStream startupOut = 
            new PrintStream(
                new TeeOutputStream(
                    System.out,
                    startupOutStream));

        CommandLine cl = getCommandLine(startupOut, args);
        if (cl == null) return;

        if (cl.hasOption('h')) {
          usage(startupOut);
          return ;
        }

        int port = 8080;
        Set<String> bindHosts = new HashSet<String>();
        String authLogin = "admin";
        String authPassword = "";
        File properties = getDefaultPropertiesFile();

        if (cl.hasOption('a')) {
            String aOption = cl.getOptionValue('a');
            int colonIndex = aOption.indexOf(':');
            if(colonIndex>-1) {
                authLogin = aOption.substring(0,colonIndex);
                authPassword = aOption.substring(colonIndex+1);
            } else {
                authPassword = aOption;
            }
        } else {
            System.err.println(
                "You must specify a password for the web interface using -a.");
            System.exit(1);
        }
        
        File jobsDir = null; 
        if (cl.hasOption('j')) {
            jobsDir = new File(cl.getOptionValue('j'));
        } else {
            jobsDir = new File("./jobs");
        }
                
        if (cl.hasOption('l')) {
            properties = new File(cl.getOptionValue('l'));
        }

        if (cl.hasOption('b')) {
            String hosts = cl.getOptionValue('b');
            List<String> list;
            if("/".equals(hosts)) {
                // '/' means all, signified by empty-list
                list = new ArrayList<String>(); 
            } else {
                list = Arrays.asList(hosts.split(","));
            }
            bindHosts.addAll(list);
        } else {
            // default: only localhost
            bindHosts.add("localhost");
        }
        if (cl.hasOption('p')) {
            port = Integer.parseInt(cl.getOptionValue('p'));
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
        try {
            engine = new Engine(jobsDir);
            component = new Component();
            
            // TODO: require SSL, generating cert if necessary
            if(bindHosts.isEmpty()) {
                // listen all addresses
                component.getServers().add(Protocol.HTTP,port);
            } else {
                // bind only to declared addresses, or just 'localhost'
                for(String address : bindHosts) {
                    component.getServers().add(Protocol.HTTP,address,port);
                }
            }
            component.getClients().add(Protocol.FILE);
            Guard guard = new Guard(null,
                    ChallengeScheme.HTTP_DIGEST, "Authentication Required");
            guard.getSecrets().put(authLogin, authPassword.toCharArray());
            guard.setNext(new EngineApplication(engine));
            component.getDefaultHost().attach(guard);
            component.start();
            startupOut.println("engine listening at port "+port);
            startupOut.println("operator login is '"+authLogin
                               +"' password '"+authPassword+"");
            if (cl.hasOption('r')) {
                engine.requestLaunch(cl.getOptionValue('r'));
            }

            
        } catch (Exception e) {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(startupOut);
            if (component != null) {
                component.stop();
            }
            throw e;
        } finally {
            startupOut.flush();
            // stop writing to side startup file
            startupOutStream.close();
            System.out.println("Heritrix version: " +
                    ArchiveUtils.VERSION);
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


    public Engine getEngine() {
        return engine;
    }


    public Component getComponent() {
        return component;
    }

}
