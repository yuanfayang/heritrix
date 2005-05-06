/* DevUtils
 *
 * Created on Oct 29, 2003
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
package org.archive.util;

import java.util.logging.Logger;


/**
 * Write a message and stack trace to the 'org.archive.util.DevUtils' logger.
 *
 * @author gojomo
 * @version $Revision$ $Date$
 */
public class DevUtils {
    public static Logger logger =
        Logger.getLogger(DevUtils.class.getName());

    /**
     * Log a warning message to the logger 'org.archive.util.DevUtils' made of
     * the passed 'note' and a stack trace based off passed exception.
     *
     * @param ex Exception we print a stacktrace on.
     * @param note Message to print ahead of the stacktrace.
     */
    public static void warnHandle(Throwable ex, String note) {
        logger.warning(TextUtils.exceptionToString(note, ex));
    }

    /**
     * @return Extra information gotten from current Thread.  May not
     * always be available in which case we return empty string.
     */
    public static String extraInfo() {
        final Thread current = Thread.currentThread();
        return (current instanceof Reporter)? ((Reporter)current).report(): "";
    }
}
