package org.archive.modules;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.archive.spring.HasKeyedProperties;
import org.archive.spring.KeyedProperties;
import org.archive.util.ArchiveUtils;
import org.archive.util.Reporter;
import org.springframework.context.Lifecycle;

/**
 * Collection of Processors to run.
 * 
 * Not just a list on another bean so that:
 *  - chain is a prominent standalone part of configuration
 *  - Lifecycle events may be propagated to members defined 
 *  as inner beans
 *  - future override capability may allow inserts at any place in
 *  order, not just end (assuming TBD specialized iterator)
 *  
 *  See subclasses CandidateChain, FetchChain, and DispositionChain
 */
public class ProcessorChain 
implements Iterable<Processor>, 
           HasKeyedProperties, 
           Reporter,
           Lifecycle {
    
    KeyedProperties kp = new KeyedProperties();
    public KeyedProperties getKeyedProperties() {
        return kp;
    }
    
    public int size() {
        return getProcessors().size();
    }

    public Iterator<Processor> iterator() {
        return getProcessors().iterator();
    }

    @SuppressWarnings("unchecked")
    public List<Processor> getProcessors() {
        return (List<Processor>) kp.get("processors");
    }
    public void setProcessors(List<Processor> processors) {
        kp.put("processors",processors);
    }

    boolean isRunning = false; 
    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        for(Processor p : getProcessors()) {
            // relies on each Processor's start() being ok to call if 
            // already running, which is part of the Lifecycle contract
            p.start(); 
        }
        isRunning = true; 
    }

    public void stop() {
        for(Processor p : getProcessors()) {
            // relies on each Processor's stop() being ok to call if 
            // not running, which is part of the Lifecycle contract
            p.stop(); 
        }
        isRunning = false; 
    }

    /**
     * Compiles and returns a human readable report on the active processors.
     * @param writer Where to write to.
     * @see org.archive.crawler.framework.Processor#report()
     */
    public void reportTo(PrintWriter writer) {
        writer.print(
            getClass().getSimpleName() + " - Processors report - "
                + ArchiveUtils.get12DigitDate()
                + "\n");
 
        writer.print("  Number of Processors: " + size() + "\n\n");

        for (Processor p: this) {
            writer.print(p.report());
            writer.println();
        }
        writer.println();
    }

    public String singleLineLegend() {
        return "";
    }

    public void singleLineReportTo(PrintWriter pw) {
        pw.print(size());
        pw.print(" processors: ");
        for(Processor p : this) {
            pw.print(p.getName());
            pw.print(" ");
        }
    }

    public void process(CrawlURI curi, ChainStatusReceiver thread) throws InterruptedException {
        assert KeyedProperties.overridesActiveFrom(curi);
        String skipToProc = null; 
        
        ploop: for(Processor curProc : this ) {
            if(skipToProc!=null && curProc.getName() != skipToProc) {
                continue;
            } else {
                skipToProc = null; 
            }
            if(thread!=null) {
                thread.atProcessor(curProc);
            }
            ArchiveUtils.continueCheck();
            ProcessResult pr = curProc.process(curi);
            switch (pr.getProcessStatus()) {
                case PROCEED:
                    continue;
                case FINISH:
                    break ploop;
                case JUMP:
                    skipToProc = pr.getJumpTarget();
                    continue;
            }
        }
    }
    
    public interface ChainStatusReceiver {
        public void atProcessor(Processor proc);
    }
}