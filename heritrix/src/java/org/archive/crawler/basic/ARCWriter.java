/* 
 * ARCWriter
 * 
 * Created on Jun 5, 2003
 *
 * $Header$

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
 *
 */
 
package org.archive.crawler.basic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.archive.io.IAGZIPOutputStream;
import org.archive.io.ReplayInputStream;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.xbill.DNS.Record;

/**
 * Processor module for writing the results of any 
 * successful fetches (and perhaps someday, certain
 * kinds of network failures) to the Internet Archive
 * ARC file format. 
 * 
 * @author Parker Thompson
 *
 */
public class ARCWriter extends Processor implements CoreAttributeConstants {

	/** 
	 * Max size of an arc file in bytes.
	 */
	private int arcMaxSize = 100000000;
	/** 
	 * File prefix for arc files. 
	 */
	private String arcPrefix = "IAH";
	/** 
	 * Output directory that holds arc files
	 */ 
	private String outputDir = ""; 
	
	private File file = null;
	private OutputStream arcOut = null;
	private OutputStream out = null;
	/** 
	 *  Arc file compression.
	 *  Compressed arc file is concatenation of individually compressed
	 *  gzip files.
	 */
	private boolean useCompression = true;
	/** 
	 *  Append unique IDs to arc files.
	 *  This assures uniqueness across threads in 
	 *  the event multiple arcwriter exist and are creating files concurrently.
	 */
	private static int arcId = 0;

	/** 
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);

		readConfiguration();

		try {
			createNewArcFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read configuration parameters for an order file.
	 */
	protected void readConfiguration() {
		// set up output directory
		CrawlOrder order = controller.getOrder();

		setUseCompression(getBooleanAt("@compress", false));
		setArcPrefix(getStringAt("@prefix", arcPrefix));
		setArcMaxSize(getIntAt("@max-size-bytes", arcMaxSize));
		setOutputDir(getStringAt("@path", outputDir));

	}

	/**
	  * Takes a CrawlURI and generates an arc record, writing it
	  * to disk.  Currently this method understands the following 
	  * uri types: dns, http
	  */
	protected synchronized void innerProcess(CrawlURI curi) {

		// if  there was a failure, or we haven't fetched the resource yet, return
		if (curi.getFetchStatus() <= 0) {
			return;
		}

		// create a new arc file if the existing one is too big
		if (isNewArcNeeded()) {
			try {
				createNewArcFile();
			} catch (Exception e) {
				//TODO deal better with not being able to write files to disk (a serious problem)
				e.printStackTrace();
			}
		}

		// find the write protocol and write this sucker
		String scheme = curi.getUURI().getScheme();

		try {

			if (scheme.equals("dns")) {
				writeDns(curi);
			} else if (scheme.equals("http")) {
				writeHttp(curi);
			}

			// catch disk write errors		
		} catch (IOException e) {
			e.printStackTrace();

			// catch errors where we're trying to write a bad record
		} catch (InvalidRecordException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Write a standard arc metaline.
	 * @param curi
	 * @param recordLength
	 * @throws InvalidRecordException
	 * @throws IOException
	 */
	protected void writeMetaLine(CrawlURI curi, int recordLength)
		throws InvalidRecordException, IOException {

		// TODO sanity check the passed curi before writing

		// figure out content type, truncate at delimiters [;, ]
		// truncated multi-part content type header at ';'.
		// apache httpclient collapses values of multiple instances of the header into one comma-separated value,
		// therefore truncated at ','.
		// Current ia_tools that work with arc files except 5-column space-separated meta-lines, 
		// therefore truncate at ' '.
		String contentType = curi.getContentType();
		if (contentType == null) {
			contentType = "no-type"; // per ARC spec
		} else if (curi.getContentType().indexOf(';') >= 0) {
			contentType = contentType.substring(0, contentType.indexOf(';'));
		} else if (curi.getContentType().indexOf(',') >= 0) {
			contentType = contentType.substring(0, contentType.indexOf(','));
		} else if (curi.getContentType().indexOf(' ') >= 0) {
			contentType = contentType.substring(0, contentType.indexOf(' '));

		}

		String hostIP = curi.getServer().getHost().getIP().getHostAddress();
		String dateStamp =
			ArchiveUtils.get14DigitDate(
				curi.getAList().getLong(A_FETCH_BEGAN_TIME));

		// fail if we're missing anything critical
		if (hostIP == null || dateStamp == null) {
			throw new InvalidRecordException("missing data elements");
		}

		String metaLineStr =
			curi.getURIString()
				+ " "
				+ hostIP
				+ " "
				+ dateStamp
				+ " "
				+ contentType
				+ " "
				+ recordLength
				+ "\n";

		try {
			out.write(metaLineStr.getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}

		// store size since we've already calculated it and it can be used 
		// to generate interesting statistics
		//curi.setContentSize((long)recordLength);
	}

	/**
	 * This method creates a new unique arc file.
	 * 
	 * @throws IOException
	 */
	protected void createNewArcFile() throws IOException {

		String date = ArchiveUtils.get14DigitDate();
		int uniqueIdentifier = getNextArcId();

		String fileExtension = ".arc";
		if (useCompression()) {
			fileExtension += ".gz";
		}

		String fileName =
			arcPrefix + date + "-" + uniqueIdentifier + fileExtension;
		String fqFileName = outputDir + fileName;

		try {
			if (out != null) {
				out.close();
			}

			file = new File(fqFileName);
			arcOut = new FileOutputStream(file);

			if (useCompression()) {
				out = new IAGZIPOutputStream(arcOut);
			} else {
				out = arcOut;
			}

			String arcFileDesc =
				"filedesc://"
					+ fileName
					+ " 0.0.0.0 "
					+ date
					+ " text/plain 77\n1 0 InternetArchive\nURL IP-address Archive-date Content-type Archive-length\n\n\n";

			out.write(arcFileDesc.getBytes());

			if (useCompression()) {
				((IAGZIPOutputStream) out).endCompressionBlock();
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
	/**
	 * This method returns true is the length of current arc file is greater
	 * than maximum arc length.
	 * 
	 * @return boolean 
	 */
	protected boolean isNewArcNeeded() {
		if (file.length() > arcMaxSize) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Set compression mode (on/off).
	 * 
	 * @param use
	 */
	public void setUseCompression(boolean use) {
		useCompression = use;
	}
	/**
	 * 
	 * @return boolean
	 */
	public boolean useCompression() {
		return useCompression;
	}
	/**
	 * Write HTTP response headers for a arc record.
	 * 
	 * @param curi
	 * @throws IOException
	 * @throws InvalidRecordException
	 */
	protected void writeHttp(CrawlURI curi)
		throws IOException, InvalidRecordException {

		if (curi.getFetchStatus() <= 0) {
			// error; do not write to ARC (for now) 
			return;
		}
		GetMethod get =
			(GetMethod) curi.getAList().getObject("http-transaction");

		if (get == null) {
			// some error occurred; nothing to write
			// TODO: capture some network errors in the ARC file for posterity
			return;
		}

		int recordLength = 0;
		recordLength += curi.getContentSize();

		if (recordLength == 0) {
			// write nothing
			return;
		}

		prewrite();
		writeMetaLine(curi, recordLength);

		ReplayInputStream capture =
			get.getHttpRecorder().getRecordedInput().getReplayInputStream();
		try {
			capture.readFullyTo(out);
			long remaining = capture.remaining();
			if (remaining > 0) {
				DevUtils.warnHandle(
					new Throwable("n/a"),
					"gap between expected and actual: "
						+ remaining
						+ "\n"
						+ DevUtils.extraInfo());
				while (remaining > 0) {
					// pad with zeros
					out.write(0);
					remaining--;
				}
			}
		} finally {
			capture.close();
		}
		out.write('\n'); // trailing newline
		postwrite();

		// OLD WAY
		//		baos.writeTo(out);
		//		out.write("\r\n".getBytes());
		//		out.write(body);
		//		out.write("\n".getBytes());
	}

	/**
	 * Close GZIP compression, if necessary
	 */
	private void postwrite() throws IOException {
		if (useCompression()) {
			((IAGZIPOutputStream) out).endCompressionBlock();
		}
	}

	/**
	 * Restart GZIP compression, if necessary
	 * 
	 * @throws IOException
	 */
	private void prewrite() throws IOException {
		if (useCompression()) {
			// zip each record individually
			 ((IAGZIPOutputStream) out).startCompressionBlock();

		} // else skip the special gzip jive and just write to a FileOutputStream
	}
	/**
	 * Write a dns record to a arc file
	 * 
	 * @param curi
	 * @throws IOException
	 * @throws InvalidRecordException
	 */
	protected void writeDns(CrawlURI curi)
		throws IOException, InvalidRecordException {

		int recordLength = 0;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// start the record with a 14-digit date per RFC 2540
		byte[] fetchDate =
			ArchiveUtils
				.get14DigitDate(curi.getAList().getLong(A_FETCH_BEGAN_TIME))
				.getBytes();
		baos.write(fetchDate);
		// don't forget the newline
		baos.write("\n".getBytes());
		recordLength = fetchDate.length + 1;

		Record[] rrSet =
			(Record[]) curi.getAList().getObject(A_RRECORD_SET_LABEL);

		if (rrSet != null) {
			for (int i = 0; i < rrSet.length; i++) {
				byte[] record = rrSet[i].toString().getBytes();
				recordLength += record.length;

				baos.write(record);

				// add the newline between records back in 
				baos.write("\n".getBytes());
				recordLength += 1;
			}
		}

		prewrite();
		writeMetaLine(curi, recordLength);

		// save the calculated contentSize for logging purposes
		// TODO handle this need more sensibly
		curi.setContentSize((long) recordLength);

		baos.writeTo(out);
		out.write("\n".getBytes());
		postwrite();
	}

	// getters and setters		
	public int getArcMaxSize() {
		return arcMaxSize;
	}

	public String getArcPrefix() {
		return arcPrefix;

	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setArcMaxSize(int i) {
		arcMaxSize = i;
	}

	public void setArcPrefix(String buffer) {
		arcPrefix = buffer;
	}

	private int getNextArcId() {
		return arcId++;
	}
	/**
	 * Sets output directory that holds arc files.
	 * If 'disk' path is not absolute, create 'disk' directory relative to the 
	 * path of the order file
	 * 
	 * @param dir
	 */
	public void setOutputDir(String dir) {

		File newDir;
		if (!ArchiveUtils.isFilePathAbsolute(dir)) {
			newDir =
				new File(
					ArchiveUtils.getFilePath(
						controller.getOrder().getCrawlOrderFilename())
						+ dir);
		} else {
			newDir = new File(dir);
		}

		if (!newDir.exists()) {
			try {
				newDir.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		outputDir = newDir.getAbsolutePath() + File.separatorChar;
	}
}
