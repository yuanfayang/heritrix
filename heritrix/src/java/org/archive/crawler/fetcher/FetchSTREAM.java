package org.archive.crawler.fetcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.fetcher.MPlayerIdentify;
import org.archive.crawler.fetcher.MPlayerDumpstream;
import org.archive.util.ArchiveUtils;

/**
 * STREAM fetcher that uses MPlayer.
 *
 * @author Nicolas
 * @version $Id$
 */
public class FetchSTREAM extends Processor
implements CoreAttributeConstants, FetchStatusCodes, CrawlStatusListener {
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(FetchSTREAM.class,1);
    
    private static Logger LOGGER =
    	Logger.getLogger(FetchSTREAM.class.getName());
   
    //private static Integer DEFAULT_TIMEOUT_SECONDS = new Integer(1200);
    
    public final static String FILE_KEY =
    	FetchSTREAM.class.getName() + ".streamFile";
    
    //private File mplayer = null;
    
    private static final String [] SCHEMES_ARRAY = {"rtsp", "pnm", "mms"};
    public static final List SCHEME;
    static {
    	SCHEME = Arrays.asList(SCHEMES_ARRAY);
    }

    /**
     * Count of crawl uris handled.
     * Would like to be 'long', but longs aren't atomic
     */
    private int curisHandled = 0;

    /**
     * Constructor.
     *
     * @param name Name of this processor.
     */
    public FetchSTREAM(String name) {
        super(name, "STREAM Fetcher");
    }

    protected void innerProcess(final CrawlURI curi)
    throws InterruptedException {
    	
        int statusCode = -2;
        int exitVal = -2;
    	
        if (!canFetch(curi)) {
            // Cannot fetch this, due to protocol, retries, or other problems
            return;
        }
        this.curisHandled++;

        // Note begin time
        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());
        
        final File streamFile = new File(this.getController().getScratchDisk(),
        	"stream" + ((ToeThread)Thread.currentThread()).getSerialNumber());
        curi.putObject(FILE_KEY, streamFile);
        
        if (LOGGER.isLoggable(Level.FINE)) {
        	LOGGER.fine("Passing " + curi.toString() + " to mplayer " +
               "LOCATION OF MPLAYER" + " using file " +
               streamFile.toString());
        }
        
        // Send URL to MPLAYER.  Wait.  Later add timeout.
        // Check the return code from MPLAYER.  Check for success or
        // failure.
        	
        // Identify STREAM (URL)
        // Get Stream Length Time for timeout
        MPlayerIdentify mpi = new MPlayerIdentify();
        statusCode = mpi.identify(curi);
        	
        if (statusCode == 0) {
        	MPlayerDumpstream mpd = new MPlayerDumpstream();
        	exitVal = mpd.dumpstream(curi, streamFile.getAbsolutePath());
        		
        	if (exitVal == 0) {
        		long size = streamFile.length();
        		curi.setContentSize(size);
        		curi.setFetchStatus(S_STREAM_SUCCESS);
        	}
        	else {
        		// requeue this uri
        		curi.setFetchStatus(S_UNATTEMPTED);
        	}
        }
        else {
        	// skip this uri
        	switch (statusCode) {
        	case 1: curi.setFetchStatus(S_TIMEOUT); break;
        	case 2: curi.setFetchStatus(S_UNFETCHABLE_URI); break;
        	}
        }
        	        	
        if (LOGGER.isLoggable(Level.FINE)) {
        	LOGGER.fine("Result of " + curi.toString() + " to mplayer " +
        			"LOCATION OF MPLAYER" + " using file " +
        			streamFile.toString());
            
        	// Make a method to populate curi with result of fetch
        	// including status code, mimetype, and error from MPLAYER     
        }
    }

    /**
     * Can this processor fetch the given CrawlURI. May set a fetch
     * status if this processor would usually handle the CrawlURI,
     * but cannot in this instance.
     *
     * @param curi
     * @return True if processor can fetch.
     */
    private boolean canFetch(CrawlURI curi) {
    	String scheme = curi.getUURI().getScheme();
    	return (!(scheme.equals("rtsp") || scheme.equals("mms") ||
    			scheme.equals("pnm")))? false: true;
    }

    public void initialTasks() {
        super.initialTasks();
        // TEST MPLAYER AND PRINT OUT VITAL INFORMATION ABOUT MPLAYER
        /*
         MPlayer mp = new MPlayer();
         exitVal = mp.check();
         if (exitVal != 0) {
         	// error: can't fetch streams without mplayer
         }
         */ 
    }
    
    public void finalTasks() {
    	// RUN ONCE.  CLEANUP.
    	// if running mplayer instances still does exist, kill them
    	// cleanup all dumped stream files
        super.finalTasks();
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: " + this.getClass().getName() + "\n");
        ret.append("  Function:          Fetch STREAM URIs\n");
        ret.append("  CrawlURIs handled: " + this.curisHandled + "\n");

        return ret.toString();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlCheckpoint(File checkpointDir) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
    }
}