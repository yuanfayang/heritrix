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
package is.hi.bok.crawler.ar.processor;

import is.hi.bok.crawler.ar.ARAttributeConstants;

import java.util.logging.Logger;

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
public class ChangeEvaluator extends Processor implements ARAttributeConstants {

    private static final Logger logger =
        Logger.getLogger(ChangeEvaluator.class.getName());

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
            logger.finest("Not handling " + curi.getURIString() + ", did not " +
                    "succeed.");
            return;
        }
        
        String currentDigest = null;
        Object digest = curi.getContentDigest();
        if(digest!=null) {
            currentDigest = Base32.encode((byte[])digest);
        }

        String oldDigest = null;
        if(curi.containsKey(A_LAST_CONTENT_DIGEST)){
            oldDigest = curi.getString(A_LAST_CONTENT_DIGEST);
        }

        // Compare the String representation of the byte arrays.
        if (currentDigest == null && oldDigest == null) {
            // Both are null, can't do a thing
            logger.finer("On " + curi.getURIString() + " both digest are null");
            return;
        } else if(currentDigest != null && oldDigest != null 
                && currentDigest.equals(oldDigest)){ 
            // If equal, we have just downloaded a duplicate.
            logger.finer("On " + curi.getURIString() + " both digest are " +
                    "equal. Old: " + oldDigest + ", new: " + currentDigest);;
            curi.setContentState(CrawlURI.CONTENT_UNCHANGED);
            // TODO: In the future processors should take not of the content
            //       state, removing the need for the following 'skip'
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            // Set content size to zero, we are not going to 'write it to disk'
            curi.setContentSize(0);
        } else {
            // Document has changed
            logger.finer("On " + curi.getURIString() + " digest are not " +
                    "equal. Old: " + (oldDigest==null? "null" : oldDigest) + 
                    ", new: " + (currentDigest==null? "null" : currentDigest));
            // currentDigest may be null, that probably means a failed download
            curi.setContentState(CrawlURI.CONTENT_CHANGED);
            curi.putString(A_LAST_CONTENT_DIGEST,currentDigest); 
        }
        
        /* Update visit and version counters */
        // Update visits
        int visits = 1;
        if(curi.containsKey(A_NUMBER_OF_VISITS)){
            visits = curi.getInt(A_NUMBER_OF_VISITS) + 1;
        }
        curi.putInt(A_NUMBER_OF_VISITS,visits);

        // Update versions
        if(curi.getContentState() == CrawlURI.CONTENT_CHANGED){
            int versions = 1;
            if(curi.containsKey(A_NUMBER_OF_VERSIONS)){
                versions = curi.getInt(A_NUMBER_OF_VERSIONS) + 1;
            }
            curi.putInt(A_NUMBER_OF_VERSIONS,versions);
        }
    }
}
