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
    private static final String STEP_NASCENT = "NASCENT";
    private static final String STEP_PAUSING = "PAUSING";
    private static final String STEP_ABOUT_TO_GET_URI = "ABOUT_TO_GET_URI";
    private static final String STEP_EXIT_PROCESSING_LOOP = "EXIT_PROCESSING_LOOP";
    private static final String STEP_FINISHED = "FINISHED";
    private static final String STEP_ABOUT_TO_BEGIN_CHAIN = "ABOUT_TO_BEGIN_CHAIN";
    private static final String STEP_ABOUT_TO_BEGIN_PROCESSOR = "ABOUT_TO_BEGIN_PROCESSOR";
    private static final String STEP_DONE_WITH_PROCESSORS = "DONE_WITH_PROCESSORS";
    private static final String STEP_HANDLING_RUNTIME_EXCEPTION = "HANDLING_RUNTIME_EXCEPTION";
    private static final String STEP_ABOUT_TO_RETURN_URI = "ABOUT_TO_RETURN_URI";
//    private static final String STEP_HANDLING_SERIOUS_ERROR = "HANDLING_SERIOUS_ERROR";
    private static final String STEP_FINISHING_PROCESS = "FINISHING_PROCESS";


    private static Logger logger = Logger.getLogger("org.archive.crawler.framework.ToeThread");
    private static int DEFAULT_TAKE_TIMEOUT = 3000;

    private volatile boolean shouldCrawl = true;
    private volatile boolean shouldPause = false;

    /**
     * If set then this thread was 'killed' by an outside agent it should
     * then terminate itself as quickly as possible.
     */
    private volatile boolean shouldDie = false;

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

    // debugging + used by kill() to know what state the toe is in.
    private String step = STEP_NASCENT;
    private static final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY-2;

    /**
     * @param c
     * @param sn
     */
    public ToeThread(CrawlController c, int sn) {
        controller = c;
        serialNumber = sn;
        setName("ToeThread #"+serialNumber);
        setPriority(DEFAULT_PRIORITY);
        httpRecorder = new HttpRecorder(controller.getScratchDisk(),"tt"+sn+"http");
        lastFinishTime = System.currentTimeMillis();
    }

//    /**
//     * @return If true then this thread is idle or dead.
//     */
//    public synchronized boolean isIdleOrDead() {
//        return currentCuri == null || shouldDie == true;
//    }

    /** (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
        String name = controller.getOrder().getCrawlOrderName();
        logger.fine(getName()+" started for order '"+name+"'");

        try {
            while ( shouldCrawl ) {
                while ( shouldPause ) {
                    step = STEP_PAUSING;
                    controller.toeChanged(this);
                    synchronized(this){
                        wait();
                    }
                }
                continueCheck();
                step = STEP_ABOUT_TO_GET_URI;
                if (controller.getFrontier() == null) {
                	    throw new NullPointerException("Frontier is null");
                }
                currentCuri = controller.getFrontier().next(DEFAULT_TAKE_TIMEOUT);
                if ( currentCuri != null ) {
                    controller.toePool.noteActive(this);
                    try {
                        processCrawlUri();
                        step = STEP_ABOUT_TO_RETURN_URI;
                        synchronized(this) {
                            continueCheck();
                            controller.getFrontier().finished(currentCuri);
                            currentCuri = null;
                        }
                        step = STEP_FINISHING_PROCESS;
                        lastFinishTime = System.currentTimeMillis();
                    } finally {
                        controller.toePool.noteInactive(this);
                    }
                }
                controller.checkFinish(); // after each URI or null
            }
            step = STEP_EXIT_PROCESSING_LOOP;
        } catch (InterruptedException e1) {
            if(!shouldDie){
                // Thread was interrupted for unknown reasons.
                System.err.println("interrupted while working on "+currentCuri);
                e1.printStackTrace();
            }
        } catch (Error err) {
            seriousError(err);
        } // finally {
            currentCuri = null;
            if(controller!=null) {
                controller.toeChanged(this);
            }
            // Do cleanup so that objects can be GC.
            controller = null;
            httpRecorder.closeRecorders();
            httpRecorder = null;
            localProcessors = null;

            logger.fine(getName()+" finished for order '"+name+"'");
            step = STEP_FINISHED;
        // }
    }

    /**
	 * @param err
	 */
	private void seriousError(Error err) {
	    // try to prevent timeslicing until we have a chance to deal with OOM
        setPriority(DEFAULT_PRIORITY+1);  
        if (controller!=null) {
            controller.freeReserveMemory();
            // actually hold all ToeThreads
            controller.lockMemory();
            controller.requestCrawlPause();
        }
        
        // OutOfMemory & StackOverflow & etc.
        System.err.println("<<<");
        System.err.println(err);
        System.err.println(DevUtils.extraInfo());
        err.printStackTrace(System.err);
        System.err.println(">>>");

        String context = "unknown";
		if(currentCuri!=null) {
			// currentCuri.setFetchStatus(S_SERIOUS_ERROR);
            context = currentCuri.getURIString();
		}
        String title = "Serious error occured processing '" + context + "'";
        String message = "The following serious error occured when trying " +
            "to process '" + context + "'\n";
		Heritrix.addAlert(new Alert(title,message.toString(),err, Level.SEVERE));
        setPriority(DEFAULT_PRIORITY);
	}

	/**
     * Perform checks as to whether normal execution shoudl proceed.
     * 
     * If an external die request is detected, throw an interrupted exception.
     * Used before anything that should not be attempted by a 'zombie' thread
     * that the Frontier/Crawl has given up on.
     * 
     * Otherwise, if the controller's memoryGate has been closed,
     * hold until it is opened. (Provides a better chance of 
     * being able to complete some tasks after an OutOfMemoryError.)
     *
     * @throws InterruptedException
     */
    private void continueCheck() throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException("die request detected");
        }
        controller.acquireMemory();
    }

    /**
     * Pass the CrawlURI to all appropriate processors
     *
     * @throws InterruptedException
     */
    private void processCrawlUri() throws InterruptedException {
        currentCuri.setThreadNumber(serialNumber);
        currentCuri.setNextProcessorChain(controller.getFirstProcessorChain());
        lastStartTime = System.currentTimeMillis();
        try {
            while (currentCuri.nextProcessorChain() != null) {
                step = STEP_ABOUT_TO_BEGIN_CHAIN;
                // Starting on a new processor chain.
                currentCuri.setNextProcessor(currentCuri.nextProcessorChain().getFirstProcessor());
                currentCuri.setNextProcessorChain(currentCuri.nextProcessorChain().getNextProcessorChain());

                while (currentCuri.nextProcessor() != null) {
                    step = STEP_ABOUT_TO_BEGIN_PROCESSOR;
                    Processor currentProcessor = getProcessor(currentCuri.nextProcessor());
                    currentProcessorName = currentProcessor.getName();
                    continueCheck();
                    currentProcessor.process(currentCuri);
                }
            }
            step = STEP_DONE_WITH_PROCESSORS;
            currentProcessorName = "";
        } catch (RuntimeException e) {
            step = STEP_HANDLING_RUNTIME_EXCEPTION;
            e.printStackTrace(System.err);
            currentCuri.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            currentCuri.getAList().putObject(A_RUNTIME_EXCEPTION,e);
            String title = "RuntimeException occured processing '" + currentCuri.getURIString() + "'";
            String message = "The following RuntimeException occure when trying " +
            		"to process '" + currentCuri.getURIString() + "'\n";
            Heritrix.addAlert(new Alert(title,message.toString(),e, Level.SEVERE));
        } catch (Error err) {
            // OutOfMemory & StackOverflow & etc.
            seriousError(err);
        }
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

    /**
     *
     */
    public synchronized void stopAfterCurrent() {
        logger.info("ToeThread " + serialNumber + " has been told to stopAfterCurrent()");
        shouldCrawl = false;
        notify();
//        if(isIdleOrDead())
//        {
//            notify();
//        }
    }

    /**
     * @return Return toe thread serial number.
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
     * TODO: This is a really bad way of doing this. Better to maintain
     * a hash of objects that the processors wish to share. This limits
     * the plugability of the processors since a part of the implementation
     * relies the implementation of the ToeThread.
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

        rep.padTo(5);
        rep.append("#"+serialNumber);
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
        rep.append("Where: "+step);
        rep.newline();
        if(shouldDie){
            rep.padTo(8);
            if(isAlive()){
                rep.append("THREAD HAS BEEN ISSUED A KILL INSTRUCTION - " +
                        "shouldDie == true - IS HUNG\n");
            } else {
                rep.append("THREAD HAS BEEN KILLED - shouldDie == true - " +
                        "HAS EXITED\n");
            }
        }

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

    /** Get the CrawlController acossiated with this thread.
     *
     * @return Returns the CrawlController.
     */
    public CrawlController getController() {
        return controller;
    }

    /**
     * Terminates a thread.
     *
     * <p> Calling this method will ensure that the current thread will stop
     * processing as soon as possible (note: this may be never). Meant to
     * 'short circuit' hung threads.
     *
     * <p> Current crawl uri will have it's fetch status set accordingly and
     * will be immediately returned to the frontier.
     *
     * <p> As noted before, this does not ensure that the thread will stop
     * running (ever). But once evoked it will not try and communicate with
     * other parts of crawler and will terminate as soon as control is
     * established.
     *
     * <p> Method should only be invoked by it's ToePool. Otherwise the pool
     * will become out of sync with the threads currently alive.
     *
     * @param newSerial New serial (id) for the thread.
     */
    protected void kill(int newSerial){
        synchronized(this) {
            shouldDie = true;
            shouldCrawl = false;
            shouldPause = false;
            serialNumber = newSerial;
            if (currentCuri!=null) {
                currentCuri.setFetchStatus(S_PROCESSING_THREAD_KILLED);
                controller.getFrontier().finished(currentCuri);
                // currentCuri = null;
            }
        }
        this.interrupt();
        if(controller!=null) {
            controller.toeChanged(this);
        }
    }

}
