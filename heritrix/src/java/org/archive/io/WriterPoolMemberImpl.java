/* WriterPoolMemberImpl
 *
 * $Id$
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
 * Abstract implementation of {@link WriterPoolMember} functionality.
 * Implements rotating off files, file naming with some guarantee of
 * uniqueness and position in file. Subclass to pick up functionality for a
 * particular Writer type.
 * @author stack
 */
public abstract class WriterPoolMemberImpl implements WriterPoolMember {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

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
     * Takes a stream.
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public WriterPoolMemberImpl(final PrintStream out, final File file,
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
    public WriterPoolMemberImpl(final List dirs, final String prefix, 
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
     * @param meta File meta data.  Can be null.  Is list of File and/or
     * String objects.
     */
    public WriterPoolMemberImpl(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize, final String extension) {
        this.suffix = suffix;
        this.prefix = prefix;
        this.maxSize = maxSize;
        this.writeDirs = dirs;
        this.compressed = cmprs;
        this.extension = extension;
    }
    
    /* (non-Javadoc)
	 * @see org.archive.io.arc.Writer#close()
	 */
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

    /* (non-Javadoc)
	 * @see org.archive.io.arc.Writer#checkARCFileSize()
	 */
    public void checkSize() throws IOException {
        if (this.out == null ||
                (this.maxSize != -1 && (this.f.length() > this.maxSize))) {
            createFile();
        }
    }

    /**
     * Create a new file.
     * @returns Timestamp and serial number used making new file. 
     * @throws IOException
     */
    protected TimestampSerialno createFile() throws IOException {
        close();
        TimestampSerialno tsn = getTimestampSerialNo();
        String name = this.prefix + '-' + getUniqueBasename(tsn) +
            ((this.suffix == null || this.suffix.length() <= 0)?
                "": "-" + this.suffix) + '.' + this.extension  +
            ((this.compressed)? '.' + COMPRESSED_FILE_EXTENSION: "") +
            OCCUPIED_SUFFIX;
        File dir = getNextDirectory(this.writeDirs);
        this.f = new File(dir, name);
        this.fos = new FileOutputStream(this.f);
        this.out = new FastBufferedOutputStream(this.fos);
        logger.info("Opened " + this.f.getAbsolutePath());
        return tsn;
    }
    
    /**
     * @param dirs List of File objects that point at directories.
     * @return Find next directory to write an arc too.  If more
     * than one, it tries to round-robin through each in turn.
     * @throws IOException
     */
    protected File getNextDirectory(List dirs)
    throws IOException {
        if (WriterPoolMemberImpl.roundRobinIndex >= dirs.size()) {
            WriterPoolMemberImpl.roundRobinIndex = 0;
        }
        File d = null;
        try {
            d = checkWriteable((File)dirs.
                get(WriterPoolMemberImpl.roundRobinIndex));
        } catch (IndexOutOfBoundsException e) {
            // Dirs list might be altered underneath us.
            // If so, we get this exception -- just keep on going.
        }
        if (d == null && dirs.size() > 1) {
            for (Iterator i = dirs.iterator(); d == null && i.hasNext();) {
                d = checkWriteable((File)i.next());
            }
        } else {
            WriterPoolMemberImpl.roundRobinIndex++;
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
            WriterPoolMemberImpl.serialNo++);
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
        return tsn.getNow() + "-" +
           WriterPoolMemberImpl.serialNoFormatter.format(tsn.getSerialNumber());
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
        return WriterPoolMemberImpl.serialNo;
    }
    
    /**
     * Call when recovering from checkpointing.
     * @param no Number to set serial number too.
     */
    public static void setSerialNo(int no) {
        WriterPoolMemberImpl.serialNo = no;
    }

    /**
     * Get the file name
     * 
     * @return the filename, as if uncompressed
     */
    protected String generateName() {
        String name = this.f.getName();
        if(this.compressed && name.endsWith(DOT_COMPRESSED_FILE_EXTENSION)) {
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

    /* (non-Javadoc)
	 * @see org.archive.io.arc.Writer#getArcFile()
	 */
    public File getFile() {
        return this.f;
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
        OutputStream getOut() {
            return this.out;
        }
    }

    /**
     * Post write tasks.
     * 
     * Has side effects.  Will open new file if we're at the upperbound.
     * If we're writing compressed files, it will write the GZIP header
     * out on the stream.
     *
     * @exception IOException
     */
    protected void preWriteRecordTasks()
    throws IOException {
        checkSize();
        if (this.compressed) {
            // The below construction immediately writes the GZIP 'default'
            // header out on the underlying stream.
            this.out = new CompressedStream(this.out);
        }
    }

    /**
     * Post file write tasks.
     *
     * @exception IOException
     */
    protected void postWriteRecordTasks()
    throws IOException {
        if (this.compressed) {
            CompressedStream o = (CompressedStream)this.out;
            o.finish();
            o.flush();
            this.out = o.getOut();
        }
    }
    
    /* (non-Javadoc)
	 * @see org.archive.io.arc.Writer#getPosition()
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

    public OutputStream getOutputStream() {
        return out;
    }

    public boolean isCompressed() {
        return compressed;
    }
}
