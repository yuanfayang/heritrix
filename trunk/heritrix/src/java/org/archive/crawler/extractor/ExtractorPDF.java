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
 * Created on Jul 11, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ToeThread;


/** Allows the caller to process a CrawlURI representing a PDF
 *  for the purpose of extracting URIs
 *
 * @author Parker Thompson
 *
 */
public class ExtractorPDF extends Processor implements CoreAttributeConstants
{
    private static int DEFAULT_MAX_SIZE_TO_PARSE = 5*1024*1024; // 5MB
    
    // TODO: make configurable
    private long maxSizeToParse = DEFAULT_MAX_SIZE_TO_PARSE;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorPDF");

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorPDF(String name) {
        super(name, "PDF extractor. Link extraction on PDF documents.");
    }

    protected void innerProcess(CrawlURI curi){

        if (!(curi.isHttpTransaction() && curi.getFetchStatus() == 200))
        {
            // TODO: generalize for when codes other than 200 might have good
            // content
            return;
        }

        String contentType = curi.getContentType();
        if ((contentType==null) || (!contentType.startsWith("application/pdf")))
        {
            // nothing to extract for other types here
            return;
        }

        numberOfCURIsHandled++;

        File tempFile;

        if(curi.getHttpRecorder().getRecordedInput().getSize()>maxSizeToParse)
        {
            return;
        }

        int sn = ((ToeThread)Thread.currentThread()).getSerialNumber();
        tempFile = new File(getController().getScratchDisk(),"tt"+sn+"tmp.pdf");

        PDFParser parser;
        ArrayList uris;
        try {
            curi.getHttpRecorder().getRecordedInput().
                copyContentBodyTo(tempFile);
            parser = new PDFParser(tempFile.getAbsolutePath());
            uris = parser.extractURIs();
        } catch (IOException e) {
            curi.addLocalizedError(getName(), e, "ExtractorPDF IOException");
            return;
        } catch (RuntimeException e) {
            // Truncated/corrupt  PDFs may generate ClassCast exceptions, or
            // other problems
            curi.addLocalizedError(getName(), e,
                "ExtractorPDF RuntimeException");
            return;
        } finally {
            tempFile.delete();
        }

        if(uris!=null && uris.size()>0) {
            numberOfLinksExtracted += uris.size();
            curi.getAList().putObject("html-links", uris);
        }

        logger.fine(curi+" has "+uris.size()+" links.");
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorPDF\n");
        ret.append("  Function:          Link extraction on PDF documents\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
