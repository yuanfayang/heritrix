/* EnhancedCharSequence
 *
 * $Id$
 *
 * Created on Mar 17, 2004
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


/**
 * Extends the <tt>java.lang.CharSequence</tt> to provide additional methods
 * to improve the performance of implementing classes.
 *
 * @author Kristinn Sigurdsson
 * @see java.lang.CharSequence
 */
public interface EnhancedCharSequence extends CharSequence {

    /**
     * Returns the string representation of a part of the character sequence.
     * @param offset Index of first char to include. Must be larger then or
     *           equal to 0 and smaller then the length of the character
     *           sequence.
     * @param length How many chars after the first to include. offset + length
     *           must be smaller then the length of the character sequence.
     *           length must be non negative.
     * @return a string consisting of exactly this sequence of characters from
     *           <code>offset</code> to <code>offset + length</code>
     */
    public String substring(int offset, int length);
}
