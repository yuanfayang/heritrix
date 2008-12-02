import java.io.IOException;
import java.text.NumberFormat;
import org.archive.io.*;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.warc.WARCReaderFactory;
import java.util.HashMap;
import java.util.Map;

public class ArcWarc 
{
	private String jobParam = "completed-basic_seed_sites-20080909192242";
	private String arcParams[] = {jobParam,"IAH-20080909203837-00001-takomaki.local.arc","24830"};
	private String arcGzParams[] = {jobParam,"IAH-20080909203837-00001-takomaki.local.arc.gz","24830"};
	private String warcParams[] = {jobParam,"IAH-20080909203837-00001-takomaki.local.warc","82491"};
	private String warcGzParams[] = {jobParam,"IAH-20080909203837-00001-takomaki.local.warc.gz","26302"};
	private Map<String,String[]> jobParams = new HashMap<String,String[]>();
	
    public String heritrix;
    public String arcReader;
    public String job;
    public String jobs;
    public String format;
    public String arc;
    public String arcs;
    public String arcFile;

    public String mode;
    public String filter;
    public long offset;
	private int recordStartIndex;
	private int recordEndIndex;
	
    public static String[] modes = {"fetch","dump","cdx","filter"};
    
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

	public long randomIndices[] = {3,7,31,127};

	public ArcWarc()
    throws IOException
    {
    }
        
    void setArcFile(String arcFile) 
    {
    	this.arcFile = arcFile;
    }
    
    void setParams(String job, String arc, Integer offset) {
    	this.job = "N/A";
    	this.arc = "N/A";
    	this.offset = offset;
    }
        
    void setDefaultParams(String arcType) {
    	this.jobParams.put("arc", arcParams);
    	this.jobParams.put("arcgz", arcGzParams);
    	this.jobParams.put("warc", warcParams);
    	this.jobParams.put("warcgz", warcGzParams);
    	this.job = this.jobParams.get(arcType)[0];
    	this.arc = this.jobParams.get(arcType)[1];
    	this.offset = (Long.valueOf(jobParams.get(arcType)[2])).longValue();
    }

    void setDefaultArcFile() {
    	this.heritrix  = "/Users/steve/Documents/dist/heritrix-2.0.1-dist";
    	this.arcReader = this.heritrix + "/bin/arcreader";
    	this.jobs      = this.heritrix + "/jobs";
    	this.arcs      = this.jobs + "/" + this.job + "/arcs";
    	this.arcFile   = this.arcs + "/" + this.arc;
    }

    void setArchiveFormat() 
    throws IOException {
    	String arcFileFmt = getArchiveFileFormat();
    	this.format = arcFileFmt;
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
		if (this.format.equals("arc") || this.format.equals("arcGzip")) {
			return ArchiveReaderFactory.get(this.arcFile);
		} else if (this.format.equals("warc") || this.format.equals("warcGzip")) {
			return WARCReaderFactory.get(this.arcFile);
		} else {
			throw new IOException("unknown format: " + this.format);
		}
	}

	private long getRandomOffset() {
		return this.randomIndices[0];
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

	private static void outputCdx(ArchiveRecordHeader h) 
	{ 
		Long rl = h.getLength();
		Long ro = h.getOffset();
		String[] hdr = { 
				h.getDate(),
				"-", // Ip4
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

	public void printInfo() {
		System.out.println("ArcWarcTests");
		System.out.println("  job:     " + this.job);
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

	public void testArc()
	throws IOException {

        String  recordFormat = ArchiveReader.DUMP;
        boolean strict = false;
        boolean parse = false;

        long    j = 0;
        long    filterCount = 0;

        ArchiveReader a = getReader();
        
        if (this.mode == "fetch") { // fetch single record
			System.out.println("fetching record at offset: " + offset + "");
			strict = true;
			parse = true;
			ARCReader r = ARCReaderFactory.get(this.arcFile,this.offset);
			r.setStrict(strict);
			r.setParseHttpHeaders(parse);
			r.outputRecord(recordFormat);
        } else if (this.mode.equals("dump")) { // dump each record - messy!
			for (ArchiveRecord r : a) {
				j++;
				if (inRecordRange(j)) {
					System.out.println(mode + " [" + j + "] ");
					r.dump();
				}
			}
        } else if (this.mode.equals("cdx")) { // output CDX
			for (ArchiveRecord r : a) {
				j++;
				if (inRecordRange(j)) {
					System.out.print(mode + " [" + j + "] ");
					outputCdx(r.getHeader());
				}
			}
        } else if (this.mode.equals("filter")) { // filter MIME type
        	for (ArchiveRecord r : a) {
        		j++;
        		if (inRecordRange(j)) {
        			if (filterMimeType(r,this.filter)==true) {
        				System.out.print(mode + " [" + j + "] ");
        				outputCdx(r.getHeader());
        				filterCount++;
        			}
        		}
        	}
        	double filterPercent = (double)filterCount/j;
        	NumberFormat filterPercentFmt = NumberFormat.getPercentInstance();
        	filterPercentFmt.setMinimumFractionDigits(2);
        	System.out.println("found (" 
        			+ filterCount + "/" + j + " = " 
        			+ filterPercentFmt.format(filterPercent)
        			+ ") mimetype=" + filter 
        			+ " records. ");
        } else { // do nothing, but count iterations 
        	for (ArchiveRecord r : a) {
        		j++;
        		r.getHeader();
        		System.out.print(".");
        		if ((j % 100) == 0) 
        			System.out.print("[" + j+ "]\n");
        	}
        }
        if (! this.mode.equals("filter"))
        	System.out.println("found (" + j + ") records. ");
		System.out.println("Done.");
	}
	
	
}
