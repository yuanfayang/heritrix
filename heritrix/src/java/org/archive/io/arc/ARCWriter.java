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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.archive.io.GzippedInputStream;
import org.archive.io.ReplayInputStream;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.archive.util.IoUtils;
import org.archive.util.MimetypeUtils;


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
 * <p>While being written, ARCs have a '.open' suffix appended.
 *
 * @author stack
 */
public class ARCWriter implements ARCConstants {
    private static final Logger logger =
        Logger.getLogger(ARCWriter.class.getName());
    private ARCWriterSettings settings = null;

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
     * Directories round-robin index.
     */
    private static int roundRobinIndex = 0;

    /**
     * NumberFormat instance for formatting serial number.
     *
     * Pads serial number with zeros.
     */
    private static NumberFormat serialNoFormatter =
        new DecimalFormat("00000");
    
    /**
     * Metadata line pattern.
     */
    private static final Pattern METADATA_LINE_PATTERN =
        Pattern.compile("^\\S+ \\S+ \\S+ \\S+ \\S+(" + LINE_SEPARATOR + "?)$");
    
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s");

    /**
     * Suffix given to files currently being written by Heritrix.
     */
    public static final String OCCUPIED_SUFFIX = ".open";
    
    public static final String UTF8 = "UTF-8";

    
    /**
     * Constructor.
     * Takes a stream.
     * @param out Where to write.
     * @param arc What to use as arc file.
     * @param cmprs Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param metadata Arc file meta data.  Can be null.  Is list of File and/or
     * String objects.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public ARCWriter(final PrintStream out, final File arc,
            final boolean cmprs, final List metadata,
            String a14DigitDate)
    throws IOException {
        this.settings = new ARCWriterSettingsImpl(cmprs, metadata);
        this.out = out;
        this.arcFile = arc;
        a14DigitDate = (a14DigitDate == null)?
            ArchiveUtils.get14DigitDate(): a14DigitDate;
        this.out.write(generateARCFileMetaData(a14DigitDate));
    }
    
    /**
     * Constructor.
     *
     * @param dirs Where to drop the ARC files.
     * @param prefix ARC file prefix to use.  If null, we use
     * DEFAULT_ARC_FILE_PREFIX.
     * @param cmprs Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param maxSize Maximum size for ARC files written.
     */
    public ARCWriter(final List dirs, final String prefix, 
            final boolean cmprs, final int maxSize) {
        this(dirs, prefix, "", cmprs, maxSize, null);
    }
            
    /**
     * Constructor.
     *
     * @param dirs Where to drop the ARC files.
     * @param prefix ARC file prefix to use.  If null, we use
     * DEFAULT_ARC_FILE_PREFIX.
     * @param cmprs Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param maxSize Maximum size for ARC files written.
     * @param suffix ARC file tail to use.  If null, unused.
     * @param meta Arc file meta data.  Can be null.  Is list of File and/or
     * String objects.
     */
    public ARCWriter(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize, final List meta) {
        this.settings =  new ARCWriterSettingsImpl(dirs, maxSize, prefix,
            cmprs, suffix, meta);
    }
    
    /**
     * Constructor.
     * @param settings
     */
    public ARCWriter(ARCWriterSettings settings) {
        this.settings = settings;
    }
    
    /**
     * Close any extant ARC file.
     *
     * Will close current ARC file.  Any subsequent attempts at using ARC file
     * will open a new file.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (this.out == null) {
            return;
        }
        this.out.close();
        this.out = null;
        if (this.arcFile != null && this.arcFile.exists()) {
            String path = this.arcFile.getAbsolutePath();
            if (path.endsWith(OCCUPIED_SUFFIX)) {
                File f = new File(path.substring(0,
                        path.length() - OCCUPIED_SUFFIX.length()));
                if (!this.arcFile.renameTo(f)) {
                    logger.warning("Failed rename of " + path);
                }
                this.arcFile = f;
            }
            logger.info("Closed " + this.arcFile.getAbsolutePath() +
                    ", size " + this.arcFile.length());
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
        if (this.out == null ||
                (this.settings.getArcMaxSize() != -1 &&
                   (this.arcFile.length() > this.settings.getArcMaxSize()))) {
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
        String name = this.settings.getArcPrefix() + '-' +
            getUniqueBasename(tsn) +
            ((this.settings.getArcSuffix() == null ||
                    this.settings.getArcSuffix().length() <= 0)?
                "": "-" + this.settings.getArcSuffix()) +
            '.' + ARC_FILE_EXTENSION +
            ((this.settings.isCompressed())?
                '.' + COMPRESSED_FILE_EXTENSION: "") +
            OCCUPIED_SUFFIX;
        File dir = getNextDirectory(this.settings.getOutputDirs());
        this.arcFile = new File(dir, name);
        this.out = new BufferedOutputStream(new FileOutputStream(this.arcFile));
        this.out.write(generateARCFileMetaData(tsn.getNow()));
        logger.info("Opened " + this.arcFile.getAbsolutePath());
    }
    
    /**
     * @param dirs List of File objects that point at directories.
     * @return Find next directory to write an arc too.  If more
     * than one, it tries to round-robin through each in turn.
     * @throws IOException
     */
    protected File getNextDirectory(List dirs)
    throws IOException {
        if (ARCWriter.roundRobinIndex >= dirs.size()) {
            ARCWriter.roundRobinIndex = 0;
        }
        File d = null;
        try {
            d = checkWriteable((File)dirs.get(ARCWriter.roundRobinIndex));
        } catch (IndexOutOfBoundsException e) {
            // Dirs list might be altered underneath us.
            // If so, we get this exception -- just keep on going.
        }
        if (d == null && dirs.size() > 1) {
            for (Iterator i = dirs.iterator(); d == null && i.hasNext();) {
                d = checkWriteable((File)i.next());
            }
        } else {
            ARCWriter.roundRobinIndex++;
        }
        if (d == null) {
            throw new IOException("None of these directories are usable.");
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
            d = null;
            logger.warning("Directory " + d.getPath() + " is not" +
                " writeable or cannot be created: " + e.getMessage());
        }
        return d;
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
	 * @throws IOException
     */
    private byte [] generateARCFileMetaData(String date)
    throws IOException {
        int metadataBodyLength = getMetadataLength();
        // If metadata body, then the minor part of the version is '1' rather
        // than '0'.
        String metadataHeaderLinesTwoAndThree =
            getMetadataHeaderLinesTwoAndThree("1 " +
                ((metadataBodyLength > 0)? "1": "0"));
        int recordLength = metadataBodyLength +
            metadataHeaderLinesTwoAndThree.getBytes(DEFAULT_ENCODING).length;
        String metadataHeaderStr = ARC_MAGIC_NUMBER + getArcName() +
            " 0.0.0.0 " + date + " text/plain " + recordLength +
            metadataHeaderLinesTwoAndThree;
        ByteArrayOutputStream metabaos =
            new ByteArrayOutputStream(recordLength);
        // Write the metadata header.
        metabaos.write(metadataHeaderStr.getBytes(DEFAULT_ENCODING));
        // Write the metadata body, if anything to write.
        if (metadataBodyLength > 0) {
            writeMetaData(metabaos);
        }
        
        // Write out a LINE_SEPARATORs to end this record.
        metabaos.write(LINE_SEPARATOR);
        
        // Now get bytes of all just written and compress if flag set.
        byte [] bytes = metabaos.toByteArray();
        
        if(this.settings.isCompressed()) {
            // GZIP the header but catch the gzipping into a byte array so we
            // can add the special IA GZIP header to the product.  After
            // manipulations, write to the output stream (The JAVA GZIP
            // implementation does not give access to GZIP header. It
            // produces a 'default' header only).  We can get away w/ these
            // maniupulations because the GZIP 'default' header doesn't
            // do the 'optional' CRC'ing of the header.
            byte [] gzippedMetaData = GzippedInputStream.gzip(bytes);
            if (gzippedMetaData[3] != 0) {
                throw new IOException("The GZIP FLG header is unexpectedly " +
                    " non-zero.  Need to add smarter code that can deal " +
                    " when already extant extra GZIP header fields.");
            }
            // Set the GZIP FLG header to '4' which says that the GZIP header
            // has extra fields.  Then insert the alex {'L', 'X', '0', '0', '0,
            // '0'} 'extra' field.  The IA GZIP header will also set byte
            // 9 (zero-based), the OS byte, to 3 (Unix).  We'll do the same.
            gzippedMetaData[3] = 4;
            gzippedMetaData[9] = 3;
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
    
    public String getMetadataHeaderLinesTwoAndThree(String version) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(LINE_SEPARATOR);
        buffer.append(version);
        buffer.append(" InternetArchive");
        buffer.append(LINE_SEPARATOR);
        buffer.append("URL IP-address Archive-date Content-type Archive-length");
        buffer.append(LINE_SEPARATOR);
        return buffer.toString();
    }

    /**
     * Get the (uncompressed) ARC name
     * 
     * @return the filename, as if uncompressed
     */
    private String getArcName() {
        String name = this.arcFile.getName();
        if(this.settings.isCompressed() && name.endsWith(".gz")) {
            return name.substring(0,name.length() - 3);
        } else if(this.settings.isCompressed() &&
                name.endsWith(".gz" + OCCUPIED_SUFFIX)) {
            return name.substring(0, name.length() -
                (3 + OCCUPIED_SUFFIX.length()));
        } else {
            return name;
        }
    }

    /**
     * Write all metadata to passed <code>baos</code>.
     *
     * @param baos Byte array to write to.
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private void writeMetaData(ByteArrayOutputStream baos)
            throws UnsupportedEncodingException, IOException {
        if (this.settings.getMetadata() == null) {
            return;
        }

        for (Iterator i = this.settings.getMetadata().iterator();
                i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof String) {
                baos.write(((String)obj).getBytes(DEFAULT_ENCODING));
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
            } else if (obj != null) {
                logger.severe("Unsupported metadata type: " + obj);
            }
        }
        return;
    }

    /**
     * @return Total length of metadata.
     * @throws UnsupportedEncodingException
     */
    private int getMetadataLength()
    throws UnsupportedEncodingException {
        int result = -1;
        if (this.settings.getMetadata() == null) {
            result = 0;
        } else {
            for (Iterator i = this.settings.getMetadata().iterator();
                    i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof String) {
                    result += ((String)obj).getBytes(DEFAULT_ENCODING).length;
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
            ByteArrayOutputStream baos)
    throws IOException {
        preWriteRecordTasks();
        try {
            this.out.write(getMetaLine(uri, contentType, hostIP,
                fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            baos.writeTo(this.out);
            this.out.write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
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
     * @param in Where to read record content from.
     * @throws IOException
     *
     * @throws IOException
     */
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, int recordLength, InputStream in)
    throws IOException {
        write(uri, contentType, hostIP, fetchBeginTimeStamp,
            recordLength, in, new byte[4 * 1024]);
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
     * @param in Where to read record content from.
     * @param buffer Buffer to use.
     *
     * @throws IOException
     */
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, int recordLength,
            InputStream in, byte [] buffer)
    throws IOException {
        preWriteRecordTasks();
        try {
            this.out.write(getMetaLine(uri, contentType, hostIP,
                fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            int read = buffer.length;
            while((read = in.read(buffer)) != -1) {
                this.out.write(buffer, 0, read);
            }
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
    throws IOException {
        preWriteRecordTasks();
        try {
            this.out.write(getMetaLine(uri, contentType, hostIP,
                fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            try {
                ris.readFullyTo(this.out);
                long remaining = ris.remaining();
                if (remaining != 0) // should be zero
                {
                    // TODO: Move this DevUtils out of this class so no
                    // dependency upon it.
                    String message = "Gap between expected and actual: "
                        +  remaining + LINE_SEPARATOR + DevUtils.extraInfo();
                    DevUtils.warnHandle(new Throwable(message), message);
                    while (remaining > 0) {
                        // Pad with zeros
                        this.out.write(0);
                        remaining--;
                    }
                }
            } finally {
                ris.close();
            }

            // Trailing newline
            this.out.write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
    }

    /**
     * Post write tasks.
     * 
     * Has side effects.  Will open new ARC if we're at the upperbound.
     * If we're writing compressed ARCs, it will write the GZIP header
     * out on the stream.
     *
     * @exception IOException
     */
    private void preWriteRecordTasks()
    throws IOException {
        checkARCFileSize();
        if (this.settings.isCompressed()) {
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
    throws IOException {
        if (this.settings.isCompressed()) {
            ARCWriterGZIPOutputStream o = (ARCWriterGZIPOutputStream)this.out;
            o.finish();
            o.flush();
            this.out = o.getOut();
            o = null;
        }
    }
    
    /**
     * @param uri
     * @param contentType
     * @param hostIP
     * @param fetchBeginTimeStamp
     * @param recordLength
     * @return Metadata line for an ARCRecord made of passed components.
     * @exception IOException
     */
    protected String getMetaLine(String uri, String contentType, String hostIP,
        long fetchBeginTimeStamp, int recordLength)
    throws IOException {
        if (fetchBeginTimeStamp <= 0) {
            throw new IOException("Bogus fetchBeginTimestamp: " +
                Long.toString(fetchBeginTimeStamp));
        }

        if (hostIP == null) {
            throw new IOException("Null hostIP passed.");
        }
        hostIP = checkForWhiteSpace(hostIP);
        
        if (contentType == null) {
            throw new IOException("Mimetype is null");
        }
        contentType = checkForWhiteSpace(contentType);
            
        if (uri == null || uri.length() <= 0) {
            throw new IOException("URI is empty: " + uri);
        }

        return validateMetaLine(makeMetaline(uri, hostIP, 
            ArchiveUtils.get14DigitDate(fetchBeginTimeStamp),
            MimetypeUtils.truncate(contentType),
            Integer.toString(recordLength)));
    }
    
    public String makeMetaline(String uri, String hostIP,
            String timeStamp, String mimetype, String recordLength) {
        return uri + HEADER_FIELD_SEPARATOR + hostIP +
            HEADER_FIELD_SEPARATOR + timeStamp +
            HEADER_FIELD_SEPARATOR + mimetype +
            HEADER_FIELD_SEPARATOR + recordLength + LINE_SEPARATOR;
    }
    
    String checkForWhiteSpace(String inStr) {
        Matcher m = WHITE_SPACE.matcher(inStr);
        if (m.find()) {
            // Replace spaces with empty string.
            inStr = m.replaceAll("");
        }
        if (inStr.length() == 0) {
            inStr = "-";
        }
        return inStr;
    }
    
    /**
     * Test that the metadata line is valid before writing.
     * @param metaLineStr
     * @throws IOException
     * @return The passed in metaline.
     */
    protected String validateMetaLine(String metaLineStr)
    throws IOException {
        if (metaLineStr.length() > MAX_METADATA_LINE_LENGTH) {
        	throw new IOException("Metadata line length is " +
                metaLineStr.length() + " which is > than maximum " +
                MAX_METADATA_LINE_LENGTH);
        }
     	Matcher m = METADATA_LINE_PATTERN.matcher(metaLineStr);
        if (!m.matches()) {
        	    throw new IOException("Metadata line doesn't match expected" +
                " pattern: " + metaLineStr);
        }
        return metaLineStr;
    }

    /**
     * @return Settings used by this writer.
     */
    public ARCWriterSettings getSettings() {
        return this.settings;
    }

    /**
     * Get arcFile.
     *
     * Used by junit test to test for creation.
     *
     * @return Current arcFile.
     */
    public File getArcFile() {
        return this.arcFile;
    }
    
    /**
     * Class to hold ARCWriter settings.
     * @author stack
     * @version $Date$, $Revision$
     */
    protected class ARCWriterSettingsImpl
    implements ARCWriterSettings {
        private final List arcDirs;
        private final int arcMaxSize;
        private final String arcPrefix;
        private final boolean compress;
        private final String arcSuffix;
        private final List metadata;
        
        protected ARCWriterSettingsImpl(boolean compress, List metadata) {
            this(null, -1, null, compress, null, metadata);
        }
        
        protected ARCWriterSettingsImpl(List dirs, int maxSize, String prefix, 
                boolean compress, String suffix, List metadata) {
            this.arcDirs = dirs;
            this.arcMaxSize = maxSize;
            this.arcPrefix = prefix;
            this.compress = compress;
            this.arcSuffix = suffix;
            this.metadata = metadata;
        }
        
        public int getArcMaxSize() {
            return this.arcMaxSize;
        }
        
        public String getArcPrefix() {
            return (this.arcPrefix == null)?
                DEFAULT_ARC_FILE_PREFIX: this.arcPrefix;
        }
        
        public String getArcSuffix() {
            return (this.arcSuffix == null)? "": this.arcSuffix;
        }
        
        public List getOutputDirs() {
            return this.arcDirs;
        }
        
        public boolean isCompressed() {
            return this.compress;
        }
        
        public List getMetadata() {
            return this.metadata;
        }
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
