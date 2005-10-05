/* UriUniqFilterImpl
*
* $Id$
*
* Created on Sep 29, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UriUniqFilter;

/**
 * UriUniqFilter based on an underlying UriSet (essentially a Set).
 * 
 * @author gojomo
 */
public abstract class SetBasedUriUniqFilter implements UriUniqFilter {
    HasUriReceiver receiver;
    PrintWriter profileLog;
    
    /**
     * 
     */
    public SetBasedUriUniqFilter() {
        super();
    }
    
    /**
     * @param key
     */
    protected abstract boolean setAdd(CharSequence key);

    /**
     * @param key
     */
    protected abstract boolean setRemove(CharSequence key);
    
    /**
     * @return
     */
    protected abstract long setCount();
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#count()
     */
    public long count() {
        return setCount();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#pending()
     */
    public long pending() {
        // no items pile up in this implementation
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#setDestination(org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver)
     */
    public void setDestination(HasUriReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * @param key
     */
    protected void profileLog(String key) {
        if(profileLog!=null) {
            profileLog.println(key);
        }
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#add(java.lang.String, org.archive.crawler.datamodel.CandidateURI)
     */
    public void add(String key, CandidateURI value) {
        profileLog(key);
        if(setAdd(key)) {
            this.receiver.receive(value);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#addNow(java.lang.String, org.archive.crawler.datamodel.CandidateURI)
     */
    public void addNow(String key, CandidateURI value) {
        add(key, value);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#addForce(java.lang.String, org.archive.crawler.datamodel.CandidateURI)
     */
    public void addForce(String key, CandidateURI value) {
        profileLog(key);
        setAdd(key);
        this.receiver.receive(value);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#note(java.lang.String)
     */
    public void note(String key) {
        profileLog(key);
        setAdd(key);
    }


    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#forget(java.lang.String, org.archive.crawler.datamodel.CandidateURI)
     */
    public void forget(String key, CandidateURI value) {
        setRemove(key);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#flush()
     */
    public long flush() {
        // unnecessary; all actions with set-based uniqfilter are immediate
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#close()
     */
    public void close() {
        // Nothing to do.
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#setProfileLog(java.io.File)
     */
    public void setProfileLog(File logfile) {
        try {
            profileLog = new PrintWriter(logfile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
