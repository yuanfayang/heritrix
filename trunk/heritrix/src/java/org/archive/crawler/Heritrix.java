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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.InvalidAttributeValueException;

import org.apache.commons.cli.Option;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.auth.User;
import org.archive.crawler.datamodel.settings.XMLSettingsHandler;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.garden.SelftestCrawlJobHandler;


/**
 * Main class for Heritrix crawler.
 * 
 * Heritrix is launched by a shell script that backgrounds heritrix and
 * that redirects all stdout and stderr emitted by heritrix to a log file.  So
 * that startup messages emitted subsequent to the redirection of stdout and
 * stderr show on the console, this class prints usage or startup output 
 * such as where the web ui can be found, etc., to a STARTLOG that the shell
 * script is waiting on.  As soon as the shell script sees output in this file,
 * it prints its content and breaks out of its wait.  See heritrix.sh.
 * 
 * @author gojomo
 * @author Kristinn Sigurdsson
 *
 */
public class Heritrix
{    
    /**
     * Name of the heritrix home system property.
     */
    private static final String HOME_KEY = "heritrix.home";
    
    /**
     * Name of the heritrix property whose presence says we're running w/ 
     * development file locations: i.e conf, webapps, and profiles are under
     * the src directory rather than at top-level.
     */
    private static final String DEVELOPMENT_KEY = "heritrix.development";
    
    /**
     * Name of the heritrix properties file.
     */
    private static final String PROPERTIES = "heritrix.properties";
    
    /**
     * Name of the heritrix version property.
     */
    private static final String VERSION_KEY = "heritrix.version";
    
    /**
     * Key to pull the heritrix jobs directory location from properties file.
     */
    private static final String JOBSDIR_KEY = "heritrix.jobsdir";
    
    /**
     * Default jobs dir location.
     */
    private static final String JOBSDIR_DEFAULT = "jobs";
    
    /**
     * The heritrix home directory.
     * 
     * Need this location so we can get at our configuration.  Is null if 
     * dir is where the JVM was launched from and no heritrix.home property 
     * supplied.
     */
    private static File heritrixHome = null;
    
    private static File confdir = null;
    private static File jobsdir = null;
    
    /**
     * Where to find WAR files to deploy under servlet container.
     */
    private static File warsdir = null;
    
    /**
     * Instance of web server if one was started.
     */
    private static SimpleHttpServer httpServer = null;

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
     * CrawlJob handler. Manages multiple crawl jobs at runtime.
     */
    protected static CrawlJobHandler jobHandler = null;

    /**
     * Heritrix start log file.
     * 
     * This file contains standard out produced by this main class.
     * Soley used by heritrix shell script.
     */
	private static final String STARTLOG = "heritrix_dmesg.log";
	
    /**
     * Where to write this classes startup output.
     */
	private static PrintWriter out = null;

    /**
     * Name of the file to which heritrix logs stdout and stderr.
     */
    private static final String HERITRIX_OUT_FILE = "heritrix_out.log";

    /**
     * When running selftest, we set in here the URL for the selftest garden.
     */
    private static String getSelftestURL = null;
    

    /**
     * Launch program
     * 
     * @param args Command line arguments.
     * 
     * @throws Exception
     */
    public static void main(String[] args)
        throws Exception
    {
        out = getOut();
        
        try
        {
            initialize();
            doStart(args);
        }
        
        catch(Exception e)
        {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(out);
            throw e;
        }
        
        finally
        {
            // Its important the STARTLOG output stream gets closed.  Its a 
            // signal to the shell that started us up that startup is done --
            // or that it failed -- and that it can move on from waiting.
            out.close();
        }
    }
    
    /**
     * If in development we print on System.out else to STARTLOG.
     * 
     * @return Printwriter to use.
     */
	private static PrintWriter getOut() throws FileNotFoundException
    {
        return new PrintWriter(isDevelopment()?
             System.out: 
             new PrintStream(new FileOutputStream(new File(STARTLOG))));
	}

	private static void initialize()
        throws IOException
    {
        String home = System.getProperty(HOME_KEY);
        if (home != null && home.length() > 0)
        {
            heritrixHome = new File(home);
            if (!heritrixHome.exists())
            {
                throw new IOException("HERITRIX_HOME <" + home +
                    "> does not exist.");
            }
        }
        confdir = getSubDir("conf");
        loadProperties();
        patchLogging();
        warsdir = getSubDir("webapps");
        String jobsdirStr = getProperty(JOBSDIR_KEY, JOBSDIR_DEFAULT);
        jobsdir =
            (jobsdirStr.startsWith(File.separator) || heritrixHome == null)?
                new File(jobsdirStr): new File(heritrixHome, jobsdirStr); 
    }
    
    /**
     * Check for existence of expected subdir.
     * 
     * If development flag set, then look for dir under src dir.
     * 
     * @param subdirName Dir to look for.
     * @return The extant subdir.
     * @throws IOException if unable to find expected subdir.
     */
    private static File getSubDir(String subdirName)
        throws IOException
    {
        String path = isDevelopment()?
            "src" + File.separator + subdirName: subdirName;
        File dir = (heritrixHome != null)?
            new File(heritrixHome, path): new File(path);
        if (!dir.exists())
        {
            throw new IOException("Cannot find subdir: " + subdirName);
        }
        return dir;
    }

    private static void doStart(String [] args)
        throws Exception
    {
        boolean noWui = false;
        int port = SimpleHttpServer.DEFAULT_PORT;
        String crawlOrderFile = null;
        String adminLoginPassword = "admin:letmein";
        boolean runMode = false;
        boolean selfTest = false;
 
        CommandLineParser clp = new CommandLineParser(args, out, getVersion());
        List arguments = clp.getCommandLineArguments();
        Option [] options = clp.getCommandLineOptions();
        
        // Check passed argument.  Only one argument, the ORDER_FILE is allowed.
        // If one argument, make sure exists and xml suffix.
        if (arguments.size() > 1)
        {
            clp.usage(1);
        }
        else if (arguments.size() == 1)
        {
            crawlOrderFile = (String)arguments.get(0);
            if (!(new File(crawlOrderFile).exists()))
            {
                clp.usage("ORDER_FILE <" + crawlOrderFile +
                    "> specified does not exist.", 1);
            }
            // Must end with '.xml'
            if (crawlOrderFile.length() > 4 &&
                    !crawlOrderFile.substring(crawlOrderFile.length() - 4).
                        equalsIgnoreCase(".xml"))
            {
                clp.usage("ORDER_FILE <" + crawlOrderFile +
                    "> does not have required '.xml' suffix.", 1);
            }
        }
        
        // Now look at options passed.
        for (int i = 0; i < options.length; i++)
        {
            switch(options[i].getId())
            {
                case 'h':
                    clp.usage();
                    break;
                
                case 'a':
                    adminLoginPassword = options[i].getValue();
                    if (!isValidLoginPasswordString(adminLoginPassword))
                    {
                        clp.usage("Invalid admin login/password value.", 1);
                    }
                    break;
                
                case 'n':
                    if (crawlOrderFile == null)
                    {
                        clp.usage("You must specify an ORDER_FILE with" +
                            " '--nowui' option.", 1);
                    }
                    noWui = true;
                    break;
                
                case 'p':
                    try
                    {
                        port = Integer.parseInt(options[i].getValue());
                    }
                    catch (NumberFormatException e)
                    {
                        clp.usage("Failed parse of port number: " +
                            options[i].getValue(), 1);
                    }
                    if (port <= 0)
                    {
                        clp.usage("Nonsensical port number: " +
                            options[i].getValue(), 1);
                    }
                    break;
                
                case 'r':
                    runMode = true;
                    break;
                
                case 's':
                    selfTest = true;
                    break;
                
                default:
                    assert false: options[i].getId();
            }
        }    
            
        // Ok, we should now have everything to launch the program.
        if (selfTest)
        {
            // If more than just '--selftest' and '--port' passed, then
            // there is confusion on what is being asked of us.  Print usage
            // rather than proceed.
            for (int i = 0; i < options.length; i++)
            {
                if (options[i].getId() != 'p' && options[i].getId() != 's')
                {
                    clp.usage(1);
                }
            }
            
            if (arguments.size() > 0)
            {
                // No arguments accepted by selftest.
                clp.usage(1);
            }
            selftest(port);
        }
        else if (noWui)
        {
            if (options.length > 1)
            {
                // If more than just '--nowui' passed, then there is 
                // confusion on what is being asked of us.  Print usage
                // rather than proceed.
                clp.usage(1);
            }
            launch(crawlOrderFile);
        }
        else
        {
            launch(crawlOrderFile, runMode, port, adminLoginPassword);
        }
    }
    
    /**
     * Test string is valid login/password string.
     * 
     * A valid login/password string has the login and password compounded
     * w/ a ':' delimiter.
     * 
     * @param str String to test.
     * @return True if valid password/login string.
     */
    private static boolean isValidLoginPasswordString(String str)
    {
        boolean isValid = false;
        StringTokenizer tokenizer = new StringTokenizer(str,  ":");
        if (tokenizer.countTokens() == 2)
        {
            String login = (String)tokenizer.nextElement();
            String password = (String)tokenizer.nextElement();
            if (login.length() > 0 && password.length() > 0)
            {
                isValid = true;
            }
        }
        return isValid;
    }
    
    private static boolean isDevelopment()
    {
        return System.getProperty(DEVELOPMENT_KEY) != null;  
    }
    
    private static void loadProperties()
        throws IOException
    {    
        InputStream is =
            new FileInputStream(new File(getConfdir(), PROPERTIES));
        if (is != null)
        {
            properties = new Properties();
            properties.load(is);
        }
    }

    /**
     * If the user hasn't altered the default logging parameters, tighten them
     * up somewhat: some of our libraries are way too verbose at the INFO or
     * WARNING levels.
     * 
     * @throws IOException
     * @throws SecurityException
     */
    private static void patchLogging()
        throws SecurityException, IOException
    {   
        if (System.getProperty("java.util.logging.config.class") != null)
        {
            return;
        }
        
        if (System.getProperty("java.util.logging.config.file") != null)
        {
            return;
        }
        
        // No user-set logging properties established; use defaults 
        // from distribution-packaged 'heritrix.properties'
        InputStream is =
            new FileInputStream(new File(getConfdir(), PROPERTIES));
        if (is != null)
        {
            LogManager.getLogManager().readConfiguration(is);
        }
    }

    /**
     * Get a property value from the properties file or from system properties.
     * 
     * System property overrides content of heritrix.properties file.
     * 
     * @param key Key for property to find.
     * 
     * @return Property if found or default if no such property.
     */
    private static String getProperty(String key)
    {
        return getProperty(key, null);
    }
    
    /**
     * Get a property value from the properties file or from system properties.
     * 
     * System property overrides content of heritrix.properties file.
     * 
     * @param key Key for property to find.
     * @param fallback Default value to use if property not found.
     * 
     * @return Property if found or default if no such property.
     */
    private static String getProperty(String key, String fallback)
    {
        String value = System.getProperty(key);
        if (value == null && properties !=  null)
        {
            value = properties.getProperty(key);
        }
        return (value != null)? value: fallback;
    }
    
    /**
     * Run the selftest
     * 
     * @param port Port number to use for web UI.
     *
     * @exception Exception
     */
    private static void selftest(int port) throws Exception
    {
        // Put up the webserver.  It has the selftest garden to go against.
        Heritrix.httpServer = new SimpleHttpServer(port);
        Heritrix.httpServer.startServer();
        File selftestDir = new File(getConfdir(), "selftest");
        File crawlOrderFile = new File(selftestDir, "job-selftest.xml");
        Heritrix.jobHandler = new SelftestCrawlJobHandler();
        // Create a job based off the selftest order file.  Then use this as
        // a template to pass jobHandler.newJob().  Doing this gets our
        // selftest output to show under the jobs directory. 
        // Pass as a seed a pointer to the webserver we just put up.
        CrawlJob job = createCrawlJob(jobHandler, crawlOrderFile, "Template");
        Heritrix.getSelftestURL =
            "http://localhost:" + Integer.toString(port) + "/garden/";
        job = Heritrix.jobHandler.newJob(job, "selftest",
            "Integration self test", getSelftestURL);
        Heritrix.jobHandler.addJob(job);
        Heritrix.jobHandler.startCrawler();
        out.println((new Date()).toString() + " Heritrix " + getVersion() +
            " selftest started.");
        out.println("Selftest first crawls " + getSelftestURL() +
            " and then runs an analysis.");
        out.println("Result of analysis printed to " + HERITRIX_OUT_FILE +
            " when done.");
        out.println("Selftest job directory for logs and arcs:\n" +
            Heritrix.jobHandler.getJobdir(job).getAbsolutePath());
    }

    /**
     * Launch the crawler without a web UI
     * 
     * @param crawlOrderFile The crawl order to crawl.
     */
    private static void launch(String crawlOrderFile)
        throws InitializationException, IOException,
            InvalidAttributeValueException
    {
        XMLSettingsHandler handler =
            new XMLSettingsHandler(new File(crawlOrderFile));
        handler.initialize();
        CrawlController controller = new CrawlController();
        controller.initialize(handler);
        controller.startCrawl();
        out.println((new Date()).toString() + " Heritrix " + getVersion() +
            " crawl started using " + crawlOrderFile + ".");
    }
    
    /**
     * Launch the crawler with a web UI.
     * 
     * @param crawlOrderFile File to crawl.  May be null.
     * @param runMode Whether crawler should be set to run mode.
     * @param port Port number to use for web UI.
     * @param adminLoginPassword Compound of login and password.
     * 
     * @exception Exception
     * 
     */
    private static void launch(String crawlOrderFile, boolean runMode, 
            int port, String adminLoginPassword)
        throws Exception
    { 
        String adminUN =
            adminLoginPassword.substring(0, adminLoginPassword.indexOf(":"));
        String adminPW =
            adminLoginPassword.substring(adminLoginPassword.indexOf(":") + 1);
		User.addLogin(adminUN, adminPW, User.ADMINISTRATOR);
        
        String status = null;

        httpServer = new SimpleHttpServer(port);
        httpServer.startServer();

        jobHandler = new CrawlJobHandler();
        if (crawlOrderFile != null)
        {
            CrawlJob job = createCrawlJob(jobHandler, new File(crawlOrderFile),
                "Auto launched");
            jobHandler.addJob(job);
            if(runMode)
            {
                jobHandler.startCrawler();
                status = "Job being crawled: " + crawlOrderFile;
            }
            else
            {
                status = "Crawl job ready and pending: " + crawlOrderFile;
            }
        }
        else if(runMode)
        {
            // The use case is that jobs are to be run on a schedule and that
            // if the crawler is in run mode, then the scheduled job will be 
            // run at appropriate time.  Otherwise, not.
            jobHandler.startCrawler();
            status = "Crawler set to run mode but no order file to crawl";
        }

        InetAddress addr = InetAddress.getLocalHost();
        String uiLocation = "http://" + addr.getHostName() + ":" + port +
            "/admin";
        out.println((new Date()).toString() + " Heritrix " + getVersion() +
            " is running.");
        out.println("Web UI is at: " + uiLocation);
        out.println("Login and password: " + adminUN + "/" + adminPW);
        if (status != null)
        {
            out.println(status);
        }
    }
    
    private static CrawlJob createCrawlJob(CrawlJobHandler jobHandler,
            File crawlOrderFile, String descriptor)
        throws InvalidAttributeValueException, FileNotFoundException
    {
        if (!crawlOrderFile.exists())
        {
            throw new FileNotFoundException(crawlOrderFile.getAbsolutePath());
        }
        XMLSettingsHandler settings = new XMLSettingsHandler(crawlOrderFile);
        settings.initialize();
        return new CrawlJob(jobHandler.getNextJobUID(), descriptor, settings,
            CrawlJob.PRIORITY_HIGH);
    }

    /**
     * Get the heritrix version.
     *
     * @return The heritrix version.  May be null.
     */
    public static String getVersion()
    {
        return (properties != null)? properties.getProperty(VERSION_KEY): null;
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

    /**
     * Get the job handler
     * 
     * @return The CrawlJobHandler being used.
     */    
    public static CrawlJobHandler getJobHandler()
    {
        return jobHandler;    
    }
    
    /**
     * @return The conf directory under HERITRIX_HOME.
     */
    public static File getConfdir()
    {
        return confdir;
    }
    
    /**
     * @return The directory into which we put jobs.
     */
    public static File getJobsdir()
    {
        return jobsdir;
    }
    
    /**
     * @return Returns the httpServer. May be null if one was not started.
     */
    public static SimpleHttpServer getHttpServer()
    {
        return httpServer;
    }
    
    /**
     * Returns the selftest URL.
     * 
     * @return Returns the selftestWebappURL.  This method returns null if 
     * we are not in selftest.  URL has a trailing '/'.
     */
    public static String getSelftestURL()
    {
        return getSelftestURL;
    }
    
    /**
     * @return Returns the directory under which reside the WAR files 
     * we're to load into the servlet container.
     */
    public static File getWarsdir()
    {
        return warsdir;
    }
    
    /**
     * @return Returns the HERITRIX_OUT_FILE.
     */
    public static String getHERITRIX_OUT_FILE()
    {
        return HERITRIX_OUT_FILE;
    }
}
