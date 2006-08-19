/* ARCReaderFactory
 *
 * $Id$
 *
 * Created on May 1, 2004
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
package org.archive.io.arc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.archive.io.GzipHeader;
import org.archive.io.GzippedInputStream;
import org.archive.io.NoGzipMagicException;
import org.archive.io.RepositionableInputStream;
import org.archive.net.UURI;
import org.archive.net.md5.Md5URLConnection;
import org.archive.net.rsync.RsyncURLConnection;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;


/**
 * Factory that returns an ARCReader.
 * 
 * Can handle compressed and uncompressed ARCs.
 *
 * @author stack
 */
public class ARCReaderFactory implements ARCConstants {
    /**
     * This factory instance.
     */
    private static final ARCReaderFactory factory = new ARCReaderFactory();

    /**
     * Shutdown any access to default constructor.
     */
    private ARCReaderFactory() {
        super();
    }
    
    /**
     * Get ARCReader on passed path or url.
     * Does primitive heuristic figuring if path or URL.
     * @param arcFileOrUrl File path or URL pointing at an ARC.
     * @return An ARCReader.
     * @throws IOException 
     * @throws MalformedURLException 
     * @throws IOException 
     */
    public static ARCReader get(String arcFileOrUrl)
    throws MalformedURLException, IOException {
        return UURI.hasScheme(arcFileOrUrl)?
            get(new URL(arcFileOrUrl)): get(new File(arcFileOrUrl));
    }
    
    /**
     * @param arcFile An arcfile to read.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final File arcFile) throws IOException {
        return get(arcFile, false, 0);
    }
    
    /**
     * @param arcFile An arcfile to read.
     * @param offset Have returned ARCReader set to start reading at passed
     * offset.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final File arcFile, final long offset)
    throws IOException {
        return get(arcFile, false, offset);
    }
    
    /**
     * @param arcFile An arcfile to read.
     * @param skipSuffixTest Set to true if want to test that ARC has proper
     * suffix. Use this method and pass <code>false</code> to open ARCs
     * with the <code>.open</code> or otherwise suffix.
     * @param offset Have returned ARCReader set to start reading at passed
     * offset.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final File arcFile,
            final boolean skipSuffixTest, final long offset)
    throws IOException {
        boolean compressed = testCompressedARCFile(arcFile, skipSuffixTest);
        if (!compressed) {
            if (!FileUtils.isReadableWithExtensionAndMagic(arcFile,
                    ARC_FILE_EXTENSION, ARC_MAGIC_NUMBER)) {
                throw new IOException(arcFile.getAbsolutePath() +
                    " is not an Internet Archive ARC file.");
            }
        }
        return compressed?
            (ARCReader)ARCReaderFactory.factory.
                new CompressedARCReader(arcFile, offset):
            (ARCReader)ARCReaderFactory.factory.
                new UncompressedARCReader(arcFile, offset);
    }
    
    protected static ARCReader get(final String arc, final InputStream is,
        final boolean atFirstRecord)
    throws IOException {
        // For now, assume stream is compressed.  Later add test of input
        // stream or handle exception thrown when figure not compressed stream.
        return ARCReaderFactory.factory.new CompressedARCReader(arc,
            is, atFirstRecord);
    }
    
    private static void addUserAgent(final HttpURLConnection connection) {
        connection.addRequestProperty("User-Agent", ARCReader.class.getName());
    }
    
    /**
     * Get an ARCReader aligned at <code>offset</code>.
     * This version of get will not bring the ARC local but will try to
     * stream across the net making an HTTP 1.1 Range request on remote
     * http server (RFC1435 Section 14.35).
     * @param arcUrl HTTP URL for an ARC (All ARCs considered remote).
     * @param offset Offset into ARC at which to start fetching.
     * @return An ARCReader aligned at offset.
     * @throws IOException
     */
    public static ARCReader get(final URL arcUrl, final long offset)
    throws IOException {
        // Get URL connection.
        URLConnection connection = arcUrl.openConnection();
        if (!(connection instanceof HttpURLConnection)) {
            throw new IOException("This method only handles HTTP connections.");
        }
        addUserAgent((HttpURLConnection)connection);
        // Use a Range request (Assumes HTTP 1.1 on other end). If length <= 0,
        // add open-ended range header to the request.  Else, because end-byte
        // is inclusive, subtract 1.
        connection.addRequestProperty("Range", "bytes=" + offset + "-");
        // TODO: Get feedback on this ARCReader maker. If fetching single
        // record remotely, might make sense to do a slimmed down
        // ARCRecord getter.
        // Good if size 2 * inflator buffer to avoid buffer boundaries
        // (TODO: Implement better handling across buffer boundaries).
        return get(arcUrl.toString(),
            new RepositionableInputStream(connection.getInputStream(),
                16 * 1024),
            (offset == 0));
    }
    
    /**
     * Get an ARCReader.
     * Pulls the ARC local into whereever the System Property
     * <code>java.io.tmpdir</code> points. It then hands back an ARCReader that
     * points at this local copy.  A close on this ARCReader instance will
     * remove the local copy.
     * @param arcUrl An URL that points at an ARC.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final URL arcUrl)
    throws IOException {
        // If url represents a local file then return file it points to.
        if (arcUrl.getPath() != null) {
            // TODO: Add scheme check and host check.
            File f = new File(arcUrl.getPath());
            if (f.exists()) {
                return get(f, 0);
            }
        }
        
        // Else bring the ARC local.
        return makeARCLocal(arcUrl.openConnection());
    }
    
    protected static ARCReader makeARCLocal(final URLConnection connection)
    throws IOException {
        File localFile = null;
        if (connection instanceof HttpURLConnection) {
            // If http url connection, bring down the resouce local.
            localFile = File.createTempFile(ARCReader.class.getName(), ".arc",
                FileUtils.TMPDIR);
            connection.connect();
            addUserAgent((HttpURLConnection)connection);
            try {
                IoUtils.readFullyToFile(connection.getInputStream(), localFile,
                        new byte[16 * 1024]);
            } catch (IOException ioe) {
                localFile.delete();
                throw ioe;
            }
        } else if (connection instanceof RsyncURLConnection) {
            // Then, connect and this will create a local file.
            // See implementation of the rsync handler.
            connection.connect();
            localFile = ((RsyncURLConnection)connection).getFile();
        } else if (connection instanceof Md5URLConnection) {
            // Then, connect and this will create a local file.
            // See implementation of the md5 handler.
            connection.connect();
            localFile = ((Md5URLConnection)connection).getFile();
        } else {
            throw new UnsupportedOperationException("No support for " +
                connection);
        }
        
        ARCReader reader = null;
        try {
            reader = get(localFile, true, 0);
        } catch (IOException e) {
            localFile.delete();
            throw e;
        }
        
        // Assign to a final variable so can assign it to inner class
        // data member.
        final ARCReader arcreader = reader;
        
        // Return a delegate that does cleanup of downloaded file on close.
        return new ARCReader() {
            private final ARCReader delegate = arcreader;
            
            public void close() throws IOException {
                this.delegate.close();
                if (this.delegate.arc != null) {
                    File f = new File(this.delegate.arc);
                    if (f.exists()) {
                        f.delete();
                    }
                    this.delegate.arc = null;
                }
            }
            
            public ARCRecord get(long o) throws IOException {
                return this.delegate.get(o);
            }
            
            public boolean isDigest() {
                return this.delegate.isDigest();
            }
            
            public boolean isParseHttpHeaders() {
                return this.delegate.isParseHttpHeaders();
            }
            
            public boolean isStrict() {
                return this.delegate.isStrict();
            }
            
            public Iterator iterator() {
                return this.delegate.iterator();
            }
            
            public void setDigest(boolean d) {
                this.delegate.setDigest(d);
            }
            
            public void setParseHttpHeaders(boolean parse) {
                this.delegate.setParseHttpHeaders(parse);
            }
            
            public void setStrict(boolean s) {
                this.delegate.setStrict(s);
            }
            
            public List validate() throws IOException {
                return this.delegate.validate();
            }
        };
    }
    
    /**
     * @param arcFile File to test.
     * @return True if <code>arcFile</code> is compressed ARC.
     * @throws IOException
     */
    public static boolean isCompressed(File arcFile) throws IOException {
        return testCompressedARCFile(arcFile);
    }
    
    /**
     * Check file is compressed and in ARC GZIP format.
     *
     * @param arcFile File to test if its Internet Archive ARC file
     * GZIP compressed.
     *
     * @return True if this is an Internet Archive GZIP'd ARC file (It begins
     * w/ the Internet Archive GZIP header and has the
     * COMPRESSED_ARC_FILE_EXTENSION suffix).
     *
     * @exception IOException If file does not exist or is not unreadable.
     */
    public static boolean testCompressedARCFile(File arcFile)
    throws IOException {
        return testCompressedARCFile(arcFile, false);
    }

    /**
     * Check file is compressed and in ARC GZIP format.
     *
     * @param arcFile File to test if its Internet Archive ARC file
     * GZIP compressed.
     * @param skipSuffixCheck Set to true if we're not to test on the
     * '.arc.gz' suffix.
     *
     * @return True if this is an Internet Archive GZIP'd ARC file (It begins
     * w/ the Internet Archive GZIP header).
     *
     * @exception IOException If file does not exist or is not unreadable.
     */
    public static boolean testCompressedARCFile(File arcFile,
            boolean skipSuffixCheck)
    throws IOException {
        boolean compressedARCFile = false;
        FileUtils.isReadable(arcFile);
        if(!skipSuffixCheck && !arcFile.getName().toLowerCase()
                .endsWith(COMPRESSED_ARC_FILE_EXTENSION)) {
            return compressedARCFile;
        }
        
        final InputStream is = new FileInputStream(arcFile);
        try {
            compressedARCFile = testCompressedARCStream(is);
        } finally {
            is.close();
        }
        return compressedARCFile;
    }
    
    /**
     * Tests passed stream is gzip stream by reading in the HEAD.
     * Does not reposition the stream.  That is left up to the caller.
     * @param is An InputStream.
     * @return True if compressed stream.
     * @throws IOException
     */
    public static boolean testCompressedARCStream(final InputStream is)
            throws IOException {
        boolean compressedARCFile = false;
        GzipHeader gh = null;
        try {
            gh = new GzipHeader(is);
        } catch (NoGzipMagicException e ) {
            return compressedARCFile;
        }
        
        byte[] fextra = gh.getFextra();
        // Now make sure following bytes are IA GZIP comment.
        // First check length. ARC_GZIP_EXTRA_FIELD includes length
        // so subtract two and start compare to ARC_GZIP_EXTRA_FIELD
        // at +2.
        if (fextra != null &&
                ARC_GZIP_EXTRA_FIELD.length - 2 == fextra.length) {
            compressedARCFile = true;
            for (int i = 0; i < fextra.length; i++) {
                if (fextra[i] != ARC_GZIP_EXTRA_FIELD[i + 2]) {
                    compressedARCFile = false;
                    break;
                }
            }
        }
        return compressedARCFile;
    }

    /**
     * Uncompressed arc file reader.
     * @author stack
     */
    private class UncompressedARCReader extends ARCReader {
        /**
         * Constructor.
         * @param f Uncompressed arcfile to read.
         * @throws IOException
         */
        public UncompressedARCReader(final File f)
        throws IOException {
            this(f, 0);
        }

        /**
         * Constructor.
         * 
         * @param f Uncompressed arcfile to read.
         * @param offset Offset at which to position ARCReader.
         * @throws IOException
         */
        public UncompressedARCReader(final File f, final long offset)
        throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = getInputStream(f, offset);
            initialize(f.getAbsolutePath());
        }
        
        /**
         * Constructor.
         * 
         * @param f Uncompressed arc to read.
         * @param is InputStream.
         */
        public UncompressedARCReader(final String f, final InputStream is) {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = is;
            initialize(f);
        }
    }
    
    /**
     * Compressed arc file reader.
     * 
     * @author stack
     */
    private class CompressedARCReader extends ARCReader {

        /**
         * Constructor.
         * 
         * @param f
         *            Compressed arcfile to read.
         * @throws IOException
         */
        public CompressedARCReader(final File f) throws IOException {
            this(f, 0);
        }

        /**
         * Constructor.
         * 
         * @param f Compressed arcfile to read.
         * @param offset Position at where to start reading file.
         * @throws IOException
         */
        public CompressedARCReader(final File f, final long offset)
                throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = new GzippedInputStream(getInputStream(f, offset));
            this.compressed = (offset == 0);
            initialize(f.getAbsolutePath());
        }
        
        /**
         * Constructor.
         * 
         * @param f Compressed arcfile.
         * @param is InputStream to use.
         * @throws IOException
         */
        public CompressedARCReader(final String f, final InputStream is,
            final boolean atFirstRecord)
        throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = new GzippedInputStream(is);
            this.compressed = true;
            this.alignedOnFirstRecord = atFirstRecord;
            initialize(f);
        }
        
        /**
         * Get record at passed <code>offset</code>.
         * 
         * @param offset
         *            Byte index into arcfile at which a record starts.
         * @return An ARCRecord reference.
         * @throws IOException
         */
        public ARCRecord get(long offset) throws IOException {
            cleanupCurrentRecord();
            ((GzippedInputStream)this.in).gzipMemberSeek(offset);
            return createARCRecord(this.in, offset);
        }
        
        public Iterator iterator() {
            /**
             * Override ARCRecordIterator so can base returned iterator on
             * GzippedInputStream iterator.
             */
            return new ARCRecordIterator() {
                private GzippedInputStream gis =
                    (GzippedInputStream)getInputStream();

                private Iterator gzipIterator = this.gis.iterator();

                protected boolean innerHasNext() {
                    return this.gzipIterator.hasNext();
                }

                protected Object innerNext() throws IOException {
                    // Get the positoin before gzipIterator.next moves
                    // it on past the gzip header.
                    long p = this.gis.position();
                    InputStream is = (InputStream) this.gzipIterator.next();
                    return createARCRecord(is, p);
                }
            };
        }
        
        protected void gotoEOR(ARCRecord rec) throws IOException {
            long skipped = ((GzippedInputStream)this.in).
                gotoEOR(LINE_SEPARATOR);
            if (skipped <= 0) {
                return;
            }
            // Report on system error the number of unexpected characters
            // at the end of this record.
            ARCRecordMetaData meta = (this.currentRecord != null)?
                rec.getMetaData(): null;
            String message = "Record ENDING at " +
                ((GzippedInputStream)this.in).position() +
                " has " + skipped + " trailing byte(s): " +
                ((meta != null)? meta.toString(): "");
            if (isStrict()) {
                throw new IOException(message);
            }
            logStdErr(Level.WARNING, message);
        }
    }
}
