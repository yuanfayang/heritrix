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
 * Log.java
 *
 * Created on June 14, 2007
 *
 * $Id:$
 */
package org.archive.crawler.webui;

import java.io.IOException;

import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.CrawlJobManager;
import org.archive.crawler.util.LogRemoteAccess;
import org.archive.crawler.util.Logs;

/**
 * Supports web UI operations for logs. 
 * @author Kristinn Sigurdsson
 */
public class Log {

	// Existing logs
	public enum Mode{
		LINE_NUMBER ("Line number"),
		TIMESTAMP ("Timestamp"),
		REGEXPR ("RegExp"),
		TAIL ("Tail");
		
		String description;
		
		Mode(String description){
			this.description = description;
		}
		
		public String getDescription(){
			return description;
		}
	}

	String job;
	Logs currentLog;
	String[] content;
    Mode mode = Mode.TAIL;
    String[] log = null;
    String logText = "";
    String logInfo = "";
    int linesToShow = 50;
    int refreshInterval = -1;
    int linenumber = 1;
    String timestamp = "";
    String regexpr = "";
    boolean ln = false;
    boolean grep = false;
    boolean indent = false;
    int linesToSkip = 1;
	
	
	private Log(){
		
	}
	
    /**
     * Retrieves the appropriate log and prepares it for display. I.e. creates
     * an proper Log object and sets the request's 'log' attribute as the new
     * object.
     * 
     * @param sc
     * @param request
     * @param response
     */
    public static void showLog(ServletContext sc, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Log log = new Log();
        // Get parameters from request

        /* Which log to display */
        if (request.getParameter("log") != null) {
            log.currentLog = Logs.valueOf(request.getParameter("log"));
        }
        if (log.currentLog == null) {
            log.currentLog = Logs.CRAWL;
        }

        /* How much of it to show at most */
        if (request.getParameter("linesToShow") != null
                && request.getParameter("linesToShow").length() > 0) {
            try {
                log.linesToShow = Integer.parseInt(request
                        .getParameter("linesToShow"));
            } catch (java.lang.NumberFormatException e) {
                log.linesToShow = 50;
            }
        }

        /* View mode */
        if (request.getParameter("mode") != null) {
            log.mode = Mode.valueOf(request.getParameter("mode"));
        }

        /* Mode dependant settings */
        // LINE_NUMBER
        try {
            if (request.getParameter("linenumber") != null) {
                log.linenumber = Integer.parseInt(request
                        .getParameter("linenumber"));
            }
        } catch (Exception e) {
            log.linenumber = 1;
        }

        // REGEXPR
        try {
            if (request.getParameter("linesToSkip") != null) {
                log.linesToSkip = Integer.parseInt(request
                        .getParameter("linesToSkip"));
            }
        } catch (Exception e) {
            log.linesToSkip = 1;
        }
        if (request.getParameter("regexpr") != null) {
            log.regexpr = request.getParameter("regexpr");
        }
        log.ln = request.getParameter("ln") != null
                && request.getParameter("ln").equalsIgnoreCase("true");
        log.grep = request.getParameter("grep") != null
                && request.getParameter("grep").equalsIgnoreCase("true");
        log.indent = request.getParameter("indent") != null
                && request.getParameter("indent").equalsIgnoreCase("true");

        // TAIL
        try {
            if (request.getParameter("time") != null) {
                log.refreshInterval = Integer.parseInt(request
                        .getParameter("time"));
            }
        } catch (Exception e) {
            log.refreshInterval = -1;
        }

        // TIMESTAMP
        if (request.getParameter("timestamp") != null) {
            log.timestamp = request.getParameter("timestamp");
        }

        // All settings parsed.
        // Get appropriate log content from crawler
        Remote<CrawlJobManager> manager = CrawlerArea.open(request);
        try {
            CrawlJob job = CrawlJob.lookup(request, manager);
            
            ObjectName oname = manager.getObject().getLogs(job.encode());
            log.job = job.getName();

            LogRemoteAccess lra = Remote.make(manager.getJMXConnector(), oname,
                    LogRemoteAccess.class).getObject();

            switch (log.mode) {
            case TAIL:
                log.log = lra.getTail(log.currentLog.toString(),
                        log.linesToShow);
                break;
            case LINE_NUMBER:
                log.log = lra.getByLinenumber(log.currentLog.toString(),
                        log.linesToShow, log.linenumber);
                break;
            case REGEXPR:
                log.log = lra.getByRegExpr(log.currentLog.toString(),
                        log.linesToShow, log.regexpr, log.grep, log.ln,
                        log.indent, log.linesToSkip);
                break;
            case TIMESTAMP:
                log.log = lra.getByPrefix(log.currentLog.toString(),
                        log.linesToShow, log.timestamp);
                break;
            }
        } finally {
            manager.close();
        }

        // Save log object
        request.setAttribute("log", log);

        // Forward to display page.
        Misc.forward(request, response, "page_show_log.jsp");
    }
	
	public String[] getContent() {
		return content;
	}

	public Logs getCurrentLog() {
		return currentLog; 
	}

	public boolean isGrep() {
		return grep;
	}

	public boolean isIndent() {
		return indent;
	}

	public String getJob() {
		return job;
	}

	public int getLinenumber() {
		return linenumber;
	}

	public int getLinesToShow() {
		return linesToShow;
	}

	public int getLinesToSkip() {
		return linesToSkip;
	}

	public boolean isLn() {
		return ln;
	}

	public String[] getLog() {
		return log;
	}

	public String getLogInfo() {
		return logInfo;
	}

	public String getLogText() {
		return logText;
	}

	public Mode getMode() {
		return mode; 
	}

	public int getRefreshInterval() {
		return refreshInterval;
	}

	public String getRegexpr() {
		return regexpr;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Create the log specific part of the query string.
	 * <p>
	 * This always includes <code>job</code>, <code>log</log>, <code>mode</code>
	 * and <code>linesToShow</code>. Other parameters depend on the mode.
	 * 
	 * @param log Which log to display
	 * @param mode Which display mode
	 * @return A query string with log specific parameters. No leading ? or &.
	 */
	public String getQueryString(Logs log, Mode mode){
		// Parameters common to all modes
		String qs = "job=" + job + 
			"&log=" + log +
			"&mode=" + mode +
			"&linesToShow=" + linesToShow;
		// Mode specific parameters
		switch(mode){
		case LINE_NUMBER :
			qs += "&linenumber=" + linenumber;
			break;
		case REGEXPR :
			qs += "&linenumber=" + linenumber +
				"&regexpr=" + regexpr + 
				"&linesToSkip=" + linesToSkip + 
				"&ln=" + ln +
				"&grep=" + grep + 
				"&indent=" + indent;
			break;
		case TAIL :
			qs += "&time=" + refreshInterval;
			break;
		case TIMESTAMP :
			qs += "&timestamp=" + timestamp;
			break;
		}
		return qs;		
	}
	
}
