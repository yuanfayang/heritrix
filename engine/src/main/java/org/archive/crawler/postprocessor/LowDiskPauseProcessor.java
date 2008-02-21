/*
 * LowDiskPauseProcessor
 *
 * $Id$
 *
 * Created on Jun 5, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.postprocessor;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.modules.PostProcessor;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.util.IoUtils;

/**
 * Processor module which uses 'df -k', where available and with
 * the expected output format (on Linux), to monitor available 
 * disk space and pause the crawl if free space on  monitored 
 * filesystems falls below certain thresholds.
 */
public class LowDiskPauseProcessor extends Processor implements PostProcessor {

    private static final long serialVersionUID = 3L;

    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(LowDiskPauseProcessor.class.getName());


    /**
     * List of filessystem mounts whose 'available' space should be monitored
     * via 'df' (if available).
     */
    final public static Key<List<String>> MONITOR_MOUNTS = 
        Key.makeList(String.class);
    

    /**
     * When available space on any monitored mounts falls below this threshold,
     * the crawl will be paused.
     */
    final public static Key<Integer> PAUSE_THRESHOLD_KB = 
        Key.make(500 * 1024); // 500MB

    
    /**
     * Available space via 'df' is rechecked after every increment of this much
     * content (uncompressed) is observed.
     */
    @Global
    final public static Key<Integer> RECHECK_THRESHOLD =
        Key.make(200 * 1024);

    
    protected int contentSinceCheck = 0;
    
    public static final Pattern VALID_DF_OUTPUT = 
        Pattern.compile("(?s)^Filesystem\\s+1K-blocks\\s+Used\\s+Available\\s+Use%\\s+Mounted on\\n.*");
    public static final Pattern AVAILABLE_EXTRACTOR = 
        Pattern.compile("(?m)\\s(\\d+)\\s+\\d+%\\s+(\\S+)$");
    
    /**
     * @param name Name of this writer.
     */
    public LowDiskPauseProcessor() {
    } 
    
    
    @Override
    protected boolean shouldProcess(ProcessorURI curi) {
        return true;
    }

    @Override
    protected void innerProcess(ProcessorURI uri) {
        throw new AssertionError();
    }
    
    /**
     * Notes a CrawlURI's content size in its running tally. If the 
     * recheck increment of content has passed through since the last
     * available-space check, checks available space and pauses the 
     * crawl if any monitored mounts are below the configured threshold. 
     * 
     * @param curi CrawlURI to process.
     */
    @Override
    protected ProcessResult innerProcessResult(ProcessorURI curi) {
        synchronized (this) {
            contentSinceCheck += curi.getContentSize();
            if (contentSinceCheck/1024 > curi.get(this, RECHECK_THRESHOLD)) {
                ProcessResult r = checkAvailableSpace(curi);
                contentSinceCheck = 0;
                return r;
            } else {
                return ProcessResult.PROCEED;
            }
        }
    }


    /**
     * Probe via 'df' to see if monitored mounts have fallen
     * below the pause available threshold. If so, request a 
     * crawl pause. 
     * @param curi Current context.
     */
    private ProcessResult checkAvailableSpace(ProcessorURI curi) {
        try {
            String df = IoUtils.readFullyAsString(Runtime.getRuntime().exec(
                    "df -k").getInputStream());
            Matcher matcher = VALID_DF_OUTPUT.matcher(df);
            if(!matcher.matches()) {
                logger.severe("'df -k' output unacceptable for low-disk checking");
                return ProcessResult.PROCEED;
            }
            List<String> monitoredMounts = curi.get(this, MONITOR_MOUNTS);
            matcher = AVAILABLE_EXTRACTOR.matcher(df);
            while (matcher.find()) {
                String mount = matcher.group(2);
                if (monitoredMounts.contains(mount)) {
                    long availKilobytes = Long.parseLong(matcher.group(1));
                    int thresholdKilobytes = curi.get(this, PAUSE_THRESHOLD_KB);
                    if (availKilobytes < thresholdKilobytes ) {
                        logger.log(Level.SEVERE, "Low Disk Pause",
                                availKilobytes + "K available on " + mount
                                        + " (below threshold "
                                        + thresholdKilobytes + "K)");
                        return ProcessResult.STUCK;
                    }
                }
            }
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
        }
        return ProcessResult.PROCEED;
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(LowDiskPauseProcessor.class);
    }
}
