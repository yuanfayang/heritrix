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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.archive.io.GzippedInputStream;
import org.archive.io.RepositionableInputStream;
import org.archive.net.UURI;
import org.archive.net.rsync.RsyncURLConnection;
import org.archive.util.IoUtils;


/**
 * Factory that returns an ARCReader.
 * 
 * Can handle compressed and uncompressed ARCs.
 *
 * @author stack
 */
public class ARCReaderFactory implements ARCConstants {
    private static final File TMPDIR =
        new File(System.getProperty("java.io.tmpdir", "/tmp"));
    
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
     * Pass an URL to an ARC.
     * Pulls the ARC local into whereever the System Property
     * <code>java.io.tmpdir</code> points. It then hands back an ARCReader that
     * points at this local copy.  A close on this ARCReader instance will
     * remove the local copy.
     * @param arcUrl An URL that points at an ARC.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final URL arcUrl) throws IOException {
        return get(arcUrl, 0);
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
    
    protected static ARCReader get(final File arcFile,
            final boolean skipSuffixTest, final long offset)
    throws IOException {
        boolean compressed =
            ARCUtils.testCompressedARCFile(arcFile, skipSuffixTest);
        if (!compressed) {
            if (!ARCUtils.testUncompressedARCFile(arcFile)) {
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
    
    protected static ARCReader get(final String arc, final InputStream is)
    throws IOException {
        // For now, assume stream is compressed.  Later add test of input
        // stream or handle exception thrown when figure not compressed stream.
        return ARCReaderFactory.factory.new CompressedARCReader(arc, is);
    }
    
    /**
     * Get an ARCReader aligned at <code>offset</code>.
     * @param arcUrl URL of an ARC -- local or remote, file or http or rsync.
     * @param offset Offset into ARC at which to start fetching.  If
     * non-zero, we will not bring the ARC local.
     * @return An ARCReader aligned at offset.
     * @throws IOException
     */
    public static ARCReader get(final URL arcUrl, final long offset)
    throws IOException {
        // If url represents a local file then return file it points to.
        if (arcUrl.getPath() != null) {
            File f = new File(arcUrl.getPath());
            if (f.exists()) {
                return get(f, offset);
            }
        }
        
        // Get URL connection.
        URLConnection connection = arcUrl.openConnection();
        if (connection instanceof HttpURLConnection && offset > 0) {
            // Special handling for case where its a http URL
            // and offset is non-zero. In this case, optimize for getting
            // a single record from the remote location only;
            // don't copy down local the complete ARC file. Use a Range request
            // (Assumes HTTP 1.1 on other end). Add open-ended range header to
            // the request.
            connection.addRequestProperty("Range", "bytes=" + offset + "-");
            return get(arcUrl.toString(),
                new RepositionableInputStream(connection.getInputStream()));
        }
        
        // Else bring the ARC local.
        return makeARCLocal(connection, offset);
    }
    
    protected static ARCReader makeARCLocal(final URLConnection connection,
            final long offset)
    throws IOException {
        File localFile = null;
        if (connection instanceof HttpURLConnection) {
            // If http url connection, bring down the resouce local.
            localFile = File.createTempFile(ARCReader.class.getName(), ".arc",
                    TMPDIR);
            connection.connect();
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
        } else {
            throw new UnsupportedOperationException("No support for " +
                connection);
        }
        
        ARCReader reader = null;
        try {
            reader = get(localFile, true, offset);
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
            this.compressed = true;
            initialize(f.getAbsolutePath());
        }
        
        /**
         * Constructor.
         * 
         * @param f Compressed arcfile.
         * @param is InputStream to use.
         * @throws IOException
         */
        public CompressedARCReader(final String f, final InputStream is)
        throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = new GzippedInputStream(is);
            this.compressed = true;
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
