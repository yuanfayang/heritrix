/* ARCReader
 *
 * $Id$
 *
 * Created on Jan 6, 2004
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;


/**
 * ARC file reader.
 *
 * ARC files are described here:
 * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
 * File Format</a>.  This class does version 1 of the ARC file format only.
 *
 * <p>This class knows how to parse an ARC file and has accessors for all of
 * the ARC file content. It can parse ARC Version 1 and 2.
 *
 * <p>{@link java.util.zip.GZIPInputStream} can't deal w/ a GZIP file made of
 * multiple GZIP members.   An instance finds the first GZIP member only.
 * Worse, when its done, its taken the underlying input stream to EOF.  So, it
 * needs to be spoon fed GZIP members by the underlying stream.
 * This is what {@link GZIPMemberPushbackInputStream} does.  It knows if any
 * more GZIP members left in the file.  If there is, we make a new
 * instance of GZIPInputStream to consume (I had trouble developing a reliable
 * reset of an extant instance -- it seems to read the header in the
 * constructor only).
 *
 * <p>ASSUMPTIONS:
 * <ul>
 * <li>That the line delimiter character is '&lt;nl&gt; (0x0A) in ARC files
 * only (The alexa c-code does this so this should be safe assumption).</li>
 * <li>No multibyte content in ARC file record headers.  Assumption is
 * ASCII only.</li>
 * </ul>
 * <p>LIMITATIONS:
 * <ul>
 * <li>Currently you can only read through the ARC file sequentially.  There
 * is no mechanism for backing up the stream.</li>
 * </ul>
 *
  * <p>I wanted to use java.nio memory-mapped files rather than old-school
 * java.io because:
 *
 * "Accessing a file through the memory-mapping mechanism can be far more
 * efficient than reading or writing data by conventional means, even when
 * using channels. No explicit system calls need to be made, which can be
 * time-consuming. More importantly, the virtual memory system of the operating
 * system automatically caches memory pages. These pages will be cached using
 * system memory and will not consume space from the JVM's memory heap.
 * Once a memory page has been made valid (brought in from disk), it can be
 * accessed again at full hardware speed without the need to make another
 * system call to get the data. Large, structured files that contain indexes or
 * other sections that are referenced or updated frequently can benefit
 * tremendously from memory mapping....", from the OReilly Java NIO By Ron
 * Hitchens.
 *
 * <p>Needing to manage buffers for the spoon feeding of GZIP members to
 * GZIPInputStream made me look again at java.nio.  Using a ByteBuffer that
 * holds the whole ARC file for sure makes the code simpler and the nice thing
 * about using memory-mapped buffers for reading is that the memory used is
 * allocated in the OS, not in the JVM.  I played around w/ this on a machine
 * w/ 512M of physical memory and a swap of 1G (/sbin/swapon -s) -- at least I
 * think its a gig.  I made a dumb program to use file channel memory-mapped
 * buffers to read a file.  I was able to read a file of 1.5G using default JVM
 * heap (64M on linux IIRC): i.e. I was able to allocate a buffer of 1.5G inside
 * inside in my small-heap program.  Anything bigger and I got complaints back
 * about  unable to allocate the memory.  So, a channel based reader would
 * limited only by memory characteristics of the machine its running on (swap
 * and physical memory -- not JVM heap size) ONLY, I discovered the following.
 *
 * <p>Really big files generated complaint out of FileChannel.map saying the
 * size parameter was > Integer.MAX_VALUE which is also odd considering the
 * type is long.  This must be an nio bug.  Means there is an upperbound of
 * Integer.MAX_VALUE (about 2.1G or so).  This is unfortunate -- particularly
 * as the c-code tools for ARC manipulations, see alexa/common/a_arcio.c,
 * support > 2.1G -- but its good enough for now (ARC files are usually
 * 100M).
 *
 * <p>The committee seems to still be out regards general nio
 * performance.  See <a
 * href="http://forum.java.sun.com/thread.jsp?forum=4&thread=227539&message=806443">NIO
 * ByteBuffer slower than BufferedInputStream</a>.  It can be 4 times slower
 * than java.io or 40% faster.
 *
 * <p>My first cut at an ARC reader was a ChannelScanARCReader class that got
 * a channel on to the ARC file and from this allocated a memory-mapped buffer
 * to hold the total ARC content.  This buffer was then wrapped by a
 * ByteBufferInputStream and it managed the doling out of
 * GZIP members to the child GZIPInputStream.  It was finding GZIP boundaries
 * by scanning ahead which is probably less optimial than technique used below.
 *
 * <p>TODO: Profiling of the two techniques -- java.io vs. memory-mapped
 * ByteBufferInputStream to see which is faster.  As is, ARCReader is SLOW.
 *
 * <p>TODO: Testing of this reader class against ARC files harvested out in
 * the wilds.  This class has only been tested to date going against small
 * files made by unit tests.  The class needs to be tested that it might
 * develop robustness.
 *
 * <p>TODO: Currently its an instance per file.  Maybe later add an open
 * method so can use one instance to open and close multiple files.
 *
 * @author stack
 */
public class ARCReader
    implements ARCConstants
{
    /**
     * Assumed maximum size of a record meta header line.
     *
     * This 100k which seems massive but its the same as the LINE_LENGTH from
     * <code>alexa/include/a_arcio.h</code>:
     * <pre>
     * #define LINE_LENGTH     (100*1024)
     * </pre>
     */
    private static final int MAX_HEADER_LINE_LENGTH = 1024 * 100;

    /**
     * The ARC file.
     */
    private File arcFile = null;

    /**
     * True if the file we are reading is compressed.
     */
    private boolean compressed = false;

    /**
     * ARC file input stream.
     *
     * Keep it around so we can close it when done.
     */
    private InputStream in = null;

    /**
     * The ARCRecord currently being read.
     *
     * You can only go serially through the file.  This is pointer to current
     * ARCRecord.
     */
    private ARCRecord currentRecord = null;

    /**
     * ARC file version.
     */
    private String version = null;

    /**
     * An array of the header field names found in the ARC file header on
     * the 3rd line.
     */
    private ArrayList headerFieldNameKeys = null;

    /**
     * List of all read metadatas.
     *
     * As we read records, we add a reference to the read metadata.
     *
     * Upon instantiation, will have at least the metadata for the ARC file
     * itself.
     */
    private List metaDatas = new ArrayList();


    public ARCReader(String arcFile)
         throws FileNotFoundException, IOException
    {
        this(new File(arcFile));
    }

    /**
     * Constructor.
     *
     * Opens the passed ARC file and reads in the ARC file header.  When done,
     * we're cue'd up to read ARC records.
     *
     * ARC file header looks like this:
     *
     * <pre>filedesc://testIsBoundary-JunitIAH200401070157520.arc 0.0.0.0 \\
     *      20040107015752 text/plain 77
     * 1 0 InternetArchive
     * URL IP-address Archive-date Content-type Archive-length</pre>
     *
     * @param arcFile ARC file to read.
     *
     * @throws IOException If file is unreadable or trouble reading first
     * few bytes as we test if passed file is legal ARC or if this is not an
     * ARC file.
     */
    public ARCReader(File arcFile)
        throws IOException
    {
        this.compressed = testCompressedARCFile(arcFile);
        if (!this.compressed)
        {
            if (!testUncompressedARCFile(arcFile))
            {
                throw new IOException(arcFile.getAbsolutePath() +
                    " is not an Internet Archive ARC file.");
            }
        }
        this.arcFile = arcFile;

        // Apart from any buffering benefits, there is a dependency on being
        // able to mark and reset the stream if the ARC if uncompressed.
        // See the ARCRecord.skip() method.
        this.in = new BufferedInputStream(new FileInputStream(this.arcFile));
        if (this.compressed)
        {
            this.in = new ARCReaderGZIPInputStream(
                new GZIPMemberPushbackInputStream(this.in));
        }

        // Read in the first ARC record.  Its the meta info for the ARC file.
        // Immediately close it since it doesn't have any content but the
        // meta info itself.  Closing cues us up at the next ARC record in
        // the file.
        createARCRecord().close();
    }

    /**
     * @param arcFile File to test.
      * @exception IOException If file does not exist or is not unreadable.
     */
    private static void isReadable(File arcFile)
        throws IOException
    {
        if (!arcFile.exists())
        {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " does not exist.");
        }

        if (!arcFile.canRead())
        {
            throw new FileNotFoundException(arcFile.getAbsolutePath() +
                " is not readable.");
        }
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
        throws IOException
    {
        boolean compressedARCFile = false;
        isReadable(arcFile);
        if(arcFile.getName().toLowerCase()
                .endsWith(COMPRESSED_ARC_FILE_EXTENSION))
        {
            FileInputStream fis = new FileInputStream(arcFile);
            int readLength = DEFAULT_GZIP_HEADER_LENGTH +
                ARC_GZIP_EXTRA_FIELD.length;
            byte [] b = new byte[readLength];
            int read = fis.read(b, 0, readLength);
            if (read == readLength)
            {
                if (b[0] == GZIP_HEADER_BEGIN[0]
                     && b[1] == GZIP_HEADER_BEGIN[1]
                     && b[2] == GZIP_HEADER_BEGIN[2])
                {
                    // Now make sure following bytes are IA GZIP comment.
                    compressedARCFile = true;
                    for (int i = 0; i < ARC_GZIP_EXTRA_FIELD.length; i++)
                    {
                        if (b[DEFAULT_GZIP_HEADER_LENGTH + i] !=
                            ARC_GZIP_EXTRA_FIELD[i])
                        {
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
    public static boolean testUncompressedARCFile(File arcFile)
        throws IOException
    {
        boolean uncompressedARCFile = false;
        isReadable(arcFile);
        if(arcFile.getName().toLowerCase().endsWith(ARC_FILE_EXTENSION))
        {
            FileInputStream fis = new FileInputStream(arcFile);
            byte [] b = new byte[ARC_MAGIC_NUMBER.length()];
            int read = fis.read(b, 0, ARC_MAGIC_NUMBER.length());
            if (read == ARC_MAGIC_NUMBER.length())
            {
                StringBuffer beginStr
                    = new StringBuffer(ARC_MAGIC_NUMBER.length());
                for (int i = 0; i < ARC_MAGIC_NUMBER.length(); i++)
                {
                    beginStr.append((char)b[i]);
                }

                if (beginStr.toString().equalsIgnoreCase(ARC_MAGIC_NUMBER))
                {
                    uncompressedARCFile = true;
                }
            }
        }

        return uncompressedARCFile;
    }

    public void close()
        throws IOException
    {
        if (this.in != null)
        {
            this.in.close();
            this.in = null;
        }
    }

    protected void finalize()
        throws Throwable
    {
        close();
        super.finalize();
    }

    /**
     * @return True if we have more ARC records to read.
     * @throws IOException
     */
    public boolean hasNext()
        throws IOException
    {
        boolean hasNext = this.in.available() > 0;
        if (!hasNext && isCompressed())
        {
            // Only come in here if we've emptied out the stream.
            // Ask the stream that underlays the GZIPInputStream if there
            // are more GZIP members to be read.
            ARCReaderGZIPInputStream agis
                = (ARCReaderGZIPInputStream)this.in;
            GZIPMemberPushbackInputStream gmpis
                = (GZIPMemberPushbackInputStream)agis.getIn();
            hasNext = gmpis.hasNext();
        }

        return hasNext;
    }

    /**
     * Return the next record.
     *
     * <p>Its unpredictable what will happen if you do not call hasNext before
     * you come in here for another record (This method does not call
     * hasNext for you).
     *
     * @return Next ARCRecord else null if no more records left.
     *
     * @throws IOException
     */
    public ARCRecord next()
        throws IOException
    {
        if (this.currentRecord != null)
        {
            // Call close on any extant record.  This will scoot us past
            // any content not yet read.
            this.currentRecord.close();
        }

        if (isCompressed())
        {
            ARCReaderGZIPInputStream agis = (ARCReaderGZIPInputStream)this.in;
            GZIPMemberPushbackInputStream gmpis =
                (GZIPMemberPushbackInputStream)agis.getIn();
            // Move underlying GZIPMemberInputStream on to the next GZIP member.
            gmpis.next();
            // Create new instance of ARCReaderGZIPInputStream.  Would be better
            // if we could just reset the old instance but I had little luck
            // trying to make a resettable instance of GZIPInputStream.  Doing
            // a new instance is (probably) more costly but cleaner.
            this.in = new ARCReaderGZIPInputStream(gmpis);
            // Make it so no bones about GC'ing old GZIPInputStream instance.
            agis = null;
        }

        return createARCRecord();
    }

    /**
     * Create new arc record.
     *
     * Encapsulate housekeeping that has to do w/ creating a new record.
     */
    private ARCRecord createARCRecord()
        throws IOException
    {
        ArrayList values = getTokenizedHeaderLine();
        boolean contentRead = false;
        if (this.currentRecord == null)
        {
            // If currentRecord is null, then no records have been read yet
            // and we're reading our first one, the record of ARC file meta
            // info.  We've just read the first line.  There are
            // two more.   The second line has misc. info.  We're only
            // interested in the first field, the version number.  The
            // third line is the list of field names.  Here's what ARC file
            // version 1 meta content looks like:
            //
            // filedesc://testIsBoundary-JunitIAH200401070157520.arc 0.0.0.0 \\
            //      20040107015752 text/plain 77
            // 1 0 InternetArchive
            // URL IP-address Archive-date Content-type Archive-length
            //
            this.version = ((String)getTokenizedHeaderLine().get(0));
            this.headerFieldNameKeys = computeHeaderFieldNameKeys();

            // There is no content in the ARC file meta record or, rather,
            // there is but its the lines 2 and 3 that we just read above
            // so set contentRead to true so ARCRecord content position
            // pointer doesn't get out of whack.
            contentRead = true;
        }

        this.currentRecord = new ARCRecord(this.in,
            computeMetaData(this.headerFieldNameKeys, values, this.version),
            contentRead);
        // Add reference to metadata into a list of metadatas.
        this.metaDatas.add(this.currentRecord.getMetaData());
        return this.currentRecord;
    }

    /**
     * Get a record header line as list of tokens.
     *
     * We keep reading till we find a LINE_SEPARATOR or we reach the end
     * of file w/o finding a LINE_SEPARATOR or the line length is crazy.
     *
     * @return List of string tokens.
     *
     * @exception IOException If problem reading stream or no line separator
     * found or EOF before EOL or we didn't get minimum header fields.
     */
    private ArrayList getTokenizedHeaderLine()
        throws IOException
    {
        ArrayList list = new ArrayList(20);
        StringBuffer buffer = new StringBuffer();
        int c = -1;
        for (int i = 0; true; i++)
        {
            if (i > MAX_HEADER_LINE_LENGTH)
            {
                throw new IOException("Header line longer than max allowed " +
                        " -- " + String.valueOf(MAX_HEADER_LINE_LENGTH) +
                " -- or passed buffer doesn't contain a line.");
            }

            c = this.in.read();
            if (c == -1)
            {
                throw new IOException("Hit EOF before header EOL.");
            }

            if (c == LINE_SEPARATOR)
            {
                if (list.size() == 0 && buffer.length() == 0)
                {
                    // Empty line at start of buffer.  Skip it and try again.
                    continue;
                }

                list.add(buffer.toString());
                // LOOP TERMINATION.
                break;
            }
            else if (c == HEADER_FIELD_SEPERATOR)
            {
                list.add(buffer.toString());
                buffer = new StringBuffer();
            }
            else
            {
                buffer.append((char)c);
            }
        }

        // List must have at least 3 elements in it and no more than 10.  If
        // it has other than this, then bogus parse.
        if (list.size() < 3 || list.size() > 10)
        {
            throw new IOException("Empty header line.");
        }

        return list;
    }

    /**
     * Get the header field names lowercased.
     *
     * Assumption is that we're cue'd up to read the 3rd line of the ARC file
     * record when this method is called.
     *
     * @return Lowercased field names parsed from 3rd line of the ARC file.
     *
     * @exception IOException If we fail reading ARC file meta line no. 3.
     */
    private ArrayList computeHeaderFieldNameKeys()
        throws IOException
    {
        ArrayList values = getTokenizedHeaderLine();
        // Lowercase the field names found.
        for (int i = 0; i < values.size(); i++)
        {
            values.set(i, ((String)values.get(i)).toLowerCase());
        }
        return values;
    }

    /**
     * Compute metadata fields.
     *
     * Here we check the meta field has right number of items in it.
     *
     * @param headerFieldNameKeys Keys to use composing headerFields map.
     * @param values Values to set into the headerFields map.
     * @param version The version of this ARC file.
     *
     * @return Metadata structure for this record.
     *
     * @exception IOException  If no. of keys doesn't match no. of values.
     */
    private ARCRecordMetaData computeMetaData(ArrayList headerFieldNameKeys,
            ArrayList values, String version)
        throws IOException
    {
        if (headerFieldNameKeys.size() != values.size())
        {
            throw new IOException("Size of field name keys does " +
            " not match count of field values.");
        }

        HashMap headerFields = new HashMap();
        for (int i = 0; i < headerFieldNameKeys.size(); i++)
        {
            headerFields.put(headerFieldNameKeys.get(i), values.get(i));
        }

        headerFields.put(VERSION_HEADER_FIELD_KEY, version);

        return new ARCRecordMetaData(headerFields);
    }

    /**
     * Validate the arcFile.
     *
     * This method iterates over the file throwing exception if it fails
     * to successfully parse.
     *
     * @throws IOException
     */
    public void validate()
        throws IOException
    {
        validate(-1);
    }

    /**
     * Validate the arcFile.
     *
     * This method iterates over the file throwing exception if it fails
     * to successfully parse.
     *
     * @param noRecords Number of records expected.  Pass -1 if number is
     * unknown.
     *
     * @throws IOException
     */
    public void validate(int noRecords)
        throws IOException
    {
        int count = 0;
        for(; hasNext(); count++)
        {
            ARCRecord r = next();
            if (r.getMetaData().getLength() <= 0
                && r.getMetaData().getMimetype().equals(NO_TYPE_MIMETYPE))
            {
                throw new IOException("ARCRecord content is empty.");
            }

            r.close();
        }

        if (noRecords != -1)
        {
            if (count != noRecords)
            {
                throw new IOException("Count of records, " +
                    Integer.toString(count) + " is less than expected " +
                    Integer.toString(noRecords));
            }
        }
    }

    /**
     * @return True if file can be successfully parsed.
     */
    public boolean isValid()
    {
        boolean valid = false;
        try
        {
            validate();
            valid = true;
        }
        catch(Exception e)
        {
            // File is not valid if exception thrown parsing.
            valid = false;
        }

        return valid;
    }

    /**
     * @return True if we are using compression.
     */
    public boolean isCompressed()
    {
        return this.compressed;
    }

    /**
     * @return The current ARC record or null if none.
     */
    public ARCRecord getCurrentRecord()
    {
        return this.currentRecord;
    }

    /**
     * Returns list of all metadatas read so far.
     *
     * On instantiation, will have the ARC files metadata only.  As file is
     * read, we add the metadata of ARCRecords found to this list.
     *
     * @return Returns the metaDatas.
     */
    public List getMetaDatas()
    {
        return this.metaDatas;
    }

    /**
     * Override that gives access to underlying input stream.
     *
     * @author stack
     */
    public class ARCReaderGZIPInputStream
        extends GZIPInputStream
    {
        public ARCReaderGZIPInputStream(InputStream in)
            throws IOException
        {
            super(in);
        }

        /**
         * @return The  underlying compressed stream.
         */
        private InputStream getIn()
        {
            return this.in;
        }
    }

    /**
     * Use this main to pass the ARCReader files to test its ability at
     * parse/reading.
     *
     * @param args List of files to validate.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String [] args)
        throws FileNotFoundException, IOException
    {
        ARCReader r = null;
        for (int i = 0; i < args.length; i++)
        {
            r = new ARCReader(args[i]);
            r.validate();
        }
    }
}
