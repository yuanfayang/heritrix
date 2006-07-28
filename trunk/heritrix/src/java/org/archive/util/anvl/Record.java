/* Record
*
* $Id$
*
* Created on July 26, 2006.
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
package org.archive.util.anvl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An ordered {@link List} with 'data' {@link Element} values.
 * @see <a
 * href="http://www.cdlib.org/inside/diglib/ark/anvlspec.pdf">A Name-Value
 * Language (ANVL)</a>
 * @author stack
 */
public class Record extends ArrayList<Element> {
	/**
	 * An ANVL 'newline'.
	 * @see http://en.wikipedia.org/wiki/CRLF
	 */
    static final String CRLF = "\r\n";
    
    static final String UTF8 = "UTF-8";
    
    public Record() {
        super();
    }

    public Record(Collection<? extends Element> c) {
        super(c);
    }

    public Record(int initialCapacity) {
        super(initialCapacity);
    }
    
    @Override
    public String toString() {
        // TODO: What to emit for empty Record?
        StringBuilder sb = new StringBuilder();
        for (final Iterator i = iterator(); i.hasNext();) {
            sb.append(i.next());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        return sb.toString();
    }
    
    /**
     * @return Record encoded as UTF-8 bytes.
     * @throws UnsupportedEncodingException 
     */
    public byte [] getUTF8Bytes()
    throws UnsupportedEncodingException {
        return toString().getBytes(UTF8);
    }
}
