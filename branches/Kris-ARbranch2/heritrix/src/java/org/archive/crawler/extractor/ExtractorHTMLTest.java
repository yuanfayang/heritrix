/* ExtractorHTMLTest
 *
 * Created on May 19, 2004
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.util.HttpRecorder;
import org.archive.util.TmpDirTestCase;


/**
 * Test html extractor.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class ExtractorHTMLTest
extends TmpDirTestCase
implements CoreAttributeConstants {
    private File orderFile = null;
    private CrawlerSettings globalSettings = null;
    private XMLSettingsHandler settingsHandler = null;
    private final String ARCHIVE_DOT_ORG = "archive.org";
    private final String LINK_TO_FIND = "http://www.hewlett.org";
    private HttpRecorder recorder = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        this.orderFile = new File(getTmpDir(),
            this.getClass().getName() + "_order.xml");
        this.settingsHandler = new XMLSettingsHandler(this.orderFile);
        this.settingsHandler.initialize();
        this.globalSettings = this.settingsHandler.getSettingsObject(null);
        final boolean USE_NET = false;
        URL url = null;
        if (USE_NET) {
            url = new URL("http://" + this.ARCHIVE_DOT_ORG);
        } else {
            File f = new File(getTmpDir(), this.ARCHIVE_DOT_ORG + ".html");
            url = new URL("file://" + f.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(("<html><head><title>test</title><body>" +
                "<a href=" + this.LINK_TO_FIND + ">Hewlett Foundation</a>" +
                "</body></html>").getBytes());
            fos.flush();
            fos.close();
        }
        this.recorder = setupRecorder(url, this.ARCHIVE_DOT_ORG, null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInnerProcess() throws IOException {
        ExtractorHTML extractor = new ExtractorHTML("html extractor");
        extractor.earlyInitialize(this.globalSettings);
        UURI uuri = UURIFactory.getInstance("http://" + this.ARCHIVE_DOT_ORG);
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        extractor.innerProcess(curi);
        Set links = (Set)curi.getObject(CoreAttributeConstants.A_HTML_LINKS);
        boolean foundLinkToHewlettFoundation = false;
        for (Iterator i = links.iterator(); i.hasNext();) {
            String link = (String)i.next();
            if (link.equals(this.LINK_TO_FIND)) {
                foundLinkToHewlettFoundation = true;
                break;
            }
        }
        assertTrue("Did not find gif url", foundLinkToHewlettFoundation);
    }
    
	/**
	 * Record the download for later playback by the extractor.
	 * @param url URL to record.
	 * @param basename of what we're recording.
	 * @param encoding Encoding.
	 * @throws IOException
	 * @return An httprecorder.
	 */
    private HttpRecorder setupRecorder(URL url, String basename,
            String encoding)
    throws IOException {
        HttpRecorder rec = new HttpRecorder(getTmpDir(), basename);
        if (encoding != null && encoding.length() > 0) {
            rec.setCharacterEncoding(encoding);
        }
        InputStream is = rec.inputWrap(new BufferedInputStream(
            url.openStream()));
        final int BUFFER_SIZE = 1024 * 4;
        byte [] buffer = new byte[BUFFER_SIZE];
        while(is.read(buffer) != -1) {
            // Just read it all down.
        }
        is.close();
        return rec;
    }
    
    private CrawlURI setupCrawlURI(HttpRecorder rec, String url)
    		throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
        curi.setContentSize(this.recorder.getRecordedInput().getSize());
        curi.setContentType("text/html");
        curi.setFetchStatus(200);
        curi.setHttpRecorder(rec);
        // Fake out the extractor that this is a HTTP transaction.
        curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION,
            new Object());
        return curi;
    }
    
    /**
     * Test single net or local filesystem page parse.
     * Set the uuri to be a net url or instead put in place a file
     * named for this class under the unit test directory.
     * 
     * @throws IOException
     */
    public void testPageParse() throws IOException {
        UURI uuri = null;
        
// DO
//      uuri = UURIFactory.getInstance("http://www.xjmu.edu.cn/");
// OR
//        File f = new File(getTmpDir(), this.getClass().getName() +
//        ".html");
//        if (f.exists()) {
//        	uuri = UURIFactory.getInstance("file://" +
//        			f.getAbsolutePath());
//        }
// OR 
//      uuri = getUURI(URL or PATH)
//
// OR 
//      Use the main method below and pass this class an argument.
//     
        if (uuri != null) {
        	runExtractor(uuri);
        }
    }
    
    protected UURI getUURI(String url) throws URIException {
        url = (url.indexOf("://") > 0)? url: "file://" + url;
        return UURIFactory.getInstance(url);
    }
    
    protected void runExtractor(UURI baseUURI) throws IOException {
        runExtractor(baseUURI, null);
    }
    
    protected void runExtractor(UURI baseUURI, String encoding)
    throws IOException {
        if (baseUURI == null) {
        	return;
        }
        ExtractorHTML extractor = new ExtractorHTML("html extractor");
        extractor.earlyInitialize(this.globalSettings);
        URL url = new URL(baseUURI.toString());
        this.recorder = setupRecorder(url, this.getClass().getName(),
            encoding);
        CrawlURI curi = setupCrawlURI(this.recorder, url.toString());
        extractor.innerProcess(curi);
        System.out.println("+" + extractor.report());
        Set links = (Set)curi.getObject(CoreAttributeConstants.A_HTML_LINKS);
        System.out.println("+HTML Links (A_HTML_LINKS):");
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                System.out.println((String)i.next());
            }
        }
        System.out.println("+HTML Embeds (A_HTML_EMBEDS):");
        links = (Set)curi.getObject(CoreAttributeConstants.A_HTML_EMBEDS);
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                System.out.println((String)i.next());
            }
        }
        System.out.
            println("+HTML Speculative Embeds (A_HTML_SPECULATIVE_EMBEDS):");
        links = (Set)curi.
            getObject(CoreAttributeConstants.A_HTML_SPECULATIVE_EMBEDS);
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                String u = (String)i.next();
                System.out.println(u + " " + curi.getUURI() + " " +
                    UURIFactory.getInstance(curi.getUURI(), u));
            }
        }
    }

    /**
     * Test a particular <embed src=...> construct that was suspicious in
     * the No10GovUk crawl.
     *
     * @throws URIException
     */
    public void testEmbedSrc() throws URIException {
        ExtractorHTML extractor = new ExtractorHTML("html extractor");
        CrawlURI curi=
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        // An example from http://www.records.pro.gov.uk/documents/prem/18/1/default.asp?PageId=62&qt=true
        CharSequence cs = "<embed src=\"/documents/prem/18/1/graphics/qtvr/" +
            "hall.mov\" width=\"320\" height=\"212\" controller=\"true\" " +
            "CORRECTION=\"FULL\" pluginspage=\"http://www.apple.com/" +
            "quicktime/download/\" /> ";
        extractor.extract(curi,cs);
        assertTrue(((Collection)curi.
            getObject(A_HTML_EMBEDS)).
                contains("/documents/prem/18/1/graphics/qtvr/hall.mov"));
    }
    
    /**
     * Test a whitespace issue found in href.
     * 
     * See [ 963965 ] Either UURI or ExtractHTML should strip whitespace better.
     * https://sourceforge.net/tracker/?func=detail&atid=539099&aid=963965&group_id=73833
     *
     * @throws URIException
     */
    public void testHrefWhitespace() throws URIException {
        ExtractorHTML extractor = new ExtractorHTML("html extractor");
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.carsound.dk"));
        CharSequence cs = "<a href=\"http://www.carsound.dk\n\n\n" +
        	"\"\ntarget=\"_blank\">C.A.R. Sound\n\n\n\n</a>";   
        extractor.extract(curi,cs);
        Collection c = (Collection)curi.getObject(A_HTML_LINKS);
        for (Iterator i = c.iterator(); i.hasNext();) {
            UURI uuri = UURIFactory.getInstance((String)i.next());
            assertTrue("Not stripping new lines",
                uuri.toString().equals("http://www.carsound.dk/"));
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: " + ExtractorHTMLTest.class.getName() +
                " URL|PATH [ENCODING]");
            System.exit(1);
        }
        ExtractorHTMLTest testCase = new ExtractorHTMLTest();
        testCase.setUp();
        try {
            testCase.runExtractor(testCase.getUURI(args[0]),
                (args.length == 2)? args[1]: null);
        } finally {
            testCase.tearDown();
        }
    }
}
