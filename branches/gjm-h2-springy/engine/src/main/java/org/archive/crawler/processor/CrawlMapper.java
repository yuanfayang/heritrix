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

import static org.archive.modules.fetcher.FetchStatusCodes.S_BLOCKED_BY_CUSTOM_PROCESSOR;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.modules.deciderules.AcceptDecideRule;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRule;
import org.archive.settings.JobHome;
import org.archive.util.ArchiveUtils;
import org.archive.util.fingerprint.ArrayLongFPCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;

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
public abstract class CrawlMapper extends Processor implements Lifecycle {

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
    boolean checkUri = true;
    public boolean getCheckUri() {
        return this.checkUri;
    }
    public void setCheckUri(boolean check) {
        this.checkUri = check;
    }

    /**
     * Whether to apply the mapping to discovered outlinks, for example after
     * extraction has occurred.
     */
    boolean checkOutlinks = true;
    public boolean getCheckOutlinks() {
        return this.checkOutlinks;
    }
    public void setCheckOutlinks(boolean check) {
        this.checkOutlinks = check;
    }

    /** 
     * Decide rules to determine if an outlink is subject to mapping.
     */ 
    DecideRule outlinkRule = new AcceptDecideRule(); 
    public DecideRule getOutlinkRule() {
        return this.outlinkRule;
    }
    public void setOutlinkRule(DecideRule rule) {
        this.outlinkRule = rule; 
    }

    /**
     * Name of local crawler node; mappings to this name result in normal
     * processing (no diversion).
     */
    String localName = ".";
    public String getLocalName() {
        return this.localName;
    }
    public void setLocalName(String name) {
        this.localName = name; 
    }
    
    /**
     * Directory to write diversion logs.
     */
    String diversionDir = "diversions";
    public String getDiversionDir() {
        return this.diversionDir;
    }
    public void setDiversionDir(String path) {
        this.diversionDir = path; 
    }
    public File resolveDiversionDir() {
        return JobHome.resolveToFile(jobHome,diversionDir,null);
    }
    
    protected JobHome jobHome;
    public JobHome getJobHome() {
        return jobHome;
    }
    @Autowired
    public void setJobHome(JobHome home) {
        this.jobHome = home;
    }
    
    /**
     * Number of timestamp digits to use as prefix of log names (grouping all
     * diversions from that period in a single log). Default is 10 (hourly log
     * rotation).
     * 
     */
    int rotationDigits = 10; 
    public int getRotationDigits() {
        return this.rotationDigits;
    }
    public void setRotationDigits(int digits) {
        this.rotationDigits = digits; 
    }

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
        
    protected ArrayLongFPCache cache;
   
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
                        getRotationDigits());
        if(!nowGeneration.equals(logGeneration)) {
            updateGeneration(nowGeneration);
        }
        
        if (curi.getFetchStatus() <= 0  // unfetched/unsuccessful
                && getCheckUri()) {
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
        
        if (getCheckOutlinks()) {
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
        DecideRule rule = getOutlinkRule();
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
        Iterator<PrintWriter> iter = diversionLogs.values().iterator();
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
            File divertDir = resolveDiversionDir();
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

    public void start() {
        cache = new ArrayLongFPCache();
    }
    
    public boolean isRunning() {
        return cache != null;
    }
    
    public void stop() {
        cache = null; 
    }
}
