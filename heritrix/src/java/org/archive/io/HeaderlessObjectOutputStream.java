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
 * HeaderlessObjectOutputStream.java
 * Created on May 21, 2004
 *
 * $Header$
 */
package org.archive.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
 * Block write of java serialization stream header.
 *
 * Default java serialization writes a header of magic bytes, version and
 * length at start of a serialization stream.  This class blocks the writing of
 * this header.  It works with the {@link HeaderlessObjectInputStream} which
 * blocks the reading of the serialization header.
 *
 * <p>Used when want more control of serialization stream.
 */
public class HeaderlessObjectOutputStream extends ObjectOutputStream {

	public HeaderlessObjectOutputStream(OutputStream out) throws IOException {
		super(out);
	}

	protected void writeStreamHeader() throws IOException {
		// do nothing
	}
}
