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
package org.archive.crawler.url;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.settings.MapType;

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
     * @param uuri UURI to canonicalize.
     * @param order A crawlorder instance.
     * @return Canonicalized string of uuri else uuri if an error.
     */
    public static String canonicalize(UURI uuri, CrawlOrder order) {
        MapType rules = null;
        String uuriStr = uuri.toString();
        try {
            rules = (MapType)order.getAttribute(uuri, CrawlOrder.ATTR_RULES);
            uuriStr = Canonicalizer.canonicalize(uuriStr,
                rules.iterator(uuri));
        } catch (AttributeNotFoundException e) {
            logger.warning("Failed canonicalization of " + uuriStr + ": " + e);
        }
        return uuriStr;
    }

    /**
     * Run the passed url through the list of rules.
     * @param url String to canonicalize.
     * @param rules Iterator of canonicalization rules to apply (Get one
     * of these on the url-canonicalizer-rules element in order files or
     * create a list externally).  Rules must implement the Rule interface.
     * @return Canonicalized URL.
     */
    public static String canonicalize(String url, Iterator rules) {
        String before = url;
        String beforeRule = null;
        for (; rules.hasNext();) {
            CanonicalizationRule r = (CanonicalizationRule)rules.next();
            if (logger.isLoggable(Level.FINE)) {
                beforeRule = url;
            }
            url = r.canonicalize(url);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rule " + r.getName() + " " + before + " => " +
                     url);
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info(before + " => " + url);
        }
        return url;
    }
}
