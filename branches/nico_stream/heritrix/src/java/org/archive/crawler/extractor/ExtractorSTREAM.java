package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.io.ReplayCharSequence;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * This extractor first detects metafiles (META) or playlists (PLS)
 * This extractor is parsing URIs from STREAM metafiles (META) and playlists (PLS).
 *
 * @author nico
 *
 **/

public class ExtractorSTREAM extends Extractor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorSTREAM");

    private static String ESCAPED_AMP = "&amp";
    
    /**
     *  META URL extractor pattern.
     *
     *  This pattern extracts URIs for metafiles and playlists
     **/
    
    /* Anything followed by a "." EXTENSION and a "?" or "#" character
     */
    static final String STREAM_PATTERNS = 
    	"(?i).*\\.(?:asf|asx|wmv|wvx|wma|wax" +
        "|ram|sram|rm|ra|mov|pls|m3u)(?:[\\?#].*)?$";

    /* Real Audio (Magic .ra\0375)
     * 0	belong		0x2e7261fd		audio/x-pn-realaudio
     * 0	string		.RMF			application/vnd.rn-realmedia
     * 
     * video/x-pn-realvideo
     * video/vnd.rn-realvideo
     * application/vnd.rn-realmedia
     * 
     * sigh, there are many mimes for that but the above are the most common.
     *
     ************************************************************************
     * Apple formats
     * Added ISO mimes
     * 4	string		moov	      	video/quicktime
     * 4	string		mdat	      	video/quicktime
     * 4	string		wide	      	video/quicktime
     * 4	string		skip	      	video/quicktime
     * 4	string		free	      	video/quicktime
     * 
     ************************************************************************
     * Microsoft formats
     * Added by nico
     * 0	belong		0x 30 26 b2 75 8e 66 cf 11 a6 d9 00 aa 00 62 ce 6c		
     * 					video/x-ms-{asf|asx|wmv|wvx|wma|wax}
     */
    
    static final String MAGIC_HEADER =
    	"(?:^\\x30\\x26\\xb2\\x75|" + // Microsoft Media Format
    	"(?i)^\\.RMF|^\\x2e\\x72\\x61\\xfd|" + // Real Media Format
    	"(?i)^.{4}moov|(?i)^.{4}mdat|(?i)^.{4}wide|(?i)^.{4}skip|(?i)^.{4}free)"; // Apple MOV Format
    
    /* STREAM URIs contained in a metafile or playlist
     * usually a simple http://, rtsp://, mms://, pnm:// or ftp:// url
     */
    static final String STREAM_URI_EXTRACTOR =
    	"(?i)((?:http://|rtsp://|mms://|pnm://|ftp://)[^\\s\"\'<>]+)(?:\\s*[\"\'<]|\\s+.+)?";
    // GROUPS:
    // (G1) URI
    
    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorSTREAM(String name) {
        super(name, "STREAM Extractor. Extracts links from metafiles and playlists\n" +
                "Microsoft: .asf .asx .wmv .wvx .wma .wax\n" +
                "RealMedia: .ram .sram .rm .ra\n" +
                "IceCast, SHOUTcast compatible: .pls .m3u\n." +
                "Apple: .mov");
    }

    /**
     * @param curi Crawl URI to process.
     */
    public void extract(CrawlURI curi) {
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }
         
        /* First main criterion is file EXTENSION
         * .asf .asx .wmv .wvx .wma .wax
         * .ram .sram .rm .ra
         * .pls .m3u  - playlists
         * .mov
         */    
        if ( !Pattern.matches(STREAM_PATTERNS, curi.toString()) ) {
        	return;
        }
        
        /* Second criterion is MAGIC HEADER
         * The 16 first bytes (Hexadecimal encoding):
         * Microsoft mediafiles: 3026b275 8e66cf11 a6d900aa 0062ce6c
         * Real mediafiles: .rmf or 2e524d46, 2e7261fd ?
         * Apple: moov, mdat, wide, skip or free
         */
        ReplayCharSequence cs = null;
        Matcher magicHeader = null;
        
        try {
        	cs = curi.getHttpRecorder().getReplayCharSequence();
            
        } catch (IOException e) {
            logger.severe("Failed getting ReplayCharSequence: " + e.getMessage());
        }
        if (cs == null) {
            logger.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }
        
        try {
        	magicHeader = TextUtils.getMatcher(MAGIC_HEADER, cs);
        }
        catch (StackOverflowError e) {
        	DevUtils.warnHandle(e, "ExtractorSTREAM StackOverflowError");
        } finally {
        	TextUtils.recycleMatcher(magicHeader);
        }
        
        if ( magicHeader.find() ) {
        	return;
        } 
        /*
        if ( Pattern.matches(MAGIC_HEADER, cs) ) {
        	return;
        }
        */
        this.numberOfCURIsHandled++;
        
        this.numberOfLinksExtracted +=
            processStyleCode(curi, cs, getController());
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

    public static long processStyleCode(CrawlURI curi, CharSequence cs,
    		CrawlController controller) {
    	long foundLinks = 0;
        Matcher uris = null;
        String streamUri;
        try {
            uris = TextUtils.getMatcher(STREAM_URI_EXTRACTOR, cs);
            while (uris.find()) {
                streamUri = uris.group(1);
                streamUri = TextUtils.replaceAll(ESCAPED_AMP, streamUri, "&");
                foundLinks++;
                try {
                    curi.createAndAddLink(streamUri,Link.EMBED_MISC,
                            Link.EMBED_HOP);
                } catch (URIException e) {
                    // There may not be a controller (e.g. If we're being run
                    // by the extractor tool).
                    if (controller != null) {
                        controller.logUriError(e, curi.getUURI(), streamUri);
                    } else {
                        logger.info(curi + ", " + streamUri + ": " +
                            e.getMessage());
                    }
                }
            }
        } catch (StackOverflowError e) {
            DevUtils.warnHandle(e, "ExtractorSTREAM StackOverflowError");
        } finally {
            TextUtils.recycleMatcher(uris);
        }
        return foundLinks;
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorSTREAM\n");
        ret.append("  Function:          Link extraction on STREAM metafiles (META) or playlists (PLS)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
