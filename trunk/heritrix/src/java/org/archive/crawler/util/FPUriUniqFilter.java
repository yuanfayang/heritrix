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
 * UriUniqFilter which only stores 64-bit UURI fingerprints, using an
 * internal LongFPSet instance. (This internal instance may be
 * disk or memory based.)
 *
 * @author gojomo
 *
 */
public class FPUriUniqFilter implements UriUniqFilter, Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(FPUriUniqFilter.class,1);
    private static Logger logger = Logger.getLogger(FPUriUniqFilter.class.getName());

    LongFPSet fpset;
    transient FPGenerator fpgen = FPGenerator.std64;
    HasUriReceiver receiver;
    
    /**
     * @param fpset
     */
    public FPUriUniqFilter(LongFPSet fpset) {
        this.fpset = fpset;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#setDestination(org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver)
     */
    public void setDestination(HasUriReceiver r) {
        this.receiver = r;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#add(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void add(HasUri obj) {
        if(fpset.add(getFp(obj))) {
            this.receiver.receive(obj);
        }
    }

    /**
     * @param obj
     * @return
     */
    private long getFp(HasUri obj) {
        return fpgen.fp(obj.getUri());
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
        fpset.add(getFp(obj));
        this.receiver.receive(obj);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#note(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void note(HasUri hu) {
        fpset.add(getFp(hu));        
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#forget(org.archive.crawler.datamodel.UriUniqFilter.HasUri)
     */
    public void forget(HasUri hu) {
        fpset.remove(getFp(hu));        
    }
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#flush()
     */
    public long flush() {
        // noop for now
        return 0;
    }
    
    
    /**
     * @see org.archive.crawler.datamodel.UriUniqFilter#count()
     */
    public long count() {
        return fpset.count();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter#pending()
     */
    public long pending() {
        // no items pile up in this implementation
        return 0;
    }

//    // custom serialization
//    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//    	stream.defaultReadObject();
//    	fpgen = FPGenerator.std64;
//    }

}
