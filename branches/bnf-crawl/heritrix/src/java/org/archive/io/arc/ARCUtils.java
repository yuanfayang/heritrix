/* ARCUtils
 *
 * Created on Aug 10, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
import java.net.URI;
import java.net.URISyntaxException;

import org.archive.io.GzipHeader;
import org.archive.net.UURI;

public class ARCUtils implements ARCConstants {
    /**
     * @param pathOrUri Path or URI to extract arc filename from.
     * @return Extracted arc file name.
     * @throws URISyntaxException 
     */
    public static String parseArcFilename(final String pathOrUri)
    throws URISyntaxException {
        String path = pathOrUri;
        if (UURI.hasScheme(pathOrUri)) {
            URI url = new URI(pathOrUri);
            path = url.getPath();
        }
        return (new File(path)).getName();
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
    public static boolean testUncompressedARCFile(File arcFile)
    throws IOException {
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
    private static void isReadable(File arcFile) throws IOException {
        if (!arcFile.exists()) {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " does not exist.");
        }

        if (!arcFile.canRead()) {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " is not readable.");
        }
    }
}
