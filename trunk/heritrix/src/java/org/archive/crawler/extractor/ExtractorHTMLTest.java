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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
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
    private final String ARCHIVE_DOT_ORG = "archive.org";
    private final String LINK_TO_FIND = "http://www.hewlett.org/";
    private HttpRecorder recorder = null;
    private ExtractorHTML extractor = null;
    
    protected ExtractorHTML createExtractor()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException {
        // Hack in a settings handler.  Do this by adding this extractor
        // to the order file (I'm adding it to a random MapType; seemingly
        // can only add to MapTypes post-construction). This takes care
        // of setting a valid SettingsHandler into the ExtractorHTML (This
        // shouldn't be so difficult).  Of note, the order file below is
        // not written to disk.
        final String name = this.getClass().getName();
        SettingsHandler handler = new XMLSettingsHandler(
            new File(getTmpDir(), name + ".order.xml"));
        handler.initialize();
        return (ExtractorHTML)((MapType)handler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler.
                getSettingsObject(null), new ExtractorHTML(name));
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        this.extractor = createExtractor();
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
        this.recorder = HttpRecorder.wrapInputStreamWithHttpRecord(getTmpDir(),
            this.getClass().getName(), url.openStream(), null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInnerProcess() throws IOException {
        UURI uuri = UURIFactory.getInstance("http://" + this.ARCHIVE_DOT_ORG);
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        this.extractor.innerProcess(curi);
        Collection links = curi.getOutLinks();
        boolean foundLinkToHewlettFoundation = false;
        for (Iterator i = links.iterator(); i.hasNext();) {
            Link link = (Link)i.next();
            if (link.getDestination().toString().equals(this.LINK_TO_FIND)) {
                foundLinkToHewlettFoundation = true;
                break;
            }
        }
        assertTrue("Did not find gif url", foundLinkToHewlettFoundation);
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
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws InvalidAttributeValueException
     */
    public void testPageParse()
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException, IOException {
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
    
    protected void runExtractor(UURI baseUURI)
    throws InvalidAttributeValueException, AttributeNotFoundException,
    MBeanException, ReflectionException, IOException {
        runExtractor(baseUURI, null);
    }
    
    protected void runExtractor(UURI baseUURI, String encoding)
    throws IOException, InvalidAttributeValueException,
    AttributeNotFoundException, MBeanException, ReflectionException {
        if (baseUURI == null) {
        	return;
        }
        this.extractor = createExtractor();
        URL url = new URL(baseUURI.toString());
        this.recorder = HttpRecorder.
            wrapInputStreamWithHttpRecord(getTmpDir(),
            this.getClass().getName(), url.openStream(), encoding);
        CrawlURI curi = setupCrawlURI(this.recorder, url.toString());
        this.extractor.innerProcess(curi);
        
        System.out.println("+" + this.extractor.report());
        int count = 0; 
        Collection links = curi.getOutLinks();
        System.out.println("+HTML Links (hopType="+Link.NAVLINK_HOP+"):");
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.NAVLINK_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.println("+HTML Embeds (hopType="+Link.EMBED_HOP+"):");
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.EMBED_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.
            println("+HTML Speculative Embeds (hopType="+Link.SPECULATIVE_HOP+"):");
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                Link link = (Link)i.next();
                if (link.getHopType()==Link.SPECULATIVE_HOP) {
                    count++;
                    System.out.println(link.getDestination());
                }
            }
        }
        System.out.
            println("+HTML Other (all other hopTypes):");
        if (links != null) {
            for (Iterator i = links.iterator(); i.hasNext();) {
                Link link = (Link) i.next();
                if (link.getHopType() != Link.SPECULATIVE_HOP
                        && link.getHopType() != Link.NAVLINK_HOP
                        && link.getHopType() != Link.EMBED_HOP) {
                    count++;
                    System.out.println(link.getHopType() + " "
                            + link.getDestination());
                }
            }
        }
        System.out.println("TOTAL URIS EXTRACTED: "+count);
    }

    /**
     * Test a particular <embed src=...> construct that was suspicious in
     * the No10GovUk crawl.
     *
     * @throws URIException
     */
    public void testEmbedSrc() throws URIException {
        CrawlURI curi=
            new CrawlURI(UURIFactory.getInstance("http://www.example.org"));
        // An example from http://www.records.pro.gov.uk/documents/prem/18/1/default.asp?PageId=62&qt=true
        CharSequence cs = "<embed src=\"/documents/prem/18/1/graphics/qtvr/" +
            "hall.mov\" width=\"320\" height=\"212\" controller=\"true\" " +
            "CORRECTION=\"FULL\" pluginspage=\"http://www.apple.com/" +
            "quicktime/download/\" /> ";
        this.extractor.extract(curi,cs);
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/documents/prem/18/1/graphics/qtvr/hall.mov")>=0;
            }
        }));
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
        CrawlURI curi =
            new CrawlURI(UURIFactory.getInstance("http://www.carsound.dk"));
        CharSequence cs = "<a href=\"http://www.carsound.dk\n\n\n" +
        	"\"\ntarget=\"_blank\">C.A.R. Sound\n\n\n\n</a>";   
        this.extractor.extract(curi,cs);
        curi.getOutLinks();
        assertTrue("Not stripping new lines", CollectionUtils.exists(curi
                .getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "http://www.carsound.dk/")>=0;
            }
        }));
    }
    
    /**
     * Test a missing whitespace issue found in form
     * 
     * [HER-1128] ExtractorHTML fails to extract FRAME SRC link without
     * whitespace before SRC http://webteam.archive.org/jira/browse/HER-1128
     */
    public void testNoWhitespaceBeforeValidAttribute() throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory
                .getInstance("http://www.example.com"));
        CharSequence cs = "<frame name=\"main\"src=\"http://www.example.com/\"> ";
        this.extractor.extract(curi, cs);
        Link[] links = curi.getOutLinks().toArray(new Link[0]);
        assertTrue("no links found",links.length==1);
        assertTrue("expected link not found", 
                links[0].getDestination().toString().equals("http://www.example.com/"));
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
