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
 * MemFPUURISet.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.io.Serializable;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.util.ArchiveUtils;
import org.archive.util.LongFPSet;

import st.ata.util.FPGenerator;

/**
 * UriUniqFilter stores 64-bit UURI fingerprints, using an internal LongFPSet
 * instance. 
 * 
 * The passed LongFPSet internal instance may be disk or memory based.
 *
 * @author gojomo
 */
public class FPUriUniqFilter implements UriUniqFilter, Serializable {
    // Be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(FPUriUniqFilter.class, 1);
    
    private static Logger logger =
        Logger.getLogger(FPUriUniqFilter.class.getName());
    
    private LongFPSet fpset;
    private transient FPGenerator fpgen = FPGenerator.std64;
    private HasUriReceiver receiver;
    
    public FPUriUniqFilter(LongFPSet fpset) {
        this.fpset = fpset;
    }

    public void setDestination(HasUriReceiver r) {
        this.receiver = r;
    }

    public synchronized void add(HasUri obj) {
        if(fpset.add(getFp(obj))) {
            this.receiver.receive(obj);
        }
    }

    private long getFp(HasUri obj) {
        return fpgen.fp(obj.getUri());
    }

    public void addNow(HasUri obj) {
        add(obj);
    }

    public synchronized void addForce(HasUri obj) {
        fpset.add(getFp(obj));
        this.receiver.receive(obj);
    }

    public synchronized void note(HasUri hu) {
        fpset.add(getFp(hu));        
    }
    
    public synchronized void forget(HasUri hu) {
        fpset.remove(getFp(hu));        
    }

    public long flush() {
        // noop for now
        return 0;
    }
    
    /**
     * @see org.archive.crawler.datamodel.UriUniqFilter#count()
     */
    public synchronized long count() {
        return fpset.count();
    }

    public long pending() {
        // No items pile up in this implementation
        return 0;
    }
}
