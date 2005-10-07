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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.util.NoopUriUniqFilter;
import org.archive.util.ArchiveUtils;
import org.archive.util.fingerprint.ArrayLongFPCache;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;

import st.ata.util.FPGenerator;

/**
 * A simple crawl splitter/mapper, dividing up CandidateURIs/CrawlURIs
 * between crawlers by diverting some range of URIs to local log files
 * (which can then be imported to other crawlers). 
 * 
 * May operate on a CrawlURI (typically early in the processing chain) or
 * its CandidateURI outlinks (late in the processing chain, after 
 * LinksScoper), or both (if inserted and configured in both places). 
 * 
 * Uses lexical comparisons of classKeys to map URIs to crawlers. The
 * 'map' is specified via either a local or HTTP-fetchable file. Each
 * line of this file should contain two space-separated tokens, the
 * first a key and the second a crawler node name (which should be
 * legal as part of a filename). All URIs will be mapped to the crawler
 * node name associated with the nearest mapping key equal or subsequent 
 * to the URI's own classKey. If there are no mapping keys equal or 
 * after the classKey, the mapping 'wraps around' to the first mapping key.
 * 
 * One crawler name is distinguished as the 'local name'; URIs mapped to
 * this name are not diverted, but continue to be processed normally.
 * 
 * For example, assume a SurtAuthorityQueueAssignmentPolicy and
 * a simple mapping file:
 * 
 *  d crawlerA
 *  ~ crawlerB
 * 
 * All URIs with "com," classKeys will find the 'd' key as the nearest
 * subsequent mapping key, and thus be mapped to 'crawlerA'. If that's
 * the 'local name', the URIs will be processed normally; otherwise, the
 * URI will be written to a diversion log aimed for 'crawlerA'. 
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class CrawlMapper extends Processor implements FetchStatusCodes {
    private static final Logger LOGGER =
        Logger.getLogger(CrawlMapper.class.getName());
    
    /**
     * PrintWriter which remembers the File to which it writes. 
     */
    public class FilePrintWriter extends PrintWriter {
        File file; 
        public FilePrintWriter(File file) throws FileNotFoundException {
            super(file);
            this.file = file; 
        }
        public File getFile() {
            return file;
        }
    }
    
    /** whether to map CrawlURI itself (if status nonpositive) */
    public static final String ATTR_CHECK_URI = "check-uri";
    public static final Boolean DEFAULT_CHECK_URI = Boolean.TRUE;
    
    /** whether to map CrawlURI's outlinks (if CandidateURIs) */
    public static final String ATTR_CHECK_OUTLINKS = "check-outlinks";
    public static final Boolean DEFAULT_CHECK_OUTLINKS = Boolean.TRUE;

    /** name of local crawler (URIs mapped to here are not diverted) */
    public static final String ATTR_LOCAL_NAME = "local-name";
    public static final String DEFAULT_LOCAL_NAME = ".";

    /** where to load map from */
    public static final String ATTR_MAP_SOURCE = "map-source";
    public static final String DEFAULT_MAP_SOURCE = "";
    
    /** where to log diversions  */
    public static final String ATTR_DIVERSION_DIR = "diversion-dir";
    public static final String DEFAULT_DIVERSION_DIR = "diversions";

    /** where to log diversions  */
    public static final String ATTR_ROTATION_DIGITS = "rotation-digits";
    public static final Integer DEFAULT_ROTATION_DIGITS = new Integer(10); // hourly

    
    /**
     * Mapping of classKey ranges (as represented by their start) to 
     * crawlers (by abstract name/filename)
     */
    TreeMap map = new TreeMap(); // String -> String
    
    /**
     * Mapping of target crawlers to logs (PrintWriters)
     */
    HashMap diversionLogs = new HashMap();

    /**
     * Truncated timestamp prefix for diversion logs; when
     * current time doesn't match, it's time to close all
     * current logs. 
     */
    String logGeneration = "";
    
    /** name of the enclosing crawler (URIs mapped here stay put) */
    protected String localName;
    
    protected ArrayLongFPCache cache;
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public CrawlMapper(String name) {
        super(name, "CrawlMapper.");
        addElementToDefinition(new SimpleType(ATTR_MAP_SOURCE,
            "Path (or HTTP URL) to map specification file. Each line " +
            "should include 2 whitespace-separated tokens: the first a " +
            "key indicating the end of a range, the second the crawler " +
            "node to which URIs in the key range should be mapped.",
            DEFAULT_MAP_SOURCE));
        addElementToDefinition(new SimpleType(ATTR_LOCAL_NAME,
            "Name of local crawler node; mappings to this name " +
            "result in normal processing (no diversion).",
            DEFAULT_LOCAL_NAME));
        addElementToDefinition(new SimpleType(ATTR_DIVERSION_DIR,
            "Directory to write diversion logs.",
            DEFAULT_DIVERSION_DIR));
        addElementToDefinition(new SimpleType(ATTR_CHECK_URI,
            "Whether to apply the mapping to a URI being processed " +
            "itself, for example early in processing (while its " +
            "status is still 'unattempted').",
            DEFAULT_CHECK_URI));
        addElementToDefinition(new SimpleType(ATTR_CHECK_OUTLINKS,
            "Whether to apply the mapping to discovered outlinks, " +
            "for example after extraction has occurred. ",
            DEFAULT_CHECK_OUTLINKS));
        addElementToDefinition(new SimpleType(ATTR_ROTATION_DIGITS,
                "Number of timestamp digits to use as prefix of log " +
                "names (grouping all diversions from that period in " +
                "a single log). Default is 10 (hourly log rotation).",
                DEFAULT_ROTATION_DIGITS));
    }
    
    protected void innerProcess(CrawlURI curi) {
        String nowGeneration = 
            ArchiveUtils.get14DigitDate().substring(
                        0,
                        ((Integer) getUncheckedAttribute(null,
                                ATTR_ROTATION_DIGITS)).intValue());
        if(!nowGeneration.equals(logGeneration)) {
            updateGeneration(nowGeneration);
        }
        
        if (curi.getFetchStatus() == 0
                && ((Boolean) getUncheckedAttribute(null, ATTR_CHECK_URI))
                        .booleanValue()) {
            // apply mapping to the CrawlURI itself
            String target = map(curi);
            if(!localName.equals(target)) {
                // CrawlURI is mapped to somewhere other than here
                curi.setFetchStatus(S_BLOCKED_BY_CUSTOM_PROCESSOR);
                curi.addAnnotation("to:"+target);
                curi.skipToProcessorChain(getController().
                        getPostprocessorChain());
                divertLog(curi,target);
            } else {
                // localName means keep locally; do nothing
            }
        }
        
        if (curi.getOutLinks().size() > 0 && 
                ((Boolean) getUncheckedAttribute(null, ATTR_CHECK_OUTLINKS))
                        .booleanValue()) {
            // consider outlinks for mapping
            Iterator iter = curi.getOutLinks().iterator(); 
            while(iter.hasNext()) {
                Object next = iter.next();
                if(! (next instanceof CandidateURI)) {
                    continue;
                }
                CandidateURI cauri = (CandidateURI)next; 
                try {
                    String host = cauri.getUURI().getHost();
                } catch (URIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // apply mapping to the CandidateURI
                String target = map(cauri);
                if(!localName.equals(target)) {
                    // CandidateURI is mapped to somewhere other than here
                    iter.remove();
                    divertLog(cauri,target);
                } else {
                    // localName means keep locally; do nothing
                }
            }
        }
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
     * Look up the crawler node name to which the given CandidateURI 
     * should be mapped. 
     * 
     * @param cauri CandidateURI to consider
     * @return String node name which should handle URI
     */
    private String map(CandidateURI cauri) {
        // get classKey, via frontier to generate if necessary
        String classKey = getController().getFrontier().getClassKey(cauri);
        SortedMap tail = map.tailMap(classKey);
        if(tail.isEmpty()) {
            // wraparound
            tail = map;
        }
        // target node is value of nearest subsequent key
        return (String) tail.get(tail.firstKey());
    }

    
    /**
     * Note the given CandidateURI in the appropriate diversion log. 
     * 
     * @param cauri CandidateURI to append to a diversion log
     * @param target String node name (log name) to receive URI
     */
    protected synchronized void divertLog(CandidateURI cauri, String target) {
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
     * @param cauri CandidateURI to test
     * @return true if URI was already in the cache; false otherwise 
     */
    private boolean recentlySeen(CandidateURI cauri) {
        long fp = FPGenerator.std64.fp(cauri.getURIString());
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
            String divertDirPath = (String) getUncheckedAttribute(null,ATTR_DIVERSION_DIR);
            File divertDir = new File(divertDirPath);
            if (!divertDir.isAbsolute()) {
                divertDir = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), divertDirPath);
            }
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

    protected void initialTasks() {
        super.initialTasks();
        localName = (String) getUncheckedAttribute(null, ATTR_LOCAL_NAME);
        cache = new ArrayLongFPCache();
        try {
            loadMap();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve and parse the mapping specification from a local path or
     * HTTP URL. 
     * 
     * @throws IOException
     */
    protected void loadMap() throws IOException {
        map.clear();
        String mapSource = (String) getUncheckedAttribute(null,ATTR_MAP_SOURCE);
        Reader reader = null;
        if(!mapSource.startsWith("http://")) {
            // file-based source
            File source = new File(mapSource);
            if (!source.isAbsolute()) {
                source = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), mapSource);
            }
            reader = new FileReader(source);
        } else {
            URLConnection conn = (new URL(mapSource)).openConnection();
            reader = new InputStreamReader(conn.getInputStream());
        }
        reader = new BufferedReader(reader);
        Iterator iter = 
            new RegexpLineIterator(
                    new LineReadingIterator((BufferedReader) reader),
                    RegexpLineIterator.COMMENT_LINE,
                    RegexpLineIterator.TRIMMED_ENTRY_TRAILING_COMMENT,
                    RegexpLineIterator.ENTRY);
        while (iter.hasNext()) {
            String[] entry = ((String) iter.next()).split("\\s+");
            map.put(entry[0],entry[1]);
        }
        reader.close();
    }
}