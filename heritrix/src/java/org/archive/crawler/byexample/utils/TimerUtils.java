package org.archive.crawler.byexample.utils;

import java.util.logging.Logger;

public class TimerUtils {
    
    long timeCounter=0;
    
    private static Logger logger =
        Logger.getLogger(TimerUtils.class.getName());

    public void startTimer(){
        timeCounter=System.currentTimeMillis();
    }
    
    public long getTimer(){        
        return System.currentTimeMillis()-timeCounter;
    }
    
    public void  reportActionTimer(String action){
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
