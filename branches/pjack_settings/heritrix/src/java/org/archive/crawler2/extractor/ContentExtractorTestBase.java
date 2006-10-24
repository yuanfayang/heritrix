/* Copyright (C) 2006 Internet Archive.
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
 * ContentExtractorTest.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.crawler2.extractor;


import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.StateProcessorTestBase;


/**
 * Abstract base class for unit testing ContentExtractor implementations.
 * 
 * @author pjack
 */
public abstract class ContentExtractorTestBase extends StateProcessorTestBase {

    
    /**
     * An extractor created during the setUp.
     */
    protected Extractor extractor;

    
    /**
     * Sets up the {@link #extractor} and 
     * {@link StateProcessorTestBase#processorClass}
     * fields.
     */
    final public void setUp() {
        processorClass = getProcessorClass();
        extractor = makeExtractor();
    }
    
    
    /**
     * Subclasses should return the Extractor subclass being tested.
     * 
     * @return  the Extractor subclass being tested
     */
    protected abstract Class getProcessorClass();
    
    
    /**
     * Subclasses should return an Extractor instance to test.
     * 
     * @return   an Extractor instance to test
     */
    protected abstract Extractor makeExtractor();
    
    
    /**
     * Returns a DefaultExtractorURI for testing purposes.
     * 
     * @return   a DefaultExtractorURI
     * @throws Exception   just in case
     */
    protected DefaultExtractorURI defaultURI() throws Exception {
        UURI uuri = UURIFactory.getInstance("http://www.archive.org/start/");
        return new DefaultExtractorURI(uuri, LinkContext.NAVLINK_MISC);
    }
    
    
    /**
     * Tests that a URI with a zero content length has no links extracted.
     * 
     * @throws Exception   just in case
     */
    public void testZeroContent() throws Exception {
        DefaultExtractorURI uri = defaultURI();
        uri.setContent("", "text/plain");
        extractor.process(uri);
        assertEquals(0, uri.getOutLinks().size());
        assertNoSideEffects(uri);
    }
    
    
    /**
     * Tests that a URI whose linkExtractionFinished flag has been set has
     * no links extracted.
     * 
     * @throws Exception   just in case
     */
    public void testFinished() throws Exception {
        DefaultExtractorURI uri = defaultURI();
        uri.linkExtractionFinished();
        extractor.process(uri);
        assertEquals(0, uri.getOutLinks().size());
        assertNoSideEffects(uri);        
    }

    
    /**
     * Asserts that the given URI has no URI errors, no localized errors, and
     * no annotations.
     * 
     * @param uri   the URI to test
     */
    protected static void assertNoSideEffects(DefaultExtractorURI uri) {
        assertEquals(0, uri.getUriErrors().size());
        assertEquals(0, uri.getLocalizedErrors().size());
        assertEquals("", uri.getAnnotations());        
    }
}
