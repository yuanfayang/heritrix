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
 * CharSubSequence.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

/**
 * @author gojomo
 *
 */
public class CharSubSequence implements CharSequence {
    CharSequence inner;
    int start;
    int end;

    public CharSubSequence(CharSequence inner, int start, int end) {
        // TODO bounds check
        super();
        this.inner = inner;
        this.start = start;
        this.end = end;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return end-start;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        return inner.charAt(start+index);
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int start, int end) {
        return new CharSubSequence(this,start,end);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer((int)length());
        for(int i=0; i<length(); i++) {
            sb.append(charAt(i));
        }
        return sb.toString();
    }

}
