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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.archive.io.GzipHeader;
import org.archive.io.GzippedInputStream;
import org.archive.net.UURIFactory;

import it.unimi.dsi.mg4j.io.FastBufferedOutputStream;


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
     * @param arcFile File to test.
     * @return True if <code>arcFile</code> is compressed ARC.
     * @throws IOException
     */
    public static boolean isCompressed(File arcFile) throws IOException {
        return ARCReaderFactory.factory.testCompressedARCFile(arcFile);
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
        return UURIFactory.hasScheme(arcFileOrUrl)?
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
        final File tmpFile = File.createTempFile("ARCReader", ".arc", TMPDIR);
        OutputStream os =
            new FastBufferedOutputStream(new FileOutputStream(tmpFile));
        InputStream is = null;
        final int size = 16 * 1024;
        final byte [] buffer = new byte[size];
        ARCReader reader = null;
        try {
            is = new BufferedInputStream(arcUrl.openConnection().
                getInputStream());
            for (int read = -1; (read = is.read(buffer, 0, size)) != -1;) {
                os.write(buffer, 0, read);
            }
            os.close();
            os = null;
            // Try and get arcreader while inside the try/catch so we cleanup
            // the tmpFile if exceptions.
            reader = get(tmpFile, true);
        } catch (IOException e) {
            tmpFile.delete();
            throw e;
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
        
        // Assign to a final variable so can assign it to inner class
        // data member.
        final ARCReader arcreader = reader;
        
        // Return a delegate that does cleanup of downloaded file on close.
        return new ARCReader() {
            private File fileToCleanup = tmpFile;
            private final ARCReader delegate = arcreader;
            
            public void close() throws IOException {
                this.delegate.close();
                if (this.fileToCleanup != null && this.fileToCleanup.exists()) {
                    this.fileToCleanup.delete();
                    this.fileToCleanup = null;
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
        boolean compressed = ARCReaderFactory.factory.
            testCompressedARCFile(arcFile, skipSuffixTest);
        if (!compressed) {
            if (!ARCReaderFactory.factory.testUncompressedARCFile(arcFile)) {
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
    public boolean testCompressedARCFile(File arcFile)
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
    public boolean testCompressedARCFile(File arcFile, boolean skipSuffixCheck)
    throws IOException {
        boolean compressedARCFile = false;
        isReadable(arcFile);
        if(!skipSuffixCheck && !arcFile.getName().toLowerCase()
                .endsWith(COMPRESSED_ARC_FILE_EXTENSION)) {
            return compressedARCFile;
        }
        
            FileInputStream fis = new FileInputStream(arcFile);
        try {
            GzipHeader gh = new GzipHeader(new FileInputStream(arcFile));
            byte[] fextra = gh.getFextra();
            // Now make sure following bytes are IA GZIP comment.
            // First check length. ARC_GZIP_EXTRA_FIELD includes length
            // so subtract two and start compare to ARC_GZIP_EXTRA_FIELD
            // at +2.
            if (ARC_GZIP_EXTRA_FIELD.length - 2 == fextra.length) {
                compressedARCFile = true;
                for (int i = 0; i < fextra.length; i++) {
                    if (fextra[i] != ARC_GZIP_EXTRA_FIELD[i + 2]) {
                        compressedARCFile = false;
                        break;
                    }
                }
            }
        } finally {
            fis.close();
        }
        return compressedARCFile;
    }

    /**
     * Check file is uncompressed ARC file.
     * 
     * @param arcFile
     *            File to test if its Internet Archive ARC file uncompressed.
     * 
     * @return True if this is an Internet Archive ARC file.
     * 
     * @exception IOException
     *                If file does not exist or is not unreadable.
     */
    public boolean testUncompressedARCFile(File arcFile) throws IOException {
        boolean uncompressedARCFile = false;
        isReadable(arcFile);
        if(arcFile.getName().toLowerCase().endsWith(ARC_FILE_EXTENSION)) {
            FileInputStream fis = new FileInputStream(arcFile);
            try {
                byte [] b = new byte[ARC_MAGIC_NUMBER.length()];
                int read = fis.read(b, 0, ARC_MAGIC_NUMBER.length());
                fis.close();
                if (read == ARC_MAGIC_NUMBER.length()) {
                    StringBuffer beginStr
                        = new StringBuffer(ARC_MAGIC_NUMBER.length());
                    for (int i = 0; i < ARC_MAGIC_NUMBER.length(); i++) {
                        beginStr.append((char)b[i]);
                    }
                    
                    if (beginStr.toString().
                            equalsIgnoreCase(ARC_MAGIC_NUMBER)) {
                        uncompressedARCFile = true;
                    }
                }
            } finally {
                fis.close();
            }
        }

        return uncompressedARCFile;
    }

    /**
     * @param arcFile File to test.
      * @exception IOException If file does not exist or is not unreadable.
     */
    private void isReadable(File arcFile) throws IOException {
        if (!arcFile.exists()) {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " does not exist.");
        }

        if (!arcFile.canRead()) {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " is not readable.");
        }
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
