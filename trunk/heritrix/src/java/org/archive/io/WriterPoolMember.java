/* WriterPoolMember
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
 * Member of {@link WriterPool}.
 * 
 * Occupies a slot in a {@link WriterPool}.  Implementations rotate off actual
 * file instances after they exceed a threshold inside the {@link checkSize}
 * implementation.  Clients must call {@link checkSize} before/after any major
 * write operation to give this facility a chance to function.
 * 
 * <p>Does not specify write methods since they can vary widely across
 * implementations.
 * 
 * @author stack
 * @see {@link WriterPool}
 */
public interface WriterPoolMember {
	/**
	 * Suffix given to files currently in use by the pool.
	 */
	public static final String OCCUPIED_SUFFIX = ".open";
    
    /**
     * Suffix appended to 'broken' files.
     */
    public static final String INVALID_SUFFIX = ".invalid";
    
    /**
     * Compressed file extention.
     */
    public static final String COMPRESSED_FILE_EXTENSION = "gz";
   
    /**
     * Dot plus compressed file extention.
     */
    public static final String DOT_COMPRESSED_FILE_EXTENSION = "." +
        COMPRESSED_FILE_EXTENSION;
    
    public static final String UTF8 = "UTF-8";
    
    /**
     * Default file prefix.
     * 
     * Stands for Internet Archive Heritrix.
     */
    public static final String DEFAULT_PREFIX = "IAH";
    
    /**
     * Value to interpolate with actual hostname.
     */
    public static final String HOSTNAME_VARIABLE = "${HOSTNAME}";
    
    /**
     * Default for file suffix.
     */
    public static final String DEFAULT_SUFFIX = HOSTNAME_VARIABLE;

	/**
	 * Close file.
	 *
	 * @throws IOException
	 */
	public abstract void close() throws IOException;

	/**
	 * Call this method just before/after any significant write.
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
	public abstract void checkSize() throws IOException;

	/**
	 * Get this file.
	 *
	 * Used by junit test to test for creation and when {@link WriterPool} wants
     * to invalidate a file.
	 *
	 * @return The current file.
	 */
	public abstract File getFile();

	/**
     * Postion in current physical file.
     * Used making accounting of bytes written.
	 * @return Position in underlying file.  Call before or after writing
     * records *only* to be safe.
	 * @throws IOException
	 */
	public abstract long getPosition() throws IOException;
}