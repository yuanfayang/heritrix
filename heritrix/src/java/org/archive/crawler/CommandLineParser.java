/* HeritrixUsage
 *
 * Created on Feb 2, 2004
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
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;


/**
 * Print Heritrix command-line usage message.
 *
 * @author stack
 * @version $Id$
 */
public class CommandLineParser
{
    private static final String USAGE = "Usage: ";
    private static final String NAME = "heritrix";
    private Options options = null;
    private CommandLine commandLine = null;
    private PrintWriter out = null;
    private String version = null;

    /**
     * Block default construction.
     *
     */
    private CommandLineParser()
    {
        super();
    }

    /**
     * Constructor.
     *
     * @param args Command-line arguments to process.
     * @param out PrintStream to write on.
     * @param version Heritrix version
     *
     * @throws ParseException Failied parse of command line.
     */
    public CommandLineParser(String [] args, PrintWriter out, String version)
        throws ParseException
    {
        super();

        this.out = out;
        this.version = version;

        this.options = new Options();
        this.options.addOption(new Option("h","help", false,
            "Prints this message and exits."));
        this.options.addOption(new Option("p","port", true,
            "Port to run web user interface on.  Default: 8080."));
        this.options.addOption(new Option("a", "admin", true,
            "Login and password for web user interface administration." +
            " Default: admin/letmenin."));
        this.options.addOption(new Option("r", "run", false,
            "Put heritrix into run mode. If ORDER_FILE begin crawl."));
        this.options.addOption(new Option("n", "nowui", false,
            "Put heritrix into run mode and begin crawl using ORDER_FILE." +
            " Do not put up web user interface."));
        this.options.addOption(new Option("s", "selftest", false,
            "Run integrated self test."));

        PosixParser parser = new PosixParser();
        try
        {
            this.commandLine = parser.parse(this.options, args, false);
        }

        catch (UnrecognizedOptionException e)
        {
            usage(e.getMessage(), 1);
        }
    }

    /**
     * Print usage then exit.
     */
    public void usage()
    {
        usage(0);
    }

    /**
     * Print usage then exit.
     *
     * @param exitCode
     */
    public void usage(int exitCode)
    {
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
    public void usage(String message, int exitCode)
    {
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
    public void message(String message, int exitCode)
    {
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
    private void outputAndExit(String message, boolean doUsage, int exitCode)
    {
        if (message !=  null)
        {
            this.out.println(message);
        }

        if (doUsage)
        {
            HeritrixHelpFormatter formatter =
                new HeritrixHelpFormatter(this.version);
            formatter.printHelp(this.out, 80, NAME, "Options:", this.options,
                1, 2, "Arguments:", false);
            this.out.println(" ORDER_FILE     Crawl order to run.\n");
        }

        // Close printwriter so stream gets flushed.
        this.out.close();
        System.exit(exitCode);
    }

    /**
     * @return Options passed on the command line.
     */
    public Option [] getCommandLineOptions()
    {
        return this.commandLine.getOptions();
    }

    /**
     * @return Arguments passed on the command line.
     */
    public List getCommandLineArguments()
    {
        return this.commandLine.getArgList();
    }

    /**
     * @return Command line.
     */
    public CommandLine getCommandLine()
    {
        return this.commandLine;
    }

    /**
     * Override so can customize usage output.
     *
     * @author stack
     * @version $Id$
     */
    public class HeritrixHelpFormatter
        extends HelpFormatter
    {
        private String version = null;

        public HeritrixHelpFormatter(String version)
        {
            super();
            this.version = version;
        }

        public void printUsage(PrintWriter pw, int width, String cmdLineSyntax)
        {
            out.println(USAGE + NAME + " --help");
            out.println(USAGE + NAME + " --nowui ORDER_FILE");
            out.println(USAGE + NAME + " [--port=PORT]" +
                " [--admin=LOGIN:PASSWORD] [--run] [ORDER_FILE]");
            out.println(USAGE + NAME + " [--port=PORT] --selftest");
            out.println("Version: " + this.version);
        }

        public void printUsage(PrintWriter pw, int width,
            String app, Options options)
        {
            this.printUsage(pw, width, app);
        }
    }
}
