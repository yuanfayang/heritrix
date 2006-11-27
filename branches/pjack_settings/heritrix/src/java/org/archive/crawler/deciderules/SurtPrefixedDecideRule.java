/* SurtPrefixedDecideRule
*
* $Id$
*
* Created on Apr 5, 2005
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
package org.archive.crawler.deciderules;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.scope.SeedListener;
import org.archive.net.UURI;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRule;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.SurtPrefixSet;



/**
 * Rule applies configured decision to any URIs that, when 
 * expressed in SURT form, begin with one of the prefixes
 * in the configured set. 
 * 
 * The set can be filled with SURT prefixes implied or
 * listed in the seeds file, or another external file. 
 *
 * The "also-check-via" option to implement "one hop off" 
 * scoping derives from a contribution by Shifra Raffel
 * of the California Digital Library. 
 * 
 * @author gojomo
 */
public class SurtPrefixedDecideRule extends DecideRule 
        implements SeedListener {

    private static final long serialVersionUID = 3L;

    //private static final Logger logger =
    //    Logger.getLogger(SurtPrefixedDecideRule.class.getName());


    /**
     * Source file from which to infer SURT prefixes. Any URLs in file will be
     * converted to the implied SURT prefix, and literal SURT prefixes may be
     * listed on lines beginning with a '+' character.
     */
    final public static Key<String> SURTS_SOURCE_FILE = Key.make("");
    

    /**
     * Should seeds also be interpreted as SURT prefixes.
     */
    final public static Key<Boolean> SEEDS_AS_SURT_PREFIXES = Key.make(true);


    /**
     * Dump file to save SURT prefixes actually used: Useful debugging SURTs.
     */
    final public static Key<String> SURTS_DUMP_FILE = Key.makeExpert("");


    /**
     * Whether to rebuild the internal structures from source files (including
     * seeds if appropriate) every time any configuration change occurs. If
     * true, rule is rebuilt from sources even when (for example) unrelated new
     * domain overrides are set. Rereading large source files can take a long
     * time.
     */
    final public static Key<Boolean> REBUILD_ON_RECONFIG = 
        Key.makeExpertFinal(true);


    /**
     * Whether to also make the configured decision if a URI's 'via' URI (the
     * URI from which it was discovered) in SURT form begins with any of the
     * established prefixes. For example, can be used to ACCEPT URIs that are
     * 'one hop off' URIs fitting the SURT prefixes. Default is false.
     */
    final public static Key<Boolean> ALSO_CHECK_VIA = 
        Key.makeExpertFinal(false);

    
    protected SurtPrefixSet surtPrefixes = null;

    
    final private CrawlController controller;


    /**
     * Usual constructor. 
     */
    public SurtPrefixedDecideRule(CrawlController controller) {
        this.controller = controller;
    }


    /**
     * Evaluate whether given object's URI is covered by the SURT prefix set
     * 
     * @param object Item to evaluate.
     * @return true if item, as SURT form URI, is prefixed by an item in the set
     */
    protected DecideResult innerDecide(ProcessorURI uri) {
        if (uri.get(this, ALSO_CHECK_VIA)) {
            if (innerDecide(uri, uri.getVia()) == DecideResult.ACCEPT) {
                return DecideResult.ACCEPT;
            }
        }

        return innerDecide(uri, uri.getUURI());
    }
    
    
    private DecideResult innerDecide(StateProvider context, UURI uuri) {
        String candidateSurt;
        candidateSurt = SurtPrefixSet.getCandidateSurt(uuri);
        if (candidateSurt == null) {
            return DecideResult.PASS;
        }
        if (getPrefixes(context).containsPrefixOf(candidateSurt)) {
            return DecideResult.ACCEPT;
        } else {
            return DecideResult.PASS;
        }
    }


    /**
     * Synchronized get of prefix set to use
     * 
     * @return SurtPrefixSet to use for check
     */
    private synchronized SurtPrefixSet getPrefixes(StateProvider uri) {
        if (surtPrefixes == null) {
            readPrefixes(uri);
        }
        return surtPrefixes;
    }

    protected void readPrefixes(StateProvider uri) {
        buildSurtPrefixSet(uri);
        dumpSurtPrefixSet(uri);
    }
    
    /**
     * Dump the current prefixes in use to configured dump file (if any)
     */
    protected void dumpSurtPrefixSet(StateProvider uri) {
        // dump surts to file, if appropriate
        String dumpPath = uri.get(this, SURTS_DUMP_FILE);
        if (dumpPath.length() > 0) {
            File dump = new File(dumpPath);
            if (!dump.isAbsolute()) {
                dump = new File(controller.getDisk(), dumpPath);
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
     * Construct the set of prefixes to use, from the seed list (
     * which may include both URIs and '+'-prefixed directives).
     */
    protected void buildSurtPrefixSet(StateProvider uri) {
        SurtPrefixSet newSurtPrefixes = new SurtPrefixSet();
        FileReader fr = null;

        // read SURTs from file, if appropriate
        String sourcePath = uri.get(this, SURTS_SOURCE_FILE);
        if (sourcePath.length() > 0) {
            File source = new File(sourcePath);
            if (!source.isAbsolute()) {
                source = new File(controller.getDisk(), sourcePath);
            }
            try {
                fr = new FileReader(source);
                try {
                    newSurtPrefixes.importFromMixed(fr, true);
                } finally {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        
        // interpret seeds as surts, if appropriate
        boolean deduceFromSeeds = uri.get(this, SEEDS_AS_SURT_PREFIXES);
        if(deduceFromSeeds) {
            try {
                fr = new FileReader(getSeedfile());
                try {
                    newSurtPrefixes.importFromMixed(fr, deduceFromSeeds);
                } finally {
                    fr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        surtPrefixes = newSurtPrefixes;
    }

    /**
     * Re-read prefixes after an update.
     * 
     * @see org.archive.crawler.framework.CrawlScope#kickUpdate()
     */
    public synchronized void kickUpdate(StateProvider provider) {
        super.kickUpdate(provider); // FIXME: Kick update
        if (provider.get(this, REBUILD_ON_RECONFIG)) {
            readPrefixes(provider);
        }
        // TODO: make conditional on file having actually changed,
        // perhaps by remembering mod-time
    }

    /**
     * Dig through everything to get the crawl-global seeds file. 
     * Add self as listener while at it. 
     * 
     * @return Seed list file
     */
    protected File getSeedfile() {
        CrawlScope scope = controller.getScope();
        scope.addSeedListener(this);
        return scope.getSeedfile();
    }

    public synchronized void addedSeed(final CandidateURI curi) {
        SurtPrefixSet newSurtPrefixes = (SurtPrefixSet) surtPrefixes.clone();
        newSurtPrefixes.add(prefixFrom(curi.toString()));
        surtPrefixes = newSurtPrefixes;
    }
    
    protected String prefixFrom(String uri) {
        return SurtPrefixSet.prefixFromPlain(uri);
    }
}
