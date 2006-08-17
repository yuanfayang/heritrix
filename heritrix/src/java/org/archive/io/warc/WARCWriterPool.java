/* $Id$
 *
 * Created on August 1st, 2006.
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.io.warc;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.archive.io.WriterPool;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;


/**
 * A pool of WARCWriters.
 * @author stack
 * @version $Revision$ $Date$
 */
public class WARCWriterPool extends WriterPool {
    /**
     * Constructor
     *
     * @param settings Settings for this pool.
     * @param poolMaximumActive
     * @param poolMaximumWait
     */
    public WARCWriterPool(final WriterPoolSettings settings,
            final int poolMaximumActive, final int poolMaximumWait) {
    	super(new BasePoolableObjectFactory() {
            public Object makeObject() throws Exception {
                return new ExperimentalWARCWriter(settings.getOutputDirs(),
                        settings.getPrefix(), settings.getSuffix(),
                        settings.isCompressed(), settings.getMaxSize(),
                        settings.getMetadata());
            }

            public void destroyObject(Object writer)
            throws Exception {
                ((WriterPoolMember)writer).close();
                super.destroyObject(writer);
            }
    	}, settings, poolMaximumActive, poolMaximumWait);
    }
}
