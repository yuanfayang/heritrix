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

/**
 * @author gojomo
 *
 */
public class DevUtils {
	private static Logger logger = Logger.getLogger("org.archive.util.DevUtils");

	public static void warnHandle(Exception ex, String note) {
		StringWriter sw = new StringWriter();
		sw.write(note);
		sw.write("\n");
		ex.printStackTrace(new PrintWriter(sw));
		logger.warning(sw.toString());
	}
}
