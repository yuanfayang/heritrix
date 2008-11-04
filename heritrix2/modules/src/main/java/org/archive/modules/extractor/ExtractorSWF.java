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
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.ProcessorURI;
import org.archive.state.KeyManager;
import org.archive.util.TextUtils;

import com.jswiff.SWFReader;
import com.jswiff.listeners.SWFListener;
import com.jswiff.swfrecords.ButtonCondAction;
import com.jswiff.swfrecords.ClipActionRecord;
import com.jswiff.swfrecords.ClipActions;
import com.jswiff.swfrecords.SWFHeader;
import com.jswiff.swfrecords.actions.ActionBlock;
import com.jswiff.swfrecords.actions.ConstantPool;
import com.jswiff.swfrecords.actions.DefineFunction;
import com.jswiff.swfrecords.actions.DefineFunction2;
import com.jswiff.swfrecords.actions.GetURL;
import com.jswiff.swfrecords.actions.Push;
import com.jswiff.swfrecords.tags.DefineButton;
import com.jswiff.swfrecords.tags.DefineButton2;
import com.jswiff.swfrecords.tags.DefineSprite;
import com.jswiff.swfrecords.tags.DoAction;
import com.jswiff.swfrecords.tags.DoInitAction;
import com.jswiff.swfrecords.tags.PlaceObject2;
import com.jswiff.swfrecords.tags.PlaceObject3;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagHeader;

/**
 * Extracts URIs from SWF (flash/shockwave) files.
 * 
 * @author Igor Ranitovic
 * @author Noah Levitt
 */
public class ExtractorSWF extends ContentExtractor {

    private static final long serialVersionUID = 3L;

    private static Logger logger = Logger.getLogger(ExtractorSWF.class
            .getName());

    protected AtomicLong linksExtracted = new AtomicLong(0);

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
            documentStream = curi.getRecorder().getRecordedInput()
                    .getContentReplayInputStream();
            if (documentStream == null) {
                return false;
            }

            SWFReader swfReader = new SWFReader(documentStream);
            ExtractorSWFListener listener = new ExtractorSWFListener(uriErrors,
                    curi);
            swfReader.addListener(listener);
            swfReader.read();

            linksExtracted.addAndGet(listener.getLinkCount());
            logger.fine(curi + " has " + listener.getLinkCount() + " links.");
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
        ret.append("  Function:          Link extraction on Shockwave Flash "
                + "documents (.swf)\n");

        ret.append("  CrawlURIs handled: " + getURICount() + "\n");
        ret.append("  Links extracted:   " + linksExtracted + "\n\n");
        return ret.toString();
    }

    private class ExtractorSWFListener extends SWFListener {
        private static final String JSSTRING = "javascript:";

        private ProcessorURI curi;

        private long linkCount;

        private UriErrorLoggerModule uriErrors;

        public ExtractorSWFListener(UriErrorLoggerModule uriErrors,
                ProcessorURI curi) {
            super();
            assert (curi != null) : "CrawlURI should not be null";
            this.curi = curi;
            this.linkCount = 0;
            this.uriErrors = uriErrors;
        }

        public long getLinkCount() {
            return this.linkCount;
        }

        /**
         * @param e
         *            the exception which occurred during tag header parsing
         */
        public void processTagHeaderReadError(Exception e) {
            logger.warning("tag header read error (parsing aborted?): " + e);
        }

        /**
         * @param tagHeader
         *            header of the malformed tag
         * @param tagData
         *            the tag data as byte array
         * @param e
         *            the exception which occurred while parsing the tag
         * 
         * @return <code>false</code>, i.e. the reader continues reading further
         *         tags after error processing
         */
        @Override
        public boolean processTagReadError(TagHeader tagHeader, byte[] tagData,
                Exception e) {
            logger.warning("tag read error: tagHeader.getCode()="
                    + tagHeader.getCode() + " tagData.length=" + tagData.length
                    + ": " + e);
            return false;
        }

        private void considerStringAsUri(String str) {
            Matcher uri = TextUtils.getMatcher(ExtractorJS.STRING_URI_DETECTOR,
                    str);
            if (uri.matches()) {
                addLink(uri.group(), LinkContext.EMBED_MISC, Hop.EMBED);
            }
            TextUtils.recycleMatcher(uri);
        }

        private void processURIString(String url) {
            if (url.startsWith(JSSTRING)) {
                linkCount += ExtractorJS.considerStrings(uriErrors, curi, url,
                        false);
            } else {
                addLink(url, LinkContext.EMBED_MISC, Hop.EMBED);
            }
        }

        private void addLink(String url, LinkContext linkContext, Hop hop) {
            logger.fine("found link: " + url);
            try {
                int max = uriErrors.getMaxOutlinks(curi);
                Link.addRelativeToVia(curi, max, url, linkContext, hop);
                linkCount++;
            } catch (URIException e) {
                logger.warning("exception adding link url=" + url + ": " + e);
            }
        }

        @Override
        public void processHeader(SWFHeader header) {
            logger.fine("header=" + header);
        }

        @Override
        public void processTag(Tag tag, long streamOffset) {
            logger.fine("streamOffset=" + streamOffset + " tag=" + tag);
            if (tag instanceof DefineButton2) {
                ButtonCondAction[] actions = ((DefineButton2) tag).getActions();
                if (actions != null) {
                    for (ButtonCondAction action : actions) {
                        processActionBlock(action.getActions());
                    }
                }
            } else if (tag instanceof DoAction) {
                processActionBlock(((DoAction) tag).getActions());
            } else if (tag instanceof DefineSprite) {
                for (Object to : ((DefineSprite) tag).getControlTags()) {
                    // must explicitly processTag() on nested tags
                    processTag((Tag) to, -1);
                }
            } else if (tag instanceof PlaceObject2) {
                ClipActions ca = ((PlaceObject2) tag).getClipActions();
                if (ca != null) {
                    for (Object caro : ca.getClipActionRecords()) {
                        processActionBlock(((ClipActionRecord) caro)
                                .getActions());
                    }
                }
            } else if (tag instanceof DefineButton) {
                processActionBlock(((DefineButton) tag).getActions());
            } else if (tag instanceof DoInitAction) {
                processActionBlock(((DoInitAction) tag).getInitActions());
            } else if (tag instanceof PlaceObject3) {
                ClipActions ca = ((PlaceObject3) tag).getClipActions();
                if (ca != null) {
                    for (Object caro : ca.getClipActionRecords()) {
                        processActionBlock(((ClipActionRecord) caro)
                                .getActions());
                    }
                }
            }
        }

        private void processActionBlock(ActionBlock actionBlock) {
            for (Object o : actionBlock.getActions()) {
                if (o instanceof GetURL) {
                    processURIString(((GetURL) o).getURL());
                } else if (o instanceof Push) {
                    for (Object svo : ((Push) o).getValues()) {
                        Push.StackValue sv = ((Push.StackValue) svo);
                        if (sv.getType() == Push.StackValue.TYPE_STRING) {
                            considerStringAsUri(sv.getString());
                        }
                    }
                } else if (o instanceof ConstantPool) {
                    for (Object value : ((ConstantPool) o).getConstants()) {
                        logger
                                .fine("considering ConstantPool/LookupTable value: "
                                        + value);
                        considerStringAsUri(value.toString());
                    }
                } else if (o instanceof DefineFunction) {
                    processActionBlock(((DefineFunction) o).getBody());
                } else if (o instanceof DefineFunction2) {
                    processActionBlock(((DefineFunction2) o).getBody());
                }
            }
        }
    };

    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(ExtractorSWF.class);
    }
}
