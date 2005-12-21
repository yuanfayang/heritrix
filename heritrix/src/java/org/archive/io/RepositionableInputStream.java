/* RepositionableInputStream.java
 *
 * $Id$
 *
 * Created Dec 20, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

import it.unimi.dsi.mg4j.io.RepositionableStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper around an {@link InputStream} to make a primitive Repositionable
 * stream. Uses a {@link BufferedInputStream}. Has limitations.
 * TODO: More robust implementation.
 * @author stack
 */
public class RepositionableInputStream extends BufferedInputStream implements
        RepositionableStream {
    private long position = 0;
    private long markPosition = 0;
    
    public RepositionableInputStream(InputStream in) {
        super(in);
    }

    public int read() throws IOException {
        int r = this.in.read();
        if (r != -1) {
            position++;
        }
        return r;
    }

    /**
     * @param offset Offset is ignored.  We call reset on underlying stream. Use
     * sparingly and only after a call to position.
     * @throws IOException 
     */
    public void position(final long offset) throws IOException {
        if (offset > this.buf.length) {
            int newPos = (int)(offset % this.buf.length);
            this.position = this.position - this.pos + newPos;
            this.pos = newPos;
        } else {
            this.pos = (int)offset;
            this.position = this.pos;
        }
    }

    public void mark(int readlimit) {
        this.markPosition = this.position;
        super.mark(readlimit);
    }

    public void reset() throws IOException {
        super.reset();
        this.position = this.markPosition;
    }

    public long position() throws IOException {
        return this.position;
    }
}