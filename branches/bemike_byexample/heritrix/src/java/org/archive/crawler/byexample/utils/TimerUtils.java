package org.archive.crawler.byexample.utils;

import java.util.logging.Logger;

/**
 * Timer utilities for timing system actions
 * 
 * @author Michael Bendersky
 *
 */
public class TimerUtils {
    
    private long timeCounter=0;
    
    private static Logger logger =
        Logger.getLogger(TimerUtils.class.getName());
    
    /**
     * Starts new timer counter.
     * Only a single timer counter can be run by an instance of 
     * <code>TimerUtils</code> class 
     *
     */
    public void startTimer(){
        timeCounter=System.currentTimeMillis();
    }
    
    /**
     * Get time elapsed (in miliseconds) since the last <i>startTimer</i> action
     * @return
     */
    public long getTimer(){        
        return System.currentTimeMillis()-timeCounter;
    }
    
    /**
     * Reports percentage of an action run so far
     * @param action Action description 
     * @param percentage Completed percentage of the action
     */
    public static void reportPartialAction(String action, int percentage){
        logger.info("Completed so far "+ percentage+ "% of the action: "+action);
    }
    
    /**
     * Reports action run time.
     * Run time is calculated by <i>getTimer()</i> action.
     * Run time is reported as: "Action [Action_name] completed in MM minutes, SS seconds".
     * The correct way to use this method would look something like:<br>
     * <pre>
     * startTimer();
     * doSomeAction();
     * reportActionTimer();
     * </pre>
     * 
     * @param action Action description
     */
    public void reportActionTimer(String action){
        long currTimer=getTimer();
        timeCounter=0;        
        long mins=currTimer/(1000*60);
        long secs=0;
        if (mins>0)
            secs=currTimer%mins;
        else
            secs=currTimer/1000;

       logger.info("Action: "+action+" completed in "+mins+" minutes, "+secs+" seconds");        
    }
}
