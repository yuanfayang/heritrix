/* Copyright (C) 2003 Internet Archive.
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
 * ToeThread.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.Alert;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.InstancePerThread;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.archive.util.HttpRecorderMarker;
import org.archive.util.HttpRecorder;
import org.archive.util.PaddingStringBuffer;

/**
 * One "worker thread"; asks for CrawlURIs, processes them,
 * repeats unless told otherwise.
 *
 * @author Gordon Mohr
 */
public class ToeThread extends Thread
    implements CoreAttributeConstants, FetchStatusCodes, HttpRecorderMarker
{
    private static Logger logger = Logger.getLogger("org.archive.crawler.framework.ToeThread");
    private static int DEFAULT_TAKE_TIMEOUT = 3000;
    
    private ToePool pool;
    private volatile boolean shouldCrawl = true;
    private volatile boolean shouldPause = false;

    CrawlController controller;
    int serialNumber;
    HttpRecorder httpRecorder;
    HashMap localProcessors = new HashMap();
    String currentProcessorName = "";

    CrawlURI currentCuri;
    long lastStartTime;
    long lastFinishTime;
    // in-process/on-hold curis? not for now
    // a queue of curis to do next? not for now

    // debugging
    int where = 0;
    
    /**
     * @param c
     * @param p
     * @param sn
     */
    public ToeThread(CrawlController c, ToePool p, int sn) {
        controller = c;
        pool = p;
        serialNumber = sn;
        setName("ToeThread #"+serialNumber);
        httpRecorder = new HttpRecorder(controller.getScratchDisk(),"tt"+sn+"http");
        lastFinishTime = System.currentTimeMillis();
    }

    /**
     * @return
     */
    public boolean isAvailable() {
        return currentCuri == null;
    }

    /** (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
        String name = controller.getOrder().getCrawlOrderName();
        logger.fine(getName()+" started for order '"+name+"'");

        while ( shouldCrawl ) {
            while ( shouldPause ) {
                pause();
            }
            try {
//                currentCuri = (CrawlURI) controller.getFrontier().newNext(DEFAULT_TAKE_TIMEOUT);
                currentCuri = (CrawlURI) controller.getFrontier().next(DEFAULT_TAKE_TIMEOUT);
            } catch (InterruptedException e1) {
                currentCuri = null;
            }
            if ( currentCuri != null ) {
                processCrawlUri();
            }
            where = 16;
        }
        where = 17;
        controller.toeFinished(this);

        // Do cleanup so that objects can be GC.
        pool = null;
        controller = null;
        httpRecorder.closeRecorders();
        httpRecorder = null;
        localProcessors = null;

        logger.fine(getName()+" finished for order '"+name+"'");
    }

    /**
     * 
     */
    private synchronized void pause() {
        try {
            wait();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void processCrawlUri() {
        currentCuri.setThreadNumber(serialNumber);
        currentCuri.setNextProcessorChain(controller.getFirstProcessorChain());
        lastStartTime = System.currentTimeMillis();
        where = 3;
        try {
            while (currentCuri.nextProcessorChain() != null) {
                where = 4;
                // Starting on a new processor chain.
                currentCuri.setNextProcessor(currentCuri.nextProcessorChain().getFirstProcessor());
                currentCuri.setNextProcessorChain(currentCuri.nextProcessorChain().getNextProcessorChain());

                while (currentCuri.nextProcessor() != null) {
                    where = 5;
                    Processor currentProcessor = getProcessor(currentCuri.nextProcessor());
                    currentProcessorName = currentProcessor.getName();
                    currentProcessor.process(currentCuri);
                }
            }
            where = 6;
        } catch (RuntimeException e) {
            where = 7;
            e.printStackTrace(System.err);
            currentCuri.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            currentCuri.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
            String title = "RuntimeException occured processing '" + currentCuri.getURIString() + "'";
            String message = "The following RuntimeException occure when trying " +
            		"to process '" + currentCuri.getURIString() + "'\n";
            Heritrix.addAlert(new Alert(title,message.toString(),e, Level.SEVERE));
        } catch (Error err) {
            where = 8;
            // OutOfMemory & StackOverflow & etc.
            System.err.println(err);
            System.err.println(DevUtils.extraInfo());
            err.printStackTrace(System.err);
            currentCuri.setFetchStatus(S_SERIOUS_ERROR);
            String title = "Serious error occured processing '" + currentCuri.getURIString() + "'";
            String message = "The following serious error occure when trying " +
            		"to process '" + currentCuri.getURIString() + "'\n";
            Heritrix.addAlert(new Alert(title,message.toString(),err, Level.SEVERE));
        }
        where = 9;
        controller.getFrontier().finished(currentCuri);
        where = 10;
        currentCuri = null;
        lastFinishTime = System.currentTimeMillis();
    }


    /**
     * @param processor
     * @return
     */
    private Processor getProcessor(Processor processor) {
        if(!(processor instanceof InstancePerThread)) {
            // just use the shared Processor
             return processor;
        }
        // must use local copy of processor
        Processor localProcessor = (Processor) localProcessors.get(
                    processor.getClass().getName());
        if (localProcessor == null) {
            localProcessor = processor.spawn(this.getSerialNumber());
            localProcessors.put(processor.getClass().getName(),localProcessor);
        }
        return localProcessor;
    }

    private boolean shouldCrawl() {
        return shouldCrawl;
    }

    /**
     * 
     */
    public synchronized void stopAfterCurrent() {
        logger.info("ToeThread " + serialNumber + " has been told to stopAfterCurrent()");
        shouldCrawl = false;
        if(isAvailable())
        {
            notify();
        }
    }

    /**
     * @return
     */
    public int getSerialNumber() {
        return serialNumber;
    }

    /**
     * @see org.archive.util.HttpRecorderMarker#getHttpRecorder()
     */
    public HttpRecorder getHttpRecorder() {
        return httpRecorder;
    }

    /**
     * @param recorder
     */
    public void setHttpRecorder(HttpRecorder recorder) {
        httpRecorder = recorder;
    }

    /**
     * @return Compiles and returns a report on its status.
     */
    public synchronized String report()
    {
        PaddingStringBuffer rep = new PaddingStringBuffer();

        rep.append("     #"+serialNumber);
        rep.padTo(11);

        if(currentCuri!=null)
        {
            rep.append(currentCuri.getURIString());
            rep.append(" ("+currentCuri.getFetchAttempts()+" attempts)");
            rep.newline();
            rep.padTo(8);
            rep.append(currentCuri.getPathFromSeed());
            if(currentCuri.getVia() != null 
                    && currentCuri.getVia() instanceof CandidateURI){
                rep.append(" ");
                rep.append(((CandidateURI)currentCuri.getVia()).getURIString());
            }
            rep.newline();
            rep.padTo(8);
            rep.append("Current processor: "+currentProcessorName);
        }
        else
        {
            rep.append("[no CrawlURI]");
        }

        rep.newline();
        rep.padTo(8);

        long now = System.currentTimeMillis();
        long time = 0;

        if(lastFinishTime > lastStartTime)
        {
            // That means we finished something after we last started something
            // or in other words we are not working on anything.
            rep.append("WAITING for ");

            time = now-lastFinishTime;
        }
        else if(lastStartTime > 0)
        {
            // We are working on something
            rep.append("ACTIVE for ");

            time = now-lastStartTime;
        }
        rep.append(ArchiveUtils.formatMillisecondsToConventional(time));
        rep.newline();
        rep.padTo(8);
        rep.append("Where: "+where);
        rep.newline();

        this.dumpStack(); // Have report() trigger a write of stack trace. DEBUGING.

        return rep.toString();
    }


    /**
     * @param b
     */
    public synchronized void setShouldPause(boolean b) {
        shouldPause = b;
        if(!shouldPause) {
            notifyAll();
        }
    }
}
