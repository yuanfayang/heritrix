/* JmxUtils
 * 
 * Created on May 18, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

import javax.management.RuntimeOperationsException;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public class JmxUtils {
    public static void checkParamsCount(String operationName, Object[] params,
            int count) {
        if ((params.length != count)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(
                  "Cannot invoke " + operationName + ": Passed " +
                  Integer.toString(params.length) + " argument(s) " +
                  "but expected " + Integer.toString(count)),
                  "Wrong number of arguments passed: unable to invoke " +
                  operationName + " method");
        } 
    }
    
    protected JmxUtils() {
        super();
    }

    public static void main(String [] args) {
        // Unused.
    }
}
