/*
 * ExtractorHTML2
 *
 * $Id$
 *
 * Created on Jan 6, 2004
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

package org.archive.crawler.extractor;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * Extended version of ExtractorHTML with more aggressive javascript link 
 * extraction where javascript code is parsed first with general HTML tags
 * regexp, than by javascript speculative link regexp.
 *
 * @author Igor Ranitovic
 *
 * TODO: more testing
 */
public class ExtractorHTML2 extends ExtractorHTML {
    static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorHTML");
    /**
     * @param name
     */
    public ExtractorHTML2(String name) {
        super(name);
    }

    /**
     * @param curi
     * @param sequence
     */
    protected void processScript(CrawlURI curi, CharSequence sequence, int endOfOpenTag) {
        // for now, do nothing
        // TODO: best effort extraction of strings

        // first, get attributes of script-open tag
        // as per any other tag
        processGeneralTag(curi,sequence.subSequence(0,6),sequence.subSequence(0,endOfOpenTag));
        // then, proccess entire javascript code as html code
        // this may cause a lot of false positves
        processGeneralTag(curi,sequence.subSequence(0,6),sequence.subSequence(endOfOpenTag,sequence.length()));
        // finally, apply best-effort string-analysis heuristics
        // against any code present (false positives are OK)
        processScriptCode(curi,sequence.subSequence(endOfOpenTag,sequence.length()));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTML2\n");
        ret.append("  Function:          Link extraction on HTML documents (including embedded CSS)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }


}
