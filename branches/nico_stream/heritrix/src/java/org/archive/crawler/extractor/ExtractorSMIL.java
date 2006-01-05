/*
 * ExtractorSMIL
 */

package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.io.ReplayCharSequence;
import org.archive.net.UURI;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * A simple extractor which finds HTTP URIs inside SMIL files,
 * inside attribute values and simple elements (those with only
 * whitespace + HTTP URI + whitespace as contents)
 *
 * @author nico
 *
 **/

public class ExtractorSMIL extends Extractor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger(ExtractorXML.class.getName());

    private static String ESCAPED_AMP = "&amp";

    static final String SMIL_URI_EXTRACTOR =
    	"(?i)[\"\'>]\\s*((?:http://|rtsp://|mms://|pnm://|ftp://)[^\\s\"\'<>]+)\\s*[\"\'<]";
    // GROUPS:
    // (G1) URI
    
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorSMIL(String name) {
        super(name, "SMIL Extractor. Extracts links from SMIL.");
    }

    /**
     * @param curi Crawl URI to process.
     */
    public void extract(CrawlURI curi) {
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }
        String mimeType = curi.getContentType();
        if (mimeType == null) {
            return;
        }
        if ((mimeType.toLowerCase().indexOf("smil") < 0) 
                && (!curi.toString().toLowerCase().endsWith(".smil"))
                && (!curi.toString().toLowerCase().endsWith(".smi"))) {
            return;
        }
        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = null;
        try {
            cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch (IOException e) {
            logger.severe("Failed getting ReplayCharSequence: " + e.getMessage());
        }
        if (cs == null) {
            logger.severe("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }
        this.numberOfLinksExtracted +=
            processSmil(curi, cs, getController());
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
        // Done w/ the ReplayCharSequence.  Close it.
        if (cs != null) {
            try {
                cs.close();
            } catch (IOException ioe) {
                logger.warning(TextUtils.exceptionToString(
                    "Failed close of ReplayCharSequence.", ioe));
            }
        }
    }

    public static long processSmil(CrawlURI curi, CharSequence cs,
            CrawlController controller) {
        long foundLinks = 0;
        Matcher uris = null;
        String smilUri;
        uris = TextUtils.getMatcher(SMIL_URI_EXTRACTOR, cs);
        while (uris.find()) {
            smilUri = uris.group(1);
            // TODO: Escape more HTML Entities.
            smilUri = TextUtils.replaceAll(ESCAPED_AMP, smilUri, "&");
            foundLinks++;
            try {
                // treat as speculative, as whether context really 
                // intends to create a followable/fetchable URI is
                // unknown
                curi.createAndAddLink(smilUri,Link.SPECULATIVE_MISC,
                        Link.SPECULATIVE_HOP);
            } catch (URIException e) {
                // There may not be a controller (e.g. If we're being run
                // by the extractor tool).
                if (controller != null) {
                    controller.logUriError(e, curi.getUURI(), smilUri);
                } else {
                    logger.info(curi + ", " + smilUri + ": " +
                        e.getMessage());
                }
            }
        }
        TextUtils.recycleMatcher(uris);
        return foundLinks;
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorSMIL\n");
        ret.append("  Function:          Link extraction on SMIL\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
