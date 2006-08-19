/* $Id$
 *
 * Created on July 21st, 2006
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

import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.archive.util.ArchiveUtils;
import org.archive.util.IoUtils;
import org.archive.util.TimestampSerialno;


/**
 * Member of {@link WriterPool}.
 * Implements rotating off files, file naming with some guarantee of
 * uniqueness, and position in file. Subclass to pick up functionality for a
 * particular Writer type.
 * @author stack
 * @version $Date$ $Revision$
 */
public abstract class WriterPoolMember {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
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
     * Reference to file we're currently writing.
     */
    private File f = null;

    /**
     *  Output stream for file.
     */
    private OutputStream out = null;
    
    /**
     * File output stream.
     * This is needed so can get at channel to find current position in file.
     */
    private FileOutputStream fos;
    
    private final boolean compressed;
    private List writeDirs = null;
    private String prefix = DEFAULT_PREFIX;
    private String suffix = DEFAULT_SUFFIX;
    private final int maxSize;
    private final String extension;

    /**
     * Creation date for the current file.
     * Set by {@link #createFile()}.
     */
	private String createTimestamp = "UNSET!!!";
    
    /**
     * A running sequence used making unique file names.
     */
    private static int serialNo = 0;
    
    /**
     * Directories round-robin index.
     */
    private static int roundRobinIndex = 0;

    /**
     * NumberFormat instance for formatting serial number.
     *
     * Pads serial number with zeros.
     */
    private static NumberFormat serialNoFormatter = new DecimalFormat("00000");
    
    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.
     * @param out Where to write.
     * @param file File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    protected WriterPoolMember(final PrintStream out, final File file,
            final boolean cmprs, String a14DigitDate)
    throws IOException {
        this(null, null, cmprs, -1, null);
        this.out = out;
        this.f = file;
    }
    
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param extension Extension to give file.
     */
    public WriterPoolMember(final List dirs, final String prefix, 
            final boolean cmprs, final int maxSize, final String extension) {
        this(dirs, prefix, "", cmprs, maxSize, extension);
    }
            
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     * @param extension Extension to give file.
     */
    public WriterPoolMember(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize, final String extension) {
        this.suffix = suffix;
        this.prefix = prefix;
        this.maxSize = maxSize;
        this.writeDirs = dirs;
        this.compressed = cmprs;
        this.extension = extension;
    }

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
    public void checkSize() throws IOException {
        if (this.out == null ||
                (this.maxSize != -1 && (this.f.length() > this.maxSize))) {
            createFile();
        }
    }

    /**
     * Create a new file.
     * Rotates off the current Writer and creates a new in its place
     * to take subsequent writes.  Usually called from {@link #checkSize()}.
     * @return Name of file created.
     * @throws IOException
     */
    protected String createFile() throws IOException {
        close();
        TimestampSerialno tsn = getTimestampSerialNo();
        String name = this.prefix + '-' + getUniqueBasename(tsn) +
            ((this.suffix == null || this.suffix.length() <= 0)?
                "": "-" + this.suffix) + '.' + this.extension  +
            ((this.compressed)? '.' + COMPRESSED_FILE_EXTENSION: "") +
            OCCUPIED_SUFFIX;
        File dir = getNextDirectory(this.writeDirs);
        this.f = new File(dir, name);
        this.createTimestamp = tsn.getTimestamp();
        this.fos = new FileOutputStream(this.f);
        this.out = new FastBufferedOutputStream(this.fos);
        logger.info("Opened " + this.f.getAbsolutePath());
        return name;
    }
    
    /**
     * @param dirs List of File objects that point at directories.
     * @return Find next directory to write an arc too.  If more
     * than one, it tries to round-robin through each in turn.
     * @throws IOException
     */
    protected File getNextDirectory(List dirs)
    throws IOException {
        if (WriterPoolMember.roundRobinIndex >= dirs.size()) {
            WriterPoolMember.roundRobinIndex = 0;
        }
        File d = null;
        try {
            d = checkWriteable((File)dirs.
                get(WriterPoolMember.roundRobinIndex));
        } catch (IndexOutOfBoundsException e) {
            // Dirs list might be altered underneath us.
            // If so, we get this exception -- just keep on going.
        }
        if (d == null && dirs.size() > 1) {
            for (Iterator i = dirs.iterator(); d == null && i.hasNext();) {
                d = checkWriteable((File)i.next());
            }
        } else {
            WriterPoolMember.roundRobinIndex++;
        }
        if (d == null) {
            throw new IOException("Directories unusable.");
        }
        return d;
    }
        
    protected File checkWriteable(File d) {
        if (d == null) {
            return d;
        }
        
        try {
            IoUtils.ensureWriteableDirectory(d);
        } catch(IOException e) {
            logger.warning("Directory " + d.getPath() + " is not" +
                " writeable or cannot be created: " + e.getMessage());
            d = null;
        }
        return d;
    }
    
    protected synchronized TimestampSerialno getTimestampSerialNo() {
        return getTimestampSerialNo(null);
    }
    
    /**
     * Do static synchronization around getting of counter and timestamp so
     * no chance of a thread getting in between the getting of timestamp and
     * allocation of serial number throwing the two out of alignment.
     * 
     * @param timestamp If non-null, use passed timestamp (must be 14 digit
     * ARC format), else if null, timestamp with now.
     * @return Instance of data structure that has timestamp and serial no.
     */
    protected synchronized TimestampSerialno
            getTimestampSerialNo(final String timestamp) {
        return new TimestampSerialno((timestamp != null)?
                timestamp: ArchiveUtils.get14DigitDate(),
            WriterPoolMember.serialNo++);
    }

    /**
     * Return a unique basename.
     *
     * Name is timestamp + an every increasing sequence number.
     *
     * @param tsn Structure with timestamp and serial number.
     *
     * @return Unique basename.
     */
    private String getUniqueBasename(TimestampSerialno tsn) {
        return tsn.getTimestamp() + "-" +
           WriterPoolMember.serialNoFormatter.format(tsn.getSerialNumber());
    }


    /**
     * Get the file name
     * 
     * @return the filename, as if uncompressed
     */
    protected String getBaseFilename() {
        String name = this.f.getName();
        if (this.compressed && name.endsWith(DOT_COMPRESSED_FILE_EXTENSION)) {
            return name.substring(0,name.length() - 3);
        } else if(this.compressed &&
                name.endsWith(DOT_COMPRESSED_FILE_EXTENSION +
                    OCCUPIED_SUFFIX)) {
            return name.substring(0, name.length() -
                (3 + OCCUPIED_SUFFIX.length()));
        } else {
            return name;
        }
    }

	/**
	 * Get this file.
	 *
	 * Used by junit test to test for creation and when {@link WriterPool} wants
     * to invalidate a file.
	 *
	 * @return The current file.
	 */
    public File getFile() {
        return this.f;
    }

    /**
     * Post write tasks.
     * 
     * Has side effects.  Will open new file if we're at the upperbound.
     * If we're writing compressed files, it will wrap output stream with a
     * GZIP writer with side effect that GZIP header is written out on the
     * stream.
     *
     * @exception IOException
     */
    protected void preWriteRecordTasks()
    throws IOException {
        checkSize();
        if (this.compressed) {
            // Wrap stream in GZIP Writer.
            // The below construction immediately writes the GZIP 'default'
            // header out on the underlying stream.
            this.out = new CompressedStream(this.out);
        }
    }

    /**
     * Post file write tasks.
     * If compressed, finishes up compression and flushes stream so any
     * subsequent checks get good reading.
     *
     * @exception IOException
     */
    protected void postWriteRecordTasks()
    throws IOException {
        if (this.compressed) {
            CompressedStream o = (CompressedStream)this.out;
            o.finish();
            o.flush();
            this.out = o.getWrappedStream();
        }
    }
    
	/**
     * Postion in current physical file.
     * Used making accounting of bytes written.
	 * @return Position in underlying file.  Call before or after writing
     * records *only* to be safe.
	 * @throws IOException
	 */
    public long getPosition() throws IOException {
        long position = 0;
        if (this.out != null) {
            this.out.flush();
        }
        if (this.fos != null) {
            // Call flush on underlying file though probably not needed assuming
            // above this.out.flush called through to this.fos.
            this.fos.flush();
            position = this.fos.getChannel().position();
        }
        return position;
    }

    public boolean isCompressed() {
        return compressed;
    }
    
    protected void write(final byte [] b) throws IOException {
    	this.out.write(b);
    }
    
	protected void flush() throws IOException {
		this.out.flush();
	}

	protected void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}

	protected void write(int b) throws IOException {
		this.out.write(b);
	}
	
	protected void readFullyFrom(final InputStream is, final long recordLength,
			final byte [] b)
	throws IOException {
        int read = b.length;
        int total = 0;
        while((read = is.read(b)) != -1 && total < recordLength) {
        	total += read;
            write(b, 0, read);
        }
        if (total != recordLength) {
        	throw new IOException("Read " + total + " but expected " +
        	    recordLength);
        }
	}
	
    public void close() throws IOException {
        if (this.out == null) {
            return;
        }
        this.out.close();
        this.out = null;
        this.fos = null;
        if (this.f != null && this.f.exists()) {
            String path = this.f.getAbsolutePath();
            if (path.endsWith(OCCUPIED_SUFFIX)) {
                File f = new File(path.substring(0,
                        path.length() - OCCUPIED_SUFFIX.length()));
                if (!this.f.renameTo(f)) {
                    logger.warning("Failed rename of " + path);
                }
                this.f = f;
            }
            logger.info("Closed " + this.f.getAbsolutePath() +
                    ", size " + this.f.length());
        }
    }
    
    protected OutputStream getOutputStream() {
    	return this.out;
    }
    
	protected String getCreateTimestamp() {
		return createTimestamp;
	}
    
    /**
     * Reset serial number.
     */
    public static synchronized void resetSerialNo() {
        setSerialNo(0);
    }
    
    /**
     * @return Serial number.
     */
    public static int getSerialNo() {
        return WriterPoolMember.serialNo;
    }
    
    /**
     * Call when recovering from checkpointing.
     * @param no Number to set serial number too.
     */
    public static void setSerialNo(int no) {
        WriterPoolMember.serialNo = no;
    }
    
    /**
     * An override so we get access to underlying output stream.
     * @author stack
     */
    private class CompressedStream extends GZIPOutputStream {
        public CompressedStream(OutputStream out)
        throws IOException {
            super(out);
        }
        
        /**
         * @return Reference to stream being compressed.
         */
        OutputStream getWrappedStream() {
            return this.out;
        }
    }
}
