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
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.archive.io.GzippedInputStream;
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
     * Currently, this method pulls the ARC local into whereever the System
     * Property 'java.io.tmpdir' points.  It then hands back an ARCReader that
     * points at this local copy.  A close on this ARCReader instance will
     * remove the local copy.
     * @param arcUrl An URL that points at an ARC.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final URL arcUrl) throws IOException {
        // If url represents a local file -- i.e. has scheme of 'file' -- then
        // return the file it points to.
        if (arcUrl.getPath() != null) {
            File f = new File(arcUrl.getPath());
            if (f.exists()) {
                return get(f);
            }
        }
        
        URLConnection connection = arcUrl.openConnection();
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
            connection.connect();
            localFile = ((RsyncURLConnection)connection).getFile();
        } else {
            throw new UnsupportedOperationException("No support for " +
                connection);
        }
        
        ARCReader reader = null;
        try {
            reader = get(localFile, true);
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
                if (this.delegate.arcFile != null &&
                        this.delegate.arcFile.exists()) {
                    this.delegate.arcFile.delete();
                    this.delegate.arcFile = null;
                }
            }
            
            public ARCRecord get(long offset) throws IOException {
                return this.delegate.get(offset);
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
     * @param arcFile An arcfile to read.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ARCReader get(final File arcFile) throws IOException {
        return get(arcFile, false);
    }
    
    protected static ARCReader get(final File arcFile,
            final boolean skipSuffixTest)
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
                new CompressedARCReader(arcFile):
            (ARCReader)ARCReaderFactory.factory.
                new UncompressedARCReader(arcFile);
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
        public UncompressedARCReader(File f)
        throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = getInputStream(f);
            initialize(f);
        }
    }
    
    /**
     * Compressed arc file reader.
     * @author stack
     */
    private class CompressedARCReader extends ARCReader {
        /**
         * Constructor.
         * @param f Compressed arcfile to read.
         * @throws IOException
         */
        public CompressedARCReader(File f)
        throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.in = new GzippedInputStream(getInputStream(f));
            this.compressed = true;
            initialize(f);
        }
        
        /**
         * Get record at passed <code>offset</code>.
         * 
         * @param offset Byte index into arcfile at which a record starts.
         * @return An ARCRecord reference.
         * @throws IOException
         */
        public ARCRecord get(long offset) throws IOException {
            cleanupCurrentRecord();
            ((GzippedInputStream)this.in).gzipMemberSeek(offset);
            return createARCRecord(this.in, offset);
        }
        
        public Iterator iterator() {
            // Override ARCRecordIterator so can base returned iterator
            // on GzippedInputStream iterator.
            return new ARCRecordIterator() {
                private GzippedInputStream gis =
                    (GzippedInputStream)getInputStream();
                private Iterator gzipIterator = this.gis.iterator();
                
                protected boolean innerHasNext() {
                    return this.gzipIterator.hasNext();
                }

                public Object next() {
                    try {
                        long offset = this.gis.position();
                        return createARCRecord((InputStream)this.gzipIterator.
                            next(), offset);
                    } catch (RecoverableIOException e) {
                        getLogger().warning("Recoverable error: " + e.getMessage());
                        if (hasNext()) {
                            return next();
                        }
                        return null;
                    } catch (IOException e) {
                        throw new NoSuchElementException(e.getClass() + ": "
                                + e.getMessage());
                    }
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
