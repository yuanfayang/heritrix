/*
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
