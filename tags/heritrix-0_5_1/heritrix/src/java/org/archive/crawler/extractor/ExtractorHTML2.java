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

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.io.ReplayCharSequence;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

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
<<<<<<< ExtractorHTML2.java
     */
    public void innerProcess(CrawlURI curi) {

        if (!curi.isHttpTransaction())
        {
            return;
        }

        if(this.ignoreUnexpectedHTML) {
            if(!expectedHTML(curi)) {
                // HTML was not expected (eg a GIF was expected) so ignore
                // (as if a soft 404)
                return;
            }
        }

        String contentType = curi.getContentType();
        if ((contentType == null) || (!contentType.startsWith("text/html")))
        {
            // nothing to extract for other types here
            return;
        }

        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = curi.getHttpRecorder().getReplayCharSequence();
        if (cs == null) {
            logger.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }

        Matcher tags = TextUtils.getMatcher(RELEVANT_TAG_EXTRACTOR, cs);
        while(tags.find()) {
            if (tags.start(8) > 0) {
                // comment match
                // for now do nothing
            } else if (tags.start(7) > 0) {
            // <meta> match
                if (processMeta(curi,cs.subSequence(tags.start(5), tags.end(5)))) {
                    // meta tag included NOFOLLOW; abort processing
                    TextUtils.freeMatcher(tags);
                    return;
                }
            } else if (tags.start(5) > 0) {
                // generic <whatever> match
                processGeneralTag(
                    curi,
                    cs.subSequence(tags.start(6),tags.end(6)),
                    cs.subSequence(tags.start(5),tags.end(5)));
            } else if (tags.start(1) > 0) {
                // <script> match
                processScript(curi, cs.subSequence(tags.start(1), tags.end(1)), tags.end(2)-tags.start(1));
            } else if (tags.start(3) > 0){
                // <style... match
                processStyle(curi, cs.subSequence(tags.start(3), tags.end(3)), tags.end(4)-tags.start(3));

            }
        }
        TextUtils.freeMatcher(tags);
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
        // Done w/ the ReplayCharSequence.  Close it.
        if (cs != null) {
            try {
                cs.close();
            } catch (IOException ioe) {
                logger.warning(DevUtils.format(
                    "Failed close of ReplayCharSequence.", ioe));
            }
        }
    }


    /**
     * @param curi
=======
>>>>>>> 1.12
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
