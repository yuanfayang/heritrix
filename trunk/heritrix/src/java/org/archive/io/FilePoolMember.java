/* FilePoolMember
 *
 * $Id$
 *
 * Created July 19th, 2006.
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
package org.archive.io;

import java.io.File;
import java.io.IOException;

/**
 * Member of {@link FilePool}.
 * @author stack
 */
public interface FilePoolMember {
	/**
	 * Suffix given to files currently in use by the pool.
	 */
	public static final String OCCUPIED_SUFFIX = ".open";

	public static final String UTF8 = "UTF-8";

	/**
	 * Close file.
	 *
	 * @throws IOException
	 */
	public abstract void close() throws IOException;

	/**
	 * Call this method just before we start to write a new record.
	 *
	 * Call at the end of the writing of a record or just before we start
	 * writing a new record.  Will close current file and open a new file
	 * if file size has passed out maxSize.
	 * 
	 * <p>Creates and opens a file if none already open.  One use of this method
	 * then is after construction, call this method to add the metadata, then
	 * call {@link #getPosition()} to find offset of first record.
	 *
	 * @exception IOException
	 */
	public abstract void checkFileSize() throws IOException;

	/**
	 * Get this file.
	 *
	 * Used by junit test to test for creation.
	 *
	 * @return Current arcFile.
	 */
	public abstract File getFile();

	/**
	 * @return Position in underlying file. Returns 0 if underlying stream
	 * does not support getting position (or there is no stream yet -- can
	 * happen after construction before call to write or to
	 * {@link #checkFileSize()}).  Position returned cannot always
	 * be trusted.  Call before or after writing records *only* to be safe.
	 * @throws IOException
	 */
	public abstract long getPosition() throws IOException;
}