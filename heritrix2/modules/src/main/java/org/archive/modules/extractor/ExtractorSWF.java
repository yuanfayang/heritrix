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

package org.archive.modules.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.archive.modules.ProcessorURI;
import org.archive.state.KeyManager;

import com.anotherbigidea.flash.interfaces.SWFActions;
import com.anotherbigidea.flash.interfaces.SWFTagTypes;
import com.anotherbigidea.flash.interfaces.SWFTags;
import com.anotherbigidea.flash.readers.ActionParser;
import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;
import com.anotherbigidea.flash.structs.AlphaTransform;
import com.anotherbigidea.flash.structs.Matrix;
import com.anotherbigidea.io.InStream;

/**
 * Extracts URIs from SWF (flash/shockwave) files.
 * 
 * To test, here is a link to an swf that has links
 * embedded inside of it: http://www.hitspring.com/index.swf.
 *
 * @author Igor Ranitovic
 */
public class ExtractorSWF extends ContentExtractor {

    private static final long serialVersionUID = 3L;

    private static Logger logger =
        Logger.getLogger(ExtractorSWF.class.getName());

    protected AtomicLong linksExtracted = new AtomicLong(0);

    private static final int MAX_READ_SIZE = 1024 * 1024; // 1MB

    /**
     * @param name
     */
    public ExtractorSWF() {
    }

    
    @Override
    protected boolean shouldExtract(ProcessorURI uri) {
        String contentType = uri.getContentType();
        if (contentType == null) {
            return false;
        }
        if ((contentType.toLowerCase().indexOf("x-shockwave-flash") < 0)
                && (!uri.toString().toLowerCase().endsWith(".swf"))) {
            return false;
        }
        return true;
    }

    
    @Override
    protected boolean innerExtract(ProcessorURI curi) {
        InputStream documentStream = null;
        // Get the SWF file's content stream.
        try {
            documentStream = curi.getRecorder().getRecordedInput().
                getContentReplayInputStream();
            if (documentStream == null) {
                return false;
            }

            // Create SWF action that will add discoved URIs to CrawlURI
            // alist(s).
            CrawlUriSWFAction curiAction = new CrawlUriSWFAction(uriErrors, 
                    curi);

            // Overwrite parsing of specific tags that might have URIs.
            CustomSWFTags customTags = new CustomSWFTags(curiAction);
            // Get a SWFReader instance.
            SWFReader reader =
                new ExtractorSWFReader(new ExtractorTagParser(customTags), documentStream);
            
            reader.readFile();
            linksExtracted.addAndGet(curiAction.getLinkCount());
            logger.fine(curi + " has " + curiAction.getLinkCount() + " links.");
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
        } finally {
            try {
                documentStream.close();
            } catch (IOException e) {
                curi.getNonFatalFailures().add(e);
            }
        }


        // Set flag to indicate that link extraction is completed.
        return true;
    }
    
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorSWF\n");
        ret.append("  Function:          Link extraction on Shockwave Flash " +
            "documents (.swf)\n");

        ret.append("  CrawlURIs handled: " + getURICount() + "\n");
        ret.append("  Links extracted:   " + linksExtracted + "\n\n");
        return ret.toString();
    }
    
    class ExtractorSWFReader extends SWFReader 
    {
        public ExtractorSWFReader(SWFTags consumer, InputStream inputstream) {
            super(consumer, inputstream);
        }

        public ExtractorSWFReader(SWFTags consumer, InStream instream) {
            super(consumer, instream);
        }    

        /**
         * Override because a corrupt SWF file can cause us to try read lengths
         * that are hundreds of megabytes in size causing us to OOME.
         * 
         * Below is copied from SWFReader parent class.
         */
        public int readOneTag() throws IOException {
            int header = mIn.readUI16();
            int type = header >> 6; // only want the top 10 bits
            int length = header & 0x3F; // only want the bottom 6 bits
            boolean longTag = (length == 0x3F);
            if (longTag) {
                length = (int) mIn.readUI32();
            }
            // Below test added for Heritrix use.
            if (length > MAX_READ_SIZE) {
                // skip to next, rather than throw IOException ending
                // processing
                mIn.skipBytes(length);
                logger.info("oversized SWF tag (type=" + type + ";length="
                        + length + ") skipped");
            } else {
                byte[] contents = mIn.read(length);
                mConsumer.tag(type, longTag, contents);
            }
            return type;
        }
    }
    
    /**
     * TagParser customized to do the following:
     *  - ignore swf tags that will never contain extractable URIs, including 
     *    all the big binary image/sound/font types, to avoid the 
     *    occasionally fatal (OutOfMemoryError) memory bloat caused by the
     *    all-in-memory SWF library handling
     *  - correctly handle clip event flags inside PlaceObject2 tags (HER-1509)
     *  - use our javaswf subclasses when parsing tags that contain other tags 
     */
    protected class ExtractorTagParser extends TagParser {

        protected ExtractorTagParser(SWFTagTypes tagtypes) {
            super(tagtypes);
        }

        protected void parseDefineBits(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in bits
        }

        protected void parseDefineBitsJPEG3(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in bits
        }

        protected void parseDefineBitsLossless(InStream in, int length, boolean hasAlpha) throws IOException {
            // DO NOTHING - no URLs to be found in bits
        }

        protected void parseDefineButtonSound(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in sound
        }

        protected void parseDefineFont(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in font
        }

        protected void parseDefineJPEG2(InStream in, int length) throws IOException {
            // DO NOTHING - no URLs to be found in jpeg
        }

        protected void parseDefineJPEGTables(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in jpeg
        }

        protected void parseDefineShape(int type, InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in shape
        }

        protected void parseDefineSound(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in sound
        }

        protected void parseFontInfo(InStream in, int length, boolean isFI2) throws IOException {
            // DO NOTHING - no URLs to be found in font info
        }

        protected void parseDefineFont2(InStream in) throws IOException {
            // DO NOTHING - no URLs to be found in bits
        }
        
        // heritrix: Overridden to use our TagParser and SWFReader. The rest of
        // the code is the same.
        @Override
        protected void parseDefineSprite(InStream in) throws IOException {
            int id = in.readUI16();
            in.readUI16(); // frame count

            SWFTagTypes sstt = mTagtypes.tagDefineSprite(id);

            if (sstt == null)
                return;

            // heritrix: only these two lines differ from
            // super.parseDefineSprite()
            TagParser parser = new ExtractorTagParser(sstt);
            SWFReader reader = new ExtractorSWFReader(parser, in);

            reader.readTags();
        }
        
        // Overridden to read 32 bit clip event flags when flash version >= 6.
        // All the rest of the code is copied directly. Fixes HER-1509.
        @Override
        protected void parsePlaceObject2( InStream in ) throws IOException
        {
            boolean hasClipActions    = in.readUBits(1) != 0;
            boolean hasClipDepth      = in.readUBits(1) != 0;
            boolean hasName           = in.readUBits(1) != 0;
            boolean hasRatio          = in.readUBits(1) != 0;
            boolean hasColorTransform = in.readUBits(1) != 0;
            boolean hasMatrix         = in.readUBits(1) != 0;
            boolean hasCharacter      = in.readUBits(1) != 0;
            boolean isMove            = in.readUBits(1) != 0;

            int depth = in.readUI16();

            int            charId    = hasCharacter      ? in.readUI16()            : 0;
            Matrix         matrix    = hasMatrix         ? new Matrix( in )         : null;
            AlphaTransform cxform    = hasColorTransform ? new AlphaTransform( in ) : null;
            int            ratio     = hasRatio          ? in.readUI16()            : -1;        
            String         name      = hasName           ? in.readString(mStringEncoding)  : null;  
            int            clipDepth = hasClipDepth      ? in.readUI16()            : 0;

            int clipEventFlags = 0;

            if (hasClipActions) {
                in.readUI16(); // reserved

                // heritrix: flags size changed in swf version 6
                clipEventFlags = mFlashVersion < 6 ? in.readUI16() : in.readSI32();
            }

            SWFActions actions = mTagtypes.tagPlaceObject2(isMove, clipDepth,
                    depth, charId, matrix, cxform, ratio, name, clipEventFlags);

            if (hasClipActions && actions != null) {
                int flags = 0;

                // heritrix: flags size changed in swf version 6
                while ((flags = mFlashVersion < 6 ? in.readUI16() : in.readSI32()) != 0) {
                    in.readUI32(); // length

                    actions.start(flags);
                    ActionParser parser = new ActionParser(actions, mFlashVersion);

                    parser.parse(in);
                }

                actions.done();
            }
        }

    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(ExtractorSWF.class);
    }
}
