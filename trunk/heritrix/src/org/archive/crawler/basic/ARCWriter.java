/*
 * ARCWriter.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.framework.Processor;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.archive.crawler.datamodel.CrawlOrder;

/**
 * @author Parker Thompson
 *
 */
public class ARCWriter extends Processor {
	
	private int arcMaxSize = 10000000;		// max size we want arc files to be (bytes)
	private String arcPrefix = "archive";			// file prefix for arcs
	private String outputDir = "";						// where should we put them?
	private File file = null;								// file handle
	private FileChannel fileChannel =null;		// manipulates file handle


  	public void initialize(CrawlController c){
  		super.initialize(c);
  		
  		// set up output directory
  		CrawlOrder order = c.getOrder();
  		setOutputDir(order.getOutputLocation());
  		
		createNewArcFile();		  		
  	}
  	
  	public void process(CrawlURI curi){
  		super.process(curi);
  		
  		//TODO add switch to handle multiple protocols (or some other dispatcher)
  		writeHttp(curi);

  	}

	public void writeArcEntry(CrawlURI curi, byte[] record){
		
		// TODO sanity check the passed curi before writing
		if (true){	
						
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
					+ record.length;
							
			ByteBuffer metaLine = ByteBuffer.wrap(metaLineStr.getBytes());
			ByteBuffer recordBuffer = ByteBuffer.wrap(record);
						
			if (isNewArcNeeded()) {
				createNewArcFile();
			}
	
			try {
				fileChannel.write(metaLine);
				fileChannel.write(recordBuffer);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void createNewArcFile() {

		String date = get14DigitDate();
		String fileName = outputDir + arcPrefix + date + ".arc";
		try {
			if(fileChannel != null){
				fileChannel.close();
			}
							
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

	public void writeHttp(CrawlURI curi){
	
		GetMethod get 						= (GetMethod) curi.getAList().getObject("http-transaction");
		//String charSet 						= get.getResponseCharSet();
		Header[] headers 					= get.getResponseHeaders();
		String responseBodyString 	= get.getResponseBodyAsString();

		int headersSize 						= 0;
		
		StringBuffer buffer = null;		// accumulate the record here
		
		for(int i=0; i < headers.length; i++){
			headersSize += headers[i].toString().length();
		}
		
		buffer = new StringBuffer(headersSize + responseBodyString.length());
				
		for(int i=0; i < headers.length; i++){
			buffer.append(headers[i].toString());
		}
		buffer.append(responseBodyString);
			
		try {
			//byte[] record = buffer.toString().getBytes(charSet);			
			byte[] record = buffer.toString().getBytes();			
			writeArcEntry(curi, record);
			
		} catch (Exception e) {
			System.out.println(e);
		}
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
		
		// start writing files with the new prefix
		createNewArcFile();
	}

	public void setOutputDir(String buffer) {

				
		// make sure it's got a trailing file.seperator so the
		// dir is not treated as a file prefix
		if(! buffer.endsWith(file.separator)){
			buffer = new String(buffer + file.separator);
		}
			
		File newDir = new File(buffer);
		
		if(!newDir.exists()){
			try{
				newDir.mkdirs();
				outputDir = buffer;
					
			}catch(Exception e){
				e.printStackTrace();
			}		
		}
			
		// start writing files to the new dir
		createNewArcFile();
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
