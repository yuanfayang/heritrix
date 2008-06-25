/* Canonicalizer
 * 
 * Created on Oct 7, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.modules.canonicalize;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * URL canonicalizer.
 * @author stack
 * @version $Date$, $Revision$
 */
public class Canonicalizer {
    private static Logger logger =
        Logger.getLogger(Canonicalizer.class.getName());
    
    /**
     * Constructor.
     * This class can't be constructed.
     * Shutdown.
     */
    private Canonicalizer() {
        super();
    }
    
    /**
     * Convenience method that is passed a settings object instance pulling
     * from it what it needs to canonicalize.
     * @param context UURI to canonicalize.
     * @param rules A crawlorder instance.
     * @return Canonicalized string of uuri else uuri if an error.
     */
    public static String canonicalize(String uri,
            Iterable<CanonicalizationRule> rules) {
        return canonicalize(uri, rules.iterator());
    }

    /**
     * Run the passed uuri through the list of rules.
     * @param context Url to canonicalize.
     * @param rules Iterator of canonicalization rules to apply (Get one
     * of these on the url-canonicalizer-rules element in order files or
     * create a list externally).  Rules must implement the Rule interface.
     * @return Canonicalized URL.
     */
    public static String canonicalize(String uri,
            Iterator<CanonicalizationRule> rules) {
        String before = uri;
        //String beforeRule = null;
        String canonical = before;
        for (; rules.hasNext();) {
            CanonicalizationRule r = rules.next();
            //if (logger.isLoggable(Level.FINER)) {
            //    beforeRule = canonical;
            //}
            if (!r.getEnabled()) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Rule " + r.getClass().getName() 
                            + " is disabled.");
                }
                continue;
            }
            canonical = r.canonicalize(canonical);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Rule " + r.getClass().getName() 
                        + " " + before + " => " +
                        canonical);
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.fine(before + " => " + canonical);
        }
        return canonical;
    }
}
