package org.archive.crawler.framework;
import java.io.IOException;

import org.archive.modules.LoggerModule;
import org.archive.openmbeans.annotations.Attribute;
import org.archive.openmbeans.annotations.Operation;

/**
 * 
 */

/**
 * @author pjack
 *
 */
public interface AlertTracker {

    
    @Attribute(desc="Returns the current number of unviewed alerts.", def="0")
    int getAlertCount();

    @Operation(desc="Resets the current alert count.")
    void resetAlertCount();
    
    @Operation(desc="Rotates log files.")
    void rotateLogFiles() throws IOException;
    

    
}
