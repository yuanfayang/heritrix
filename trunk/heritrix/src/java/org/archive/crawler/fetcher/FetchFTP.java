/* FetchFTP.java
 * 
 * $Id$
 * 
 * Created on Mar 17, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.fetcher;

import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;
import org.archive.crawler.framework.Processor;
;

/**
 * A processor that fetches documents from FTP servers.
 * 
 * <p> Makes use of the 
 * <a href="http://jakarta.apache.org/commons/net/">Apache Jakarta Commons Net</a>
 * library.
 * 
 * @author Kristinn Sigurdsson
 */
public class FetchFTP extends Processor {

    // Default socket timeout
    public static final String ATTR_SOTIMEOUT_MS = "sotimeout-ms";
    private static Integer DEFAULT_SOTIMEOUT_MS = new Integer(20000);
    
    
    /**
     * @param name Identifing name in the settings framework
     */
    public FetchFTP(String name) {
        super(name, "FTP Fetcher");
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_SOTIMEOUT_MS,
                "If the socket is unresponsive for this number of milliseconds, "
                + "give up (and retry later)", DEFAULT_SOTIMEOUT_MS));
        t.setExpertSetting(true);
        // Add any configurable settings here
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {
        if(shouldFetch(curi)==false){
            // Shouldn't get this document (probably not an FTP document),
            // might also be limited by retries or other arbitrary factors.
            return;
        }
        // TODO: Identify file / directory listing?
        // TODO: How do we handle directory listings?
        // TODO: Identify ASCII or Binary transfer? Maybe just set binary?
        // TODO: Fetch file / directory listing.
        FTPClient ftpc = new FTPClient();
        ftpc.setDefaultPort(FTP.DEFAULT_PORT);
        try {
            ftpc.connect(curi.getUURI().getHost());
            // TODO: Handling FTPs on ports other then 21?

            if(FTPReply.isPositiveCompletion(ftpc.getReplyCode())==false) {
              ftpc.disconnect();
              // TODO: Record failure. Should maybe be retryable?
              return;
            }
            
            ftpc.setSoTimeout(getSocketTimeout(curi));
            ftpc.setFileTransferMode(FTP.BINARY_FILE_TYPE); //TODO: Is this ok?
            // TODO: How to handle inputstreams????
            ftpc.retrieveFileStream(curi.getUURI().getPath());
            // TODO: Maybe we should 'listNames()' and store them somewhere
            //       for the FTPDirectoryExtractor to manage?
            //       Should there be a seperate extractor or just a toggle on
            //       the fetcher since it requires communication with the 
            //       server anyway? If so what rules should apply.
            // TODO: Determine mime type 
        } catch (IOException e){
            //TODO: Error handling.
            e.printStackTrace();
        }
    }

    /**
     * Verifies if the specified curi should be fetched by this processor.
     * @return true if this processor should fetch the specified curi.
     */
    private boolean shouldFetch(CrawlURI curi) {
        String scheme = curi.getUURI().getScheme();
        if (scheme.equals("ftp") == false) {
            // Not an ftp.
            return false;
        }
        return true;
    }

    private int getSocketTimeout(CrawlURI curi) {
        Integer res;
        try {
            res = (Integer) getAttribute(ATTR_SOTIMEOUT_MS, curi);
        } catch (Exception e) {
            res = DEFAULT_SOTIMEOUT_MS;
        }
        return res.intValue();
    }
}
