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
 * A UriUniqFilter passes add()ed URI (HasUri) objects to a destination
 * (receiver) if the passed URI has not been previously seen.
 * 
 * If already seen, the passed URI is dropped.
 *
 * <p>For efficiency in comparison against a large history of
 * seen URIs, HasUris may not be passed immediately, unless 
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
     * @return Count of items added not yet evaluated (TODO: Whats this mean?).
     */
    public long pending();

    /**
     * Receiver of uniq URIs.
     * Items that have not been seen before are pass through to this object.
     * @param receiver Object that will be passed items. Must implement
     * HasUriReceiver interface.
     */
    public void setDestination(HasUriReceiver receiver);
    
    /**
     * Add given item, if not already present.
     * @param item Item to add.
     */
    public void add(HasUri item);
    
    /**
     * Immediately add item.
     * @param item Item to add immediately.
     */
    public void addNow(HasUri item);
    
    /**
     * Add given item, even if already present
     * 
     * TODO: What is this for?
     * 
     * @param item Item to force add.
     */
    public void addForce(HasUri hu);
    
    /**
     * Note item as seen, without passing through to receiver.
     * @param item Item to note as seen.
     */
    public void note(HasUri item);
    
    /**
     * Forget item was seen
     * @param item Item to forget.
     */
    public void forget(HasUri item);
    
    /**
     * Force pending items to be added/dropped.
     * @return Number added.
     */
    public long flush();
    
    /**
     * Objects that have a URI implement this interface.
     * 
     * Implementers of this interface would include UURI.
     * 
     * @author gojomo
     */
    public interface HasUri {
        /**
         * @return Return contained URI as a String.
         */
        public String getUri();
    }
    
    /**
     * URIs that have not been seen before 'visit' this 'Visitor'.
     * 
     * Usually implementations of Frontier implement this interface.
     * @author gojomo
     */
    public interface HasUriReceiver {
        /**
         * @param item Item that is 'visiting'.
         */
        public void receive(HasUri item);
    }
}
