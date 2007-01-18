/* Copyright (C) 2007 Internet Archive.
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
 * SingleSheetCleanUpThread.java
 * Created on January 18, 2007
 *
 * $Header$
 */
package org.archive.settings;

import java.lang.ref.ReferenceQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class SingleSheetCleanUpThread extends Thread {

    final private static Logger LOGGER = 
        Logger.getLogger(SingleSheetCleanUpThread.class.getName());
    
    final public static ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    
    final public static SingleSheetCleanUpThread INSTANCE = 
        new SingleSheetCleanUpThread();

    
    static {
        INSTANCE.start();
    }
    
    
    private SingleSheetCleanUpThread() {
        setDaemon(true);
        setName("SingleSheetCleanUpThread");
    }
    
    public void run() {
        LOGGER.finest("Running SingleSheetCleanUpThread.");
        while (true) try {
            ModuleReference mr = (ModuleReference)queue.remove();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Removing " + mr.getModuleIdentity());
            }
            mr.getSingleSheet().remove(mr);
        } catch (InterruptedException e) {
            // do nothing
        }
    }
    
    
    public static void enqueue(SingleSheet sheet, Object module) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            int id = System.identityHashCode(module);
            LOGGER.finest("Adding " + id);
        }
        new ModuleReference(sheet, module);
    }
}
