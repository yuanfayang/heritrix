/*
 * ARCWriter.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.xbill.DNS.Record;

/**
 * @author Parker Thompson
 *
 */
public class ARCWriter extends Processor implements CoreAttributeConstants {
	
	private int arcMaxSize = 10000000;		// max size we want arc files to be (bytes)
	private String arcPrefix = "archive";			// file prefix for arcs
	private String outputDir = "";						// where should we put them?
	private File file = null;								// file handle
	private FileOutputStream out = null;		// for writing to files


  	public void initialize(CrawlController c){
  		super.initialize(c);
  		
  		// set up output directory
  		CrawlOrder order = c.getOrder();
  		setOutputDir(order.getOutputLocation());
  		
		createNewArcFile();		  		
  	}
  	
  	public void process(CrawlURI curi){
  		super.process(curi);
  		
  		// find the write protocol and write this sucker
  		String scheme = curi.getUURI().getUri().getScheme();
  		
  		try{
  			if(scheme.equals("dns")){
  				writeDns(curi);
  				
  			}else if(scheme.equals("http")){
	  			writeHttp(curi);
  			}
  					
  		}catch(IOException e){
  			e.printStackTrace();
  		}
  	}


  	protected void writeMetaLine(CrawlURI curi, int recordLength){

		// TODO sanity check the passed curi before writing
		if (true){	
			
			// figure out content type, truncate at delimiter ';'
			String contentType;
			if(curi.getContentType().indexOf(';') >= 0 ){
				contentType = curi.getContentType().substring(0,curi.getContentType().indexOf(';'));
			}else{
				contentType = curi.getContentType();
			}
						
			String metaLineStr = "\n"
					+ curi.getURIString()
					+ " "
					+ curi.getHost().getIP().getHostAddress()
					+ " "
					+ curi.getAList().getLong(A_FETCH_BEGAN_TIME)
					+ " "
					// eliminate additonal args (e.g. "text/html; charset=iso-8859-1" => text/html)
					+  contentType
					+ " "
					+ recordLength
					+ "\n";	
					
			if(isNewArcNeeded()) {
							createNewArcFile();
			}
					
			try{	
			out.write(metaLineStr.getBytes());  		
			}catch(IOException e){
				e.printStackTrace();
			}
  		}
  	}
  	
	protected void createNewArcFile() {

		String date = get14DigitDate();
		String fileName = outputDir + arcPrefix + date + ".arc";
		try {
			if(out != null){
				out.close();
			}
							
			file = new File(fileName);
			//out = new FileOutputStream(file, true).getChannel();
			out = new FileOutputStream(file);
			
			String arcFileDesc =
				"filedesc://"
					+ fileName
					+ " 0.0.0.0 "
					+ date
					+ " text/plain 77\n1 0 InternetArchive\nURL IP-address Archive-date Content-type Archive-length\n";
			
			out.write(arcFileDesc.getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected boolean isNewArcNeeded(){
		if (file.length() > arcMaxSize){
				return true;
		}else{
			return false;
		}
	}		
	
	protected void writeHttp(CrawlURI curi) throws IOException {

		GetMethod get =
			(GetMethod) curi.getAList().getObject("http-transaction");
			
		if (get == null ) {
			// some error occurred; nothing to write
			// TODO: capture some network errors in the ARC file for posterity
			return;
		}
		
		int headersSize = 0;
		int recordLength = 0;
		Header[] headers = get.getResponseHeaders();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for(int i=0; i < headers.length; i++){
			baos.write(headers[i].toExternalForm().getBytes());
		}
		recordLength += baos.size();
		
		// get body so we can calc length for metaline
		byte[] body = get.getResponseBody();
		recordLength += body.length;
		// don't forget the extra CRLF between headers and body
		recordLength += 2;
		
		writeMetaLine(curi,  recordLength);
		
		baos.writeTo(out);
		out.write("\r\n".getBytes());
		out.write(body);
		out.write("\n".getBytes());
	}
	
	public void writeDns(CrawlURI curi) throws IOException {
	
		int recordLength = 0;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		Record[] rrSet = (Record[]) curi.getAList().getObject(A_RRECORD_SET_LABEL);
		
		for(int i=0; i < rrSet.length; i++){
			byte[] record = rrSet[i].rdataToString().getBytes();
			recordLength += record.length;
			
			baos.write(record);
		}
		
		writeMetaLine(curi, recordLength);
		
		baos.writeTo(out);
	 	out.write("\n".getBytes());
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
		if(! buffer.endsWith(File.separator)){
			buffer = new String(buffer + File.separator);
		}
			
		File newDir = new File(buffer);
		
		if(!newDir.exists()){
			try{
				newDir.mkdirs();
				outputDir = buffer;
					
			}catch(Exception e){
				e.printStackTrace();
			}		
		}else{
			outputDir = buffer;
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
