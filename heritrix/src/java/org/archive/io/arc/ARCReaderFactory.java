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
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import org.archive.io.GZIPMemberInputStream;
import org.archive.io.MappedByteBufferInputStream;
import org.archive.io.Position;

/**
 * Return an ARCReader.
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
	 * @param arcFile An arcfile to read.
	 * @return An ARCReader.
     * @throws IOException
	 */
	public static ARCReader get(File arcFile) throws IOException {
        boolean compressed =
        		ARCReaderFactory.factory.testCompressedARCFile(arcFile);
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
    public boolean testCompressedARCFile(File arcFile) throws IOException {
        boolean compressedARCFile = false;
        isReadable(arcFile);
        if(arcFile.getName().toLowerCase()
            .endsWith(COMPRESSED_ARC_FILE_EXTENSION)) {
        	
            FileInputStream fis = new FileInputStream(arcFile);
            int readLength = DEFAULT_GZIP_HEADER_LENGTH +
                ARC_GZIP_EXTRA_FIELD.length;
            byte [] b = new byte[readLength];
            int read = fis.read(b, 0, readLength);
            fis.close();
            if (read == readLength) {
                if (b[0] == GZIP_HEADER_BEGIN[0]
                     && b[1] == GZIP_HEADER_BEGIN[1]
                     && b[2] == GZIP_HEADER_BEGIN[2]) {
                    // Now make sure following bytes are IA GZIP comment.
                    compressedARCFile = true;
                    for (int i = 0; i < ARC_GZIP_EXTRA_FIELD.length; i++) {
                        if (b[DEFAULT_GZIP_HEADER_LENGTH + i] !=
                            		ARC_GZIP_EXTRA_FIELD[i]) {
                            compressedARCFile = false;
                            break;
                        }
                    }
                }
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
            byte [] b = new byte[ARC_MAGIC_NUMBER.length()];
            int read = fis.read(b, 0, ARC_MAGIC_NUMBER.length());
            fis.close();
            if (read == ARC_MAGIC_NUMBER.length()) {
                StringBuffer beginStr
                    = new StringBuffer(ARC_MAGIC_NUMBER.length());
                for (int i = 0; i < ARC_MAGIC_NUMBER.length(); i++) {
                    beginStr.append((char)b[i]);
                }

                if (beginStr.toString().equalsIgnoreCase(ARC_MAGIC_NUMBER)) {
                    uncompressedARCFile = true;
                }
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
		 * @param arcfile Uncompressed arcfile to read.
         * @throws IOException
		 */
		public UncompressedARCReader(File arcfile) throws IOException {
			// Arc file has been tested for existence by time it has come
			// to here.
            this.in = new MappedByteBufferInputStream(initialize(arcfile));
			// Read in the arcfile header.
            createARCRecord(this.in, ((Position)this.in).getFilePointer());
		}
	}
	
	/**
	 * Compressed arc file reader.
	 * @author stack
	 */
	private class CompressedARCReader extends ARCReader {

		/**
		 * Constructor.
		 * @param arcfile Uncompressed arcfile to read.
         * @throws IOException
		 */
		public CompressedARCReader(File arcfile) throws IOException {
			// Arc file has been tested for existence by time it has come
			// to here.
			this.in = new GZIPMemberInputStream(initialize(arcfile));
            // Read in arcfile header record.
            createARCRecord(this.in, ((Position)this.in).getFilePointer());
		}

        /**
         * Return the next record.
         *
         * Override so can move the underlying stream on to the next gzip
         * member.
         *
         * <p>Its unpredictable what will happen if you do not call hasNext
         * before you come in here for another record (This method does not
         * call hasNext for you).
         *
         * @return Next ARCRecord else null if no more records left.  You need to
         * cast result to ARCRecord.
         */
        public Object next() {
            if (this.currentRecord != null) {
                // Call close on any extant record.  This will scoot us past
                // any content not yet read.
                try {
                    cleanupCurrentRecord();
                } catch (IOException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
            }

            // Here, move the underlying gzip member input stream on to
            // the next record.
            ((GZIPMemberInputStream)this.in).next();

            try {
                return createARCRecord(this.in,
                    ((Position)this.in).getFilePointer());
            } catch (IOException e) {
                throw new NoSuchElementException(e.getClass() + ": " +
                    e.getMessage());
            }
        }

        /**
         * Create new arc record.
         *
         * Override so can attach a GZIPInputStream onto the passed stream.
         *
         * <p>Encapsulate housekeeping that has to do w/ creating a new record.
         *
         * <p>Call this method at end of constructor to read in the
         * arcfile header.  Will be problems reading subsequent arc records
         * if you don't since arcfile header has the list of metadata fields for
         * all records that follow.
         *
         * @param is InputStream to use.
         * @param offset Absolute offset into arc file.
         * @throws IOException
         * @return An arc record.
         */
		protected ARCRecord createARCRecord(InputStream is, long offset)
				throws IOException {
			return super.createARCRecord(new GZIPInputStream(is), offset);
		}
	}
}
