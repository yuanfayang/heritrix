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
 * DiskBackedByteQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FIFO byte queue, using disk space as needed, up to provided
 * maximum queue size. 
 * 
 * Flips between two backing files: as soon as reading head 
 * reached beginning of one, writing tail flips to other. SO,
 * avoid using for small amounts of data that are immediately
 * read -- a lot of 
 * 
 * @author gojomo
 *
 */
public class DiskBackedByteQueue {
	long maxSize;
	FlipFileInputStream headStream;
	FlipFileOutputStream tailStream;

	public DiskBackedByteQueue(File tempDir, String backingFilenamePrefix) throws FileNotFoundException {
		super();
		tailStream = new FlipFileOutputStream(tempDir,backingFilenamePrefix);
		headStream = new FlipFileInputStream(tailStream);
	}

	public InputStream getHeadStream() {
		return headStream;
	}

	public OutputStream getTailStream() {
		return tailStream;
	}

	/**
	 * @throws IOException
	 */
	public void close() throws IOException {
		headStream.close();
		tailStream.close();
	}

	public void discard() {
		tailStream.discard();
	}

}
