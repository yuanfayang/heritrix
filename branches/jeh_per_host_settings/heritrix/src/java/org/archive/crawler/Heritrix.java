/* 
 * Heritrix
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.InvalidAttributeValueException;

import org.archive.crawler.admin.SimpleCrawlJob;
import org.archive.crawler.admin.SimpleHandler;
import org.archive.crawler.admin.SimpleHttpServer;
import org.archive.crawler.admin.auth.User;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.settings.XMLSettingsHandler;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.exceptions.InitializationException;


/**
 * Main class for Heritrix crawler.
 * 
 * See {@link #usage() usage()} for how to launch the program.
 * 
 * @author gojomo
 * @author Kristinn Sigurdsson
 *
 */
public class Heritrix {

    /**
     * Name of the heritrix properties file.
     *
     * Should be available on the classpath.
     */
    private static final String PROPERTIES = "heritrix.properties";
    
    /**
     * Name of the heritrix version property.
     */
    private static final String VERSION = "heritrix.version";

    /**
     * Heritrix logging instance.
     */
    protected static Logger logger
        = Logger.getLogger("org.archive.crawler.Heritrix");
    
    /**
     * Heritrix properties.
     * 
     * Read from properties file on startup and cached thereafter.
     */
    protected static Properties properties = null;
    
        
    /**
     * Logging handler.
     *
     * TODO: Make implementation of CrawlJobHandler configurable
     */
    protected static SimpleHandler handler;


    /**
     * Launch program
     * 
     * @param args See usage() for details.
     * 
     * @see #usage()
     */
    public static void main(String[] args) {
        loadProperties();
        patchLogging();
        boolean noWUI = false;
        int port = -1;
        String crawlOrderFile = null;
        String admin = "admin:letmein";
        String user = "user:archive";
        // 0 = no crawl order specified, 1 = start, 2 = wait, 3 = set as default.
        int crawllaunch = -1; 
        
        if(args.length > 7){
            // Too many arguments. Display usage
            usage();
            return;
        }
        else {
            for(int i=0 ; i < args.length ; i++) {
                String arg = args[i];
                if(arg.indexOf("-") == 0) {
                    // This is a command, not the crawl order file.
                    if(arg.equalsIgnoreCase("--no-wui")){
                        if(port != -1){
                            // If the port set you can't specify no web UI.
                            usage();
                            return;
                        }
                        else{
                            noWUI = true;            
                        }
                    }
                    else if(arg.indexOf("--port=") == 0){
                        //Defining web UI port.
                        if(port != -1){
                            // Can't double define port.
                            usage();
                            return;
                        }
                        else{
                            try{
                                String p = arg.substring(7);
                                port = Integer.parseInt(p);
                            }
                            catch(NumberFormatException e){
                                usage();
                                return;
                            }
                        }
                    }
                    else if(arg.equals("--start")){
                        if(crawllaunch != -1){
                            // Can't define crawllaunch twice.
                            usage();
                            return;
                        }
                        else{
                            crawllaunch = 1;
                        }
                    }
                    else if(arg.equals("--wait")){
                        if(crawllaunch != -1){
                            // Can't define crawllaunch twice.
                            usage();
                            return;
                        }
                        else{
                            crawllaunch = 2;
                        }
                    }
                    else if(arg.equals("--set")){
                        if(crawllaunch != -1){
                            // Can't define crawllaunch twice.
                            usage();
                            return;
                        }
                        else{
                            crawllaunch = 3;
                        }
                    }
                    else if(arg.equals("--admin")){
                    	// Overwriting the default admin login options.
                    	if(args[i+1].indexOf("-") != 0 
                    	   && args[i+1].indexOf(":")>0 
                    	   && args[i+1].indexOf(":") < args[i+1].length()){
							// Ok everything looks right. Increment i and overwrite admin login.
							admin = args[++i];
                    	} else {
							// The next argument sould be the new login info but it doesn't look right.
							// Should not start with "-" and must contain ":" somewhere (but not at the 
							// front of the string or at the end.
							usage();
							return;
                    	}
                    }
					else if(arg.equals("--user")){
						// Overwriting the default user login options.
						if(args[i+1].indexOf("-") != 0 
						   && args[i+1].indexOf(":")>0 
						   && args[i+1].indexOf(":") < args[i+1].length()){
							// Ok everything looks right. Increment i and overwrite user login.
							admin = args[++i];
					    } else {
							// The next argument sould be the new login info but it doesn't look right.
							// Should not start with "-" and must contain ":" somewhere (but not at the 
							// front of the string or at the end.
							usage();
							return;
						}
					}
                    else{
                        //Unknown or -?
                        usage();
                        return;
                    }
                }
                else {
                    // Must be the crawl order file.
                    if(crawlOrderFile != null){
                        // Trying to set the crawl order file for the second
                        // time. This is an error.
                        usage();
                        return;
                    }
                    else{
                        // Must end with '.xml'
                        if(arg.length() > 4 && 
                            arg.substring(arg.length()-4)
                                .equalsIgnoreCase(".xml"))
                        {
                            crawlOrderFile = arg;
                        }
                        else
                        {
                            usage();
                            return;
                        }
                    }
                }
            }
            
            if(crawlOrderFile == null){
                if(noWUI){
                    // Can't do anything.
                    usage();
                    return;
                }
                crawllaunch = 0;
            }
            
            if(port == -1){
                // No port, use default.
                port = SimpleHttpServer.DEFAULT_PORT;
            }
            
            if(crawllaunch == -1 && crawlOrderFile != null && noWUI == false)
            {
                //Set default crawllaunch behavior
                crawllaunch = 2;
            }
            // Ok, we should now have everything to launch the program.
            
            if(noWUI){
                launch(crawlOrderFile);
            }
            else{
                launch(port,crawlOrderFile,crawllaunch,admin,user);
            }
        }
    }
    
    /**
     * If the user hasn't altered the default logging parameters,
     * tighten them up somewhat: some of our libraries are way
     * too verbose at the INFO or WARNING levels. 
     */
    private static void patchLogging() {
        if (System.getProperty("java.util.logging.config.class")!=null) {
            return;
        }
        if (System.getProperty("java.util.logging.config.file")!=null) {
            return;
        }
        // no user-set logging properties established; use defaults 
        // from distribution-packaged 'heritrix.properties'
        InputStream properties 
            = ClassLoader.getSystemResourceAsStream(PROPERTIES);
        if (properties != null ) {
            try {
                LogManager.getLogManager().readConfiguration(properties);
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Launch the crawler without a web UI
     * 
     * @param crawlOrderFile The crawl order to crawl.
     */
    protected static void launch(String crawlOrderFile){
        try {
            XMLSettingsHandler handler = new XMLSettingsHandler(new File(crawlOrderFile));
            handler.initialize();
            CrawlController controller = new CrawlController();
            controller.initialize(handler);
            controller.startCrawl();



            //CrawlController controller = new CrawlController();
            //CrawlOrder order = CrawlOrder.readFromFile(crawlOrderFile);
            //controller.initialize(order);
            //controller.startCrawl();
            // catch all configuration exceptions, which at this level are fatal
        }catch(InitializationException e){
            System.out.println("Fatal configuration exception: " + 
                    e.toString());
            return; 
        } catch (InvalidAttributeValueException e) {
            System.out.println("Fatal configuration exception: " + 
                    e.toString());
            return; 
        }
        System.out.println("Heritrix " + getVersion() + " is running.");
        System.out.println("\tNo web UI");
        System.out.println("\tCrawling " + crawlOrderFile);
    }
    
    /**
     * Launch the crawler with a web UI
     * 
     * @param port The port that the web UI will run on
     * @param crawlOrderFile A crawl order file to use
     * @param crawllaunch How to use the crawl order file 
     *                       (1 = start crawling, 
     *                     2 = ready for crawl but don't start,
     *                     3 = set as default configuration,
     *                        Any other = no crawl order specified)
     */
    protected static void launch(int port, String crawlOrderFile,
                                 int crawllaunch, String admin, String user)
    {
        handler = new SimpleHandler();
        String status = "";
        
        // Deconstruction of login permissions.
        String adminUN = admin.substring(0,admin.indexOf(":"));
        String adminPW = admin.substring(admin.indexOf(":")+1);
        User.addLogin(adminUN, adminPW,User.ADMINISTRATOR);
        String userUN = user.substring(0,user.indexOf(":"));
        String userPW = user.substring(user.indexOf(":")+1);
		User.addLogin(userUN, userPW,User.USER);

        if(crawllaunch == 3){
            // Set crawl order file as new default 
            handler.setDefaultCrawlOrder(crawlOrderFile);
            status = "\t- default crawl order updated to match: " + 
                crawlOrderFile;
        }
        else if(crawllaunch == 1 || crawllaunch == 2){
            CrawlJob cjob = new SimpleCrawlJob(handler.getNextJobUID(),
                    "Auto launched",crawlOrderFile, CrawlJob.PRIORITY_HIGH);
            handler.addJob(cjob);
            status = "\t1 crawl job ready and pending: " + crawlOrderFile;
            if(crawllaunch == 1){
                handler.startCrawler();
                status = "\t1 job being crawled: " + crawlOrderFile;
            }
        }

        try {
            SimpleHttpServer server = new SimpleHttpServer(port);
            server.startServer();
        } catch (Exception e) {
            System.out.println("Fatal IO error: " + e.getMessage());
            return;
        }
        System.out.println("Heritrix is running");
        System.out.println(" Web UI on port " + port);
        try {
          InetAddress addr = InetAddress.getLocalHost();
          
          // Get IP Address
          byte[] ipAddr = addr.getAddress();
          
          // Get hostname
          String hostname = addr.getHostName();
          System.out.println(" http://"+hostname+":"+port+"/admin");
        } catch (UnknownHostException e) {
        }
        System.out.println(
      " operator login/password = "
        + adminUN
        + "/"
        + adminPW);
        System.out.println(status);
    }

    /**
     * Print out the command line argument usage for this program.
     * <p>
     * <pre>
     * Usage: java org.archive.crawler.Heritrix --help
     * Usage: java org.archive.crawler.Heritrix --no-wui ORDER.XML
     * Usage: java org.archive.crawler.Heritrix [--port=PORT] \
     *       [--admin username:password] [--user username:password] \ 
     *       [ORDER.XML [--start|--wait|--set]]
     * Options:
     * --help|-h   Prints this message.
     * --no-wui    Start crawler without a web User Interface.
     * --port      PORT is port the web UI runs on. Default: 8080.
     * --admin     Set the username and password for the WUI administrator.
     * --user      Set the username and password for the WUI lesser access. 
     * ORDER.XML   The crawl to launch. Optional if '--no-wui' NOT specified.
     * --start     Start crawling using specified ORDER.XML:
     * --wait      Load job specified by ORDER.XML but do not start. Default.
     * --set       Set specified ORDER.XML as the default.
     *</pre>
     */
    protected static void usage() {
        System.out.println("Heritrix version " + getVersion());
        System.out.print("Usage: java org.archive.crawler.Heritrix");
        System.out.println(" --help|-h");
        System.out.print("Usage: java org.archive.crawler.Heritrix");
        System.out.println(" --no-wui ORDER.XML");
        System.out.print("Usage: java org.archive.crawler.Heritrix");
        System.out.println(" [--port=PORT] \\");
        System.out.println("\t\t\t[--admin username:password] [--user username:password] \\");
		System.out.println("\t\t\t[ORDER.XML [--start|--wait|--set]]");
        System.out.println("Options:");
        System.out.println("  --help|-h\tPrints this message.");
        System.out.print("  --no-wui\t");
        System.out.println("Start crawler without a web User Interface.");
		System.out.print("  --admin\t");
		System.out.println("Set the username and password for the WUI administrator.");
		System.out.print("  --user\t");
		System.out.println("Set the username and password for the WUI lesser access.");
        System.out.print("  ORDER.XML\t");
        System.out.print("The crawl to launch. Optional if '--no-wui'");
        System.out.println(" NOT specified.");
        System.out.print("  --start\t");
        System.out.println("Start crawling using specified ORDER.XML:");
        System.out.print("  --wait\t");
        System.out.print("Load job specified by ORDER.XML but do not start.");
        System.out.println(" Default.");
        System.out.print("  --set\t\t");
        System.out.println("Set specified ORDER.XML as the default.");
    }

    /**
     * Get the heritrix version.
     *
     * @return The heritrix version.  May be null.
     */
    public static String getVersion()
    {
        return (properties != null)? properties.getProperty(VERSION): null;
    }
    
    /**
     * Return heritrix properties. 
     * 
     * @return The returned Properties contains the content of 
     * heritrix.properties.  May be null if we failed initial read of 
     * 'heritrix.properties'.
     */
    public static Properties getProperties()
    {
    	return properties;
    }
    
    protected static void loadProperties()
    {
    	InputStream is = ClassLoader.getSystemResourceAsStream(PROPERTIES);
        if (is == null)
        {
            logger.warning("Failed to find heritrix.properties on classpath.");
        }
        else
        {
        	properties = new Properties();
        	try
    		{
                properties.load(is);
        	}
                
        	catch(IOException e)
			{
        		logger.warning("Failed loading heritrix properties: " +
                     e.getMessage());
        	}
        }
    }   
    
    /**
     * Get the job handler
     * 
     * @return The CrawlJobHandler being used.
     */    
    public static SimpleHandler getHandler()
    {
        return handler;    
    }
}
