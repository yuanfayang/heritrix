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
 * LogRemoteAccessImpl.java
 *
 * Created on June 14, 2007
 *
 * $Id:$
 */
package org.archive.crawler.util;

import java.io.File;
import java.io.Serializable;

import org.archive.openmbeans.annotations.Bean;

/**
 * Implements the {@link LogRemoteAccess} interface and provides JMX access
 * to Heritrix logs.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see LogRemoteAccess
 */
public class LogRemoteAccessImpl extends Bean 
							  implements Serializable, LogRemoteAccess{

	private static final long serialVersionUID = 1L;
	String logDirectory;
	
	public LogRemoteAccessImpl(String logDirectory){
        super(LogRemoteAccess.class);
		this.logDirectory = logDirectory;
	}

	public String[] getTail(String log, int lines) {
		String logfile = Logs.valueOf(log).getFilename(); 
		return LogReader.tail(logDirectory + File.separator + logfile, lines);
	}

	
	public String[] getByRegExpr(
			String log,
			int lines,
			String regexpr,
			boolean grep,
			boolean ln,
			boolean indent,
			int linesToSkip) {
		String logfile = Logs.valueOf(log).getFilename(); 
		if(regexpr==null){
			// If null regexpr match everything.
			regexpr = ".*";
		}
		
        if(grep){
            regexpr = ".*" + regexpr + ".*";
        }
        
        if(indent) {
            return LogReader.
                getByRegExprFromSeries(logDirectory + File.separator + logfile,
                    regexpr, " ", ln,linesToSkip-1, lines);
        } else {
            return LogReader.
                getByRegExprFromSeries(logDirectory + File.separator + logfile,
                    regexpr, 0, ln,linesToSkip-1, lines);
        }
	}

	public String[] getByPrefix(
			String log,
			int lines,
			String prefix){
		String logfile = Logs.valueOf(log).getFilename(); 

		if(prefix == null){
			// If null prefix then match first line.
			prefix = "";
		}
		
        int prefixLinenumber = LogReader.findFirstLineBeginningFromSeries(
            	logDirectory + File.separator + logfile, 
            	prefix);
        
        return LogReader.getFromSeries(
        		logDirectory + File.separator + logfile,
        		prefixLinenumber, 
        		lines);
		
	}
	
	public String[] getByLinenumber(
			String log,
			int lines,
			int linenumber){
		String logfile = Logs.valueOf(log).getFilename(); 
		return LogReader.getFromSeries(
				logDirectory + File.separator + logfile,
                linenumber, lines); 

	}
}
