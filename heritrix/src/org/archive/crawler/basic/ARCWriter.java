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
	
	private int arcMaxSize = 100000000;		// max size we want arc files to be (bytes)
	private String arcPrefix = "IAH";			// file prefix for arcs
	private String outputDir = "";						// where should we put them?
	private File file = null;								// file handle
	private OutputStream arcOut = null;		// for writing to files
	private OutputStream out = null;
	private boolean useCompression = true;	// should we compress the output?
	
	//	append to arc files to assure uniqueness across threads in 
	//  the event multiple arcwriter exist and are creating files concurrently
	private static int arcId = 0;						
	
  	public void initialize(CrawlController c){
  		super.initialize(c);
  
		readConfiguration();
		
		try{
			createNewArcFile();		  		
		}catch(IOException e){
			e.printStackTrace();
		}
  	}
  	
  	protected void readConfiguration(){
		// set up output directory
		CrawlOrder order = controller.getOrder();
		
		setUseCompression(getBooleanAt("@compress",false));
		setArcPrefix(getStringAt("@prefix",arcPrefix));
		setArcMaxSize(getIntAt("@max-size-bytes",arcMaxSize));
		setOutputDir(getStringAt("@path",outputDir));

  	}

	/**
  	 * Takes a CrawlURI and generates an arc record, writing it
  	 * to disk.  Currently
  	 * this method understands the following uri types: dns, http
  	 */
  	protected synchronized void innerProcess(CrawlURI curi){
  		
  		// if  there was a failure, or we haven't fetched the resource yet, return
		if(curi.getFetchStatus()<=0){
			return;
		}
		
		// create a new arc file if the existing one is too big
		if(isNewArcNeeded()) {
			try{
				createNewArcFile();
			}catch(Exception e){
				//TODO deal better with not being able to write files to disk (a serious problem)
				e.printStackTrace();
			}
		}

  		// find the write protocol and write this sucker
  		String scheme = curi.getUURI().getScheme();
  	
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
  	
  	/**
  	 *  Write a standard arc metaline
  	 * @param curi
  	 * @param recordLength
  	 * @throws InvalidRecordException
  	 * @throws IOException
  	 */
  	protected void writeMetaLine(CrawlURI curi, int recordLength) throws InvalidRecordException, IOException{

		// TODO sanity check the passed curi before writing

		// figure out content type, truncate at delimiter ';'
		String contentType = curi.getContentType();
		if(contentType==null) {
			contentType = "no-type"; // per ARC spec
		} else if(curi.getContentType().indexOf(';') >= 0 ){
			contentType = contentType.substring(0,contentType.indexOf(';'));
		}
		
		String hostIP = curi.getServer().getHost().getIP().getHostAddress();
		String dateStamp = ArchiveUtils.get14DigitDate(curi.getAList().getLong(A_FETCH_BEGAN_TIME));			
		
		// fail if we're missing anythign critical
		if(hostIP == null || dateStamp == null){
			throw new InvalidRecordException("missing data elements");
		}		
		
		String metaLineStr = 
				curi.getURIString()
				+ " "
				+ hostIP
				+ " "
				+ dateStamp
				+ " "
				+  contentType
				+ " "
				+ recordLength
				+ "\n";	
				
		try{	
			out.write(metaLineStr.getBytes());  		
		
		}catch(IOException e){
			e.printStackTrace();
		}
			
		// store size since we've already calculated it and it can be used 
		// to generate interesting statistics
		//curi.setContentSize((long)recordLength);
 	}
  	
	protected void createNewArcFile() throws IOException {

		String date = ArchiveUtils.get14DigitDate();
		int uniqueIdentifier = getNextArcId();
		
		String fileExtension = ".arc";
		if(useCompression()){
			fileExtension += ".gz";
		}
		
		String fileName = arcPrefix + date +  "-" + uniqueIdentifier + fileExtension;
		String fqFileName = outputDir + fileName;
		
		try {
			if(out != null){
				out.close();
			}
							
			file = new File(fqFileName);
			arcOut = new FileOutputStream(file);
			
			if(useCompression()){
				out = new IAGZIPOutputStream(arcOut);
			}else{
				out = arcOut;
			}
					
			String arcFileDesc =
				"filedesc://"
					+ fileName
					+ " 0.0.0.0 "
					+ date
					+ " text/plain 77\n1 0 InternetArchive\nURL IP-address Archive-date Content-type Archive-length\n\n\n";
			
			out.write(arcFileDesc.getBytes());
		
			if(useCompression()){
				((IAGZIPOutputStream)out).endCompressionBlock();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	protected boolean isNewArcNeeded(){
		if (file.length() > arcMaxSize){
				return true;
		}else{
			return false;
		}
	}		
	
	public void setUseCompression(boolean use){
		useCompression = use;
	}
	
	public boolean useCompression(){
		return useCompression;
	}
	
	protected void writeHttp(CrawlURI curi) throws IOException, InvalidRecordException {

		if (curi.getFetchStatus()<=0) {
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

		if (recordLength==0) {
			// write nothing
			return;
		}
		
		prewrite();
		writeMetaLine(curi,  recordLength);
		
		ReplayInputStream capture = get.getHttpRecorder().getRecordedInput().getReplayInputStream();
		try {
			capture.readFullyTo(out);
			long remaining = capture.remaining();
			if(remaining>0) {
				DevUtils.warnHandle(new Throwable("n/a"),"gap between expected and actual: "+remaining+"\n"+DevUtils.extraInfo());
				while(remaining>0) {
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
		if(useCompression()){
			((IAGZIPOutputStream)out).endCompressionBlock();
		}
	}

	/**
	 * Restart GZIP compression, if necessary
	 */
	private void prewrite() throws IOException {
		if(useCompression()){ 			
			// zip each record individually
			 ((IAGZIPOutputStream)out).startCompressionBlock();

		} // else skip the special gzip jive and just write to a FileOutputStream
	}

	protected void writeDns(CrawlURI curi) throws IOException, InvalidRecordException {
	
		int recordLength = 0;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// start the record with a 14-digit date per RFC 2540
		byte[] fetchDate = ArchiveUtils.get14DigitDate(curi.getAList().getLong(A_FETCH_BEGAN_TIME)).getBytes();
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
		
		prewrite();
		writeMetaLine(curi, recordLength);
	
		// save the calculated contentSize for logging purposes
		// TODO handle this need more sensibly
		curi.setContentSize((long)recordLength);
	
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
	
	private int getNextArcId(){
		return arcId++;
	}

	public void setOutputDir(String buffer) {

		// make sure it's got a trailing file.separator so the
		// dir is not treated as a file prefix
		if ((buffer.length() > 0) && !buffer.endsWith(File.separator)) {
			buffer = new String(buffer + File.separator);
		}

		File newDir = new File(controller.getDisk(), buffer);

		if (!newDir.exists()) {
			try {
				newDir.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		outputDir = newDir.getAbsolutePath()+ File.separatorChar;
	}
}
