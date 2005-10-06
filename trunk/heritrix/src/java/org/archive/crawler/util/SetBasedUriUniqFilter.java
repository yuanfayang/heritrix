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
    protected HasUriReceiver receiver;
    protected PrintWriter profileLog;
    
    public SetBasedUriUniqFilter() {
        super();
    }
    
    protected abstract boolean setAdd(CharSequence key);

    protected abstract boolean setRemove(CharSequence key);

    protected abstract long setCount();
    
    public long count() {
        return setCount();
    }

    public long pending() {
        // no items pile up in this implementation
        return 0;
    }

    public void setDestination(HasUriReceiver receiver) {
        this.receiver = receiver;
    }

    protected void profileLog(String key) {
        if (profileLog != null) {
            profileLog.println(key);
        }
    }
    
    public void add(String key, CandidateURI value) {
        profileLog(key);
        if(setAdd(key)) {
            this.receiver.receive(value);
        }
    }

    public void addNow(String key, CandidateURI value) {
        add(key, value);
    }
    
    public void addForce(String key, CandidateURI value) {
        profileLog(key);
        setAdd(key);
        this.receiver.receive(value);
    }

    public void note(String key) {
        profileLog(key);
        setAdd(key);
    }

    public void forget(String key, CandidateURI value) {
        setRemove(key);
    }

    public long flush() {
        // unnecessary; all actions with set-based uniqfilter are immediate
        return 0;
    }

    public void close() {
        // Nothing to do.
    }

    public void setProfileLog(File logfile) {
        try {
            profileLog = new PrintWriter(logfile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
