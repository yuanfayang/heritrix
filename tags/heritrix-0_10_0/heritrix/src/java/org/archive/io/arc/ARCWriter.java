/*
 * ARCWriter
 *
 * $Id$
 *
 * Created on Jun 5, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.archive.io.ReplayInputStream;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;


/**
 * Write ARC files.
 *
 * Assumption is that the caller is managing access to this ARCWriter ensuring
 * only one thread of control accessing this ARC file instance at any one time.
 *
 * <p>ARC files are described here:
 * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
 * File Format</a>.  This class does version 1 of the ARC file format.  It also
 * writes version 1.1 which is version 1 with data stuffed into the body of the
 * first arc record in the file, the arc file meta record itself.
 *
 * <p>An ARC file is three lines of meta data followed by an optional 'body' and
 * then a couple of '\n' and then: record, '\n', record, '\n', record, etc.
 * If we are writing compressed ARC files, then each of the ARC file records is
 * individually gzipped and concatenated together to make up a single ARC file.
 * In GZIP terms, each ARC record is a GZIP <i>member</i> of a total gzip'd
 * file.
 *
 * <p>The GZIPping of the ARC file meta data is exceptional.  It is GZIPped
 * w/ an extra GZIP header, a special Internet Archive (IA) extra header field
 * (e.g. FEXTRA is set in the GZIP header FLG field and an extra field is
 * appended to the GZIP header).  The extra field has little in it but its
 * presence denotes this GZIP as an Internet Archive gzipped ARC.  See RFC1952
 * to learn about the GZIP header structure.
 *
 * <p>This class then does its GZIPping in the following fashion.  Each GZIP
 * member is written w/ a new instance of GZIPOutputStream -- actually
 * ARCWriterGZIPOututStream so we can get access to the underlying stream.
 * The underlying stream stays open across GZIPoutputStream instantiations.
 * For the 'special' GZIPing of the ARC file meta data, we cheat by catching the
 * GZIPOutputStream output into a byte array, manipulating it adding the
 * IA GZIP header, before writing to the stream.
 *
 * <p>I tried writing a resettable GZIPOutputStream and could make it work w/
 * the SUN JDK but the IBM JDK threw NPE inside in the deflate.reset -- its zlib
 * native call doesn't seem to like the notion of resetting -- so I gave up on
 * it.
 *
 * <p>Because of such as the above and troubles with GZIPInputStream, we should
 * write our own GZIP*Streams, ones that resettable and consious of gzip
 * members.
 *
 * <p>This class will write until we hit >= maxSize.  The check is done at
 * record boundary.  Records do not span ARC files.  We will then close current
 * file and open another and then continue writing.
 *
 * <p><b>TESTING: </b>Here is how to test that produced ARC files are good
 * using the
 * <a href="http://www.archive.org/web/researcher/tool_documentation.php">alexa
 * ARC c-tools</a>:
 * <pre>
 * % av_procarc hx20040109230030-0.arc.gz | av_ziparc > \
 *     /tmp/hx20040109230030-0.dat.gz
 * % av_ripdat /tmp/hx20040109230030-0.dat.gz > /tmp/hx20040109230030-0.cdx
 * </pre>
 * Examine the produced cdx file to make sure it makes sense.  Search
 * for 'no-type 0'.  If found, then we're opening a gzip record w/o data to
 * write.  This is bad.
 *
 * <p>You can also do <code>gzip -t FILENAME</code> and it will tell you if the
 * ARC makes sense to GZIP.
 *
 * @author stack
 */
public class ARCWriter implements ARCConstants {
    private static final Logger logger =
        Logger.getLogger(ARCWriter.class.getName());

    /**
     * Max size we allow ARC files to be (bytes).
     *
     * Default is ArcConstants.DEFAULT_MAX_ARC_FILE_SIZE.  Note that ARC
     * files will usually be bigger than maxSize; they'll be maxSize + length
     * to next record boundary.
     */
    private int maxSize = DEFAULT_MAX_ARC_FILE_SIZE;

    /**
     * File prefix for ARCs.
     *
     * Default is ARCConstants.DEFAULT_ARC_FILE_PREFIX.
     */
    private String prefix = DEFAULT_ARC_FILE_PREFIX;

    /**
     * Tail to add to arc file name.
     *
     * Default is no tail.
     */
    private String suffix = "";

    /**
     * Directory into which we drop ARC files.
     */
    private File arcsDir = null;

    /**
     * Use compression flag.
     *
     * Default is ARCConstants.DEFAULT_COMPRESS.
     */
    private boolean compress = DEFAULT_COMPRESS;

    /**
     * Reference to ARC file we're currently writing.
     */
    private File arcFile = null;

    /**
     *  Output stream for arcFile.
     */
    private OutputStream out = null;

    /**
     * A running sequence used making unique ARC file names.
     */
    private static int serialNo = 0;

    /**
     * NumberFormat instance for formatting serial number.
     *
     * Pads serial number with zeros.
     */
    private static NumberFormat serialNoFormatter =
        new DecimalFormat("00000");

    /**
     * Arc file meta data list of files.
     *
     * Can be null.  Else list of file and/or string objects to write into the
     * 'body' of the first record in the arc file, the arc file meta record.
     */
    private final List metadata;

    /**
     * Encoding to use getting bytes from strings.
     *
     * Specify an encoding rather than leave it to chance: i.e whatever the
     * JVMs encoding.  Use an encoding that gets the stream as bytes, not chars.
     */
    private static final String ENCODING = "ISO-8859-1";


    /**
     * Constructor.
     *
     * @param arcsDir Where to drop the ARC files.
     *
     * @exception IOException If passed directory does not exist or is not
     * a directory.
     */
    public ARCWriter(File arcsDir) throws IOException {
        this(arcsDir, DEFAULT_ARC_FILE_PREFIX);
    }

    /**
     * Constructor.
     *
     * @param arcsDir Where to drop the ARC files.
     * @param prefix ARC file prefix to use.
     *
     * @exception IOException If passed directory does not exist or is not
     * a directory.
     */
    public ARCWriter(File arcsDir, String prefix) throws IOException {
        this(arcsDir, prefix, DEFAULT_COMPRESS, DEFAULT_MAX_ARC_FILE_SIZE);
    }

    /**
     * Constructor.
     *
     * @param arcsDir Where to drop the ARC files.
     * @param prefix ARC file prefix to use.  If null, we use
     * DEFAULT_ARC_FILE_PREFIX.
     * @param compress Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param maxSize Maximum size for ARC files written.
     *
     * @exception IOException If passed directory does not exist or is not
     * a directory.
     */
    public ARCWriter(File arcsDir, String prefix, boolean compress, int maxSize)
            throws IOException {
        this(arcsDir, prefix, null, compress, maxSize, null);
    }

    /**
     * Constructor.
     *
     * @param arcsDir Where to drop the ARC files.
     * @param prefix ARC file prefix to use.  If null, we use
     * DEFAULT_ARC_FILE_PREFIX.
     * @param compress Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param maxSize Maximum size for ARC files written.
     * @param suffix ARC file tail to use.  If null, unused.
     * @param metadata Arc file meta data.  Can be null.  Is list of File and/or
     * String objects.
     *
     * @exception IOException If passed directory does not exist or is not
     * a directory.
     */
    public ARCWriter(File arcsDir, String prefix, String suffix,
            boolean compress, int maxSize, List metadata) throws IOException {
        this.arcsDir = ArchiveUtils.ensureWriteableDirectory(arcsDir);
        this.prefix = (prefix != null)? prefix: DEFAULT_ARC_FILE_PREFIX;
        this.compress = compress;
        if (maxSize < 0) {
            throw new IOException("Unreasonable maximum file size: " +
                Integer.toString(maxSize));
        }
        this.maxSize = maxSize;
        if (suffix != null) {
            this.suffix = suffix;
        }
        this.metadata = metadata;
    }

    /**
     * Close any extant ARC file.
     *
     * Will close current ARC file.  Any subsequent attempts at using ARC file
     * will open a new file one.  Provided as a convenience. Used by unit
     * testing code.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (this.out != null) {
            this.out.close();
            this.out = null;
        }
    }

    /**
     * Call this method just before we start to write a new record to the ARC.
     *
     * Call at the end of the writing of a record or just before we start
     * writing a new record.  Will close current ARC file and open a new file
     * if ARC file size has passed out maxSize.
     *
     * @exception IOException
     */
    private void checkARCFileSize() throws IOException {
        if (this.out == null || (this.arcFile.length() > this.maxSize)) {
            createARCFile();
        }
    }

    /**
     * Create an ARC file.
     *
     * @throws IOException
     */
    private void createARCFile() throws IOException {
        close();
        TimestampSerialNumber tsn = getTimestampSerialNumber(this);
        String name = this.prefix + '-' + getUniqueBasename(tsn) +
            ((this.suffix == null || this.suffix.length() <= 0)?
                "": "-" + this.suffix) +
            '.' + ARC_FILE_EXTENSION +
            ((this.compress)? '.' + COMPRESSED_FILE_EXTENSION: "");
        this.arcFile = new File(this.arcsDir, name);
        this.out = new BufferedOutputStream(new FileOutputStream(this.arcFile));
        this.out.write(generateARCFileMetaData(tsn.getNow()));
        logger.fine("Created new arc file: " + this.arcFile.getAbsolutePath());
    }
    
    /**
     * Do static synchronization around getting of counter and timestamp so
     * no chance of a thread getting in between the getting of timestamp and
     * allocation of serial number throwing the two out of alignment.
     * 
     * @param writer An instance of ARCWriter.
     * @return Instance of data structure that has timestamp and serial no.
     */
    private static synchronized TimestampSerialNumber
    		getTimestampSerialNumber(ARCWriter writer) {
        return writer.new TimestampSerialNumber(ArchiveUtils.get14DigitDate(),
            ARCWriter.serialNo++);
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
    private String getUniqueBasename(TimestampSerialNumber tsn) {
        return tsn.getNow() + "-" + 
        	ARCWriter.serialNoFormatter.format(tsn.getSerialNumber());
    }

    /**
     * Reset the serial number.
     */
    public static synchronized void resetSerialNo() {
        ARCWriter.serialNo = 0;
    }

	/**
     * Write out the ARCMetaData.
     *
     * <p>Generate ARC file meta data.  Currently we only do version 1 of the
     * ARC file formats or version 1.1 when metadata has been supplied (We
     * write it into the body of the first record in the arc file).
     *
     * <p>Version 1 metadata looks roughly like this:
     *
     * <pre>filedesc://testWriteRecord-JunitIAH20040110013326-2.arc 0.0.0.0 \\
     *  20040110013326 text/plain 77
     * 1 0 InternetArchive
     * URL IP-address Archive-date Content-type Archive-length
     * </pre>
     *
     * <p>If compress is set, then we generate a header that has been gzipped
     * in the Internet Archive manner.   Such a gzipping enables the FEXTRA
     * flag in the FLG field of the gzip header.  It then appends an extra
     * header field: '8', '0', 'L', 'X', '0', '0', '0', '0'.  The first two
     * bytes are the length of the field and the last 6 bytes the Internet
     * Archive header.  To learn about GZIP format, see RFC1952.  To learn
     * about the Internet Archive extra header field, read the source for
     * av_ziparc which can be found at
     * <code>alexa/vista/alexa-tools-1.2/src/av_ziparc.cc</code>.
     *
     * <p>We do things in this roundabout manner because the java
     * GZIPOutputStream does not give access to GZIP header fields.
     *
     * @param date Date to put into the ARC metadata.
     *
     * @return Byte array filled w/ the arc header.
     */
    private byte [] generateARCFileMetaData(String date) throws IOException {
        int metadataBodyLength = getMetadataLength();
        // If metadata body, then the minor part of the version is '1' rather
        // than '0'.
        String minorVersionPart = (metadataBodyLength > 0)? "1": "0";
        String metadataHeaderLinesTwoAndThree =
            LINE_SEPARATOR +
            "1 " + minorVersionPart + " InternetArchive" +
            LINE_SEPARATOR +
            "URL IP-address Archive-date Content-type Archive-length" +
            LINE_SEPARATOR;
        int recordLength = metadataBodyLength +
            metadataHeaderLinesTwoAndThree.getBytes(ENCODING).length;
        String metadataHeaderStr = ARC_MAGIC_NUMBER + this.arcFile.getName() +
            " 0.0.0.0 " + date + " text/plain " + recordLength +
            metadataHeaderLinesTwoAndThree;
        ByteArrayOutputStream metabaos =
            new ByteArrayOutputStream(recordLength);
        // Write the metadata header.
        metabaos.write(metadataHeaderStr.getBytes(ENCODING));
        // Write the metadata body, if anything to write.
        if (metadataBodyLength > 0) {
            writeMetaData(metabaos);
        }
        // Write out a couple of LINE_SEPARATORs to end this record.
        metabaos.write(("" + LINE_SEPARATOR + LINE_SEPARATOR).
            getBytes(ENCODING));
        // Now get bytes of all just written and compress if flag set.
        byte [] bytes = metabaos.toByteArray();
        if(isCompress())
        {
            // GZIP the header but catch the gzipping into a byte array so we
            // can add the special IA GZIP header to the product.  After
            // manipulations, write to the output stream (The JAVA GZIP
            // implementation does not give access to GZIP header. It
            // produces a 'default' header only).  We can get away w/ these
            // maniupulations because the GZIP 'default' header doesn't
            // do the 'optional' CRC'ing of the header.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOS = new GZIPOutputStream(baos);
            gzipOS.write(bytes, 0, bytes.length);
            gzipOS.close();
            byte [] gzippedMetaData = baos.toByteArray();
            if (gzippedMetaData[3] != 0)
            {
                throw new IOException("The GZIP FLG header is unexpectedly " +
                    " non-zero.  Need to add smarter code that can deal " +
                    " when already extant extra GZIP header fields.");
            }
            // Set the GZIP FLG header to '4' which says that the GZIP header
            // has extra fields.  Then insert the alex {'L', 'X', '0', '0', '0,
            // '0'} 'extra' field.  The IA GZIP header will also set byte
            // 9, the OS byte, to 3 (Unix).  We won't do that since its java
            // that is doing the gzipping.
            gzippedMetaData[3] = 4;
            byte [] assemblyBuffer = new byte[gzippedMetaData.length +
                ARC_GZIP_EXTRA_FIELD.length];
            // '10' in the below is a pointer past the following bytes of the
            // GZIP header: ID1 ID2 CM FLG + MTIME(4-bytes) XFL OS.  See
            // RFC1952 for explaination of the abbreviations just used.
            System.arraycopy(gzippedMetaData, 0, assemblyBuffer, 0, 10);
            System.arraycopy(ARC_GZIP_EXTRA_FIELD, 0, assemblyBuffer, 10,
                ARC_GZIP_EXTRA_FIELD.length);
            System.arraycopy(gzippedMetaData, 10, assemblyBuffer,
                10 + ARC_GZIP_EXTRA_FIELD.length, gzippedMetaData.length - 10);
            bytes = assemblyBuffer;
        }
        return bytes;
    }

    /**
     * Write all metadata to passed <code>baos</code>.
     *
     * @param baos Byte array to write to.
     */
    private void writeMetaData(ByteArrayOutputStream baos)
            throws UnsupportedEncodingException, IOException {
        if (this.metadata == null) {
            return;
        }

        for (Iterator i = this.metadata.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof String) {
                baos.write(((String)obj).getBytes(ENCODING));
            } else if (obj instanceof File) {
                InputStream is = null;
                try {
                    is = new BufferedInputStream(
                        new FileInputStream((File)obj));
                    byte [] buffer = new byte[4096];
                    for (int read = -1; (read = is.read(buffer)) != -1;) {
                        baos.write(buffer, 0, read);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } else {
                logger.severe("Unsupported metadata type: " + obj);
            }
        }
        return;
    }

    /**
     * @return Total length of metadata.
     */
    private int getMetadataLength() throws UnsupportedEncodingException {
        int result = -1;
        if (this.metadata == null) {
            result = 0;
        } else {
            for (Iterator i = this.metadata.iterator(); i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof String) {
                    result += ((String)obj).getBytes(ENCODING).length;
                } else if (obj instanceof File) {
                    result += ((File)obj).length();
                } else {
                    logger.severe("Unsupported metadata type: " + obj);
                }
            }
        }
        return result;
    }

    /**
     * Write a record to ARC file.
     *
     * @param uri URI of page we're writing metaline for.  Candidate URI would
     *        be output of curi.getURIString().
     * @param contentType Content type of content meta line describes.
     * @param hostIP IP of host we got content from.
     * @param fetchBeginTimeStamp Time at which fetch began.
     * @param recordLength Length of the content fetched.
     * @param baos Where to read record content from.
     *
     * @throws IOException
     */
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, int recordLength,
            ByteArrayOutputStream baos) throws IOException {
        preWriteRecordTasks();
        try {
            writeMetaLine(uri, contentType, hostIP, fetchBeginTimeStamp,
                recordLength);
            baos.writeTo(this.out);
            this.out.write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
    }

    /**
     * Write a record to ARC file.
     *
     * TODO: Clean up and have it call the above method that takes a BAOS.
     *
     * @param uri URI of page we're writing metaline for.  Candidate URI would
     *        be output of curi.getURIString().
     * @param contentType Content type of content meta line describes.
     * @param hostIP IP of host we got content from.
     * @param fetchBeginTimeStamp Time at which fetch began.
     * @param recordLength Length of the content fetched.
     * @param ris Where to read record content from.
     *
     * @throws IOException
     */
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, int recordLength, ReplayInputStream ris)
        throws IOException
    {
        preWriteRecordTasks();
        try
        {
            writeMetaLine(uri, contentType, hostIP, fetchBeginTimeStamp,
                    recordLength);
            try
            {
                ris.readFullyTo(this.out);
                long remaining = ris.remaining();
                if (remaining > 0)
                {
                    // TODO: Move this DevUtils out of this class so no
                    // dependency upon it.
                    String message = "Gap between expected and actual: "
                        +  remaining + LINE_SEPARATOR + DevUtils.extraInfo();
                    DevUtils.warnHandle(new Throwable(message), message);
                    while (remaining > 0)
                    {
                        // Pad with zeros
                        this.out.write(0);
                        remaining--;
                    }
                }
            }
            finally
            {
                ris.close();
            }

            // Trailing newline
            this.out.write(LINE_SEPARATOR);
        }
        finally
        {
            postWriteRecordTasks();
        }
    }

    /**
     * Post write tasks.
     *
     * @exception IOException
     */
    private void preWriteRecordTasks()
        throws IOException
    {
        checkARCFileSize();
        if (isCompress())
        {
            // The below construction immediately writes the GZIP 'default'
            // header out on the underlying stream.
            this.out = new ARCWriterGZIPOutputStream(this.out);
        }
    }

    /**
     * Post write tasks.
     *
     * @exception IOException
     */
    private void postWriteRecordTasks()
        throws IOException
    {
        if (isCompress())
        {
            ARCWriterGZIPOutputStream o = (ARCWriterGZIPOutputStream)this.out;
            o.finish();
            o.flush();
            this.out = o.getOut();
            o = null;
        }
    }

    /**
     * Clean-up passed content.
     *
     * Figure out content type, truncate at delimiters [;, ].
     * Truncate multi-part content type header at ';'.
     * Apache httpclient collapses values of multiple instances of the
     * header into one comma-separated value,therefore truncated at ','.
     * Current ia_tools that work with arc files expect 5-column
     * space-separated meta-lines, therefore truncate at ' '.
     *
     * @param contentType Raw content-type.
     *
     * @return Computed content-type made from passed content-type after
     * running it through a set of rules.
     */
    private String processContentType(String contentType)
    {
        if (contentType == null)
        {
            contentType = NO_TYPE_MIMETYPE;
        }
        else if (contentType.indexOf(';') >= 0 )
        {
            contentType = contentType.substring(0,contentType.indexOf(';'));
        }
        else if (contentType.indexOf(',') >= 0 )
        {
            contentType = contentType.substring(0,contentType.indexOf(','));
        }
        else if (contentType.indexOf(' ') >= 0 )
        {
            contentType = contentType.substring(0,contentType.indexOf(' '));
        }

        return contentType;
    }

    /**
     * Write ARC file version 1 metaline.
     *
     * @param uri URI of page we're writing metaline for.  Candidate URI would
     *        be output of curi.getURIString().
     * @param contentType Content type of content meta line describes.
     * @param hostIP IP of host we got content from.
     * @param fetchBeginTimeStamp Time at which fetch began.
     * @param recordLength Length of the content fetched.
     *
     * @throws IOException
     */
    private void writeMetaLine(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, int recordLength)
        throws IOException
    {
        if (fetchBeginTimeStamp <= 0)
        {
            throw new IOException("Bogus fetchBeginTimestamp: " +
                Long.toString(fetchBeginTimeStamp));
        }

        if (hostIP == null)
        {
            throw new IOException("Null hostIP passed.");
        }

        if (uri == null || uri.length() <= 0)
        {
            throw new IOException("URI is empty: " + uri);
        }

        String metaLineStr = uri + HEADER_FIELD_SEPERATOR + hostIP +
            HEADER_FIELD_SEPERATOR +
            ArchiveUtils.get14DigitDate(fetchBeginTimeStamp) +
            HEADER_FIELD_SEPERATOR + processContentType(contentType) +
            HEADER_FIELD_SEPERATOR + recordLength + LINE_SEPARATOR;
        this.out.write(metaLineStr.getBytes("UTF-8"));
     }

    /**
     * @return True if we are using compression.
     */
    public boolean isCompress()
    {
        return this.compress;
    }

    /**
     * @return Maximum file size allowed by this ARCWriter.
     */
    public int getMaxSize()
    {
        return this.maxSize;
    }

    /**
     * @return Prefix being used by this ARCWriter.
     */
    public String getPrefix()
    {
        return this.prefix;
    }

    /**
     * @return Dir ARCare being dropped into.
     */
    public File getArcsDir()
    {
        return this.arcsDir;
    }

    /**
     * Get arcFile.
     *
     * Used by junit test to test for creation.
     *
     * @return Current arcFile.
     */
    public File getArcFile()
    {
        return this.arcFile;
    }

    /**
     * An override so we get access to underlying output stream.
     *
     * @author stack
     */
    private class ARCWriterGZIPOutputStream extends GZIPOutputStream {
        public ARCWriterGZIPOutputStream(OutputStream out) throws IOException {
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
     * Immutable data structure that holds a timestamp and an accompanying
     * serial number.
	 * 
	 * For Igor!
     *
     * @author stack
     */
    private class TimestampSerialNumber {
        private final String now;
        private final int serialNumber;
        
        private TimestampSerialNumber(String now, int serialNo) {
            this.now = now;
            this.serialNumber = serialNo;
        }
        
        /**
         * @return Returns the now.
         */
        public String getNow() {
            return this.now;
        }
        
        /**
         * @return Returns the serialNumber.
         */
        public int getSerialNumber() {
            return this.serialNumber;
        }
    }
}
