/* LexicalCrawlMapper
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Frontier;
import org.archive.state.FileModule;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.util.iterator.LineReadingIterator;
import org.archive.util.iterator.RegexpLineIterator;


/**
 * A simple crawl splitter/mapper, dividing up CrawlURIs/CrawlURIs
 * between crawlers by diverting some range of URIs to local log files
 * (which can then be imported to other crawlers). 
 * 
 * May operate on a CrawlURI (typically early in the processing chain) or
 * its CrawlURI outlinks (late in the processing chain, after 
 * LinksScoper), or both (if inserted and configured in both places). 
 * 
 * <p>Uses lexical comparisons of classKeys to map URIs to crawlers. The
 * 'map' is specified via either a local or HTTP-fetchable file. Each
 * line of this file should contain two space-separated tokens, the
 * first a key and the second a crawler node name (which should be
 * legal as part of a filename). All URIs will be mapped to the crawler
 * node name associated with the nearest mapping key equal or subsequent 
 * to the URI's own classKey. If there are no mapping keys equal or 
 * after the classKey, the mapping 'wraps around' to the first mapping key.
 * 
 * <p>One crawler name is distinguished as the 'local name'; URIs mapped to
 * this name are not diverted, but continue to be processed normally.
 * 
 * <p>For example, assume a SurtAuthorityQueueAssignmentPolicy and
 * a simple mapping file:
 * 
 * <pre>
 *  d crawlerA
 *  ~ crawlerB
 * </pre>
 * <p>All URIs with "com," classKeys will find the 'd' key as the nearest
 * subsequent mapping key, and thus be mapped to 'crawlerA'. If that's
 * the 'local name', the URIs will be processed normally; otherwise, the
 * URI will be written to a diversion log aimed for 'crawlerA'. 
 * 
 * <p>If using the JMX importUris operation importing URLs dropped by
 * a {@link LexicalCrawlMapper} instance, use <code>recoveryLog</code> style.
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class LexicalCrawlMapper extends CrawlMapper {
    
    
    private static final long serialVersionUID = 2L;


    /**
     * Path (or HTTP URL) to map specification file. Each line should include 2
     * whitespace-separated tokens: the first a key indicating the end of a
     * range, the second the crawler node to which URIs in the key range should
     * be mapped.
     */
    @Immutable
    final public static Key<String> MAP_SOURCE = Key.make("");


    @Immutable
    final public static Key<Frontier> FRONTIER = Key.make(Frontier.class, null); 
    
    @Immutable
    final public static Key<FileModule> DIR = 
        Key.make(FileModule.class, null);
    
    
    /**
     * Mapping of classKey ranges (as represented by their start) to 
     * crawlers (by abstract name/filename)
     */
    TreeMap<String, String> map = new TreeMap<String, String>();

    private Frontier frontier;
    private FileModule dir;
    
    /**
     * Constructor.
     */
    public LexicalCrawlMapper() {
        super();
    }


    /**
     * Look up the crawler node name to which the given CrawlURI 
     * should be mapped. 
     * 
     * @param cauri CrawlURI to consider
     * @return String node name which should handle URI
     */
    protected String map(CrawlURI cauri) {
        // get classKey, via frontier to generate if necessary
        String classKey = frontier.getClassKey(cauri);
        SortedMap tail = map.tailMap(classKey);
        if(tail.isEmpty()) {
            // wraparound
            tail = map;
        }
        // target node is value of nearest subsequent key
        return (String) tail.get(tail.firstKey());
    }

    public void initialTasks(StateProvider context) {
        super.initialTasks(context);
        this.frontier = context.get(this, FRONTIER);
        this.dir = context.get(this, DIR);

        try {
            loadMap(context);
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
    protected void loadMap(StateProvider context) throws IOException {
        map.clear();
        String mapSource = context.get(this, MAP_SOURCE);
        Reader reader = null;
        if(!mapSource.startsWith("http://")) {
            // file-based source
            File source = new File(mapSource);
            if (!source.isAbsolute()) {
                source = new File(dir.getFile(), mapSource);
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
    
 // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(LexicalCrawlMapper.class);
    }
}