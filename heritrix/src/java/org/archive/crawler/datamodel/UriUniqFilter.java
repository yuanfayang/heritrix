/* Copyright (C) 2003 Internet Archive.
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
 *
 * UURISet.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;


/**
 * A UriUniqFilter passes add()ed HasUri objects to its 
 * destination, unless the corresponding URI was previously 
 * seen, in which case the HasUri is dropped. 
 *
 * For efficiency in comparison against a large history of
 * seen URIs, HasUris may not be passed immediately, unless 
 * the addNow() is used or a flush() is forced.
 * 
 * @author gojomo
 */
public interface UriUniqFilter {
    public long count();   // total known URIs
    public long pending(); // items added not yet evaluated
    public void setDestination(HasUriReceiver r); // inner receiver of uniq URIs
    public void add(HasUri hu); // add given item, if not already present
    public void addNow(HasUri hu); // add given item, lagless 
    public void addForce(HasUri hu); // add given item, even if already present
    public void note(HasUri hu); // note item as seen, without passing through
    public void forget(HasUri hu); // forget that item was seen
    public void flush(); // force all items to be evaluated, added or dropped
    
    /**
     * @author gojomo
     */
    public interface HasUri {
        public String getUri();
    }
    /**
     * @author gojomo
     */
    public interface HasUriReceiver {
        public void receive(HasUri h);
    }

//  public boolean contains(UURI u);
//  public boolean contains(CandidateURI curi);

//    public void add(UURI u);
//    public void remove(UURI u);
//
//    public void add(CandidateURI curi); // convenience; only really adds the UURI
//    public void remove(CandidateURI curi); // convenience; only really adds the UURI
}
