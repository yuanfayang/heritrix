/* ChangeEvaluator
 * 
 * $Id$
 * 
 * Created on 11.11.2004
 *
 * Copyright (C) 2004 Kristinn Sigurdsson.
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
package is.hi.bok.crawler.processor;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.Base32;

/**
 * This processor examins compares the CrawlURI's current 
 * {@link org.archive.crawler.datamodel.CrawlURI#contentDigest content digest}
 * with the one from a previous crawl. If they are equal, then further 
 * processing is skipped (going straight to the post processor chain) and the
 * CrawlURI is marked appropriately.
 *
 * @author Kristinn Sigurdsson
 */
public class ChangeEvaluator extends Processor {

    // CrawlURI AList constants
    /** Designates a field in the CrawlURIs AList for the content digest of
     *  an earlier visit. */
    public static final String A_LAST_CONTENT_DIGEST = "last-content-digest";
    /** Dsignates a field in the CrawlURIs AList to write information about
     *  wheter newly fetched content differs from content fetched earlier */
    public static final String A_CHANGE_STATUS = "change-status";
    
    public static final int UNCHANGED = 0;
    public static final int CHANGED = 1;
    
    /**
     * Constructor
     * @param name The name of the module
     */
    public ChangeEvaluator(String name) {
        super(name, "This processor examins compares the CrawlURI's current " +
                "content digest with the one from a previous crawl. If they " +
                "are equal, then further processing is skipped (going " +
                "straight to the post processor chain) and the CrawlURI is " +
                "marked appropriately.\nShould be located at the start of " +
                "the Extractor chain.");

    }

    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        if(curi.isSuccess() == false){
            // Early return. No point in doing comparison on failed downloads.
            return;
        }
        
        // TODO: Consider, will digest (hash) always be byte array? Should there be an interface for 'digest' classes that wrap such information?
        String currentDigest = null;
        String oldDigest = null;
        Object tmp = curi.getAList().getObject(A_LAST_CONTENT_DIGEST);
        if(tmp != null){
            currentDigest = Base32.encode((byte[])curi.getContentDigest());
            oldDigest = Base32.encode((byte[])curi.getAList().getObject(A_LAST_CONTENT_DIGEST));
        }
        
        // Compare the String representation of the byte arrays.
        if (currentDigest == null && oldDigest == null) {
            // Both are null, can't do a thing
            return;
        } else if(currentDigest != null && oldDigest != null && currentDigest.equals(oldDigest)){ 
            // If equal, we have just downloaded a duplicate.
            // TODO: There should be a status for this! curi.setFetchStatus();
            curi.getAList().putInt(A_CHANGE_STATUS, CHANGED);
            // TODO: Maybe only skip to Writing chain? Configurable?
            curi.skipToProcessorChain(getController().getPostprocessorChain());
        } else {
            // Document has changed
            // currentDigest may be null, that probably means a failed download
            curi.getAList().putInt(A_CHANGE_STATUS, UNCHANGED);
            curi.getAList().putObject(A_LAST_CONTENT_DIGEST,currentDigest); 
        }
    }
}
