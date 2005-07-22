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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.commons.cli.Option;
import org.archive.crawler.admin.Alert;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobErrorHandler;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.selftest.SelfTestCrawlJobHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURI;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.JmxUtils;
import org.archive.util.TextUtils;

import sun.net.www.protocol.file.FileURLConnection;


/**
 * Main class for Heritrix crawler.
 *
 * Heritrix is launched by a shell script that backgrounds heritrix and
 * that redirects all stdout and stderr emitted by heritrix to a log file.  So
 * that startup messages emitted subsequent to the redirection of stdout and
 * stderr show on the console, this class prints usage or startup output
 * such as where the web UI can be found, etc., to a STARTLOG that the shell
 * script is waiting on.  As soon as the shell script sees output in this file,
 * it prints its content and breaks out of its wait.
 * See ${HERITRIX_HOME}/bin/heritrix.
 *
 * @author gojomo
 * @author Kristinn Sigurdsson
 *
 */
public class Heritrix implements DynamicMBean {
    /**
     * Heritrix logging instance.
     */
    private static final Logger logger =
        Logger.getLogger(Heritrix.class.getName());
    
    private static final File TMPDIR =
        new File(System.getProperty("java.io.tmpdir", "/tmp"));

    /**
     * Name of the heritrix properties file.
     */
    private static final String PROPERTIES = "heritrix.properties";

    /**
     * Name of the key to use specifying alternate heritrix properties on
     * command line.
     */
    private static final String PROPERTIES_KEY = PROPERTIES;

    /**
     * Heritrix properties.
     *
     * Read from properties file on startup and cached thereafter.
     */
    private static Properties properties = null;

    /**
     * Instance of web server if one was started.
     */
    private static SimpleHttpServer httpServer = null;

    /**
     * CrawlJob handler. Manages multiple crawl jobs at runtime.
     */
    private static CrawlJobHandler jobHandler = null;

    /**
     * Heritrix start log file.
     *
     * This file contains standard out produced by this main class for startup
     * only.  Used by heritrix shell script.  Name here MUST match that in the
     * <code>bin/heritrix</code> shell script.  This is a DEPENDENCY the shell
     * wrapper has on this here java heritrix.
     */
    private static final String STARTLOG = "heritrix_dmesg.log";

    /**
     * Default encoding.
     * 
     * Used for content when fetching if none specified.
     */
	public static final String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * Heritrix stderr/stdout log file.
     *
     * This file should have nothing in it except messages over which we have
     * no control (JVM stacktrace, 3rd-party lib emissions).  The wrapper
     * startup script directs stderr/stdout here. This is an INTERDEPENDENCY
     * this program has with the wrapper shell script.  Shell can actually
     * pass us an alternate to use for this file.
     */
    private static String DEFAULT_HERITRIX_OUT = "heritrix_out.log";

    /**
     * Where to write this classes startup output.
     * 
     * This out should only be used if Heritrix is being run from the
     * command-line.
     */
    private static PrintWriter out = null;

    /**
     * When running selftest, we set in here the URL for the selftest.
     */
    private static String selftestURL = null;

    /**
     * Alerts that have occured
     */
    private static Vector alerts = new Vector();

    /**
     * The crawler package.
     */
	private static final String CRAWLER_PACKAGE = Heritrix.class.getName().
        substring(0, Heritrix.class.getName().lastIndexOf('.'));
    
    /**
     * The root context for a webapp.
     */
    private static final String ROOT_CONTEXT = "/";

    /**
     * Set to true if application is running from a command line.
     */
    private static boolean commandLine = false;
    
    /**
     * Set if we're running with a web UI.
     */
    private static boolean noWui = false;
    
    // OpenMBean support.
    /**
     * The MBean we've registered ourselves with (May be null
     * throughout life of Heritrix).
     */
    private static MBeanServer mbeanServer = null;
    private static ObjectInstance registeredHeritrixObjectInstance =
        null;
    private final OpenMBeanInfoSupport openMBeanInfo;
    
    private final static String STATUS_ATTR = "Status";
    private final static List ATTRIBUTE_LIST;
    static {
        ATTRIBUTE_LIST = Arrays.asList(new String [] {STATUS_ATTR});
    }
    
    private final static String START_OPER = "start";
    private final static String STOP_OPER = "stop";
    private final static String INTERRUPT_OPER = "interrupt";
    private final static String START_CRAWLING_OPER = "startCrawling";
    private final static String STOP_CRAWLING_OPER = "stopCrawling";
    private final static String ADD_CRAWL_JOB_OPER = "addJob";
    private final static String ALERT_OPER = "alert";
    private final static String NEW_ALERT_OPER = "newAlert";
    private final static String ADD_CRAWL_JOB_BASEDON_OPER = "addJobBasedon";
    private final static String PENDING_JOBS_OPER = "pendingJobs";
    private final static String COMPLETED_JOBS_OPER = "completedJobs";
    private final static String CRAWLEND_REPORT_OPER = "crawlendReport";
    private final static List OPERATION_LIST;
    static {
        OPERATION_LIST = Arrays.asList(new String [] {START_OPER, STOP_OPER,
            INTERRUPT_OPER, START_CRAWLING_OPER, STOP_CRAWLING_OPER,
            ADD_CRAWL_JOB_OPER, ADD_CRAWL_JOB_BASEDON_OPER,
            ALERT_OPER, NEW_ALERT_OPER, PENDING_JOBS_OPER,
            COMPLETED_JOBS_OPER, CRAWLEND_REPORT_OPER});
    }
    private CompositeType jobCompositeType = null;
    private TabularType jobsTabularType = null;
    private static final String [] JOB_KEYS =
        new String [] {"uid", "name", "status"};
    
    private static final String JAR_SUFFIX = ".jar";

    /**
     * Constructor.
     * @throws IOException
     */
    public Heritrix()
    throws IOException {
        super();
        this.openMBeanInfo = buildMBeanInfo();
        Heritrix.loadProperties();
        Heritrix.patchLogging();
        // Register a shutdownHook so we get called on JVM KILL SIGNAL
        Runtime.getRuntime().addShutdownHook(new Thread("HeritrixShutdown") {
            public void run() {
                Heritrix.prepareHeritrixShutDown();
            }
        });
    } 
    
    /**
     * Launch program.
     *
     * @param args Command line arguments.
     *
     * @throws Exception
     */
    public static void main(String[] args)
    throws Exception {
        Heritrix.commandLine = true;
        
        // Set timezone here.  Would be problematic doing it if we're running
        // inside in a container.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        
        File startLog = new File(getHeritrixHome(), STARTLOG);
        Heritrix.out = new PrintWriter(isDevelopment()? 
            System.out: new PrintStream(new FileOutputStream(startLog)));
        
        try {
            loadProperties();
            patchLogging();
            configureTrustStore();
            String status = doCmdLineArgs(args);
            if (status != null) {
                Heritrix.out.println(status);
            }
        }

        catch(Exception e) {
            // Show any exceptions in STARTLOG.
            e.printStackTrace(Heritrix.out);
            throw e;
        }

        finally {
            // If not development, close the file that signals the wrapper
            // script that we've started.  Otherwise, just flush it; if in
            // development, the output is probably a console.
            if (!isDevelopment()) {
                if (Heritrix.out != null) {
                    Heritrix.out.close();
                }
                System.out.println("Heritrix version: " + getVersion());
            } else {
                if (Heritrix.out != null) {
                    Heritrix.out.flush();
                }
            }
        }
    }

    protected static String doCmdLineArgs(String [] args)
    throws Exception {
        // Get defaults for commandline arguments from the properties file.
        String tmpStr = getPropertyOrNull("heritrix.cmdline.port");
        int port = (tmpStr == null)?
            SimpleHttpServer.DEFAULT_PORT: Integer.parseInt(tmpStr);
        tmpStr = getPropertyOrNull("heritrix.cmdline.admin");
        String adminLoginPassword = (tmpStr == null)?
            "admin:letmein": tmpStr;
        String crawlOrderFile = getPropertyOrNull("heritrix.cmdline.order");
        tmpStr = getPropertyOrNull("heritrix.cmdline.run");
        boolean runMode = getBooleanProperty("heritrix.cmdline.run");
        Heritrix.noWui = getBooleanProperty("heritrix.cmdline.nowui");    
        boolean selfTest = false;
        String selfTestName = null;
        CommandLineParser clp = new CommandLineParser(args, Heritrix.out,
            getVersion());
        List arguments = clp.getCommandLineArguments();
        Option [] options = clp.getCommandLineOptions();

        // Check passed argument.  Only one argument, the ORDER_FILE is allowed.
        // If one argument, make sure exists and xml suffix.
        if (arguments.size() > 1) {
            clp.usage(1);
        } else if (arguments.size() == 1) {
            crawlOrderFile = (String)arguments.get(0);
            if (!(new File(crawlOrderFile).exists())) {
                clp.usage("ORDER.XML <" + crawlOrderFile +
                    "> specified does not exist.", 1);
            }
            // Must end with '.xml'
            if (crawlOrderFile.length() > 4 &&
                    !crawlOrderFile.substring(crawlOrderFile.length() - 4).
                        equalsIgnoreCase(".xml")) {
                clp.usage("ORDER.XML <" + crawlOrderFile +
                    "> does not have required '.xml' suffix.", 1);
            }
        }

        // Now look at options passed.
        for (int i = 0; i < options.length; i++) {
            switch(options[i].getId()) {
                case 'h':
                    clp.usage();
                    break;

                case 'a':
                    adminLoginPassword = options[i].getValue();
                    if (!isValidLoginPasswordString(adminLoginPassword)) {
                        clp.usage("Invalid admin login/password value.", 1);
                    }
                    break;

                case 'n':
                    if (crawlOrderFile == null) {
                        clp.usage("You must specify an ORDER_FILE with" +
                            " '--nowui' option.", 1);
                    }
                    Heritrix.noWui = true;
                    break;

                case 'p':
                    try {
                        port = Integer.parseInt(options[i].getValue());
                    } catch (NumberFormatException e) {
                        clp.usage("Failed parse of port number: " +
                            options[i].getValue(), 1);
                    }
                    if (port <= 0) {
                        clp.usage("Nonsensical port number: " +
                            options[i].getValue(), 1);
                    }
                    break;

                case 'r':
                    runMode = true;
                    break;

                case 's':
                    selfTestName = options[i].getValue();
                    selfTest = true;
                    break;

                default:
                    assert false: options[i].getId();
            }
        }

        String status = null;
        // Ok, we should now have everything to launch the program.
        if (selfTest) {
            // If more than just '--selftest' and '--port' passed, then
            // there is confusion on what is being asked of us.  Print usage
            // rather than proceed.
            for (int i = 0; i < options.length; i++) {
                if (options[i].getId() != 'p' && options[i].getId() != 's') {
                    clp.usage(1);
                }
            }

            if (arguments.size() > 0) {
                // No arguments accepted by selftest.
                clp.usage(1);
            }
            Heritrix h = new Heritrix();
            registerHeritrixMBean(h);
            status = h.selftest(selfTestName, port);
        } else if (Heritrix.noWui) {
            if (options.length > 1) {
                // If more than just '--nowui' passed, then there is
                // confusion on what is being asked of us.  Print usage
                // rather than proceed.
                clp.usage(1);
            }
            Heritrix h = new Heritrix();
            registerHeritrixMBean(h);
            status = h.doOneCrawl(crawlOrderFile);
        } else {
            status = startEmbeddedWebserver(port, adminLoginPassword);
            Heritrix h = new Heritrix();
            registerHeritrixMBean(h);
            String tmp = h.launch(crawlOrderFile, runMode);
            if (tmp != null) {
                status += ('\n' + tmp);
            }
        }
        return status;
    }
    
    /**
     * @return The file we dump stdout and stderr into.
     */
    public static String getHeritrixOut() {
        String tmp = System.getProperty("heritrix.out");
        if (tmp == null || tmp.length() == 0) {
            tmp = Heritrix.DEFAULT_HERITRIX_OUT;
        }
        return tmp;
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
    
    /**
     * @return The directory into which we put jobs.
     * @throws IOException
     */
    public static File getJobsdir() throws IOException {
        String jobsdirStr = getProperty("heritrix.jobsdir", "jobs");
        return jobsdirStr.startsWith(File.separator)?
            new File(jobsdirStr):
            new File(getHeritrixHome(), jobsdirStr);
    }
    
    /**
     * Get and check for existence of expected subdir.
     *
     * If development flag set, then look for dir under src dir.
     *
     * @param subdirName Dir to look for.
     * @return The extant subdir.  Otherwise null if we're running
     * in a webapp context where there is no conf directory available.
     * @throws IOException if unable to find expected subdir.
     */
    protected static File getSubDir(String subdirName)
    throws IOException {
        return getSubDir(subdirName, true);
    }
    
    /**
     * Get and optionally check for existence of subdir.
     *
     * If development flag set, then look for dir under src dir.
     *
     * @param subdirName Dir to look for.
     * @param fail True if we are to fail if directory does not
     * exist; false if we are to return false if the directory does not exist.
     * @return The extant subdir.  Otherwise null if we're running
     * in a webapp context where there is no subdir directory available.
     * @throws IOException if unable to find expected subdir.
     */
    protected static File getSubDir(String subdirName, boolean fail)
    throws IOException {
        String path = isDevelopment()?
            "src" + File.separator + subdirName:
            subdirName;
        File dir = new File(getHeritrixHome(), path);
        if (!dir.exists()) {
            if (fail) {
                throw new IOException("Cannot find subdir: " + subdirName);
            }
            dir = null;
        }
        return dir;
    }
    
    /**
     * @param key Property key.
     * @return Named property or null if the property is null or empty.
     */
    protected static String getPropertyOrNull(String key) {
        String value = (String)Heritrix.properties.get(key);
        return (value == null || value.length() <= 0)? null: value;
    }
    
    /**
     * @param key Property key.
     * @return Boolean value or false if null or unreadable.
     */
    protected static boolean getBooleanProperty(String key) {
        return (getPropertyOrNull(key) == null)?
            false: Boolean.valueOf(getPropertyOrNull(key)).booleanValue();
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
    protected static boolean isValidLoginPasswordString(String str) {
        boolean isValid = false;
        StringTokenizer tokenizer = new StringTokenizer(str,  ":");
        if (tokenizer.countTokens() == 2) {
            String login = ((String)tokenizer.nextElement()).trim();
            String password = ((String)tokenizer.nextElement()).trim();
            if (login.length() > 0 && password.length() > 0) {
                isValid = true;
            }
        }
        return isValid;
    }

    protected static boolean isDevelopment() {
        return System.getProperty("heritrix.development") != null;
    }

    /**
     * Loads the heritrix.properties file.
     * 
     * Also loads any property that starts with
     * <code>CRAWLER_PACKAGE</code> into system properties
     * (except logging '.level' directives).
     * 
     * @throws IOException
     */
    protected static void loadProperties()
    throws IOException {
        if (Heritrix.properties != null) {
            return;   
        }
        Heritrix.properties = new Properties();
        Heritrix.properties.load(getPropertiesInputStream());
        
        // Any property that begins with CRAWLER_PACKAGE, make it
        // into a system property.
        for (Enumeration e = properties.keys(); e.hasMoreElements();) {
            String key = ((String)e.nextElement()).trim();
        	if (key.startsWith(CRAWLER_PACKAGE)) {
                // Don't add the heritrix.properties entries that are
                // changing the logging level of particular classes.
                if (key.indexOf(".level") < 0) {
                	System.setProperty(key,
                        properties.getProperty(key).trim());
                }
            }
        }
    }

    protected static InputStream getPropertiesInputStream()
    throws IOException {
        File file = null;
        // Look to see if properties have been passed on the cmd-line.
        String alternateProperties = System.getProperty(PROPERTIES_KEY);
        if (alternateProperties != null && alternateProperties.length() > 0) {
            file = new File(alternateProperties);
        }
        // Get properties from conf directory if one available.
        if ((file == null || !file.exists()) && getConfdir(false) != null) {
            file = new File(getConfdir(), PROPERTIES);
            if (!file.exists()) {
                // If no properties file in the conf dir, set file back to
                // null so we go looking for heritrix.properties on classpath.
                file = null;
            }
        }
        // If not on the command-line, there is no conf dir. Then get the
        // properties from the CLASSPATH (Classpath file separator is always
        // '/', whatever the platform.
        InputStream is = (file != null)?
            new FileInputStream(file):
            Heritrix.class.getResourceAsStream("/" + PROPERTIES_KEY);
        if (is == null) {
            throw new IOException("Failed to load properties file from" +
                " filesystem or from classpath.");
        }
        return is;
    }

    /**
     * If the user hasn't altered the default logging parameters, tighten them
     * up somewhat: some of our libraries are way too verbose at the INFO or
     * WARNING levels.
     * 
     * This might be a problem running inside in someone else's
     * container.  Container's seem to prefer commons logging so we
     * ain't messing them doing the below.
     *
     * @throws IOException
     * @throws SecurityException
     */
    protected static void patchLogging()
    throws SecurityException, IOException {
        if (System.getProperty("java.util.logging.config.class") != null) {
            return;
        }

        if (System.getProperty("java.util.logging.config.file") != null) {
            return;
        }

        // No user-set logging properties established; use defaults
        // from distribution-packaged 'heritrix.properties'.
        LogManager.getLogManager().
            readConfiguration(getPropertiesInputStream());
    }

    /**
     * Configure our trust store.
     *
     * If system property is defined, then use it for our truststore.  Otherwise
     * use the heritrix truststore under conf directory if it exists.
     * 
     * <p>If we're not launched from the command-line, we will not be able
     * to find our truststore.  The truststore is nor normally used so rare
     * should this be a problem (In case where we don't use find our trust
     * store, we'll use the 'default' -- either the JVMs or the containers).
     */
    protected static void configureTrustStore() {
        // Below must be defined in jsse somewhere but can' find it.
        final String TRUSTSTORE_KEY = "javax.net.ssl.trustStore";
        String value = getProperty(TRUSTSTORE_KEY);
        File confdir = null;
        try {
			confdir = getConfdir(false);
		} catch (IOException e) {
			logger.warning("Failed to get confdir.");
		}
        if ((value == null || value.length() <= 0) && confdir != null) {
            // Use the heritrix store if it exists on disk.
            File heritrixStore = new File(confdir, "heritrix.cacerts");
            if(heritrixStore.exists()) {
                value = heritrixStore.getAbsolutePath();
            }
        }

        if (value != null && value.length() > 0) {
            System.setProperty(TRUSTSTORE_KEY, value);
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
    public static String getProperty(String key) {
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
    public static String getProperty(String key, String fallback)
    {
        String value = System.getProperty(key);
        if (value == null && properties !=  null) {
            value = properties.getProperty(key);
        }
        return (value != null)? value: fallback;
    }
    
    
    public static int getIntProperty(String key, int fallback) {
        String tmp = getProperty(key);
        int result = fallback;
        if (tmp != null && tmp.length() > 0) {
            result = Integer.parseInt(tmp);
        }
        return result;
    }

    /**
     * Run the selftest
     *
     * @param oneSelfTestName Name of a test if we are to run one only rather
     * than the default running all tests.
     * @param port Port number to use for web UI.
     *
     * @exception Exception
     * @return Status of how selftest startup went.
     */
    protected String selftest(String oneSelfTestName, int port)
        throws Exception {
        // Put up the webserver w/ the root and selftest webapps only.
        final String SELFTEST = "selftest";
        Heritrix.httpServer = new SimpleHttpServer(SELFTEST, ROOT_CONTEXT,
            port, true);
        // Set up digest auth for a section of the server so selftest can run
        // auth tests.  Looks like can only set one login realm going by the
        // web.xml dtd.  Otherwise, would be nice to selftest basic and digest.
        // Have login, password and role all be SELFTEST.  Must match what is
        // in the selftest order.xml file.
        Heritrix.httpServer.setAuthentication(SELFTEST, ROOT_CONTEXT, SELFTEST,
            SELFTEST, SELFTEST);
        // Start server.
        Heritrix.httpServer.startServer();
        // Get the order file from the CLASSPATH unless we're running in dev
        // environment.
        File selftestDir = (isDevelopment())?
            new File(getConfdir(), SELFTEST):
            new File(File.separator + SELFTEST);
        File crawlOrderFile = new File(selftestDir, "order.xml");
        Heritrix.jobHandler = new SelfTestCrawlJobHandler(oneSelfTestName);
        // Create a job based off the selftest order file.  Then use this as
        // a template to pass jobHandler.newJob().  Doing this gets our
        // selftest output to show under the jobs directory.
        // Pass as a seed a pointer to the webserver we just put up.
        final String ROOTURI = "127.0.0.1:" + Integer.toString(port);
        Heritrix.selftestURL = "http://" + ROOTURI + '/';
        if (oneSelfTestName != null && oneSelfTestName.length() > 0) {
            selftestURL += (oneSelfTestName + '/');
        }
        CrawlJob job = createCrawlJob(jobHandler, crawlOrderFile, "Template");
        job = Heritrix.jobHandler.newJob(job, false, SELFTEST,
            "Integration self test", Heritrix.selftestURL,
            CrawlJob.PRIORITY_CRITICAL);
        Heritrix.jobHandler.addJob(job);
        // Before we start, need to change some items in the settings file.
        CredentialStore cs = (CredentialStore)job.getSettingsHandler().
            getOrder().getAttribute(CredentialStore.ATTR_NAME);
        for (Iterator i = cs.iterator(null); i.hasNext();) {
            ((Credential)i.next()).setCredentialDomain(null, ROOTURI);
        }
        Heritrix.jobHandler.startCrawler();
        StringBuffer buffer = new StringBuffer();
        buffer.append("Heritrix " + getVersion() + " selftest started.");
        buffer.append("\nSelftest first crawls " + getSelftestURL() +
            " and then runs an analysis.");
        buffer.append("\nResult of analysis printed to " +
            getHeritrixOut() + " when done.");
        buffer.append("\nSelftest job directory for logs and arcs:\n" +
            job.getDirectory().getAbsolutePath());
        return buffer.toString();
    }

    /**
     * Launch the crawler without a web UI.
     * 
     * Run the passed crawl only.
     *
     * @param crawlOrderFile The crawl order to crawl.
     * @throws InitializationException
     * @throws InvalidAttributeValueException
     * @return Status string.
     */
    protected String doOneCrawl(String crawlOrderFile)
    throws InitializationException, InvalidAttributeValueException {
        return doOneCrawl(crawlOrderFile, null);
    }
    
    /**
     * Launch the crawler without a web UI.
     * 
     * Run the passed crawl only.
     *
     * @param crawlOrderFile The crawl order to crawl.
     * @param listener Register this crawl status listener before starting
     * crawl (You can use this listener to notice end-of-crawl).
     * @throws InitializationException
     * @throws InvalidAttributeValueException
     * @return Status string.
     */
    protected String doOneCrawl(String crawlOrderFile,
        CrawlStatusListener listener)
    throws InitializationException, InvalidAttributeValueException {
        XMLSettingsHandler handler =
            new XMLSettingsHandler(new File(crawlOrderFile));
        handler.initialize();
        CrawlController controller = new CrawlController();
        controller.initialize(handler);
        if (listener != null) {
            controller.addCrawlStatusListener(listener);
        }
        controller.requestCrawlStart();
        return "Crawl started using " + crawlOrderFile + ".";
    }
    
    /**
     * Launch the crawler for a web UI.
     *
     * Crawler hangs around waiting on jobs.
     *
     * @exception Exception
     * @return A status string describing how the launch went.
     * @throws Exception
     */
    public String launch() throws Exception {
        return launch(null, false);
    }

    /**
     * Launch the crawler for a web UI.
     *
     * Crawler hangs around waiting on jobs.
     * 
     * @param crawlOrderFile File to crawl.  May be null.
     * @param runMode Whether crawler should be set to run mode.
     *
     * @exception Exception
     * @return A status string describing how the launch went.
     */
    public String launch(String crawlOrderFile, boolean runMode)
    throws Exception {
        Heritrix.jobHandler = new CrawlJobHandler();
        String status = null;
        if (crawlOrderFile != null) {
            addCrawlJob(crawlOrderFile, "Auto launched.", "", "");
            if(runMode) {
                jobHandler.startCrawler();
                status = "Job being crawled: " + crawlOrderFile;
            } else {
                status = "Crawl job ready and pending: " + crawlOrderFile;
            }
        } else if(runMode) {
            // The use case is that jobs are to be run on a schedule and that
            // if the crawler is in run mode, then the scheduled job will be
            // run at appropriate time.  Otherwise, not.
            jobHandler.startCrawler();
            status = "Crawler set to run mode but no order file to crawl";
        }
        return status;
    }
    
    /**
     * Start up the embedded Jetty webserver instance.
     * This is done when we're run from the command-line.
     * @param port Port number to use for web UI.
     * @param adminLoginPassword Compound of login and password.
     * @throws Exception
     * @return Status on webserver startup.
     */
    protected static String startEmbeddedWebserver(int port,
        String adminLoginPassword)
    throws Exception {
        String adminUN =
            adminLoginPassword.substring(0, adminLoginPassword.indexOf(":"));
        String adminPW =
            adminLoginPassword.substring(adminLoginPassword.indexOf(":") + 1);
        httpServer = new SimpleHttpServer("admin", ROOT_CONTEXT, port, false);
        
        final String DOTWAR = ".war";
        final String ADMIN = "admin";
        final String SELFTEST = "selftest";
        
        // Look for additional WAR files beyond 'selftest' and 'admin'.
        File[] wars = getWarsdir().listFiles();
        for(int i = 0; i < wars.length; i++) {
            if(wars[i].isFile()) {
                final String warName = wars[i].getName();
                final String warNameNC = warName.toLowerCase();
                if(warNameNC.endsWith(DOTWAR) &&
                        !warNameNC.equals(ADMIN + DOTWAR) &&
                        !warNameNC.equals(SELFTEST + DOTWAR)) {
                    int dot = warName.indexOf('.');
                    httpServer.addWebapp(warName.substring(0, dot), null, true);
                }
            }
        }
        
        // Name of passed 'realm' must match what is in configured in web.xml.
        // We'll use ROLE for 'realm' and 'role'.
        final String ROLE = ADMIN;
        Heritrix.httpServer.setAuthentication(ROLE, ROOT_CONTEXT, adminUN,
            adminPW, ROLE);
        httpServer.startServer();
        InetAddress addr = InetAddress.getLocalHost();
        String uiLocation = "http://" + addr.getHostName() + ":" + port;
        StringBuffer buffer = new StringBuffer();
        buffer.append("Heritrix " + getVersion() + " is running.");
        buffer.append("\nWeb console is at: " + uiLocation);
        buffer.append("\nWeb console login and password: " +
            adminUN + "/" + adminPW);
        return buffer.toString();
    }

    protected static CrawlJob createCrawlJob(CrawlJobHandler handler,
            File crawlOrderFile, String name)
    throws InvalidAttributeValueException {
        XMLSettingsHandler settings = new XMLSettingsHandler(crawlOrderFile);
        settings.initialize();
        return new CrawlJob(handler.getNextJobUID(), name, settings,
            new CrawlJobErrorHandler(Level.SEVERE),
            CrawlJob.PRIORITY_HIGH,
            crawlOrderFile.getAbsoluteFile().getParentFile());
    }
    
    /**
     * This method is called when we have an order file to hand that we want
     * to base a job on.  It leaves the order file in place and just starts up
     * a job that uses all the order points to for locations for logs, etc.
     * @param orderPathOrUrl Path to an order file or to a seeds file.
     * @param name Name to use for this job.
     * @param description 
     * @param seeds 
     * @return A status string.
     * @throws IOException 
     * @throws FatalConfigurationException 
     */
    public String addCrawlJob(String orderPathOrUrl, String name,
            String description, String seeds)
    throws IOException, FatalConfigurationException {
        if (!UURI.hasScheme(orderPathOrUrl)) {
            // Assume its a file path.
            return addCrawlJob(new File(orderPathOrUrl), name, description,
                    seeds);
        }

        // Otherwise, must be an URL.
        URL url = new URL(orderPathOrUrl);

        // Handle http and file only for now (Tried to handle JarUrlConnection
        // but too awkward undoing jar stream.  Rather just look for URLs that
        // end in '.jar').
        String result = null;
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            result = addCrawlJob(url, (HttpURLConnection)connection, name,
                description, seeds);
        } else if (connection instanceof FileURLConnection) {
            result = addCrawlJob(new File(url.getPath()), name, description,
                seeds);
        } else {
            throw new UnsupportedOperationException("No support for "
                + connection);
        }

        return result;
    }
    
    protected String addCrawlJob(final URL url,
            final HttpURLConnection connection,
            final String name, final String description, final String seeds)
    throws IOException, FatalConfigurationException {
        // Look see if its a jar file.  If it is undo it.
        boolean isJar = url.getPath() != null &&
            url.getPath().toLowerCase().endsWith(JAR_SUFFIX);
        // If http url connection, bring down the resource local.
        File localFile = File.createTempFile(Heritrix.class.getName(),
           isJar? JAR_SUFFIX: null, TMPDIR);
        connection.connect();
        String result = null;
        try {
            IoUtils.readFullyToFile(connection.getInputStream(), localFile);
            addCrawlJob(localFile, name, description, seeds);
        } catch (IOException ioe) {
            // Cleanup if an Exception.
            localFile.delete();
            localFile = null;
        } finally {
             connection.disconnect();
             // If its a jar file, then we made a job based on the jar contents.
             // Its no longer needed.  Remove it.  If not a jar file, then leave
             // the file around because the job depends on it.
             if (isJar && localFile != null && localFile.exists()) {
                 localFile.delete();
             }
        }
        return result;
    }
    
    protected String addCrawlJob(final File order, final String name,
            final String description, final String seeds)
    throws FatalConfigurationException, IOException {
        if (Heritrix.jobHandler == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        try {
            if (order.getName().toLowerCase().endsWith(JAR_SUFFIX)) {
                return addCrawlJobBasedonJar(order, name, description, seeds);
            }
            Heritrix.jobHandler.addJob(createCrawlJob(jobHandler, order, name));
        } catch (InvalidAttributeValueException e) {
            FatalConfigurationException fce = new FatalConfigurationException(
                "Converted InvalidAttributeValueException on " +
                order.getAbsolutePath() + ": " + e.getMessage());
            fce.setStackTrace(e.getStackTrace());
        }
        return "Added " + order.getAbsolutePath();
    }
    
    /**
     * Undo jar file and use as basis for a new job.
     * @param jarFile Pointer to file that holds jar.
     * @param name Name to use for new job.
     * @param description 
     * @param seeds 
     * @return Message.
     * @throws IOException
     * @throws FatalConfigurationException
     */
    protected String addCrawlJobBasedonJar(final File jarFile,
            final String name, final String description, final String seeds)
    throws IOException, FatalConfigurationException {
        if (jarFile == null || !jarFile.exists()) {
            throw new FileNotFoundException(jarFile.getAbsolutePath());
        }
        // Create a directory with a tmp name.  Do it by first creating file,
        // removing it, then creating the directory. There is a hole during
        // which the OS may put a file of same exact name in our way but
        // unlikely.
        File dir = File.createTempFile(Heritrix.class.getName(), ".expandedjar",
            TMPDIR);
        dir.delete();
        dir.mkdir();
        try {
            org.archive.crawler.util.IoUtils.unzip(jarFile, dir);
            // Expect to find an order file at least.
            File orderFile = new File(dir, "order.xml");
            if (!orderFile.exists()) {
                throw new IOException("Missing order: " +
                    orderFile.getAbsolutePath());
            }
            CrawlJob job =
                createCrawlJobBasedOn(orderFile, name, description, seeds);
            // Copy into place any seeds and settings directories before we
            // add job to Heritrix to crawl.
            File seedsFile = new File(dir, "seeds.txt");
            if (seedsFile.exists()) {
                FileUtils.copyFiles(seedsFile, new File(job.getDirectory(),
                    seedsFile.getName()));
            }
            File settingsDir = new File(dir, "settings");
            if (settingsDir.exists()) {
                FileUtils.copyFiles(settingsDir, job.getDirectory());
            }
            addCrawlJob(job);
            return "Added " + job.getUID();
         } finally {
             // After job has been added, no more need of expanded content.
             // (Let the caller be responsible for cleanup of jar. Sometimes
             // its should be deleted -- when its a local copy of a jar pulled
             // across the net -- wherease other times, if its a jar passed
             // in w/ a 'file' scheme, it shouldn't be deleted.
             org.archive.util.FileUtils.deleteDir(dir);
         }
    }
    
    public String addCrawlJobBasedOn(String jobUidOrProfile,
            String name, String description, String seeds) {
        try {
            CrawlJob cj = Heritrix.jobHandler.getJob(jobUidOrProfile);
            if (cj == null) {
                throw new InvalidAttributeValueException(jobUidOrProfile +
                    " is not a job UID or profile name (Job UIDs are " +
                    " usually the 14 digit date portion of job name).");
            }
            CrawlJob job = addCrawlJobBasedOn(
                cj.getSettingsHandler().getOrderFile(), name, description,
                    seeds);
            return "Added " + job.getUID();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception on " + jobUidOrProfile + ": " + e.getMessage();
        } 
    }
    
    protected CrawlJob addCrawlJobBasedOn(final File orderFile,
        final String name, final String description, final String seeds)
    throws FatalConfigurationException, IOException {
        return addCrawlJob(createCrawlJobBasedOn(orderFile, name, description,
                seeds));
    }
    
    protected CrawlJob createCrawlJobBasedOn(final File orderFile,
            final String name, final String description, final String seeds)
    throws FatalConfigurationException, IOException {
        CrawlJob job = Heritrix.jobHandler.newJob(orderFile, name, description,
                seeds);
        return CrawlJobHandler.ensureNewJobWritten(job, name, description);
    }
    
    protected CrawlJob addCrawlJob(final CrawlJob job) {
        return Heritrix.jobHandler.addJob(job);
    }
    
    public void startCrawling() {
        if (Heritrix.jobHandler == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        Heritrix.jobHandler.startCrawler();
    }

    public void stopCrawling() {
        if (Heritrix.jobHandler == null) {
            throw new NullPointerException("Heritrix jobhandler is null.");
        }
        Heritrix.jobHandler.stopCrawler();
    }

    /**
     * Get the heritrix version.
     *
     * @return The heritrix version.  May be null.
     */
    public static String getVersion() {
        return (properties != null)?
            properties.getProperty("heritrix.version"): null;
    }

    /**
     * Return heritrix properties.
     *
     * @return The returned Properties contains the content of
     * heritrix.properties.  May be null if we failed initial read of
     * 'heritrix.properties'.
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Get the job handler
     *
     * @return The CrawlJobHandler being used.
     */
    public static CrawlJobHandler getJobHandler() {
        return jobHandler;
    }

    /**
     * Get the configuration directory.
     * @return The conf directory under HERITRIX_HOME or null if none can
     * be found.
     * @throws IOException
     */
    public static File getConfdir()
    throws IOException {
        return getConfdir(true);
    }

    /**
     * Get the configuration directory.
     * @param fail Throw IOE if can't find directory if true, else just
     * return null.
     * @return The conf directory under HERITRIX_HOME or null (or an IOE) if
     * can't be found.
     * @throws IOException
     */
    public static File getConfdir(final boolean fail)
    throws IOException {
        return getSubDir("conf", fail);
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
    public static String getSelftestURL() {
        return selftestURL;
    }

    /**
     * @throws IOException
     * @return Returns the directory under which reside the WAR files
     * we're to load into the servlet container.
     */
    public static File getWarsdir()
    throws IOException {
        return getSubDir("webapps");
    }

    /**
     * Prepars for program shutdown. This method does it's best to prepare the
     * program so that it can exit normally. It will kill the httpServer and
     * terminate any running job.<br>
     * It is advisible to wait a few (~1000) millisec after calling this method
     * and before calling performHeritrixShutDown() to allow as many threads as
     * possible to finish what they are doing.
     */
    public static void prepareHeritrixShutDown(){
        if(Heritrix.httpServer != null) {
            // Shut down the web access.
            try {
                Heritrix.httpServer.stopServer();
            } catch (InterruptedException e) {
                // Generally this can be ignored, but we'll print a stack trace
                // just in case.
                e.printStackTrace();
            } finally {
                Heritrix.httpServer = null;
            }
        }
        
        if (Heritrix.jobHandler != null) {
            // Shut down the jobHandler.
            if(jobHandler.isCrawling()){
                jobHandler.deleteJob(jobHandler.getCurrentJob().getUID());
            }
            jobHandler.requestCrawlStop();
            Heritrix.jobHandler = null;
        }
        
        // Unregister MBean.
        if (Heritrix.registeredHeritrixObjectInstance != null) {
            unregisterMBean(Heritrix.registeredHeritrixObjectInstance);
            Heritrix.registeredHeritrixObjectInstance = null;
        }
    }

    /**
     * Exit program. Recommended that prepareHeritrixShutDown() be invoked
     * prior to this method.
     *
     */
    public static void performHeritrixShutDown() {
        performHeritrixShutDown(0);
    }

    /**
     * Exit program. Recommended that prepareHeritrixShutDown() be invoked
     * prior to this method.
     *
     * @param exitCode Code to pass System.exit.
     *
     */
    public static void performHeritrixShutDown(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Shutdown heritrix.
     *
     * Stops crawling, shutsdown webserver and system exits.
     */
    public static void shutdown() {
    	shutdown(0);
    }

    /**
     * Shutdown heritrix.
     *
	 * @param exitCode Exit code to pass system exit.
	 */
	public static void shutdown(int exitCode) {
        prepareHeritrixShutDown();
        performHeritrixShutDown(exitCode);
	}

	/**
     * Add a new alert
     * @param alert the new alert
     */
    public static void addAlert(Alert alert){
        if(Heritrix.noWui){
            alert.print(logger);
        }
        alerts.add(alert);
    }

    /**
     * Get all current alerts
     * @return all current alerts
     */
    public static Vector getAlerts(){
        return alerts;
    }

    /**
     * Returns an alert with the given ID. If no alert is found for given ID
     * null is returned
     * @param alertID the ID of the alert
     * @return an alert with the given ID
     */
    public static Alert getAlert(String alertID){
        if(alertID == null){
            // null will never match any alert
            return null;
        }
        for( int i = 0 ; i < alerts.size() ; i++ ){
            Alert tmp = (Alert)alerts.get(i);
            if(alertID.equals(tmp.getID())){
                return tmp;
            }
        }
        return null;
    }

    /**
     * Get the number of new alerts.
     * @return the number of new alerts
     */
    public static int getNewAlertsCount() {
        int n = 0;
        for(int i = 0 ; i < alerts.size() ; i++ ) {
            Alert tmp = (Alert)alerts.get(i);
            if(tmp.isNew()){
                n++;
            }
        }
        return n;
    }
    
    public static Vector getNewAlerts() {
        Vector newAlerts = null;
        for(int i = 0 ; i < alerts.size(); i++ ) {
            Alert tmp = (Alert)alerts.get(i);
            if(tmp.isNew()) {
                if (newAlerts == null) {
                    newAlerts = new Vector();
                }
                newAlerts.add(tmp);
            }
        }
        return newAlerts;
    }

    /**
     * Remove an alert.
     * @param alertID the ID of the alert
     */
    public static void removeAlert(String alertID){
        for( int i = 0 ; i < alerts.size() ; i++ ){
            Alert tmp = (Alert)alerts.get(i);
            if(alertID.equals(tmp.getID())){
                alerts.remove(i--);
            }
        }
    }

    /**
     * Mark a specific alert as seen (That is, not now).
     * @param alertID the ID of the alert
     */
    public static void seenAlert(String alertID){
        for( int i = 0 ; i < alerts.size() ; i++ ){
            Alert tmp = (Alert)alerts.get(i);
            if(alertID.equals(tmp.getID())){
                tmp.setAlertSeen();
            }
        }
    }
    
    /**
     * Register Heritrix MBean if an agent to register ourselves with.
     * This method will only have effect if we're running in a 1.5.0
     * JDK and command line options such as
     * '-Dcom.sun.management.jmxremote.port=8082
     * -Dcom.sun.management.jmxremote.authenticate=false
     * -Dcom.sun.management.jmxremote.ssl=false' are supplied.
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html">Monitoring
     * and Management Using JMX</a>
     * for more on the command line options and how to connect to the
     * Heritrix bean using the JDK 1.5.0 jconsole tool.  Will only register
     * with the JMX agent started by the JVM (i.e.
     * com.sun.jmx.mbeanserver.JmxMBeanServer).
     * @param h Instance of heritrix to register.
     * @throws NotCompliantMBeanException
     * @throws MBeanRegistrationException
     * @throws InstanceAlreadyExistsException
     * @throws NullPointerException
     * @throws MalformedObjectNameException
     * @throws NotCompliantMBeanException
     * @throws MBeanRegistrationException
     * @throws InstanceAlreadyExistsException
     * @throws MalformedObjectNameException
     * @throws NullPointerException
     */
    static void registerHeritrixMBean(Heritrix h)
    throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException, MalformedObjectNameException,
            NullPointerException {
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers == null) {
            return;
        }
        for (Iterator i = servers.iterator(); i.hasNext();) {
            MBeanServer server = (MBeanServer)i.next();
            if (server != null) {
                ObjectName on = new ObjectName(getJmxName());
                boolean registeredAlready = true;
                try {
                    Heritrix.registeredHeritrixObjectInstance =
                        server.getObjectInstance(on);
                } catch (InstanceNotFoundException e) {
                    registeredAlready = false;
                }
                if (!registeredAlready) {
                    Heritrix.registeredHeritrixObjectInstance =
                        server.registerMBean(h, on);
                }
                System.out.println(on.getCanonicalName() +
                    (registeredAlready? " (already)": "") +
                    " registered to " + server.toString());
                Heritrix.mbeanServer = server;
                break;
            }
        }
    }
    
    public static ObjectInstance registerMBean(Object objToRegister,
            String type) {
        if (Heritrix.mbeanServer == null) {
            return null;
        }
        
        ObjectInstance result = null;
        try {
            String name = getJmxName(type);
            result = Heritrix.mbeanServer.registerMBean(objToRegister,
                new ObjectName(name));
            logger.info("Registered bean " + name);
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public static void unregisterMBean(ObjectInstance obj) {
        if (Heritrix.mbeanServer != null && obj !=  null) {
            try {
                Heritrix.mbeanServer.unregisterMBean(obj.getObjectName());
                logger.info("Unregistered bean " + obj.getObjectName());
            } catch (InstanceNotFoundException e) {
                e.printStackTrace();
            } catch (MBeanRegistrationException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * @return Jmx ObjectName used by this Heritrix instance.
     */
    public static String getJmxName() {
        return getJmxName("Service");
    }
    
    public static String getJmxName(String type) {
        return CRAWLER_PACKAGE + ":name=Heritrix,type=" + type;
    }
    
    /**
     * @return Returns true if Heritrix was launched from the command line.
     */
    public static boolean isCommandLine() {
        return Heritrix.commandLine;
    }
    
    /**
     * @return True if heritrix has been started.
     */
    public boolean isStarted() {
        return Heritrix.jobHandler != null;
    }
    
    public String getStatus() {
        StringBuffer buffer = new StringBuffer();
        if (Heritrix.getJobHandler() != null) {
            buffer.append("isRunning=");
            buffer.append(Heritrix.getJobHandler().isRunning());
            buffer.append(" isCrawling=");
            buffer.append(Heritrix.getJobHandler().isCrawling());
            buffer.append(" alertCount=");
            buffer.append((getAlerts() == null)? 0: getAlerts().size());
            buffer.append(" newAlertCount=");
            buffer.append(Heritrix.getNewAlertsCount());
        }
        return buffer.toString();
    }
    
    /**
     * Start Heritrix.
     * 
     * Used by JMX and webapp initialization for starting Heritrix.
     * Idempotent.
     */
    public void start() {
        // Don't start if we've been launched from the command line.
        // Don't start if already started.
        if (!Heritrix.isCommandLine() && !isStarted()) {
            try {
                // Register ourselves w/ any agent that might be running.
                registerHeritrixMBean(this);
                launch();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Stop Heritrix.
     * 
     * Used by JMX and webapp initialization for stopping Heritrix.
     * Starts up a shutdown thread rather than synchronously waiting
     * so returns immediately.
     */
    public void stop() {
        Thread t = new Thread() {
            public void run() {
                shutdown();
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public String interrupt(String threadName) {
        String result = "Thread " + threadName + " not found";
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        if (group == null) {
            return result;
        }
        // Back up to the root threadgroup before starting
        // to iterate over threads.
        ThreadGroup parent = null;
        while((parent = group.getParent()) != null) {
            group = parent;
        }
        // Do an array that is twice the size of active
        // thread count.  That should be big enough.
        final int max = group.activeCount() * 2;
        Thread [] threads = new Thread[max];
        int threadCount = group.enumerate(threads, true);
        if (threadCount >= max) {
            logger.info("Some threads not found...array too small: " +
                max);
        }
        for (int j = 0; j < threadCount; j++) {
            if (threads[j].getName().equals(threadName)) {
                threads[j].interrupt();
                result = "Interrupt sent to " + threadName;
                break;
            }
        }
        return result;
    }

    // OpenMBean implementation.
    
    /**
     * Build up the MBean info for Heritrix main.
     * @return Return created mbean info instance.
     */
    protected OpenMBeanInfoSupport buildMBeanInfo() {
        OpenMBeanAttributeInfoSupport[] attributes =
            new OpenMBeanAttributeInfoSupport[Heritrix.ATTRIBUTE_LIST.size()];
        OpenMBeanConstructorInfoSupport[] constructors =
            new OpenMBeanConstructorInfoSupport[1];
        OpenMBeanOperationInfoSupport[] operations =
            new OpenMBeanOperationInfoSupport[Heritrix.OPERATION_LIST.size()];
        MBeanNotificationInfo[] notifications =
            new MBeanNotificationInfo[0];

        // Attributes.
        attributes[0] =
            new OpenMBeanAttributeInfoSupport(Heritrix.STATUS_ATTR,
                "Short basic status message", SimpleType.STRING, true,
                false, false);

        // Constructors.
        constructors[0] = new OpenMBeanConstructorInfoSupport(
            "HeritrixOpenMBean", "Constructs Heritrix OpenMBean instance ",
            new OpenMBeanParameterInfoSupport[0]);

        // Operations.
        operations[0] = new OpenMBeanOperationInfoSupport(
            Heritrix.START_OPER, "Start Heritrix instance", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        operations[1] = new OpenMBeanOperationInfoSupport(
            Heritrix.STOP_OPER, "Stop Heritrix instance", null,
                SimpleType.VOID, MBeanOperationInfo.ACTION);
        
        OpenMBeanParameterInfo[] args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("threadName",
            "Name of thread to send interrupt", SimpleType.STRING);
        operations[2] = new OpenMBeanOperationInfoSupport(
            Heritrix.INTERRUPT_OPER, "Send thread an interrupt " +
                "(Used debugging)", args, SimpleType.STRING,
                MBeanOperationInfo.ACTION_INFO);
        
        operations[3] = new OpenMBeanOperationInfoSupport(
            Heritrix.START_CRAWLING_OPER, "Set Heritrix instance " +
                "into crawling mode", null, SimpleType.VOID,
                MBeanOperationInfo.ACTION);
        
        operations[4] = new OpenMBeanOperationInfoSupport(
            Heritrix.STOP_CRAWLING_OPER, "Unset Heritrix instance " +
                " crawling mode", null, SimpleType.VOID,
                MBeanOperationInfo.ACTION);
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("pathOrURL",
            "Path/URL to order or jar of order+seed",
            SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
            "Basename for new job", SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("description",
            "Description to save with new job", SimpleType.STRING);
        args[3] = new OpenMBeanParameterInfoSupport("seeds",
            "Initial seed(s)", SimpleType.STRING);
        operations[5] = new OpenMBeanOperationInfoSupport(
            Heritrix.ADD_CRAWL_JOB_OPER, "Add new crawl job", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        args = new OpenMBeanParameterInfoSupport[4];
        args[0] = new OpenMBeanParameterInfoSupport("uidOrName",
            "Job UID or profile name", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
            "Basename for new job", SimpleType.STRING);
        args[2] = new OpenMBeanParameterInfoSupport("description",
            "Description to save with new job", SimpleType.STRING);
        args[3] = new OpenMBeanParameterInfoSupport("seeds",
            "Initial seed(s)", SimpleType.STRING);
        operations[6] = new OpenMBeanOperationInfoSupport(
            Heritrix.ADD_CRAWL_JOB_BASEDON_OPER,
            "Add a new crawl job based on passed Job UID or profile",
            args, SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("index",
            "Zero-based index into array of NEW alerts", SimpleType.INTEGER);
        operations[7] = new OpenMBeanOperationInfoSupport(
            Heritrix.NEW_ALERT_OPER, "Return NEW alert at passed index", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        args = new OpenMBeanParameterInfoSupport[1];
        args[0] = new OpenMBeanParameterInfoSupport("index",
            "Zero-based index into array of alerts", SimpleType.INTEGER);
        operations[8] = new OpenMBeanOperationInfoSupport(
            Heritrix.ALERT_OPER, "Return alert at passed index", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);
        
        try {
            this.jobCompositeType = new CompositeType("job",
                    "Job attributes", JOB_KEYS,
                    new String [] {"Job unique ID", "Job name", "Job status"},
                    new OpenType [] {SimpleType.STRING, SimpleType.STRING,
                        SimpleType.STRING});
            this.jobsTabularType = new TabularType("jobs", "List of jobs",
                    this.jobCompositeType, new String [] {"uid"});
        } catch (OpenDataException e) {
            // This should never happen.
            throw new RuntimeException(e);
        }
        operations[9] = new OpenMBeanOperationInfoSupport(
            Heritrix.PENDING_JOBS_OPER,
                "List of pending jobs (or null if none)", null,
                this.jobsTabularType, MBeanOperationInfo.INFO);
        operations[10] = new OpenMBeanOperationInfoSupport(
                Heritrix.COMPLETED_JOBS_OPER,
                    "List of completed jobs (or null if none)", null,
                    this.jobsTabularType, MBeanOperationInfo.INFO);
        
        args = new OpenMBeanParameterInfoSupport[2];
        args[0] = new OpenMBeanParameterInfoSupport("uid",
            "Job unique ID", SimpleType.STRING);
        args[1] = new OpenMBeanParameterInfoSupport("name",
                "Report name (e.g. crawl-report, etc.)",
                SimpleType.STRING);
        operations[11] = new OpenMBeanOperationInfoSupport(
            Heritrix.CRAWLEND_REPORT_OPER, "Return crawl-end report", args,
                SimpleType.STRING, MBeanOperationInfo.ACTION_INFO);

        // Build the info object.
        return new OpenMBeanInfoSupport(this.getClass().getName(),
            "Heritrix Main OpenMBean", attributes, constructors, operations,
            notifications);
    }
    
    public Object getAttribute(String attribute_name)
    throws AttributeNotFoundException {
        if (attribute_name == null) {
            throw new RuntimeOperationsException(
                 new IllegalArgumentException("Attribute name cannot be null"),
                 "Cannot call getAttribute with null attribute name");
        }
        if (!Heritrix.ATTRIBUTE_LIST.contains(attribute_name)) {
            throw new AttributeNotFoundException("Attribute " +
                 attribute_name + " is unimplemented.");
        }
        // The pattern in the below is to match an attribute and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the AttributeNotFoundException for case where we've an
        // attribute but no handler.
        if (attribute_name.equals(STATUS_ATTR)) {
            return getStatus();
        }
        throw new AttributeNotFoundException("Attribute " +
            attribute_name + " not found.");
    }

    public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException {
        throw new AttributeNotFoundException("No attribute can be set in " +
            "this MBean");
    }

    public AttributeList getAttributes(String [] attributeNames) {
        if (attributeNames == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("attributeNames[] cannot be " +
                "null"), "Cannot call getAttributes with null attribute " +
                "names");
        }
        AttributeList resultList = new AttributeList();
        if (attributeNames.length == 0) {
            return resultList;
        }
        for (int i = 0; i < attributeNames.length; i++) {
            try {
                Object value = getAttribute(attributeNames[i]);
                resultList.add(new Attribute(attributeNames[i], value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return(resultList);
    }

    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList(); // always empty
    }

    public Object invoke(String operationName, Object[] params,
        String[] signature)
    throws ReflectionException {
        if (operationName == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Operation name cannot be null"),
                "Cannot call invoke with null operation name");
        }
        // The pattern in the below is to match an operation and when found
        // do a return out of if clause.  Doing it this way, I can fall
        // on to the MethodNotFoundException for case where we've an
        // attribute but no handler.
        if (operationName.equals(START_OPER)) {
            JmxUtils.checkParamsCount(START_OPER, params, 0);
            start();
            return null;
        }
        if (operationName.equals(STOP_OPER)) {
            JmxUtils.checkParamsCount(STOP_OPER, params, 0);
            stop();
            return null;
        }
        if (operationName.equals(INTERRUPT_OPER)) {
            JmxUtils.checkParamsCount(INTERRUPT_OPER, params, 1);
            return interrupt((String)params[0]);
        }       
        if (operationName.equals(START_CRAWLING_OPER)) {
            JmxUtils.checkParamsCount(START_CRAWLING_OPER, params, 0);
            startCrawling();
            return null;
        }
        if (operationName.equals(STOP_CRAWLING_OPER)) {
            JmxUtils.checkParamsCount(STOP_CRAWLING_OPER, params, 0);
            stopCrawling();
            return null;
        }
        if (operationName.equals(ADD_CRAWL_JOB_OPER)) {
            JmxUtils.checkParamsCount(ADD_CRAWL_JOB_OPER, params, 4);
            try {
                return addCrawlJob((String)params[0], (String)params[1],
                    checkForEmptyPlaceHolder((String)params[2]),
                    checkForEmptyPlaceHolder((String)params[3]));
            } catch (IOException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            } catch (FatalConfigurationException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        if (operationName.equals(ADD_CRAWL_JOB_BASEDON_OPER)) {
            JmxUtils.checkParamsCount(ADD_CRAWL_JOB_BASEDON_OPER, params, 4);
            return addCrawlJobBasedOn((String)params[0], (String)params[1],
                    checkForEmptyPlaceHolder((String)params[2]),
                    checkForEmptyPlaceHolder((String)params[3]));
        }       
        if (operationName.equals(ALERT_OPER)) {
            JmxUtils.checkParamsCount(ALERT_OPER, params, 1);
            Vector v = getAlerts();
            if (v == null) {
                throw new RuntimeOperationsException(
                    new NullPointerException("There are no alerts"),
                        "Empty alerts vector");
            }
            return getAlertStringRepresentation(((Alert)v.
                get(((Integer)params[0]).intValue())));
        }
        if (operationName.equals(NEW_ALERT_OPER)) {
            JmxUtils.checkParamsCount(NEW_ALERT_OPER, params, 1);
            Vector v = getNewAlerts();
            if (v == null) {
                throw new RuntimeOperationsException(
                    new NullPointerException("There are no new alerts"),
                        "Empty new alerts vector");
            }
            return getAlertStringRepresentation(((Alert)v.
                    get(((Integer)params[0]).intValue())));
        }
        
        if (operationName.equals(PENDING_JOBS_OPER)) {
                JmxUtils.checkParamsCount(PENDING_JOBS_OPER, params, 0);
            try {
                return makeJobsTabularData(getJobHandler().getPendingJobs());
            } catch (OpenDataException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        if (operationName.equals(COMPLETED_JOBS_OPER)) {
                JmxUtils.checkParamsCount(COMPLETED_JOBS_OPER, params, 0);
            try {
                return makeJobsTabularData(getJobHandler().getCompletedJobs());
            } catch (OpenDataException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        if (operationName.equals(CRAWLEND_REPORT_OPER)) {
            JmxUtils.checkParamsCount(CRAWLEND_REPORT_OPER, params, 2);
            try {
                return getCrawlendReport((String)params[0], (String) params[1]);
            } catch (IOException e) {
                throw new RuntimeOperationsException(new RuntimeException(e));
            }
        }
        
        throw new ReflectionException(
            new NoSuchMethodException(operationName),
                "Cannot find the operation " + operationName);
    }
    
    /**
     * Return named crawl end report for job with passed uid.
     * Crawler makes reports when its finished its crawl.  Use this method
     * to get a String version of one of these files.
     * @param jobUid The unique ID for the job whose reports you want to see
     * (Must be a completed job).
     * @param reportName Name of report minus '.txt' (e.g. crawl-report).
     * @return String version of the on-disk report.
     * @throws IOException 
     */
    protected String getCrawlendReport(String jobUid, String reportName)
    throws IOException {
        CrawlJob job = getJobHandler().getJob(jobUid);
        if (job == null) {
            throw new IOException("No such job: " + jobUid);
        }
        File report = new File(job.getDirectory(), reportName + ".txt");
        if (!report.exists()) {
            throw new FileNotFoundException(report.getAbsolutePath());
        }
        return FileUtils.readFileAsString(report);
    }
    
    protected TabularData makeJobsTabularData(List jobs)
    throws OpenDataException {
        if (jobs == null || jobs.size() == 0) {
            return null;
        }
        TabularData td = new TabularDataSupport(this.jobsTabularType);
        for (Iterator i = jobs.iterator(); i.hasNext();) {
            CrawlJob job = (CrawlJob)i.next();
            CompositeData cd = new CompositeDataSupport(this.jobCompositeType,
                JOB_KEYS,
                new String [] {job.getUID(), job.getJobName(), job.getStatus()});
            td.put(cd);
        }
        return td;
    }
    
    /**
     * If passed str has placeholder for the empty string, return the empty
     * string else return orginal.
     * Dumb jmx clients can't pass empty string so they'll pass a representation
     * of empty string such as ' ' or '-'.  Convert such strings to empty
     * string.
     * @param str String to check.
     * @return Original <code>str</code> or empty string if <code>str</code>
     * contains a placeholder for the empty-string (e.g. '-', or ' ').
     */
    protected String checkForEmptyPlaceHolder(String str) {
        return TextUtils.matches("-| +", str)? "": str;
    }

    protected String getAlertStringRepresentation(Alert a) {
        if (a.isNew()) {
            a.setAlertSeen();
        }
        return a.getID() + "\n" + a.getBody();
    }

    public MBeanInfo getMBeanInfo() {
        return this.openMBeanInfo;
    }
}