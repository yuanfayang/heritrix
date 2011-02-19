/* ExtractorXMLTest
 * 
 * Copyright (C) 2011 Internet Archive.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

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

import com.google.common.base.Charsets;


/**
 * Test XML extractor.
 *
 * @contributor gojomo
 * @contributor stack
 * @version $Revision: 6830 $, $Date: 2010-04-21 16:39:57 -0700 (Wed, 21 Apr 2010) $
 */
public class ExtractorXMLTest
extends TmpDirTestCase
implements CoreAttributeConstants {
    private final String LINK_TO_FIND = "http://www.example.org/";
    private HttpRecorder recorder = null;
    private ExtractorXML extractor = null;
    
    protected ExtractorXML createExtractor()
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
        return (ExtractorXML)((MapType)handler.getOrder().
            getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler.
                getSettingsObject(null), new ExtractorXML(name));
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        this.extractor = createExtractor();
        ByteArrayInputStream bais = new ByteArrayInputStream(
            "<?xml version=\"1.0\"?><x y='http://www.example.org'>z</x>".getBytes(Charsets.UTF_8));
        this.recorder = HttpRecorder.wrapInputStreamWithHttpRecord(getTmpDir(),
            this.getClass().getName(), bais, "UTF-8");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNoHintsOtherThanContentPrefix() throws IOException {
        UURI uuri = UURIFactory.getInstance("http://www.example.com"); // no XML-related suffix
        CrawlURI curi = setupCrawlURI(this.recorder, uuri.toString());
        curi.setContentType("image/gif"); // [SIC] remove mimetype indicator
        this.extractor.innerProcess(curi);
        Collection<Link> links = curi.getOutLinks();
        boolean foundLink = false;
        for (Iterator<Link> i = links.iterator(); i.hasNext();) {
            Link link = (Link)i.next();
            if (link.getDestination().toString().equals(this.LINK_TO_FIND)) {
                foundLink = true;
            }
        }
        assertTrue("Did not find url", foundLink);
    }
    
    private CrawlURI setupCrawlURI(HttpRecorder rec, String url)
    		throws URIException {
        CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
        curi.setContentSize(this.recorder.getRecordedInput().getSize());
        curi.setContentType("text/xml"); // FIXME: try other recommended XML types
        curi.setFetchStatus(200);
        curi.setHttpRecorder(rec);
        // Fake out the extractor that this is a HTTP transaction.
        curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION,
            new Object());
        return curi;
    }
}
