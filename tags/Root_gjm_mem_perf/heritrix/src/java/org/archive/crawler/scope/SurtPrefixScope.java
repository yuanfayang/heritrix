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
 * SurtPrefixScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.util.SurtPrefixSet;

/**
 * A specialized CrawlScope suitable for the most common crawl needs.
 * 
 * Roughly, as with other existing CrawlScope variants, SurtPrefixScope's logic
 * is that a URI is included if:
 * <pre>
 *  ( isSeed(uri) || focusFilter.accepts(uri) ) ||
 *     transitiveFilter.accepts(uri) ) && ! excludeFilter.accepts(uri)
 * </pre>
 * Specifically, SurtPrefixScope uses a SurtFilter to test for focus-inclusion.
 * 
 * @author gojomo
 *  
 */
public class SurtPrefixScope extends RefinedScope {
    public static final String ATTR_SURTS_SOURCE_FILE = "surts-source-file";
    public static final String ATTR_SEEDS_AS_SURT_PREFIXES = "seeds-as-surt-prefixes";
    public static final String ATTR_SURTS_DUMP_FILE = "surts-dump=file";
    
    private static final Boolean DEFAULT_SEEDS_AS_SURT_PREFIXES = new Boolean(true);

    SurtPrefixSet surtPrefixes = null;

    public SurtPrefixScope(String name) {
        super(name);
        setDescription(
                  "A scope for crawls limited to regions of the web defined by"
                + "a set of SURT prefixes. (The SURT form of a URI has had its"
                + "hostname reordered to ease sorting and grouping by domain"
                + "hierarchies.)");
        addElementToDefinition(
                new SimpleType(ATTR_SURTS_SOURCE_FILE, "Source file from which to " +
                        "read SURT prefixes.", ""));
        addElementToDefinition(
                new SimpleType(ATTR_SEEDS_AS_SURT_PREFIXES, "Should seeds also " +
                        "be intepreted as SURT prefixes.", 
                        DEFAULT_SEEDS_AS_SURT_PREFIXES));
        
        Type t = addElementToDefinition(
                new SimpleType(ATTR_SURTS_DUMP_FILE, "Dump file to save SURT " +
                        "prefixes actually used.", ""));
        t.setExpertSetting(true);

    }

    /**
     * Check if a URI is part of this scope.
     * 
     * @param o
     *            An instance of UURI or of CandidateURI.
     * @return True if focus filter accepts passed object.
     */
    protected synchronized boolean focusAccepts(Object o) {
        if (surtPrefixes == null) {
            readPrefixes();
        }
        
        UURI u = getUURI(o);
        if (u == null) {
            return false;
        }
        String candidateSurt = u.getSurtForm();
        // also want to treat https as http
        if(candidateSurt.startsWith("https:")) {
            candidateSurt = "http:"+candidateSurt.substring(6);
        }
        return surtPrefixes.containsPrefixOf(candidateSurt);
    }
    
    private void readPrefixes() {
        surtPrefixes = new SurtPrefixSet(); 
        FileReader fr = null;
        
        // read SURTs from file, if appropriate 
        String sourcePath = (String) getUncheckedAttribute(null,
                ATTR_SURTS_SOURCE_FILE);
        if(sourcePath.length()>0) {
            File source = new File(sourcePath);
            if (!source.isAbsolute()) {
                source = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), sourcePath);
            }
            try {
                fr = new FileReader(source);
                try {
                    surtPrefixes.importFrom(fr);
                } finally {
                    fr.close();
                }
        
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } 
        }
        
        // interpret seeds as surts, if appropriate
        if (((Boolean) getUncheckedAttribute(null, ATTR_SEEDS_AS_SURT_PREFIXES))
                .booleanValue()) {
            try {
                fr = new FileReader(getSeedfile());
                try {
                    surtPrefixes.importFromUris(fr);
                } finally {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }      

        // dump surts to file, if appropriate
        String dumpPath = (String) getUncheckedAttribute(null,
                ATTR_SURTS_DUMP_FILE);
        if(dumpPath.length()>0) {
            File dump = new File(dumpPath);
            if (!dump.isAbsolute()) {
                dump = new File(getSettingsHandler().getOrder()
                        .getController().getDisk(), dumpPath);
            }
            try {
                FileWriter fw = new FileWriter(dump);
                try {
                    surtPrefixes.exportTo(fw);
                } finally {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Re-read prefixes after an update. 
     * 
     * @see org.archive.crawler.framework.CrawlScope#kickUpdate()
     */
    public synchronized void kickUpdate() {
        super.kickUpdate();
        // TODO: make conditional on file having actually changed,
        // perhaps by remembering mod-time
        readPrefixes();
    }
}
