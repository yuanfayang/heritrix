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
 * LogRemoteAccess.java
 *
 * Created on June 14, 2007
 *
 * $Id:$
 */
package org.archive.crawler.util;

import org.archive.openmbeans.annotations.Operation;
import org.archive.openmbeans.annotations.Parameter;

/**
 * Provides remote access to Heritrix logs. 
 * @author Kristinn Sigurdsson
 */
public interface LogRemoteAccess{

	@Operation(desc = "Gets the tail of a log", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
	String[] getTail(
			@Parameter(name = "log", desc = "Which log, as described by the Logs enum")
			String log, 
			@Parameter(name = "lines", desc = "Number of lines to show")
			int lines);

	@Operation(desc = "Get lines in log matching a regular expression", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
	String[] getByRegExpr(
			@Parameter(name = "log", desc = "Which log, as described by the Logs enum")
			String log, 
			@Parameter(name = "lines", desc = "The number of matching lines to return")
			int lines,
			@Parameter(name = "regexpr", desc = "The regular expression")
			String regexpr,
			@Parameter(name = "grep", desc = "Grep style matching")
			boolean grep,
			@Parameter(name = "prependLineNumbers", desc = "Prepend lines with their sequential number in the log")
			boolean ln,
			@Parameter(name = "indent", desc = "Include following indented lines")
			boolean indent,
			@Parameter(name = "linesToSkip", desc = "Number of matches to skip over")
			int linesToSkip);

	@Operation(desc = "Get lines in log starting from specified prefix", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
	String[] getByPrefix(
			@Parameter(name = "log", desc = "Which log, as described by the Logs enum")
			String log, 
			@Parameter(name = "lines", desc = "The number of matching lines to return")
			int lines,
			@Parameter(name = "prefix", desc = "The prefix")
			String prefix);
	
	@Operation(desc = "Get lines in log starting from specified line number", type = "org.archive.settings.jmx.Types.GET_DATA_ARRAY")
	String[] getByLinenumber(
			@Parameter(name = "log", desc = "Which log, as described by the Logs enum")
			String log, 
			@Parameter(name = "lines", desc = "The number of matching lines to return")
			int lines,
			@Parameter(name = "linenumber", desc = "The line number")
			int linenumber);
}