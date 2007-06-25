/* CrawlMapper
 * 
 * Created on Sep 30, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
 */
package org.archive.crawler.processor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import org.archive.crawler.datamodel.CrawlURI;
import static org.archive.processors.fetcher.FetchStatusCodes.*;
import org.archive.processors.ProcessResult;
import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRule;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.state.FileModule;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.fingerprint.ArrayLongFPCache;

import st.ata.util.FPGenerator;

/**
 * A simple crawl splitter/mapper, dividing up CrawlURIs/CrawlURIs
 * between crawlers by diverting some range of URIs to local log files
 * (which can then be imported to other crawlers). 
 * 
 * May operate on a CrawlURI (typically early in the processing chain) or
 * its CrawlURI outlinks (late in the processing chain, after 
 * LinksScoper), or both (if inserted and configured in both places). 
 * 
 * <p>Applies a map() method, supplied by a concrete subclass, to
 * classKeys to map URIs to crawlers by name. 
 *
 * <p>One crawler name is distinguished as the 'local name'; URIs mapped to
 * this name are not diverted, but continue to be processed normally.
 *
 * <p>If using the JMX importUris operation importing URLs dropped by
 * a {@link CrawlMapper} instance, use <code>recoveryLog</code> style.
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public abstract class CrawlMapper extends Processor {

    /**
     * PrintWriter which remembers the File to which it writes. 
     */
    private class FilePrintWriter extends PrintWriter {
        File file; 
        public FilePrintWriter(File file) throws FileNotFoundException {
            super(new BufferedOutputStream(new FileOutputStream(file)));
            this.file = file; 
        }
        public File getFile() {
            return file;
        }
    }
    

    /**
     * Whether to apply the mapping to a URI being processed itself, for example
     * early in processing (while its status is still 'unattempted').
     */
    final public static Key<Boolean> CHECK_URI = Key.make(true);
    

    /**
     * Whether to apply the mapping to discovered outlinks, for example after
     * extraction has occurred.
     */
    final public static Key<Boolean> CHECK_OUTLINKS = Key.make(true);


    /** 
     * Decide rules to determine if an outlink is subject to mapping.
     */ 
    final public static Key<DecideRuleSequence> OUTLINK_DECIDE_RULES
    = Key.make(new DecideRuleSequence());


    /**
     * Name of local crawler node; mappings to this name result in normal
     * processing (no diversion).
     */
    final public static Key<String> LOCAL_NAME = Key.make(".");
    

    /**
     * Directory to write diversion logs.
     */
    @Immutable
    final public static Key<FileModule> DIVERSION_DIR = 
        Key.make(FileModule.class, null);


    /**
     * Number of timestamp digits to use as prefix of log names (grouping all
     * diversions from that period in a single log). Default is 10 (hourly log
     * rotation).
     * 
     */
    final public static Key<Integer> ROTATION_DIGITS = Key.make(10); // hourly
    

    /**
     * Mapping of target crawlers to logs (PrintWriters)
     */
    HashMap<String,PrintWriter> diversionLogs
     = new HashMap<String,PrintWriter>();

    /**
     * Truncated timestamp prefix for diversion logs; when
     * current time doesn't match, it's time to close all
     * current logs. 
     */
    String logGeneration = "";
    
    /** name of the enclosing crawler (URIs mapped here stay put) */
    protected String localName;
    
    protected ArrayLongFPCache cache;

    private FileModule diversionDir;
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public CrawlMapper() {
        super();
    }

    
    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        return true;
    }
    
    @Override
    protected void innerProcess(ProcessorURI puri) {
        throw new AssertionError();
    }

    @Override
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        String nowGeneration = 
            ArchiveUtils.get14DigitDate().substring(
                        0,
                        curi.get(this, ROTATION_DIGITS));
        if(!nowGeneration.equals(logGeneration)) {
            updateGeneration(nowGeneration);
        }
        
        if (curi.getFetchStatus() == 0 && curi.get(this, CHECK_URI)) {
            // apply mapping to the CrawlURI itself
            String target = map(curi);
            if(!localName.equals(target)) {
                // CrawlURI is mapped to somewhere other than here
                curi.setFetchStatus(S_BLOCKED_BY_CUSTOM_PROCESSOR);
                curi.getAnnotations().add("to:"+target);
                divertLog(curi,target);
                return ProcessResult.FINISH;
            } else {
                // localName means keep locally; do nothing
            }
        }
        
        if (curi.getOutLinks().size() > 0 && curi.get(this, CHECK_OUTLINKS)) {
            // consider outlinks for mapping
            Iterator<CrawlURI> iter = curi.getOutCandidates().iterator(); 
            while(iter.hasNext()) {
                CrawlURI cauri = iter.next();
                if (decideToMapOutlink(cauri)) {
                    // apply mapping to the CrawlURI
                    String target = map(cauri);
                    if(!localName.equals(target)) {
                        // CrawlURI is mapped to somewhere other than here
                        iter.remove();
                        divertLog(cauri,target);
                    } else {
                        // localName means keep locally; do nothing
                    }
                }
            }
        }
        return ProcessResult.PROCEED;
    }
    
    protected boolean decideToMapOutlink(CrawlURI cauri) {
        DecideRule rule = cauri.get(this, OUTLINK_DECIDE_RULES);
        boolean rejected = rule.decisionFor(cauri)
                .equals(DecideResult.REJECT);
        return !rejected;
    }
    
    
    /**
     * Close and mark as finished all existing diversion logs, and
     * arrange for new logs to use the new generation prefix.
     * 
     * @param nowGeneration new generation (timestamp prefix) to use
     */
    protected synchronized void updateGeneration(String nowGeneration) {
        // all existing logs are of a previous generation
        Iterator iter = diversionLogs.values().iterator();
        while(iter.hasNext()) {
            FilePrintWriter writer = (FilePrintWriter) iter.next();
            writer.close();
            writer.getFile().renameTo(
                    new File(writer.getFile().getAbsolutePath()
                            .replaceFirst("\\.open$", ".divert")));
        }
        diversionLogs.clear();
        logGeneration = nowGeneration;
    }

    /**
     * Look up the crawler node name to which the given CrawlURI 
     * should be mapped. 
     * 
     * @param cauri CrawlURI to consider
     * @return String node name which should handle URI
     */
    protected abstract String map(CrawlURI cauri);

    
    /**
     * Note the given CrawlURI in the appropriate diversion log. 
     * 
     * @param cauri CrawlURI to append to a diversion log
     * @param target String node name (log name) to receive URI
     */
    protected synchronized void divertLog(CrawlURI cauri, String target) {
        if(recentlySeen(cauri)) {
            return;
        }
        PrintWriter diversionLog = getDiversionLog(target);
        cauri.singleLineReportTo(diversionLog);
        diversionLog.println();
    }
    
    /**
     * Consult the cache to determine if the given URI
     * has been recently seen -- entering it if not. 
     * 
     * @param cauri CrawlURI to test
     * @return true if URI was already in the cache; false otherwise 
     */
    private boolean recentlySeen(CrawlURI cauri) {
        long fp = FPGenerator.std64.fp(cauri.toString());
        return ! cache.add(fp);
    }

    /**
     * Get the diversion log for a given target crawler node node. 
     * 
     * @param target crawler node name of requested log
     * @return PrintWriter open on an appropriately-named 
     * log file
     */
    protected PrintWriter getDiversionLog(String target) {
        FilePrintWriter writer = (FilePrintWriter) diversionLogs.get(target);
        if(writer == null) {
            File divertDir = this.diversionDir.getFile();
            divertDir.mkdirs();
            File divertLog = 
                new File(divertDir,
                         logGeneration+"-"+localName+"-to-"+target+".open");
            try {
                writer = new FilePrintWriter(divertLog);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            diversionLogs.put(target,writer);
        } 
        return writer;
    }

    public void initialTasks(StateProvider context) {
        super.initialTasks(context);
        localName = context.get(this, LOCAL_NAME);
        cache = new ArrayLongFPCache();
        diversionDir = context.get(this, DIVERSION_DIR);
    }


}
