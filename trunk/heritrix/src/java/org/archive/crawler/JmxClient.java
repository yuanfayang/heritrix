/*
 * JmxClient
 *
 * $Id$
 *
 * Created on Nov 12, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.archive.util.OneLineSimpleLogger;


/**
 * Heritrix JMX Client.
 * Used to control Heritrix remotely.
 * Connects to the JDK 1.5.0 JMX Agent.
 * This client doesn't currently do security.
 * See {@link Heritrix#registerHeritrixMBean(Heritrix)} to see
 * how to start the remote JMX Agent and to learn more about how
 * this JMX client/server interaction works.  Because this client
 * doesn't yet do security, start the remote MBean with SSL
 * and authentication disabled: e.g. Pass the following on
 * command line: <code>-Dcom.sun.management.jmxremote.authenticate=false
 * -Dcom.sun.management.jmxremote.ssl=false</code>.
 * <p>TODO: Move this out of Heritrix package.  Has no real dependency
 * on Heritrix.  Just packaged it with Heritrix for convenience.
 * @author stack
 */
public class JmxClient {
    /**
     * Logging instance.
     */
    private static final Logger logger =
        Logger.getLogger(JmxClient.class.getName());
    
    // Attributes.
    private static final String STARTED = "Started";
    private static final String STATUS = "State";
    private static final String [] ATTRS_ARRAY = {STARTED, STATUS};
    private static final List ATTRS =
        new ArrayList(Arrays.asList(ATTRS_ARRAY));
    
    // Operations.
    private static final String STOP = "stop";
    private static final String START = "start";
    private static final String [] CMDS_ARRAY = {START, STOP};
    private static final List CMDS = new ArrayList(Arrays.asList(CMDS_ARRAY));
    
    /**
     * Default port to connect to.
     */
    private static final int DEFAULT_PORT = 8081;
    
    
	public static void main(String[] args) throws Exception {
        JmxClient client = new JmxClient();
        OneLineSimpleLogger.setConsoleHandler();
        doCmdLine(client, args);
	}

    protected static void doCmdLine(JmxClient client, String [] args)
    throws Exception {
        String cmd = null;
        String hostname = "localhost";
        int port = DEFAULT_PORT;
        JmxClientCommandLineParser clp =
            client.new JmxClientCommandLineParser(args,
                new PrintWriter(System.out));
        List arguments = clp.getCommandLineArguments();
        Option [] options = clp.getCommandLineOptions();

        // Check passed argument.
        if (arguments.size() > 0) {
            clp.usage(1);
        }
        
        // Now look at options passed.
        for (int i = 0; i < options.length; i++) {
        	switch(options[i].getId())
			{
				case 'h':
					clp.usage();
					break;
					
				case 'c':
					cmd = options[i].getValue();
					if (!CMDS.contains(cmd) && !ATTRS.contains(cmd)) {
						clp.usage("Unrecognized command: " + cmd, 1);
					}
					break;
					
				case 'p':
					String tmp = options[i].getValue();
					int index = tmp.indexOf(":");
					if (index >= 0) {
						try {
							port = Integer.parseInt(tmp.substring(index + 1));
						} catch (NumberFormatException e) {
							clp.usage("Failed parse of port number: " +
									options[i].getValue(), 1);
						}
						hostname = tmp.substring(0, index);
					} else {
						hostname = tmp;   
					}
					break;
					
				default:
					throw new Exception("Unknown option: " +
							options[i].getId());
			}
        }
        
        if (cmd == null) {
            clp.usage("You must supply a command to run.", 1);   
        }
        
        client.doCommand(hostname, port, cmd);
    }
    
    protected void doCommand(String hostname, int port, String cmd)
    throws Exception {
        JMXServiceURL url = 
            new JMXServiceURL("service:jmx:rmi://" + hostname +
                "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi"); 
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        try {
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            ObjectInstance instance = mbsc.
                getObjectInstance(new ObjectName(Heritrix.getJmxName()));
            Object result = null;
            if (ATTRS.contains(cmd)) {
                result = mbsc.getAttribute(instance.getObjectName(), cmd);
            } else if (CMDS.contains(cmd)) {
                result = mbsc.invoke(instance.getObjectName(), cmd,
                    null, null);
            } else {
                throw new Exception("Unsupported command: " + cmd);   
            }
            logger.info(cmd + ": " + result);
        } finally {
        	    jmxc.close();
        }
    }
    
	/**
	 * Manage JmxClient Command Line.
	 *
	 * @author stack
	 * @version $Id$
	 */
	private class JmxClientCommandLineParser {
		private static final String USAGE = "Usage: ";
		private static final String NAME = "jmxclient";
		private Options options = null;
		private CommandLine commandLine = null;
		private PrintWriter out = null;
		private String version = null;
		
		/**
		 * Block default construction.
		 */
		private JmxClientCommandLineParser() {
			super();
		}
        
        /**
         * @return Return version of this client.
         */
        public String getVersion() {
            return this.version;
        }

        /**
         * @param args Command-line arguments to process.
         * @param out PrintStream to write on.
         * @throws ParseException Failied parse of command line.
         */
        private JmxClientCommandLineParser(String [] args, PrintWriter out)
        throws ParseException {
            this(args, out, null);
        }
		
		/**
		 * Constructor.
		 *
		 * @param args Command-line arguments to process.
		 * @param out PrintStream to write on.
		 * @param version Version string.  Can be null.
		 *
		 * @throws ParseException Failied parse of command line.
		 */
		private JmxClientCommandLineParser(String [] args, PrintWriter out,
				String version)
		throws ParseException {
			super();
			
			this.out = out;
			this.version = version;
			
			this.options = new Options();
			this.options.addOption(new Option("h","help", false,
			    "Prints this message and exits."));
			this.options.addOption(new Option("p","hostport", true,
			    "'Hostname:port' to use connecting to Heritrix jmxserver." +
				" Default: localhost:" + DEFAULT_PORT +
                "."));
			this.options.addOption(new Option("c", "command", true,
			    "Command to send Heritrix: start | stop | etc."));
			
			PosixParser parser = new PosixParser();
			try {
				this.commandLine = parser.parse(this.options, args, false);
			}
			
			catch (UnrecognizedOptionException e) {
				usage(e.getMessage(), 1);
			}
		}
		
		/**
		 * Print usage then exit.
		 */
		public void usage() {
			usage(0);
		}
		
		/**
		 * Print usage then exit.
		 *
		 * @param exitCode
		 */
		public void usage(int exitCode) {
			usage(null, exitCode);
		}
		
		/**
		 * Print message then usage then exit.
		 *
		 * The JVM exits inside in this method.
		 *
		 * @param message Message to print before we do usage.
		 * @param exitCode Exit code to use in call to System.exit.
		 */
		public void usage(String message, int exitCode) {
			outputAndExit(message, true, exitCode);
		}
		
		/**
		 * Print message and then exit.
		 *
		 * The JVM exits inside in this method.
		 *
		 * @param message Message to print before we do usage.
		 * @param exitCode Exit code to use in call to System.exit.
		 */
		public void message(String message, int exitCode) {
			outputAndExit(message, false, exitCode);
		}
		
		/**
		 * Print out optional message an optional usage and then exit.
		 *
		 * Private utility method.  JVM exits from inside in this method.
		 *
		 * @param message Message to print before we do usage.
		 * @param doUsage True if we are to print out the usage message.
		 * @param exitCode Exit code to use in call to System.exit.
		 */
		private void outputAndExit(String message, boolean doUsage, int exitCode) {
			if (message !=  null) {
				this.out.println(message);
			}
			
			if (doUsage) {
				HeritrixHelpFormatter formatter =
					new HeritrixHelpFormatter();
				formatter.printHelp(this.out, 80, NAME, "Options:",
                    this.options, 1, 2, "", false);
			}
			
			// Close printwriter so stream gets flushed.
			this.out.close();
			System.exit(exitCode);
		}
		
		/**
		 * @return Options passed on the command line.
		 */
		public Option [] getCommandLineOptions() {
			return this.commandLine.getOptions();
		}
		
		/**
		 * @return Arguments passed on the command line.
		 */
		public List getCommandLineArguments() {
			return this.commandLine.getArgList();
		}
		
		/**
		 * @return Command line.
		 */
		public CommandLine getCommandLine() {
			return this.commandLine;
		}
		
		/**
		 * Override so can customize usage output.
		 *
		 * @author stack
		 * @version $Id$
		 */
		public class HeritrixHelpFormatter
		extends HelpFormatter {
			public HeritrixHelpFormatter() {
				super();
			}
			
			public void printUsage(PrintWriter pw, int width,
                    String cmdLineSyntax) {
				out.println(USAGE + NAME + " --help");
				out.println(USAGE + NAME + " [--hostport=HOST:PORT] --command=CMD");
                if (getVersion() != null) {
                	    out.println("Version: " + getVersion());
                }
			}
			
			public void printUsage(PrintWriter pw, int width,
					String app, Options options) {
				this.printUsage(pw, width, app);
			}
		}
	}
}
