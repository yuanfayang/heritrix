/*
 * ARCWriter.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.framework.Processor;
import org.archive.crawler.datamodel.CrawlURI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.httpclient.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Parker Thompson
 *
 */
public class ARCWriter extends Processor {
	
	private int arcMaxSize;							// max size we want arc files to be
	private int totalsize;									// ??
	private String arcPrefix;							// file prefix for arcs
	private String outputDir;							// where should we put them?
	private File file = null;								// file handle
	private FileChannel fileChannel =null;		// manipulates file handle


	public void writeArcEntry(CrawlURI curi){
		
		// TODO sanity check the passed curi before writing
		if (true){	//curi.getResponse() != null) {
			
		/*			
			HttpResponse response = (HttpResponse) curi.getAList().getObject("httpdocument");
			ByteBuffer startLine =
			ByteBuffer.wrap((response.getStartLine()).getBytes());
			ByteBuffer headers =
			ByteBuffer.wrap(response.getHeader().getBytes());
			ByteBuffer payload = 
			ByteBuffer.wrap(response.getPayload().getBytes(),0, response.getPayload().getSize());
			*/
			
			String metaLineStr = "\n"
					+ curi.getURIString()
					+ " "
					+ curi.getHost().getIP().getHostAddress()
					+ " "
					+ get14DigitDate()
					+ " "
					+  curi.getContentType()
					+ " "
					//+ (response.getPayload().getSize() + startLine.capacity() + headers.capacity()) + "\n";
					// TODO generate actual size count
					+ '1';
							
			ByteBuffer metaLine =
			ByteBuffer.wrap(metaLineStr.getBytes());
						
			if (isNewArcNeeded()) {
				createNewArcFile();
			}
	
			try {
				fileChannel.write(metaLine);
			/*	fileChannel.write(startLine);
				fileChannel.write(headers);
				fileChannel.write(payload);
			*/
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createNewArcFile() {

		String date = get12DigitDate();
		String fileName = outputDir + arcPrefix + date + ".arc";
		try {
			if(fileChannel != null)
				fileChannel.close();				
			file = new File(fileName);
			fileChannel = new FileOutputStream(file, true).getChannel();
			String arcFileDesc =
				"filedesc://"
					+ fileName
					+ " 0.0.0.0 "
					+ date
					+ " text/plain 77\n1 0 InternetArchive\nURL IP-address Archive-date Content-type Archive-length\n";
			ByteBuffer fileDescBuff = ByteBuffer.wrap(arcFileDesc.getBytes());
			fileChannel.write(fileDescBuff);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isNewArcNeeded(){
		try{
			if (fileChannel.size() > arcMaxSize)
				return true;
		}catch(IOException e){
			e.printStackTrace();	
		}	
		return false;
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

	public void setOutputDir(String buffer) {
		outputDir = buffer;
	}
	
	// utility functions for creating arc-style date stamps
	public static String get14DigitDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		return sdf.format(new Date());
	}
	public static String get12DigitDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmm");		
		return sdf.format(new Date()); 
	}
	
}
