package org.archive.crawler.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;


/**
 * Writes documents captured via FTP to ARC files.
 * 
 * @author pjack
 * @deprecated  The superclass will be modified to handle FTP in the near future
 */
public class FTPARCWriterProcessor extends ARCWriterProcessor {

    
    private static final long serialVersionUID = -4811445610539188322L;

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    
    public FTPARCWriterProcessor(String name) {
        super(name);
    }
    
    
    public void innerProcess(CrawlURI curi) {
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return;
        }
        
        // If no content, don't write record.
        int recordLength = (int)curi.getContentSize();
        if (recordLength <= 0) {
            // Write nothing.
            return;
        }
        
        String scheme = curi.getUURI().getScheme().toLowerCase();
        if (!scheme.equals("ftp")) {
            return;
        }
        
        try {
            InputStream is = curi.getHttpRecorder().getRecordedInput().
             getReplayInputStream();
            write(curi, recordLength, is, getHostAddress(curi));
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "WriteRecord: " + curi);
            logger.log(Level.SEVERE, "Failed write of Record: " + curi, e);
        }
    }
}
