/* Marker
 *
 * $Id$
 *
 * Created on Feb 28, 2004
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
package org.archive.crawler.frontier;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.FrontierMarker;

/**
 * An implementation of <code>URIFrontierMarker</code> suitable for the
 * <code>URIFrontier</code> implementation in this package.
 * @author Kristinn Sigurdsson
 * @see org.archive.crawler.framework.FrontierMarker
 * @see org.archive.crawler.framework.Frontier
 * @see org.archive.crawler.frontier.HostQueuesFrontier
 * @deprecated As of 1.4.  No replacement.
 */
public class HostQueuesFrontierMarker implements FrontierMarker {

    String match;
    Pattern p;
    protected boolean inCacheOnly;
    protected boolean hasNext;
    protected long nextItemNumber;
    protected ArrayList keyQueues;
    // -1 -> PendingQueue, > 0 -> KeyQueue at index...
    protected int currentQueue;
    // The absolute position (ignoring matches or no matches) in the current
    // queue to BEGIN.
    protected long absolutePositionInCurrentQueue;

    public HostQueuesFrontierMarker(String match, boolean inCacheOnly, ArrayList keyqueues){
        this.match = match;
        this.inCacheOnly = inCacheOnly;
        p = Pattern.compile(match);
        nextItemNumber=1;
        this.keyQueues = keyqueues;
        currentQueue = 0;
        absolutePositionInCurrentQueue = 0;
        hasNext = true;
    }

    public void nextQueue() {
        if(++currentQueue==keyQueues.size()){
            currentQueue = -1;
        }
        absolutePositionInCurrentQueue = 0;
    }

    public boolean match(CandidateURI caURI){
        return p.matcher(caURI.getURIString()).matches();
    }

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Marker#getNextItemNumber()
	 */
	public long getNextItemNumber() {
		return nextItemNumber;
	}
    
    /**
     * Increment next item number.
     */
    public synchronized void incrementNextItemNumber() {
    	    this.nextItemNumber++;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontierMarker#getMatchExpression()
     */
    public String getMatchExpression() {
        return match;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontierMarker#hasNext()
     */
    public boolean hasNext() {
        return hasNext;
    }

	/**
	 * @return Returns the currentQueue.
	 */
	public int getCurrentQueue() {
		return currentQueue;
	}
	/**
	 * @return Returns the keyQueues.
	 */
	public ArrayList getKeyQueues() {
		return keyQueues;
	}
	/**
	 * @return Returns the absolutePositionInCurrentQueue.
	 */
	public long getAbsolutePositionInCurrentQueue() {
		return absolutePositionInCurrentQueue;
	}
	/**
	 * @return Returns the inCacheOnly.
	 */
	public boolean isInCacheOnly() {
		return inCacheOnly;
	}
	/**
	 * @param absolutePositionInCurrentQueue The absolutePositionInCurrentQueue
     * to set.
	 */
	public synchronized void setAbsolutePositionInCurrentQueue(
			long absolutePositionInCurrentQueue) {
		this.absolutePositionInCurrentQueue = absolutePositionInCurrentQueue;
	}
}