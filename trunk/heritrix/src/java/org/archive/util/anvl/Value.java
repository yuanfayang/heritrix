/* Value
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
 * TODO: Implement 'folding'.
 * <p>TODO: Study <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Properties.html#load(java.io.InputStream)">Properties#load</a>
 * @author stack
 * @version $Date$ $Version$
 */
class Value extends SubElement {
	private static final String PREFIX = " ";
    private StringBuilder sb;
    private boolean folding = false;
	
    private Value() {
        this(null);
    }
    
    public Value(final String s) {
        super(s);
    }
    
    protected String baseCheck(String s) {
        this.sb = new StringBuilder(s.length() * 2);
        super.baseCheck(s);
        return sb.toString();
    }
    
    @Override
    protected void checkCharacter(char c, String srcStr, int index) {
        checkControlCharacter(c, srcStr, index);
        // Now, rewrite the value String with folding (If CR or LF or CRLF
        // present.
        if (isCR(c)) {
            this.folding = true;
            this.sb.append(ANVLRecord.FOLD_PREFIX);
        } else if (isLF(c)) {
            if (!this.folding) {
                this.folding = true;
                this.sb.append(ANVLRecord.FOLD_PREFIX);
            } else {
                // Previous character was a CR. Fold prefix has been added.
            }
        } else if (this.folding && Character.isWhitespace(c)) {
            // Only write out one whitespace character. Skip.
        } else {
            this.folding = false;
            this.sb.append(c);
        }
    }
    
    @Override
    public String toString() {
        return PREFIX + super.toString();
    }
}