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

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import org.archive.crawler.datamodel.CrawlURI;

import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.modules.PostProcessor;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.fetcher.HostResolver;
import org.archive.io.SinkHandlerLogThread;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.archive.util.Recorder;
import org.archive.util.RecorderMarker;
import org.archive.util.ProgressStatisticsReporter;
import org.archive.util.Reporter;

import com.sleepycat.util.RuntimeExceptionWrapper;

/**
 * One "worker thread"; asks for CrawlURIs, processes them,
 * repeats unless told otherwise.
 *
 * @author Gordon Mohr
 */
public class ToeThread extends Thread
implements RecorderMarker, Reporter, ProgressStatisticsReporter, 
           HostResolver, SinkHandlerLogThread {

    private static final String STEP_NASCENT = "NASCENT";
    private static final String STEP_ABOUT_TO_GET_URI = "ABOUT_TO_GET_URI";
    private static final String STEP_FINISHED = "FINISHED";
    private static final String STEP_ABOUT_TO_BEGIN_PROCESSOR =
        "ABOUT_TO_BEGIN_PROCESSOR";
    private static final String STEP_DONE_WITH_PROCESSORS =
        "DONE_WITH_PROCESSORS";
    private static final String STEP_HANDLING_RUNTIME_EXCEPTION =
        "HANDLING_RUNTIME_EXCEPTION";
    private static final String STEP_ABOUT_TO_RETURN_URI =
        "ABOUT_TO_RETURN_URI";
    private static final String STEP_FINISHING_PROCESS = "FINISHING_PROCESS";

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.ToeThread");

    private CrawlControllerImpl controller;
    private int serialNumber;
    
    /**
     * Each ToeThead has an instance of HttpRecord that gets used
     * over and over by each request.
     * 
     * @see org.archive.util.RecorderMarker
     */
    private Recorder httpRecorder = null;
    
 //   private HashMap<String,Processor> localProcessors
 //    = new HashMap<String,Processor>();
    private String currentProcessorName = "";

    private String coreName;
    private CrawlURI currentCuri;
    private long lastStartTime;
    private long lastFinishTime;

    // activity monitoring, debugging, and problem detection
    private String step = STEP_NASCENT;
    private long atStepSince;
    
    // default priority; may not be meaningful in recent JVMs
    private static final int DEFAULT_PRIORITY = Thread.NORM_PRIORITY-2;
    
    // indicator that a thread is now surplus based on current desired
    // count; it should wrap up cleanly
    private volatile boolean shouldRetire = false;
    
    /**
     * Create a ToeThread
     * 
     * @param g ToeThreadGroup
     * @param sn serial number
     */
    public ToeThread(ToePool g, int sn) {
        // TODO: add crawl name?
        super(g,"ToeThread #" + sn);
        coreName="ToeThread #" + sn + ": ";
        controller = g.getController();
        serialNumber = sn;
        setPriority(DEFAULT_PRIORITY);
        int outBufferSize = controller
                .get(controller, CrawlControllerImpl.RECORDER_OUT_BUFFER_BYTES);
        int inBufferSize = controller
                .get(controller, CrawlControllerImpl.RECORDER_IN_BUFFER_BYTES);
        httpRecorder = new Recorder(controller.getScratchDir(),
            "tt" + sn + "http", outBufferSize, inBufferSize);
        lastFinishTime = System.currentTimeMillis();
    }

    /** (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
        String name = controller.getSheetManager().getCrawlName();
        logger.fine(getName()+" started for order '"+name+"'");

        try {
            while ( true ) {
                // TODO check for thread-abort? or is waiting for interrupt enough?
                continueCheck();
                
                setStep(STEP_ABOUT_TO_GET_URI);

                CrawlURI curi = controller.getFrontier().next();
                
                synchronized(this) {
                    continueCheck();
                    setCurrentCuri(curi);
                }
                
                processCrawlUri();
                
                setStep(STEP_ABOUT_TO_RETURN_URI);
                continueCheck();

                synchronized(this) {
                    controller.getFrontier().finished(currentCuri);
                    setCurrentCuri(null);
                }
                
                setStep(STEP_FINISHING_PROCESS);
                lastFinishTime = System.currentTimeMillis();
                controller.releaseContinuePermission();
                if(shouldRetire) {
                    break; // from while(true)
                }
            }
        } catch (InterruptedException e) {
            // thread interrupted, ok to end
            logger.log(Level.FINE,this.getName()+ " ended with Interruption");
        } catch (EndedException e) {
            // crawl ended (or thread was retired), so allow thread to end
            logger.log(Level.FINE,this.getName()+ " ended with EndedException");
        } catch (Exception e) {
            // everything else (including interruption)
            logger.log(Level.SEVERE,"Fatal exception in "+getName(),e);
        } catch (OutOfMemoryError err) {
            seriousError(err);
        } finally {
            controller.releaseContinuePermission();
        }
        setCurrentCuri(null);
        // Do cleanup so that objects can be GC.
        this.httpRecorder.closeRecorders();
        this.httpRecorder = null;
//        localProcessors = null;

        logger.fine(getName()+" finished for order '"+name+"'");
        setStep(STEP_FINISHED);
        controller.toeEnded();
        controller = null;
    }

    /**
     * Set currentCuri, updating thread name as appropriate
     * @param curi
     */
    private void setCurrentCuri(CrawlURI curi) {
        if(curi==null) {
            setName(coreName);
        } else {
            setName(coreName+curi);
        }
        currentCuri = curi;
    }

    /**
     * @param s
     */
    private void setStep(String s) {
        step=s;
        atStepSince = System.currentTimeMillis();
    }

        private void seriousError(Error err) {
            // try to prevent timeslicing until we have a chance to deal with OOM
        // TODO: recognize that new JVM priority indifference may make this
        // priority-jumbling pointless
        setPriority(DEFAULT_PRIORITY+1);  
        if (controller!=null) {
            // hold all ToeThreads from proceeding to next processor
            controller.singleThreadMode();
            // TODO: consider if SoftReferences would be a better way to 
            // engineer a soft-landing for low-memory conditions
            controller.freeReserveMemory();
            controller.requestCrawlPause();
            if (controller.getFrontier().getFrontierJournal() != null) {
                controller.getFrontier().getFrontierJournal().seriousError(
                    getName() + err.getMessage());
            }
        }
        
        // OutOfMemory etc.
        String extraInfo = DevUtils.extraInfo();
        System.err.println("<<<");
        System.err.println(ArchiveUtils.getLog17Date());
        System.err.println(err);
        System.err.println(extraInfo);
        err.printStackTrace(System.err);
        
        if (controller!=null) {
            PrintWriter pw = new PrintWriter(System.err);
            controller.getToePool().compactReportTo(pw);
            pw.flush();
        }
        System.err.println(">>>");
//        DevUtils.sigquitSelf();
        
        String context = "unknown";
                if(currentCuri!=null) {
            // update fetch-status, saving original as annotation
            currentCuri.getAnnotations().add("err="+err.getClass().getName());
            currentCuri.getAnnotations().add("os"+currentCuri.getFetchStatus());
                        currentCuri.setFetchStatus(S_SERIOUS_ERROR);
            context = currentCuri.singleLineReport() + " in " + currentProcessorName;
                }
        String message = "Serious error occured trying " +
            "to process '" + context + "'\n" + extraInfo;
        logger.log(Level.SEVERE, message.toString(), err);
        setPriority(DEFAULT_PRIORITY);
        }

        /**
     * Perform checks as to whether normal execution should proceed.
     * 
     * If an external interrupt is detected, throw an interrupted exception.
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
        controller.acquireContinuePermission();
    }

    /**
     * Pass the CrawlURI to all appropriate processors
     *
     * @throws InterruptedException
     */
    private void processCrawlUri() throws InterruptedException {
        currentCuri.setThreadNumber(this.serialNumber);
        lastStartTime = System.currentTimeMillis();
        Map<String,Processor> localProcessors = 
            controller.get(controller, CrawlControllerImpl.PROCESSORS);
        currentCuri.setStateProvider(controller.getSheetManager());
        currentCuri.setRecorder(httpRecorder);
        try {
            Set<Map.Entry<String,Processor>> procs = localProcessors.entrySet();
            Iterator<Map.Entry<String,Processor>> iter = procs.iterator();
            Map.Entry<String,Processor> curProc = 
                iter.hasNext() ? iter.next() : null;
            while (curProc != null) {
                setStep(STEP_ABOUT_TO_BEGIN_PROCESSOR);
                currentProcessorName = curProc.getKey();
                continueCheck();
                ProcessResult pr = curProc.getValue().process(currentCuri);
                switch (pr.getProcessStatus()) {
                    case PROCEED:
                        curProc = iter.hasNext() ? iter.next() : null;
                        break;
                    case STUCK:
                        controller.requestCrawlPause();
                        curProc = null;
                        break;
                    case FINISH:
                        curProc = advanceToPostProcessing(iter);
                        break;
                    case JUMP:
                        curProc = advanceToNamed(iter, pr.getJumpTarget());
                        break;
                }
            }
            setStep(STEP_DONE_WITH_PROCESSORS);
            currentProcessorName = "";
        } catch (RuntimeExceptionWrapper e) {
            // Workaround to get cause from BDB
            if(e.getCause() == null) {
                e.initCause(e.getCause());
            }
            recoverableProblem(e);
        } catch (AssertionError ae) {
            // This risks leaving crawl in fatally inconsistent state, 
            // but is often reasonable for per-Processor assertion problems 
            recoverableProblem(ae);
        } catch (RuntimeException e) {
            recoverableProblem(e);
        } catch (StackOverflowError err) {
            recoverableProblem(err);
        } catch (Error err) {
            // OutOfMemory and any others
            seriousError(err); 
        }
    }

    
    private Map.Entry<String,Processor> advanceToNamed(
            Iterator<Map.Entry<String,Processor>> iter, String name) {
        while (iter.hasNext()) {
            Map.Entry<String,Processor> me = iter.next();
            if (me.getKey().equals(name)) {
                return me;
            }
        }
        return null;
    }

    
    private Map.Entry<String,Processor> advanceToPostProcessing(
            Iterator<Map.Entry<String,Processor>> iter) {
        while (iter.hasNext()) {
            Map.Entry<String,Processor> me = iter.next();
            if (me.getValue() instanceof PostProcessor) {
                return me;
            }
        }
        return null;
    }

    
    /**
     * Handling for exceptions and errors that are possibly recoverable.
     * 
     * @param e
     */
    private void recoverableProblem(Throwable e) {
        Object previousStep = step;
        setStep(STEP_HANDLING_RUNTIME_EXCEPTION);
        e.printStackTrace(System.err);
        currentCuri.setFetchStatus(S_RUNTIME_EXCEPTION);
        // store exception temporarily for logging
        currentCuri.getAnnotations().add("err="+e.getClass().getName());
        currentCuri.getData().put(A_RUNTIME_EXCEPTION, e);
        String message = "Problem " + e + 
                " occured when trying to process '"
                + currentCuri.toString()
                + "' at step " + previousStep 
                + " in " + currentProcessorName +"\n";
        logger.log(Level.SEVERE, message.toString(), e);
    }


    /**
     * @return Return toe thread serial number.
     */
    public int getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Used to get current threads HttpRecorder instance.
     * Implementation of the HttpRecorderMarker interface.
     * @return Returns instance of HttpRecorder carried by this thread.
     * @see org.archive.util.RecorderMarker#getHttpRecorder()
     */
    public Recorder getHttpRecorder() {
        return this.httpRecorder;
    }
    
    /** Get the CrawlController acossiated with this thread.
     *
     * @return Returns the CrawlController.
     */
    public CrawlControllerImpl getController() {
        return controller;
    }

    /**
     * Terminates a thread.
     *
     * <p> Calling this method will ensure that the current thread will stop
     * processing as soon as possible (note: this may be never). Meant to
     * 'short circuit' hung threads.
     *
     * <p> Current crawl uri will have its fetch status set accordingly and
     * will be immediately returned to the frontier.
     *
     * <p> As noted before, this does not ensure that the thread will stop
     * running (ever). But once evoked it will not try and communicate with
     * other parts of crawler and will terminate as soon as control is
     * established.
     */
    protected void kill(){
        this.interrupt();
        synchronized(this) {
            if (currentCuri!=null) {
                currentCuri.setFetchStatus(S_PROCESSING_THREAD_KILLED);
                controller.getFrontier().finished(currentCuri);
             }
        }
    }

        /**
         * @return Current step (For debugging/reporting, give abstract step
     * where this thread is).
         */
        public Object getStep() {
                return step;
        }

    /**
     * Is this thread validly processing a URI, not paused, waiting for 
     * a URI, or interrupted?
     * @return whether thread is actively processing a URI
     */
    public boolean isActive() {
        // if alive and not waiting in/for frontier.next(), we're 'active'
        return this.isAlive() && (currentCuri != null) && !isInterrupted();
    }
    
    /**
     * Request that this thread retire (exit cleanly) at the earliest
     * opportunity.
     */
    public void retire() {
        shouldRetire = true;
    }

    /**
     * Whether this thread should cleanly retire at the earliest 
     * opportunity. 
     * 
     * @return True if should retire.
     */
    public boolean shouldRetire() {
        return shouldRetire;
    }

    //
    // Reporter implementation
    // 
    
    /**
     * Compiles and returns a report on its status.
     * @param name Report name.
     * @param pw Where to print.
     */
    public void reportTo(String name, PrintWriter pw) {
        // name is ignored for now: only one kind of report
        
        pw.print("[");
        pw.println(getName());

        // Make a local copy of the currentCuri reference in case it gets
        // nulled while we're using it.  We're doing this because
        // alternative is synchronizing and we don't want to do this --
        // it causes hang ups as controller waits on a lock for this thread,
        // something it gets easily enough on old threading model but something
        // it can wait interminably for on NPTL threading model.
        // See [ 994946 ] Pause/Terminate ignored on 2.6 kernel 1.5 JVM.
        CrawlURI c = currentCuri;
        if(c != null) {
            pw.print(" ");
            c.singleLineReportTo(pw);
            pw.print("    ");
            pw.print(c.getFetchAttempts());
            pw.print(" attempts");
            pw.println();
            pw.print("    ");
            pw.print("in processor: ");
            pw.print(currentProcessorName);
        } else {
            pw.print(" -no CrawlURI- ");
        }
        pw.println();

        long now = System.currentTimeMillis();
        long time = 0;

        pw.print("    ");
        if(lastFinishTime > lastStartTime) {
            // That means we finished something after we last started something
            // or in other words we are not working on anything.
            pw.print("WAITING for ");
            time = now - lastFinishTime;
        } else if(lastStartTime > 0) {
            // We are working on something
            pw.print("ACTIVE for ");
            time = now-lastStartTime;
        }
        pw.print(ArchiveUtils.formatMillisecondsToConventional(time));
        pw.println();

        pw.print("    ");
        pw.print("step: ");
        pw.print(step);
        pw.print(" for ");
        pw.print(ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis()-atStepSince));
        pw.println();

        reportThread(this, pw);
        pw.print("]");
        pw.println();
        
        pw.flush();
    }

    /**
     * @param t Thread
     * @param pw PrintWriter
     */
    static public void reportThread(Thread t, PrintWriter pw) {
        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
        ThreadInfo info = tmxb.getThreadInfo(t.getId());
        pw.print("Java Thread State: ");
        pw.println(info.getThreadState());
        pw.print("Blocked/Waiting On: ");
        if (info.getLockOwnerId() >= 0) {
            pw.print(info.getLockName());
            pw.print(" which is owned by ");
            pw.print(info.getLockOwnerName());
            pw.print("(");
            pw.print(info.getLockOwnerId());
            pw.println(")");
        } else {
            pw.println("NONE");
        }
        
        StackTraceElement[] ste = t.getStackTrace();
        for(int i=0;i<ste.length;i++) {
            pw.print("    ");
            pw.print(ste[i].toString());
            pw.println();
        }
    }

    /**
     * @param w PrintWriter to write to.
     */
    public void singleLineReportTo(PrintWriter w)
    {
        w.print("#");
        w.print(this.serialNumber);

        // Make a local copy of the currentCuri reference in case it gets
        // nulled while we're using it.  We're doing this because
        // alternative is synchronizing and we don't want to do this --
        // it causes hang ups as controller waits on a lock for this thread,
        // something it gets easily enough on old threading model but something
        // it can wait interminably for on NPTL threading model.
        // See [ 994946 ] Pause/Terminate ignored on 2.6 kernel 1.5 JVM.
        CrawlURI c = currentCuri;
        if(c != null) {
            w.print(" ");
            w.print(currentProcessorName);
            w.print(" ");
            w.print(c.toString());
            w.print(" (");
            w.print(c.getFetchAttempts());
            w.print(") ");
        } else {
            w.print(" [no CrawlURI] ");
        }
        
        long now = System.currentTimeMillis();
        long time = 0;

        if(lastFinishTime > lastStartTime) {
            // That means we finished something after we last started something
            // or in other words we are not working on anything.
            w.print("WAITING for ");
            time = now - lastFinishTime;
        } else if(lastStartTime > 0) {
            // We are working on something
            w.print("ACTIVE for ");
            time = now-lastStartTime;
        }
        w.print(ArchiveUtils.formatMillisecondsToConventional(time));
        w.print(" at ");
        w.print(step);
        w.print(" for ");
        w.print(ArchiveUtils.formatMillisecondsToConventional(now-atStepSince));
        w.print("\n");
        w.flush();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "#serialNumber processorName currentUri (fetchAttempts) threadState threadStep";
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#getReports()
     */
    public String[] getReports() {
        // for now none but the default
        return new String[] {};
    }

    public void reportTo(PrintWriter writer) {
        reportTo(null, writer);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    public void progressStatisticsLine(PrintWriter writer) {
        writer.print(getController().getStatistics()
            .getProgressStatisticsLine());
        writer.print("\n");
    }

    public void progressStatisticsLegend(PrintWriter writer) {
        writer.print(getController().getStatistics()
            .progressStatisticsLegend());
        writer.print("\n");
    }
    
    public String getCurrentProcessorName() {
        return currentProcessorName;
    }
    
    
    public InetAddress resolve(String host) {
        return controller.getServerCache().getHostFor(host).getIP();
    }
}
