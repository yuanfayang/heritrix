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

import org.archive.crawler.basic.InvalidRecordException;
import org.archive.crawler.datamodel.CrawlerBehavior;

import org.w3c.dom.Node;

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
	
	private int arcMaxSize = 10000000;		// max size we want arc files to be (bytes)
	private String arcPrefix = "archive";			// file prefix for arcs
	private String outputDir = "";						// where should we put them?
	private File file = null;								// file handle
	private FileOutputStream out = null;		// for writing to files
	
	//	append to arc files to assure uniqueness across threads. 
	private static int arcId = 0;						

  	public void initialize(CrawlController c){
  		super.initialize(c);
  
		readConfiguration();
		createNewArcFile();		  		
  	}
  	
  	public void readConfiguration(){
		// set up output directory
		CrawlOrder order = controller.getOrder();
		CrawlerBehavior behavior = order.getBehavior();
		
		
		// retrieve any nodes we think we need from the dom(s)
		Node filePrefix = order.getNodeAt("/crawl-order/arc-file/@prefix");
		Node maxSize = getNodeAt("./arc-files/@max-size-bytes");
		Node path = order.getNodeAt("//disk/@path");
		

		setArcPrefix( 
			( (filePrefix==null) ? arcPrefix : filePrefix.getNodeValue() )
		);
		
		setArcMaxSize(
			( (maxSize==null) ? arcMaxSize : (new Integer(maxSize.getNodeValue())).intValue() )
		);
		
		setOutputDir(
			( (path==null) ? outputDir : path.getNodeValue() )
		);

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
  			
  		// catch disk write errors		
  		}catch(IOException e){
  			e.printStackTrace();
  			
  		// catch errors where we're trying to write a bad record
  		}catch(InvalidRecordException e){
  			e.printStackTrace();
  		}
  	}


  	protected void writeMetaLine(CrawlURI curi, int recordLength) throws InvalidRecordException{

		// TODO sanity check the passed curi before writing
		if (true){	
			
			// figure out content type, truncate at delimiter ';'
			String contentType;
			if(curi.getContentType().indexOf(';') >= 0 ){
				contentType = curi.getContentType().substring(0,curi.getContentType().indexOf(';'));
			}else{
				contentType = curi.getContentType();
			}
			
			String hostIP = curi.getHost().getIP().getHostAddress();
			String dateStamp = get14DigitDate(curi.getAList().getLong(A_FETCH_BEGAN_TIME));			
			
			// fail if we're missing anythign critical
			if(hostIP == null || dateStamp == null){
				throw new InvalidRecordException("missing data elements");
			}		
			
			String metaLineStr = "\n"
					+ curi.getURIString()
					+ " "
					+ hostIP
					+ " "
					+ dateStamp
					+ " "
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
		int uniqueIdentifier = getNextArcId();
		
		String fileName = outputDir + arcPrefix + date +  "-" + uniqueIdentifier + ".arc";
		
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
	
	protected void writeHttp(CrawlURI curi) throws IOException, InvalidRecordException {

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
	
	protected void writeDns(CrawlURI curi) throws IOException, InvalidRecordException {
	
		int recordLength = 0;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// start the record with a 14-digit date per RFC 2540
		byte[] fetchDate = get14DigitDate(curi.getAList().getLong(A_FETCH_BEGAN_TIME)).getBytes();
		baos.write(fetchDate);
		// don't forget the newline
		baos.write("\n".getBytes());
		recordLength = fetchDate.length + 1;  
		
		Record[] rrSet = (Record[]) curi.getAList().getObject(A_RRECORD_SET_LABEL);
		
		if(rrSet != null){
			for(int i=0; i < rrSet.length; i++){
				byte[] record = rrSet[i].toString().getBytes();
				recordLength += record.length;
			
				baos.write(record);
				
				// add the newline between records back in 
				baos.write("\n".getBytes());
				recordLength += 1;	
			}
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
	
	private int getNextArcId(){
		return arcId++;
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
	public static String get14DigitDate(long date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		return sdf.format(new Date(date));
	}
	public static String get12DigitDate(long date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmm");		
		return sdf.format(new Date(date)); 
	}	
	
	
}
