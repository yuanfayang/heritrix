/* Copyright (C) 2003 Internet Archive.
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
 * DevUtils.java
 * Created on Oct 29, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.archive.crawler.framework.ToeThread;

/**
 * @author gojomo
 *
 */
public class DevUtils {
    public static Logger logger = Logger.getLogger("org.archive.util.DevUtils");

    public static void warnHandle(Throwable ex, String note) {
    	StringWriter sw = new StringWriter();
    	sw.write(note);
    	sw.write("\n");
    	ex.printStackTrace(new PrintWriter(sw));
    	logger.warning(sw.toString());
    }

    public static String extraInfo() {
    	Thread current = Thread.currentThread();
    	if (current instanceof ToeThread) {
    		return ((ToeThread)current).report();
    	}
    	return "";
    }
}
