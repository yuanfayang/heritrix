import java.io.IOException;
import java.text.NumberFormat;
import org.archive.io.*;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ArcWarc 
{
	public String heritrix;
    public String arcReader;
    public String job;
    public String jobs;
    public String format;
    public String arc;
    public String arcs;
    public String arcFile;
	public String arcType;

    public String mode;
    public String filter;
    public long offset;
	private int recordStartIndex;
	private int recordEndIndex;
	private Logger logger;
	
    public static String[] modes = {"index","replay","dump","cdx","filter"};

    public static final int BUFFER_SIZE = 1024;
    
	public static String[] mimeTypes = 
	{
		// $WARCTOOLS/warcdump -f IAH-20080909203837-00001-takomaki.local.warc 
		//   | grep "Content-Type" 
		//   | tr -s " " 
		//   | sort 
		//   | uniq
			"image/gif",
			"image/png",
			"text/css",
			"text/dns",
			"text/html",
			"text/plain"
	};

	public ArcWarc() throws IOException {
        this.logger = Logger.getLogger(this.getClass().getName());
	}
        
    void setArcFile(String arcFile) 
    {
    	this.arcFile = arcFile;
    }
    
    void setParams(String job, String arc, Integer offset) {
    	this.job = null;
    	this.arc = null;
    	this.offset = offset;
    }
        
    void setArchiveFormat() 
    throws IOException {
    	String arcFileFmt = getArchiveFileFormat();
    	this.format = arcFileFmt;
    	if (this.isARCFormat()) {
    		this.arcType = "ARC"; 
    	} else {
    		this.arcType = "WARC";
    	}
   	}
    
    public void setFilter(String filter) { this.filter = filter; }

	public void setMode(String mode) { 
		this.mode = mode; 
	}

	public void setOffset(long offset) { this.offset = offset; }
	
    void setRecordRange(int start, int end) {
		this.recordStartIndex = start;
		this.recordEndIndex = end;
	}

	private boolean isARCFormat() {
		if (this.format.equals("arc") || this.format.equals("arcGz")) {
			return true;
		} else {
			return false;
		}
	}
    
	String getArchiveFileFormat()
	throws IOException {
		int l = this.arcFile.length();
		if (this.arcFile.substring(l-3).equals(".gz")) {
			if (this.arcFile.substring(l-8,l-3).equals(".warc")) {
				return "warcGzip";
			} else if (this.arcFile.substring(l-7,l-3).equals(".arc")) {
	    		return "arcGzip";
			} else {
				throw new IOException("unknown archive file format!");
			}
		} else {
			if (this.arcFile.substring(l-5).equals(".warc")) {
				return "warc";
			} else if (this.arcFile.substring(l-4).equals(".arc")) {
	    		return "arc";
			} else {
				throw new IOException("unknown archive file format!");
			}
		}
	}

	private ArchiveReader getReader() 
	throws IOException 
	{
		if (this.isARCFormat()) {
			return ARCReaderFactory.get(this.arcFile);
		} else {
			return WARCReaderFactory.get(this.arcFile);
		}  
	}

	private boolean inRecordRange(long index) {
    	if (index >= this.recordStartIndex && index <= this.recordEndIndex)
    		return true;
    	else
    		return false;
    }
    
	private static boolean filterMimeType(ArchiveRecord r, String filter) 
	{
		// ArchiveRecordHeader h = r.getHeader();
		if (r.getHeader().getMimetype().equals(filter)) 
			return true;
		else
			return false;
	}

	private void logRecordErrors(ArchiveRecord record) {
        Logger logger = Logger.getLogger(this.getClass().getName());
        if (this.isARCFormat()) {
        	ARCRecord arcRecord = (ARCRecord) record;
        	if (arcRecord.hasErrors()) {
        		ArchiveRecordHeader header = record.getHeader();
        		logger.warning("record at offset: " + header.getOffset()
        				+ " has errors: " + arcRecord.getErrors());
        	}
        } else {
        	WARCRecord warcRecord = (WARCRecord) record;
        	warcRecord.getHeader();
        }
	}
	
	private static void outputCdx(ArchiveRecordHeader h) 
	{ 
		Long rl = h.getLength();
		Long ro = h.getOffset();
		String[] hdr = { 
				h.getDate(),
				"-", // Ip
				h.getUrl(),
				h.getMimetype(),
				"-", // status code
				"-", // digest
				ro.toString(),
				rl.toString(),
		};
		for (String fld : hdr) 
			System.out.print(fld + " ");
		System.out.println();
	}

	public void printMetadata(ARCRecord record, ArchiveRecordHeader header) {
		System.out.print( "  Date  : " + header.getDate() + "\n"
				+ "  IP    : " + ((ARCRecordMetaData)header).getIp()  + "\n"
				+ "  URL   : " + header.getUrl() + "\n"
				+ "  MIME  : " + header.getMimetype()   + "\n"
				+ "  Status: " + ((ARCRecordMetaData)header).getStatusCode() + "\n"
				+ "  Digest: " + record.getDigestStr() + "\n"
				+ "  Offset: " + header.getOffset() + "\n"
				+ "  Length: " + header.getLength() + "\n");
	}
	
	public void printMetadata(WARCRecord record, ArchiveRecordHeader header) {
		System.out.print( "  Date  : " + header.getDate() + "\n"
				+ "  IP    : " + header.getHeaderValue("WARC-IP-Address") + "\n"
				+ "  URL   : " + header.getUrl() + "\n"
				+ "  MIME  : " + header.getMimetype() + "\n"
				+ "  Status: " + "-" + "\n"
				+ "  Digest: " + header.getHeaderValue("WARC-Payload-Digest") + "\n"
				+ "  Offset: " + header.getOffset() + "\n"
				+ "  Length: " + header.getLength() + "\n");
	}
	
	public void printInfo() {
		System.out.println(this.getClass().getName());
		if (this.job != null) 
			System.out.println("  job:     " + this.job);
		if (this.arc != null) 
			System.out.println("  archive: " + this.arc);
		System.out.println("  file:    " + this.arcFile);
		System.out.println("  format:  " + this.format);
		System.out.println("  mode:    " + this.mode);
		if (this.mode.equals("filter"))
			System.out.println("  filter:  " + this.filter);
		if (this.mode.equals("fetch"))
		System.out.println("  offset:  " + this.offset);
		if (this.mode.equals("filter") || this.mode.equals("cdx") || this.mode.equals("dump"))
			System.out.println("  range:   " + "[" + this.recordStartIndex + "," + this.recordEndIndex + "]");
	}

	public void readArchive()
	throws IOException {

        long j = 0;
        long filterCount = 0;
    	ArchiveReader reader = this.getReader();

        if (this.mode.equals("index")) {
        	// operates on a record, parse HTTP header only
			System.out.println("INDEX " + this.arcType 
					+ " record at offset: " + offset);
			if (this.isARCFormat()) {
				this.indexRecord((ARCReader)reader);
			} else {
				this.indexRecord((WARCReader)reader);
			}
        } else if (this.mode.equals("replay")) {
        	// operates on a record, skip header and read  
        	System.out.println("REPLAY " + this.arcType 
        			+ " record at offset: " + offset + "");
        	if (this.isARCFormat()) {
        		this.replayRecord((ARCReader)reader);
        	} else {
        		this.replayRecord((WARCReader)reader);
        	}
        } else if (this.mode.equals("dump")) { // dump each record - messy!
			System.out.println("DUMP record at offset: " + offset);
        	for (ArchiveRecord record : reader) {
				j++;
				if (inRecordRange(j)) {
					System.out.println(mode + " [" + j + "] ");
					record.dump();
				} 
	    		if (j > this.recordEndIndex)
	    			break;
			}
        } else if (this.mode.equals("cdx")) { // output CDX
			for (ArchiveRecord record : reader) {
				j++;
				if (inRecordRange(j)) {
					System.out.print(mode + " [" + j + "] ");
					logRecordErrors(record);
					outputCdx(record.getHeader());
				}
	    		if (j > this.recordEndIndex)
	    			break;
			}
        } else if (this.mode.equals("filter")) { // filter MIME type
        	// ARCReader reader = (ARCReader)ArchiveReaderFactory.get(this.arcFile);
        	for (ArchiveRecord record : reader) {
        		j++;
        		if (inRecordRange(j)) {
        			if (filterMimeType(record,this.filter)==true) {
        				System.out.print(mode + " [" + j + "] ");
        				outputCdx(record.getHeader());
        				filterCount++;
        			}
        		}
        		if (j > this.recordEndIndex)
        			break;
        	}
        	double filterPercent = (double)filterCount/j;
        	NumberFormat filterPercentFmt = NumberFormat.getPercentInstance();
        	filterPercentFmt.setMinimumFractionDigits(2);
        	System.out.println("\n========== found: " 
        			+ filterCount + "/" + j + " = " 
        			+ filterPercentFmt.format(filterPercent)
        			+ " mimetype=" + filter 
        			+ " records. ");
        } else { // do nothing, but count iterations
        	System.out.println();
        	for (ArchiveRecord record : reader) {
        		j++;
        		logRecordErrors(record);
        		System.out.print(".");
        		if ((j % 100) == 0) 
        			System.out.print("[" + j+ "]\n");
        	}
        }
        if (this.offset == -1) {
        	System.out.println("\n========== found: " + j + " records. ");
        }
		System.out.println("\n========== Done.");
	}

	private void replayRecord(ARCReader arcReader) throws IOException {
    	arcReader.setStrict(true);
    	ARCRecord arcRecord = (ARCRecord) arcReader.get();
    	arcRecord.skipHttpHeader();
    	if (arcRecord.hasErrors()) {
    		logger.warning("record has errors: " + arcRecord.getErrors());
    	}
    	byte[] buffer = new byte[BUFFER_SIZE];
    	if (arcRecord.available() > 0) {
    		// for (int r = -1; (r = arcRecord.read(buffer, 0, BUFFER_SIZE)) != -1;) {
    		int r = -1;
    		while((r = arcRecord.read(buffer, 0, BUFFER_SIZE)) != -1) {
    			// os.write(buffer, 0, r);
    			System.out.write(buffer, 0, r);
    		}
    	} else {
    		System.out.println("record bytes available: " 
    				+ arcRecord.available());
    	}
	}
	
	private void replayRecord(WARCReader warcReader) throws IOException {
		warcReader.setStrict(true);
		WARCRecord warcRecord = (WARCRecord) warcReader.get();
    	byte[] buffer = new byte[BUFFER_SIZE];
    	if (warcRecord.available() > 0) {
    		int r = -1;
    		while((r = warcRecord.read(buffer, 0, BUFFER_SIZE)) != -1) {
    			System.out.write(buffer, 0, r);
    		}
    	}
		System.out.println("record bytes available: "
				+ warcRecord.available());
	}

	private void indexRecord(ARCReader arcReader) throws IOException {
		arcReader.setStrict(true);
		arcReader.setParseHttpHeaders(true);
		ARCRecord arcRecord = (ARCRecord) arcReader.get();
		ArchiveRecordHeader header = arcRecord.getHeader();
		if (arcRecord.hasErrors()) 
			logger.warning("========== record has errors: " 
					+ arcRecord.getErrors());
		System.out.println("========== dumping HTTP header:");
		arcRecord.dumpHttpHeader();
		System.out.println("========== selected metadata:");
		arcRecord.close(); // must close record to get digest
		printMetadata(arcRecord,header);
		System.out.println("========== getting metadata:");
		System.out.println(arcRecord.getMetaData());
		System.out.println("\n"
				+ "record length declared: " 
				+ header.getLength()
				+ "header bytes read     : " 
				+ arcRecord.httpHeaderBytesRead);
	}

	private void indexRecord(WARCReader warcReader) throws IOException {
		warcReader.setStrict(true);
		// warcReader.setParseHttpHeaders(true);
		WARCRecord warcRecord = (WARCRecord)warcReader.get(this.offset);
		ArchiveRecordHeader header = warcRecord.getHeader();
		System.out.println("========== selected metadata:");
		warcRecord.close(); // must close record to get digest
		printMetadata(warcRecord,header);
		System.out.println("========== header: \n" + header);
	}

}
