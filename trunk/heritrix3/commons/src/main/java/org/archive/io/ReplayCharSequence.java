/* ReplayCharSequence
 *
 * Created on Mar 5, 2004
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
package org.archive.io;

import java.io.Closeable;
import java.io.IOException;


/**
 * CharSequence interface with addition of a {@link #close()} method.
 *
 * Users of implementations of this interface must call {@link #close()} so
 * implementations get a chance at cleaning up after themselves.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public interface ReplayCharSequence extends CharSequence, Closeable {

    /** charset to use in replay when declared value 
     * is absent/illegal/unavailable */
//  String FALLBACK_CHARSET_NAME = "UTF-8";
    String FALLBACK_CHARSET_NAME = "ISO8859_1";
    
    /**
     * Call this method when done so implementation has chance to clean up
     * resources.
     *
     * @throws IOException Problem cleaning up file system resources.
     */
    public void close() throws IOException;
}