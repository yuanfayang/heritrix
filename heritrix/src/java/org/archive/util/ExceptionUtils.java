/* ExceptionUtils.java
 *
 * $Id$
 *
 * Created Jul 20, 2005
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class ExceptionUtils {
    private static final Logger LOGGER =
        Logger.getLogger(ExceptionUtils.class.getName());
    
    public static IOException convertToIOException(Throwable e) {
        return (IOException)convertException(e, IOException.class);
    }
    
    public static RuntimeException convertToRuntime(Throwable e) {
        return (RuntimeException)convertException(e, RuntimeException.class);
    }
    
    /**
     * Convert passed Thowable instance <code>e</code> to the an exception of
     * specified class.
     * @param e Exception to convert.
     * @param toException Class to convert to.
     * @return New exception with message that makes mention of convertion.
     */
    public static Throwable convertException(Throwable e,
            Class toException) {
        String newMessage = "Converted from " + e.getClass().getName() +
            ": " + e.getMessage();
        Throwable t = null;
        try {
            Constructor c =
                toException.getConstructor(new Class [] {String.class});
            t = (Throwable)c.newInstance(new String [] {newMessage});
            t.setStackTrace(e.getStackTrace());
        } catch (Exception exception) {
            LOGGER.severe("Failed converting " + e.getClass().getName() +
                    " to " + toException.getClass() + ": Stack trace follows.");
            e.printStackTrace();
        }
        return t;
    }
    
    public static void main(String[] args) throws Throwable {
        // Test method.
        IllegalAccessException e = new IllegalAccessException("test");
        throw convertException(e, NullPointerException.class);
    }
}