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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.archive.io.GzipHeader;


/**
 * Return an ARCReader.
 *
 * @author stack
 */
public class ARCReaderFactory2 implements ARCConstants {

    /**
     * GZIPInputStream which can report how much it 'overread' 
     * underlying stream (grabbing data that wasn't part of the
     * GZIP record it was reading.
     * 
     * @author gojomo
     */
    public class GZIPInputStream2 extends GZIPInputStream {
        public int getOverread() {
            // overread is the 'remaining' in the inflate buffer,
            // minus the 8-byte crc that was used. If remaining was 
            // less than 8, crc was read (at least partially) from
            // stream, so stream is cued up properly. 
            // TODO: verify on larger set of ARCs, this could be wrong
            return Math.max(inf.getRemaining()-8, 0);
        }
        /**
         * @param in
         * @throws IOException
         */
        public GZIPInputStream2(InputStream in) throws IOException {
            super(in);
        }


    }
    /**
     * This factory instance.
     */
    private static final ARCReaderFactory2 factory = new ARCReaderFactory2();

    /**
     * Shutdown any access to default constructor.
     */
    private ARCReaderFactory2() {
        super();
    }
    
    public static boolean isCompressed(File arcFile) throws IOException {
        return ARCReaderFactory2.factory.testCompressedARCFile(arcFile);
    }
    
    /**
     * @param arcFile An arcfile to read.
     * @return An ARCReader.
     * @throws IOException
     */
    public static ARCReader2 get(File arcFile) throws IOException {
        boolean compressed = isCompressed(arcFile);
        if (!compressed) {
            if (!ARCReaderFactory2.factory.testUncompressedARCFile(arcFile)) {
                throw new IOException(arcFile.getAbsolutePath() +
                    " is not an Internet Archive ARC file.");
            }
        }
        return compressed?
            (ARCReader2)ARCReaderFactory2.factory.
                new CompressedARCReader(arcFile):
            (ARCReader2)ARCReaderFactory2.factory.
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
    public boolean testCompressedARCFile(File arcFile) throws IOException {
        boolean compressedARCFile = false;
        isReadable(arcFile);
        if(arcFile.getName().toLowerCase()
            .endsWith(COMPRESSED_ARC_FILE_EXTENSION)) {
            
            FileInputStream fis = new FileInputStream(arcFile);
            try {
                GzipHeader gh = new GzipHeader(new FileInputStream(arcFile));
                byte [] fextra = gh.getFextra();
                // Now make sure following bytes are IA GZIP comment.
                // First check length.  ARC_GZIP_EXTRA_FIELD includes length
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
        }

        return compressedARCFile;
    }

    /**
     * Check file is uncompressed ARC file.
     *
     * @param arcFile File to test if its Internet Archive ARC file
     * uncompressed.
     *
     * @return True if this is an Internet Archive GZIP'd ARC file (It begins
     * w/ the Internet Archive GZIP header and has the
     * COMPRESSED_ARC_FILE_EXTENSION suffix).
     *
     * @exception IOException If file does not exist or is not unreadable.
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
    private class UncompressedARCReader extends ARCReader2 {
        
        /**
         * Constructor.
         * @param arcfile Uncompressed arcfile to read.
         * @throws IOException
         */
        public UncompressedARCReader(File arcFile) throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.rawStream = new MarkableFileInputStream(arcFile);
            initialize(arcFile);
        }

        /* (non-Javadoc)
         * @see org.archive.io.arc.ARCReader2#getReadAllAvailable()
         */
        boolean getReadAllAvailable() {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see org.archive.io.arc.ARCReader2#getReadStream()
         */
        InputStream getReadStream() {
            // TODO Auto-generated method stub
            return this.rawStream;
        }
    }
    
    /**
     * Compressed arc file reader.
     * @author stack
     */
    private class CompressedARCReader extends ARCReader2 {
        GZIPInputStream2 gzipIn;
        
        /**
         * Constructor.
         * @param arcFile Compressed arcfile to read.
         * @throws IOException
         */
        public CompressedARCReader(File arcFile) throws IOException {
            // Arc file has been tested for existence by time it has come
            // to here.
            this.rawStream = new MarkableFileInputStream(arcFile);
            initialize(arcFile);
        }

        /* (non-Javadoc)
         * @see org.archive.io.arc.ARCReader2#getReadAllAvailable()
         */
        boolean getReadAllAvailable() {
            // TODO Auto-generated method stub
            return true;
        }

        /* (non-Javadoc)
         * @see org.archive.io.arc.ARCReader2#getReadStream()
         */
        InputStream getReadStream() throws IOException {
            gzipIn = new GZIPInputStream2(this.rawStream);
            return gzipIn;
        }
       
        /* (non-Javadoc)
         * @see org.archive.io.arc.ARCReader2#cleanupCurrentRecord()
         */
        protected void cleanupCurrentRecord() throws IOException {
            super.cleanupCurrentRecord();
            if (gzipIn != null) {
                // realign based on previous overread by GZIP stream
                int overread = gzipIn.getOverread();
                this.rawStream.getChannel().position(this.rawStream.getChannel().position()-overread);
                gzipIn = null;
            }
        }
    }
}
