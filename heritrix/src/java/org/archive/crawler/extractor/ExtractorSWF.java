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
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

import com.anotherbigidea.flash.interfaces.SWFVectors;
import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;
import com.anotherbigidea.flash.structs.Rect;
import com.anotherbigidea.io.InStream;

/**
 * Extracts URIs from SWF (flash/shockwave) files.
 * 
 * To test, here is a link to an swf that has links
 * embedded inside of it: http://www.hitspring.com/index.swf.
 *
 * @author Igor Ranitovic
 */
public class ExtractorSWF
extends Processor
implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger(ExtractorSWF.class.getName());
    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;
    private static final int MAX_READ_SIZE = 16 * 1024 * 1024;

    /**
     * @param name
     */
    public ExtractorSWF(String name) {
        super(name, "Flash extractor. Extracts URIs from SWF " +
            "(flash/shockwave) files.");
    }

    protected void innerProcess(CrawlURI curi) {
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }

        String contentType = curi.getContentType();
        if (contentType == null) {
            return;
        }
        if ((contentType.toLowerCase().indexOf("x-shockwave-flash") < 0)
                && (!curi.getURIString().toLowerCase().endsWith(".swf"))) {
            return;
        }

        numberOfCURIsHandled++;

        InputStream documentStream = null;
        // Get the SWF file's content stream.
        try {
            documentStream = curi.getHttpRecorder().getRecordedInput().
                getContentReplayInputStream();
            if (documentStream == null) {
                return;
            }

            // Create SWF action that will add discoved URIs to CrawlURI
            // alist(s).
            CrawlUriSWFAction curiAction = new CrawlUriSWFAction(curi,
                    getController());
            // Overwrite parsing of specific tags that might have URIs.
            CustomSWFTags customTags = new CustomSWFTags(curiAction);
            // Get a SWFReader instance.
            SWFReader reader =
                new SWFReader(getTagParser(customTags), documentStream) {
                /**
                 * Override because a corrupt SWF file can cause us to try
                 * read lengths that are hundreds of megabytes in size
                 * causing us to OOME.
                 * 
                 * Below is copied from SWFReader parent class.
                 */
                public int readOneTag() throws IOException {
                    int header = mIn.readUI16();
                    int  type   = header >> 6;    //only want the top 10 bits
                    int  length = header & 0x3F;  //only want the bottom 6 bits
                    boolean longTag = (length == 0x3F);
                    if(longTag) {
                        length = (int)mIn.readUI32();
                    }
                    // Below test added for Heritrix use.
                    if (length > MAX_READ_SIZE) {
                        throw new IOException("Length to read too big: " +
                            length);
                    }
                    byte[] contents = mIn.read(length);
                    mConsumer.tag(type, longTag, contents);
                    return type;
                }
            };
            
            reader.readFile();
            numberOfLinksExtracted += curiAction.getLinkCount();
        } catch (IOException e) {
            curi.addLocalizedError(getName(), e, "Fail reading.");
        } finally {
            try {
                documentStream.close();
            } catch (IOException e) {
                curi.addLocalizedError(getName(), e, "Fail on close.");
            }
        }

        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
        logger.fine(curi + " has " + numberOfLinksExtracted + " links.");
    }

    /**
     * Get a TagParser
     * 
     * The below is a bit hard to read.  We are subclassing the javaswf
     * TagParser so we can comment out a System.out.println debug statement.
     * 
     * @param customTags A custom tag parser.
     * @return An SWFReader.
     */
    private TagParser getTagParser(CustomSWFTags customTags) {
        return new TagParser(customTags) {
            /**
             * Override just so I can comment out a System.out.println
             * that was left in parent TagParser#parseDefineFont2.
             */
            protected void parseDefineFont2(InStream in) throws IOException {
                int id = in.readUI16();
                int flags = in.readUI8();
                int reservedFlags = in.readUI8();

                int nameLength = in.readUI8();
                String name = new String(in.read(nameLength));

                int glyphCount = in.readUI16();
                Vector glyphs = new Vector();

                int [] offsets = new int[glyphCount + 1];
                boolean is32 = (flags & FONT2_32OFFSETS) != 0;
                for (int i = 0; i <= glyphCount; i++) {
                    offsets[i] = is32? (int)in.readUI32(): in.readUI16();
                }

                for (int i = 1; i <= glyphCount; i++) {
                    int glyphSize = offsets[i] - offsets[i - 1];
                    // Here is an SWF which returns massive size to read
                    // causing OOME:
                    // http://pya.cc/pyaimg/img3/2004080708.swf
                    if (glyphSize > MAX_READ_SIZE) {
                        throw new IOException("Glyphsize to read is too big "
                                + glyphSize + " (Max is " + MAX_READ_SIZE
                                + ").");
                    }
                    byte [] glyphBytes = in.read(glyphSize);
                    glyphs.addElement(glyphBytes);
                }

                boolean isWide = ((flags & FONT2_WIDECHARS) != 0)
                        || (glyphCount > 256);

                int [] codes = new int[glyphCount];
                for (int i = 0; i < glyphCount; i++) {
                    codes[i] = isWide? in.readUI16(): in.readUI8();
                }

                // Commented out. Was leaving dross in heritrix_out.log.
                // System.out.println( "glyphCount=" + glyphCount +
                // " flags=" + Integer.toBinaryString( flags ) );

                int ascent = 0;
                int descent = 0;
                int leading = 0;
                int [] advances = new int[0];
                Rect [] bounds = new Rect[0];
                int [] kerningCodes1 = new int[0];
                int [] kerningCodes2 = new int[0];
                int [] kerningAdjustments = new int[0];

                if ((flags & FONT2_HAS_LAYOUT) != 0) {
                    ascent = in.readSI16();
                    descent = in.readSI16();
                    leading = in.readSI16();

                    advances = new int[glyphCount];

                    for (int i = 0; i < glyphCount; i++) {
                        advances[i] = in.readSI16();
                    }

                    bounds = new Rect[glyphCount];

                    for (int i = 0; i < glyphCount; i++) {
                        bounds[i] = new Rect(in);
                    }

                    int kerningCount = in.readUI16();

                    kerningCodes1 = new int[kerningCount];
                    kerningCodes2 = new int[kerningCount];
                    kerningAdjustments = new int[kerningCount];

                    for (int i = 0; i < kerningCount; i++) {
                        kerningCodes1[i] = isWide? in.readUI16(): in.readUI8();
                        kerningCodes2[i] = isWide? in.readUI16(): in.readUI8();
                        kerningAdjustments[i] = in.readSI16();
                    }
                }

                SWFVectors vectors = mTagtypes.tagDefineFont2(id, flags, name,
                        glyphCount, ascent, descent, leading, codes, advances,
                        bounds, kerningCodes1, kerningCodes2,
                        kerningAdjustments);

                if (vectors == null)
                    return;

                if (glyphs.isEmpty()) {
                    vectors.done();
                } else {
                    for (Enumeration e = glyphs.elements();
                            e.hasMoreElements();) {
                        byte [] glyphBytes = (byte [])e.nextElement();
                        InStream glyphIn = new InStream(glyphBytes);
                        parseShape(glyphIn, vectors, false, false);
                    }
                }
            }
        };
    }
    
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
