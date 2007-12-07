/**
 * 
 */
package org.archive.crawler.framework;

import org.archive.io.GenerationFileHandler;

/**
 * @author pjack
 *
 */
public class AlertThreadGroup extends ThreadGroup {

    
    private GenerationFileHandler handler;
    private int count;
    
    public AlertThreadGroup(String name) {
        super(name);
    }
    
    
    public GenerationFileHandler getDelegate() {
        return handler;
    }
    
    
    public int getAlertCount() {
        return count;
    }
    
    public void incrementAlertCount() {
        count++;
    }
    
    
    public void resetAlertCount() {
        count = 0;
    }
    
    public static void setCurrentHandler(GenerationFileHandler handler) {
        current().handler = handler;
    }

    public static AlertThreadGroup current() {
        Thread t = Thread.currentThread();
        ThreadGroup th = t.getThreadGroup();
        while ((th != null) && !(th instanceof AlertThreadGroup)) {
            th = th.getParent();
        }
        return (AlertThreadGroup)th;
    }


    public static GenerationFileHandler currentHandler() {
        AlertThreadGroup current = current();
        if (current == null) {
            return null;
        }
        return current.handler;
    }


    public static void closeCurrent() {
        AlertThreadGroup current = current();
        if (current == null) {
            return;
        }
        current.handler.close();
        current.handler = null;
    }
}
