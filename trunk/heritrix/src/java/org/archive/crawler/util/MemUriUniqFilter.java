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
 * MemUURISet.java
 * Created on Sep 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.HashSet;

import org.archive.crawler.datamodel.UriUniqFilter;

/**
 * A purely in-memory UriUniqFilter based on a HashSet, which remembers
 * every full URI string it sees. 
 * 
 * @author gojomo
 *
 */
public class MemUriUniqFilter extends HashSet implements UriUniqFilter {
    HasUriReceiver receiver;

    public void setDestination(HasUriReceiver r) {
        this.receiver = r;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#add(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void add(HasUri obj) {
        if(super.add(obj.getUri())) {
            this.receiver.receive(obj);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#addNow(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void addNow(HasUri obj) {
        add(obj);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#addForce(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void addForce(HasUri obj) {
        super.add(obj.getUri());
        this.receiver.receive(obj);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#note(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void note(HasUri hu) {
        super.add(hu.getUri());
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#forget(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void forget(HasUri hu) {
        super.remove(hu.getUri());
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#flush()
     */
    public long flush() {
        // unnecessary; all actions here are immediate
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#count()
     */
    public long count() {
        return size();
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#pending()
     */
    public long pending() {
        // no items pile up in this implementation
        return 0;
    }
}
