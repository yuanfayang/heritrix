/* UriUniqFilter
 * 
 * Created on Apr 17, 2003
 * 
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.datamodel;


/**
 * A UriUniqFilter passes URI objects to a destination
 * (receiver) if the passed URI object has not been previously seen.
 * 
 * If already seen, the passed URI object is dropped.
 *
 * <p>For efficiency in comparison against a large history of
 * seen URIs, URI objects may not be passed immediately, unless 
 * the addNow() is used or a flush() is forced.
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public interface UriUniqFilter {
    
    /**
     * @return Count of already seen URIs.
     */
    public long count();
    
    /**
     * Count of items added, but not yet filtered in or out. 
     * 
     * Some implementations may buffer up large numbers of pending
     * items to be evaluated in a later large batch/scan/merge with 
     * disk files. 
     * 
     * @return Count of items added not yet evaluated 
     */
    public long pending();

    /**
     * Receiver of uniq URIs.
     * 
     * Items that have not been seen before are pass through to this object.
     * @param receiver Object that will be passed items. Must implement
     * HasUriReceiver interface.
     */
    public void setDestination(HasUriReceiver receiver);
    
    /**
     * Add given uri, if not already present.
     * @param uri Item to add.
     * @param canonical The canonicalized version of <code>uri</code>.
     * This is the key used doing lookups, forgets and insertions on the
     * already included list.
     */
    public void add(CandidateURI uri, String canonical);
    
    /**
     * Immediately add uri.
     * @param uri Uri item to add immediately.
     * @param canonical The canonicalized version of <code>uri</code>.
     * This is the key used doing lookups, forgets and insertions on the
     * already included list.
     */
    public void addNow(CandidateURI uri, String canonical);
    
    /**
     * Add given uri, even if already present
     * 
     * TODO: What is this for?
     * 
     * @param uri Uri item to force add.
     * @param canonical The canonicalized version of <code>uri</code>.
     * This is the key used doing lookups, forgets and insertions on the
     * already included list.
     */
    public void addForce(CandidateURI uri, String canonical);
    
    /**
     * Note item as seen, without passing through to receiver.
     * @param canonical The canonicalized version of <code>uri</code>.
     * This is the key used doing lookups, forgets and insertions on the
     * already included list.
     */
    public void note(String canonical);
    
    /**
     * Forget item was seen
     * @param canonical The canonicalized version of <code>uri</code>.
     * This is the key used doing lookups, forgets and insertions on the
     * already included list.
     */
    public void forget(String canonical);
    
    /**
     * Force pending items to be added/dropped.
     * @return Number added.
     */
    public long flush();
    
    /**
     * Close down any allocated resources.
     * Makes sense calling this when checkpointing.
     */
    public void close();
    
    /**
     * URIs that have not been seen before 'visit' this 'Visitor'.
     * 
     * Usually implementations of Frontier implement this interface.
     * @author gojomo
     */
    public interface HasUriReceiver {
        /**
         * @param item Candidate uri tem that is 'visiting'.
         */
        public void receive(CandidateURI item);
    }
}