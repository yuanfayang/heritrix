/*
 * CrawlUriSWFAction
 *
 * $Id$
 *
 * Created on March 15, 2004
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

package org.archive.modules.extractor;

import java.io.IOException;
import java.util.regex.Matcher;

import org.archive.modules.ProcessorURI;
import org.archive.util.TextUtils;

import com.anotherbigidea.flash.writers.SWFActionsImpl;

/**
 * SWF action that handles discovered URIs.
 *
 * @author Igor Ranitovic
 */
public class CrawlUriSWFAction
extends SWFActionsImpl {
    
    ProcessorURI curi;
    
    private long linkCount;
    private UriErrorLoggerModule uriErrors;
    static final String JSSTRING = "javascript:";

    /**
     *
     * @param curi
     */
    public CrawlUriSWFAction(UriErrorLoggerModule uriErrors, ProcessorURI curi) {
        assert (curi != null) : "CrawlURI should not be null";
        this.curi = curi;
        this.linkCount = 0;
        this.uriErrors = uriErrors;
    }

    /**
     * Overwrite handling of discovered URIs.
     *
     * @param url Discovered URL.
     * @param target Discovered target (currently not being used.)
     * @throws IOException
     */
    public void getURL(String url, String target)
    throws IOException {
        if (url.startsWith(JSSTRING)) {
            linkCount += ExtractorJS.considerStrings(uriErrors, curi, url, 
                    false);
        } else {
            int max = uriErrors.getMaxOutlinks(curi);
            Link.addRelativeToVia(curi, max, url, LinkContext.EMBED_MISC,
                    Hop.EMBED);
            linkCount++;
        }
    }

    public void considerStringAsUri(String str) throws IOException {
        Matcher uri = TextUtils.getMatcher(ExtractorJS.STRING_URI_DETECTOR,
                str);

        if (uri.matches()) {
            int max = uriErrors.getMaxOutlinks(curi);
            Link.addRelativeToVia(curi, max, uri.group(), LinkContext.SPECULATIVE_MISC, Hop.SPECULATIVE);
            linkCount++;
        }
        TextUtils.recycleMatcher(uri);
    }


    public void lookupTable(String[] strings) throws IOException {
        for (String str : strings) {
            considerStringAsUri(str);
        }
    }

    public void push(String value) throws IOException {
        considerStringAsUri(value);
    }
    
    /**
     * @return Total number of links extracted from a swf file.
     */
    public long getLinkCount() {
        return linkCount;
    }
}
