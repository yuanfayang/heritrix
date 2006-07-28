/* SubElement
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

/**
 * Abstract ANVL 'data element' sub-part.
 * @author stack
 */
abstract class SubElement {
    private final String e;

    protected SubElement() {
        this(null);
    }

    public SubElement(final String s) {
        this.e = baseCheck(s);
    }

    protected String baseCheck(final String s) {
        // Check for null.
        if (s == null || s.length() <= 0) {
            throw new IllegalArgumentException("Can't be null or empty");
        }
        // Check for CRLF.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                throw new IllegalArgumentException(s +
                    " contains a control character(s): 0x" +
                    Integer.toHexString(i));
            } else if (c == Record.CRLF.charAt(0) ||
            			c == Record.CRLF.charAt(1)) {
                throw new IllegalArgumentException(s + " CR or LF (TODO: " +
                	"Allow for folding and then only check for CRLF)");
            }
        }
        return s;
    }
    
    @Override
    public String toString() {
        return e;
    }
}
