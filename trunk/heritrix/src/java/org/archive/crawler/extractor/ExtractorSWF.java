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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
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
 * Process SWF (flash/shockwave) files for strings that are likely to be
 * crawlable URIs.
 * 
 * @author Igor Ranitovic
 * @author Noah Levitt
 */
public class ExtractorSWF extends Extractor implements CoreAttributeConstants {

	private static final long serialVersionUID = 3627359592408010589L;

	private static Logger logger = Logger.getLogger(ExtractorSWF.class
			.getName());

	protected long numberOfCURIsHandled = 0;
	protected long numberOfLinksExtracted = 0;

	/**
	 * @param name
	 */
	public ExtractorSWF(String name) {
		super(name, "Flash extractor. Extracts URIs from SWF "
				+ "(flash/shockwave) files.");
		logger.setLevel(Level.ALL);
	}

	@Override
	protected void extract(CrawlURI curi) {
		if (!isHttpTransactionContentToProcess(curi)) {
			return;
		}

		String contentType = curi.getContentType();
		if (contentType == null) {
			return;
		}
		if ((contentType.toLowerCase().indexOf("x-shockwave-flash") < 0)
				&& (!curi.toString().toLowerCase().endsWith(".swf"))) {
			return;
		}

		InputStream documentStream = null;
		try {
		    logger.fine("curi=" + curi);

		    documentStream = curi.getHttpRecorder().getRecordedInput()
		    .getContentReplayInputStream();

		    SWFReader swfReader = new SWFReader(documentStream);
		    swfReader.addListener(new ExtractorSWFListener(curi, this.getController()));
		    numberOfCURIsHandled++;
		    swfReader.read();
		} catch (IOException e) {
		    curi.addLocalizedError(getName(), e, "failed reading");
		} catch (NullPointerException e) {
		    curi.addLocalizedError(getName(), e, "bad .swf file");
		} catch (NegativeArraySizeException e) {
		    curi.addLocalizedError(getName(), e, "bad .swf file");
		} finally {
		    IOUtils.closeQuietly(documentStream);
		}

		// Set flag to indicate that link extraction is completed.
		curi.linkExtractorFinished();
		logger.info(curi + " has " + numberOfLinksExtracted + " links.");
	}

	@Override
	public String report() {
		StringBuffer ret = new StringBuffer();
		ret.append("Processor: org.archive.crawler.extractor.ExtractorSWF\n");
		ret.append("  Function:          Link extraction on Shockwave Flash "
				+ "documents (.swf)\n");

		ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
		ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
		return ret.toString();
	}

	private class ExtractorSWFListener extends SWFListener {
		private static final String JSSTRING = "javascript:";

		private CrawlURI curi;
		private CrawlController controller;

		private ExtractorSWFListener(CrawlURI curi, CrawlController controller) {
			super();
			this.curi = curi;
			this.controller = controller;
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
						processActionBlock(((ClipActionRecord) caro).getActions());
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
						processActionBlock(((ClipActionRecord) caro).getActions());
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
						logger.fine("considering ConstantPool/LookupTable value: "
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
		 * @return <code>false</code>, i.e. the reader continues reading
		 *         further tags after error processing
		 */
		@Override
		public boolean processTagReadError(TagHeader tagHeader, byte[] tagData,
				Exception e) {
			logger.warning("tag read error: tagHeader.getCode()=" + tagHeader.getCode()
					+ " tagData.length=" + tagData.length + ": " + e);
			return false;
		}

		private void considerStringAsUri(String str) {
			Matcher uri = TextUtils.getMatcher(ExtractorJS.STRING_URI_DETECTOR,
					str);

			if (uri.matches()) {
				addLink(uri.group(), Link.SPECULATIVE_MISC,
						Link.SPECULATIVE_HOP);
				incrementLinkCount(1);
			}
			TextUtils.recycleMatcher(uri);
		}

		private void processURIString(String url) {
			if (url.startsWith(JSSTRING)) {
				incrementLinkCount(ExtractorJS.considerStrings(curi, url,
						controller, false));
			} else {
				addLink(url, Link.EMBED_MISC, Link.EMBED_HOP);
			}
		}

		private void addLink(String url, String context, char hopType) {
			logger.fine("found link: " + url);
			try {
				curi.createAndAddLinkRelativeToVia(url, context, hopType);
				incrementLinkCount(1);
			} catch (URIException e) {
				logger.warning("exception adding link url=" + url + ": " + e);
			}
		}

		private void incrementLinkCount(long count) {
			numberOfLinksExtracted += count;
		}

		@Override
		public void processHeader(SWFHeader header) {
			logger.fine("header=" + header);
		}
	};

}
