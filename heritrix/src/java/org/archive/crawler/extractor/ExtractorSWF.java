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
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.util.TextUtils;

import com.anotherbigidea.flash.interfaces.SWFActions;
import com.anotherbigidea.flash.interfaces.SWFTagTypes;
import com.anotherbigidea.flash.readers.SWFReader;
import com.anotherbigidea.flash.readers.TagParser;
import com.anotherbigidea.flash.structs.AlphaTransform;
import com.anotherbigidea.flash.structs.Matrix;
import com.anotherbigidea.flash.writers.SWFActionsImpl;
import com.anotherbigidea.flash.writers.SWFTagTypesImpl;
import com.anotherbigidea.io.InStream;

/**
 * Process SWF (flash/shockwave) files for strings that are likely to be
 * crawlable URIs.
 * 
 * @author Igor Ranitovic
 */
public class ExtractorSWF extends Extractor implements CoreAttributeConstants {

	private static final long serialVersionUID = 3627359592408010589L;

	private static Logger logger = Logger.getLogger(ExtractorSWF.class
			.getName());

	protected long numberOfCURIsHandled = 0;

	protected long numberOfLinksExtracted = 0;

	// TODO: consider if this should be even smaller, because anything
	// containing URLs wouldn't be this big
	private static final int MAX_READ_SIZE = 1024 * 1024; // 1MB

	/**
	 * @param name
	 */
	public ExtractorSWF(String name) {
		super(name, "Flash extractor. Extracts URIs from SWF "
				+ "(flash/shockwave) files.");
	}

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

		// Get link extracting SWF reader
		SWFReader reader = getSWFReader(curi);
		if (reader == null) {
			return;
		}

		numberOfCURIsHandled++;
		try {
			// Parse file for links
			reader.readFile();
		} catch (IOException e) {
			curi.addLocalizedError(getName(), e, "failed reading");
		} catch (NullPointerException e) {
			curi.addLocalizedError(getName(), e, "bad .swf file");
		} catch (NegativeArraySizeException e) {
			curi.addLocalizedError(getName(), e, "bad .swf file");
		}

		// Set flag to indicate that link extraction is completed.
		curi.linkExtractorFinished();
		logger.fine(curi + " has " + numberOfLinksExtracted + " links.");

	}

	/**
	 * Get a link extracting SWFParser.
	 * 
	 * A custom SWFReader which parses links from .swf file.
	 * 
	 * @param curi A CrawlURI to be processed.
	 * @return An SWFReader.
	 */
	private SWFReader getSWFReader(CrawlURI curi) {

		InputStream documentStream = null;
		// Get the SWF file's content stream.
		try {
			documentStream = curi.getHttpRecorder().getRecordedInput()
					.getContentReplayInputStream();
			if (documentStream == null) {
				return null;
			}
		} catch (IOException e) {
			curi.addLocalizedError(getName(), e, "Fail reading.");
		} finally {
			try {
				documentStream.close();
			} catch (IOException e) {
				curi.addLocalizedError(getName(), e, "Fail on close.");
			}
		}

		// Create SWF actions that will add discoved URIs to CrawlURI
		// alist(s).
		ExtractorSWFActions actions = new ExtractorSWFActions(curi,
				getController());
		// Overwrite parsing of specific tags that might have URIs.
		ExtractorSWFTags tags = new ExtractorSWFTags(actions);
		// Get a SWFReader instance.
		SWFReader reader = new SWFReader(getTagParser(tags), documentStream) {
			/**
			 * Override because a corrupt SWF file can cause us to try read
			 * lengths that are hundreds of megabytes in size causing us to
			 * OOME.
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
		};
		return reader;
	}

	/**
	 * Get a TagParser
	 * 
	 * A custom ExtractorTagParser which ignores all the big binary image/
	 * sound/font types which don't carry URLs is used, to avoid the
	 * occasionally fatal (OutOfMemoryError) memory bloat caused by the
	 * all-in-memory SWF library handling.
	 * 
	 * @param customTags
	 *            A custom tag parser.
	 * @return An SWFReader.
	 */
	private TagParser getTagParser(SWFTagTypes customTags) {
		return new ExtractorTagParser(customTags);
	}

	/**
	 * TagParser customized to ignore SWFTags that will never contain
	 * extractable URIs.
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

		protected void parseDefineBitsLossless(InStream in, int length,
				boolean hasAlpha) throws IOException {
			// DO NOTHING - no URLs to be found in bits
		}

		protected void parseDefineButtonSound(InStream in) throws IOException {
			// DO NOTHING - no URLs to be found in sound
		}

		protected void parseDefineFont(InStream in) throws IOException {
			// DO NOTHING - no URLs to be found in font
		}

		protected void parseDefineJPEG2(InStream in, int length)
				throws IOException {
			// DO NOTHING - no URLs to be found in jpeg
		}

		protected void parseDefineJPEGTables(InStream in) throws IOException {
			// DO NOTHING - no URLs to be found in jpeg
		}

		protected void parseDefineShape(int type, InStream in)
				throws IOException {
			// DO NOTHING - no URLs to be found in shape
		}

		protected void parseDefineSound(InStream in) throws IOException {
			// DO NOTHING - no URLs to be found in sound
		}

		protected void parseFontInfo(InStream in, int length, boolean isFI2)
				throws IOException {
			// DO NOTHING - no URLs to be found in font info
		}

		protected void parseDefineFont2(InStream in) throws IOException {
			// DO NOTHING - no URLs to be found in bits
		}
	}

	/**
	 * SWFTagTypes customized to use <code>ExtractorSWFActions</code>, which
	 * parse URI-like strings.
	 */
	protected class ExtractorSWFTags extends SWFTagTypesImpl {

		private SWFActions actions;

		public ExtractorSWFTags(SWFActions acts) {
			super(null);
			actions = acts;
		}

		public SWFActions tagDefineButton(int id, Vector buttonRecords)
				throws IOException {

			return actions;
		}

		public SWFActions tagDefineButton2(int id, boolean trackAsMenu,
				Vector buttonRecord2s) throws IOException {

			return actions;
		}

		public SWFActions tagDoAction() throws IOException {
			return actions;
		}

		public SWFActions tagDoInActions(int spriteId) throws IOException {
			return actions;
		}

		public SWFTagTypes tagDefineSprite(int id) throws IOException {
			return this;
		}

		public SWFActions tagPlaceObject2(boolean isMove, int clipDepth,
				int depth, int charId, Matrix matrix, AlphaTransform cxform,
				int ratio, String name, int clipActionFlags) throws IOException {

			return actions;
		}

	}

	/**
	 * SWFActions that parse URI-like strings. Links discovered using
	 * <code>ExtractorJS</code> are marked as speculative links (hop X). All
	 * other links are marked as embedded links (hop E).
	 * 
	 */
	protected class ExtractorSWFActions extends SWFActionsImpl {

		private CrawlURI curi;

		private CrawlController controller;

		static final String JSSTRING = "javascript:";

		/**
		 * @param curi
		 *            SWF URL to handle
		 * @param controller
		 *            Crawl controller need for error reporting
		 */
		public ExtractorSWFActions(CrawlURI curi, CrawlController controller) {
			assert (curi != null) : "CrawlURI should not be null";
			this.curi = curi;
			this.controller = controller;
		}

		/**
		 * Overwrite handling of discovered URIs.
		 * 
		 * @param url
		 *            Discovered URL.
		 * @param target
		 *            Discovered target (currently not being used.)
		 * @throws IOException
		 */
		public void getURL(String url, String target) throws IOException {
			// I have done tests on a few tens of swf files and have not seen a
			// need
			// to use 'target.' Most of the time 'target' is not set, or it is
			// set
			// to '_self' or '_blank'.
			processURIString(url);
		}

		public void lookupTable(String[] strings) throws IOException {
			for (String str : strings) {
				considerStringAsUri(str);
			}
		}

		public void push(String value) throws IOException {
			considerStringAsUri(value);
		}

		public void considerStringAsUri(String str) throws IOException {
			Matcher uri = TextUtils.getMatcher(ExtractorJS.STRING_URI_DETECTOR,
					str);

			if (uri.matches()) {
				curi.createAndAddLinkRelativeToVia(uri.group(),
						Link.SPECULATIVE_MISC, Link.SPECULATIVE_HOP);
				incrementLinkCount(1);
			}
			TextUtils.recycleMatcher(uri);
		}

		public void processURIString(String url) throws IOException {
			if (url.startsWith(JSSTRING)) {
				incrementLinkCount(ExtractorJS.considerStrings(
						curi, url, controller,false));
			} else {
				curi.createAndAddLinkRelativeToVia(url, Link.EMBED_MISC,
						Link.EMBED_HOP);
				incrementLinkCount(1);
			}
		}

		private void incrementLinkCount(long count) {
			numberOfLinksExtracted += count;
		}
	}

	public String report() {
		StringBuffer ret = new StringBuffer();
		ret.append("Processor: org.archive.crawler.extractor.ExtractorSWF\n");
		ret.append("  Function:          Link extraction on Shockwave Flash "
				+ "documents (.swf)\n");

		ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
		ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
		return ret.toString();
	}

}
