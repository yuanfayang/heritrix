/* Copyright (C) 2005 Internet Archive.
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
 * ClassicScope.java
 * Created on Apr 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;


import org.archive.crawler.datamodel.CrawlURI;
//import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.framework.CrawlScope;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.processors.extractor.Hop;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * ClassicScope: superclass with shared Scope behavior for
 * most common scopes. 
 *
 * Roughly, its logic is captured in innerAccept(). A URI is 
 * included if:
 * <pre>
 *    forceAccepts(uri)
 *    || ((     (isSeed(uri) 
 *                    || focusAccepts(uri)) 
 *                          || additionalFocusAccepts(uri) 
 *                              || transitiveAccepts(uri))
 *       && !excludeAccepts(uri));</pre>
 *
 * Subclasses should override focusAccepts, additionalFocusAccepts,
 * and transitiveAccepts. 
 *
 * The excludeFilter may be specified by supplying
 * a <code>exclude</code> subelement. If unspecified, a
 * accepts-none filter will be used -- meaning that
 * no URIs will pass the filter and thus be excluded.
 *  
 * @author gojomo
 */
public class ClassicScope extends CrawlScope {

    private static final long serialVersionUID = 3L;

    
    /**
     * Max link hops to include. URIs more than this number of links from a seed
     * will not be ruled in-scope. (Such determination does not preclude later
     * inclusion if a shorter path is later discovered.)
     */
    final public static Key<Integer> MAX_LINK_HOPS = Key.make(25);


    /**
     * Max transitive hops (embeds, referrals, preconditions) to include. URIs
     * reached by more than this number of transitive hops will not be ruled
     * in-scope, even if otherwise on an in-focus site. (Such determination does
     * not preclude later inclusion if a shorter path is later discovered.)
     */
    final public static Key<Integer> MAX_TRANS_HOPS = Key.make(5);

    
    final public static Key<DecideRuleSequence> EXCLUDE_RULES = 
        Key.make(new DecideRuleSequence());

    static {
        KeyManager.addKeys(ClassicScope.class);
    }
    
    public ClassicScope() {
        super();
        // Try to preserve the values of these attributes when we exchange
        // scopes.
        // FIXME: What?
//        setPreservedFields(new String[] { ATTR_SEEDS, ATTR_MAX_LINK_HOPS,
//            ATTR_MAX_TRANS_HOPS, ATTR_EXCLUDE_FILTER,
//            ATTR_FORCE_ACCEPT_FILTER });
    }


    /**
     * Returns whether the given object (typically a CrawlURI) falls within
     * this scope.
     * 
     * @param o
     *            Object to test.
     * @return Whether the given object (typically a CrawlURI) falls within
     *         this scope.
     */
    protected final DecideResult innerDecide(ProcessorURI o) {
        if (forceAccepts(o) || (((isSeed(o) || focusAccepts(o)) ||
            additionalFocusAccepts(o) || transitiveAccepts(o)) &&
            !excludeAccepts(o))) {
            return DecideResult.ACCEPT;
        } else {
            return DecideResult.REJECT;
        }
    }

    /**
     * Check if URI is accepted by the additional focus of this scope.
     * 
     * This method should be overridden in subclasses.
     * 
     * @param o
     *            the URI to check.
     * @return True if additional focus filter accepts passed object.
     */
    protected boolean additionalFocusAccepts(ProcessorURI o) {
        return false;
    }

    /**
     * @param o
     *            the URI to check.
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(ProcessorURI o) {
        return false;
    }

    /**
     * @param o the URI to check.
     * @return True if force-accepts filter accepts passed object.
     */
    protected boolean forceAccepts(ProcessorURI o) {
        return false;
    }
    
    /**
     * Check if URI is accepted by the focus of this scope.
     * 
     * This method should be overridden in subclasses.
     * 
     * @param o
     *            the URI to check.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(ProcessorURI o) {
        // The CrawlScope doesn't accept any URIs
        return false;
    }

    /**
     * Check if URI is excluded by any filters.
     * 
     * @param o
     *            the URI to check.
     * @return True if exclude filter accepts passed object.
     */
    protected boolean excludeAccepts(ProcessorURI o) {
        DecideRuleSequence seq = o.get(this, EXCLUDE_RULES);
        if (seq.decisionFor(o) == DecideResult.ACCEPT) {
            return true;
        }
        return exceedsMaxHops(o);
    }


    /**
     * Check if there are too many hops
     * 
     * @param o
     *            URI to check.
     * @return true if too many hops.
     */
    protected boolean exceedsMaxHops(ProcessorURI o) {
        if (!(o instanceof CrawlURI)) {
            return false;
        }

        int maxLinkHops = o.get(this, MAX_LINK_HOPS);

        CrawlURI cand = (CrawlURI) o;

        String path = cand.getPathFromSeed();
        int linkCount = 0;
        int transCount = 0;
        for (int i = path.length() - 1; i >= 0; i--) {
            if (path.charAt(i) == Hop.NAVLINK.getHopChar()) {
                linkCount++;
            } else if (linkCount == 0) {
                transCount++;
            }
        }
//      return (linkCount > maxLinkHops) || (transCount > maxTransHops);
        // base only on links, don't treat trans count as hard max
        return (linkCount > maxLinkHops);
    }


}
