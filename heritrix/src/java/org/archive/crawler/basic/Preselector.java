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
 * SimplePreselector.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Processor;
import org.archive.util.TextUtils;

/**
 * If set to recheck the crawl's scope, gives a yes/no on whether
 * a CrawlURI should be processed at all. If not, its status
 * will be marked OUT_OF_SCOPE and the URI will skip directly
 * to the first "postprocessor".
 *
 *
 * @author gojomo
 *
 */
public class Preselector extends Processor implements FetchStatusCodes {

    public static String ATTR_RECHECK_SCOPE = "recheck-scope";
    public static String ATTR_BLOCK_ALL = "block-all";
    public static String ATTR_BLOCK_BY_REGEXP = "block-by-regexp";
    
    /**
     * @param name
     */
    public Preselector(String name) {
        super(name, "Preselector");
        addElementToDefinition(new SimpleType(ATTR_RECHECK_SCOPE,
                "Recheck if uri is in scope. This is meaningfull if the scope" +
                " is altered during a crawl. URIs are checked against the" +
                " scope when they are added to queues. Setting this value to" +
                " true forces the URI to be checked against the scope when it" +
                " is comming out of the queue, possibly after the scope is" +
                " altered.", new Boolean(false)));
        
        addElementToDefinition(new SimpleType(ATTR_BLOCK_ALL,
                "Block all uris from beeing processed. This is most likely to" +
                " be used in overrides to easily reject certain hosts from" +
                " beeing processed.", new Boolean(false)));

        addElementToDefinition(new SimpleType(ATTR_BLOCK_BY_REGEXP,
                "Block all uris matching the regular expression from beeing" +
                " processed.", ""));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {
        // Check if uris should be blocked
        try {
            if (((Boolean) getAttribute(ATTR_BLOCK_ALL, curi)).booleanValue()) {
                curi.setFetchStatus(S_BLOCKED_BY_USER);
                curi.skipToProcessor(getController().getPostprocessor());
            }
        } catch (AttributeNotFoundException e) {
            // Act as attribute was false, that is: do nothing.
        }

        // Check if blocked by regular expression
        try {
            String regexp = (String) getAttribute(ATTR_BLOCK_BY_REGEXP, curi);
            if (regexp != null && !regexp.equals("")) {
                if (TextUtils.matches(regexp, curi.getURIString())) {
                    curi.setFetchStatus(S_BLOCKED_BY_USER);
                    curi.skipToProcessor(getController().getPostprocessor());
                }
            }
        } catch (AttributeNotFoundException e) {
            // Act as regexp was null, that is: do nothing.
        }

        // Possibly recheck scope
        try {
            if (((Boolean) getAttribute(ATTR_RECHECK_SCOPE, curi)).booleanValue()) {
                CrawlScope scope = getController().getScope();
                if(!scope.accepts(curi)) {
                    // scope rejected
                    curi.setFetchStatus(S_OUT_OF_SCOPE);
                    curi.skipToProcessor(getController().getPostprocessor());
                }
            }
        } catch (AttributeNotFoundException e) {
            // Act as attribute was false, that is: do nothing.
        }
    }

}
