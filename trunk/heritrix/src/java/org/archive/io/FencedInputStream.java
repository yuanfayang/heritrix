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
 * FencedInputStream.java
 * Created on Sep 26, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author gojomo
 *
 */
public class FencedInputStream extends FilterInputStream {
    long maxToRead;
    long position = 0;

    /**
     * @param in
     */
    protected FencedInputStream(InputStream in, long maxToRead) {
        super(in);
        this.maxToRead = maxToRead;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        if (position < maxToRead) {
            int b = super.read();
            if (b>=0) {
                position++;
            }
            return b;
        } else {
            return -1; // virtual EOF
        }
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        // TODO Auto-generated method stub
        return super.read(b, off, len);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        // TODO Auto-generated method stub
        return super.read(b);
    }

}
