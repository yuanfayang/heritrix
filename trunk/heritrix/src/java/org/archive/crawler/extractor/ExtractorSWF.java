/*
 * Heritrix
 *
 * $Id$
 *
 * Created on March 19, 2004
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

package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;

/**
 * Extracts URIs from SWF (flash/shockwave) files.
 *
 * @author Igor Ranitovic
 *
 */
public class ExtractorSWF extends Processor implements CoreAttributeConstants {

    private static Logger logger = 
        Logger.getLogger("org.archive.crawler.extractor.ExtractorSWF");

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorSWF(String name) {
        super(name, "Flash extractor. Extracts URIs from SWF " +
            "(flash/shockwave) files.");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {

        if (!curi.isHttpTransaction()) {
            return;
        }

        String contentType = curi.getContentType();
        if ((contentType == null) || (!contentType.toLowerCase().
                startsWith("application/x-shockwave-flash"))) {
            return;
        }

        numberOfCURIsHandled++;

        InputStream documentStream = null;
        // Get the SWF file's content stream.
        try {
            documentStream = curi.getHttpRecorder()
                .getRecordedInput().getContentReplayInputStream();

            if (documentStream == null) {
                return;
            }

            // Create SWF action taht will add discoved URIs to CrawlURI 
            // alist(s).             
            CrawlUriSWFAction curiAction = new CrawlUriSWFAction(curi);
            // Overwirte parsing of specific tags that might have URIs.
            CustomSWFTags customTags = new CustomSWFTags(curiAction);
            TagParser linkParser = new TagParser(customTags);
            // Parse the file.
            SWFReader reader = new SWFReader(linkParser, documentStream);
            reader.readFile();
            
            numberOfLinksExtracted += curiAction.getLinkCount();
            
        } catch (IOException e) {
            curi.addLocalizedError(getName(), e, null);
        } finally {
            try {
                documentStream.close();
            } catch (IOException e) {
                // TODO: Report the problem.
            }
        }
        
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished(); 
        logger.fine(curi + " has " + numberOfLinksExtracted + " links.");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorSWF\n");
        ret.append("  Function:          Link extraction on Shockwave Flash " +
            "documents (.swf)\n");

        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        return ret.toString();
    }
}
