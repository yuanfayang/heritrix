/**
 * 
 */
package org.archive.crawler.framework;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.archive.io.SinkHandlerLogThread;


/**
 * @author pjack
 *
 */
public class AlertHandler extends Handler {


    final public static AlertHandler INSTANCE = new AlertHandler();


    static {
        AlertHandler h = new AlertHandler();
        h.setLevel(Level.WARNING);
        Logger.getLogger("").addHandler(h);
        h.setFormatter(new SimpleFormatter());
    }


    @Override
    public void close() throws SecurityException {
        AlertThreadGroup.closeCurrent();
    }


    @Override
    public void flush() {
        Handler current = AlertThreadGroup.currentHandler();
        if (current != null) {
            current.flush();
        }
    }

    
    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        AlertThreadGroup atg = AlertThreadGroup.current();
        if (atg == null) {
            return;
        }
        String orig = record.getMessage();
        StringBuilder newMessage = new StringBuilder(256);
        Thread current = Thread.currentThread();
        newMessage.append(orig).append(" (in thread '");
        newMessage.append(current.getName()).append("'");
        if (current instanceof SinkHandlerLogThread) {
            SinkHandlerLogThread tt = (SinkHandlerLogThread)current;
            if(tt.getCurrentProcessorName().length()>0) {
                newMessage.append("; in processor '");
                newMessage.append(tt.getCurrentProcessorName());
                newMessage.append("'");
            }
        }
        newMessage.append(")");
        record.setMessage(newMessage.toString());
        Handler handler = atg.getDelegate();
        if (handler != null) {
            handler.publish(record);
            atg.incrementAlertCount();
        }
    }

}
