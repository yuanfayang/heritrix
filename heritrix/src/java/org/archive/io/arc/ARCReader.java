/* ARCReader
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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.archive.io.MappedByteBufferInputStream;
import org.archive.io.PositionableStream;

/**
 * Get an iterator on an arc file or get a record by absolute position.
 *
 * ARC files are described here:
 * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
 * File Format</a>.
 *
 * <p>This class knows how to parse an ARC file and has accessors for all of
 * the ARC file content. It can parse ARC Version 1 and 2.
 *
 * <p>ARC file header looks like this:
 *
 * <pre>filedesc://testIsBoundary-JunitIAH200401070157520.arc 0.0.0.0 \\
 *      20040107015752 text/plain 77
 * 1 0 InternetArchive
 * URL IP-address Archive-date Content-type Archive-length</pre>
 *
 * <p>Iterator returns ARCRecords (though {@link #next()} returns
 * java.lang.Object).  Cast the return.
 *
 * <p>Profiling java.io vs. memory-mapped ByteBufferInputStream shows the
 * latter slightly slower -- but not by much.  TODO: Test more.  Just
 * change {@link #getInputStream(File)}.
 *
 * <p>TODO: Testing of this reader class against ARC files harvested out in
 * the wilds.  This class has only been tested to date going against small
 * files made by unit tests.  The class needs to be tested that it might
 * develop robustness.
 *
 * @author stack
 */
public abstract class ARCReader implements ARCConstants, Iterator {
    
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
     * The ARCRecord currently being read.
     *
     * Keep this ongoing reference so we'll close the record
     * even if the caller doesn't.  On construction, has the
     * arcfile header metadata.
     */
    protected ARCRecord currentRecord = null;
    
    /**
     * ARC file input stream.
     *
     * Keep it around so we can close it when done.
     *
     * <p>Set in constructor.  Must support the 
     * PositionableStream interface.  Constructor should check.
     */
    protected InputStream in = null;
    
    /**
     * Channel we got the memory mapped byte buffer from.
     *
     * Keep around so can close when done.
     */
    protected FileChannel channel = null;
    
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
     * The file this arcreader is going against.
     */
    protected File arcFile = null;
    

    /**
     * Convenience method used by subclass constructors.
     * @param arcFile ARC that this reader goes against.
     * @throws IOException
     */
    protected void initialize(File arcFile) throws IOException {
        this.arcFile = arcFile;
        // Read in the first record so headerFieldNameKeys gets populated.
        // Always do it even if we're creating ARCReader just to do a get
        // to get a record at any old offset.
        createARCRecord(this.in, 0).close();
    }
    
    /**
     * Rewinds stream to start of the arc file.
     * @throws IOException if stream is not resettable.
     */
    protected void rewind() throws IOException {
        cleanupCurrentRecord();
        if (this.in instanceof PositionableStream) {
            try {
                ((PositionableStream)this.in).seek(0);
            } catch (IOException e) {
                throw new RuntimeException(e.getClass().getName() + ": " +
                    e.getMessage());
            }
       } else {
           throw new IOException("Stream is not resettable.");
       }
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
        PositionableStream ps = (PositionableStream)this.in;
        long currentOffset = ps.getFilePointer();
        if (currentOffset != offset) {
            currentOffset = offset;
            ps.seek(offset);
        }
        return createARCRecord(this.in, currentOffset);
    }
    
    /**
     * Convenience method for constructors.
     * 
     * @param arcfile File to read.
     * @return InputStream to read from.
     * @throws IOException If failed open or fail to get a memory
     * mapped byte buffer on file.
     */
    protected InputStream getInputStream(File arcfile) throws IOException {
        FileInputStream fis = new FileInputStream(arcfile);
        this.channel = fis.getChannel();
        return new MappedByteBufferInputStream(
                this.channel.map(FileChannel.MapMode.READ_ONLY, 0,
                        this.channel.size()));
                       
    }

    /**
     * Cleanout the current record if there is one.
     * @throws IOException
     */
    protected void cleanupCurrentRecord() throws IOException {
        if (this.currentRecord != null) {
            this.currentRecord.close();
            this.currentRecord = null;
        }
    }
    
    /**
     * Call close when done so we can cleanup after ourselves.
     * @throws IOException
     */
    public void close() throws IOException {
        cleanupCurrentRecord();
        if (this.in != null) {
            this.in.close();
            this.in = null;
        }
        if (this.channel != null && this.channel.isOpen()) {
                this.channel.close();
                this.channel = null;
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
    
    /**
     * @return An iterator over the total arcfile.
     */
    public Iterator iterator() {
            // Eat up any record outstanding.
            try {
                    cleanupCurrentRecord();
            } catch (IOException e) {
                    throw new RuntimeException(e.getClass().getName() + ": " +
                    e.getMessage());
            }
        
        // Now reset stream to the start of the arc file.
        try {
            rewind();
        } catch (IOException e) {
            throw new RuntimeException(e.getClass().getName() + ": " +
                    e.getMessage());
        }
        return (Iterator)this;
    }
    
    /**
     * @return True if we have more ARC records to read.
     */
    public boolean hasNext() {
        if (this.currentRecord != null) {
            // Call close on any extant record.  This will scoot us past
            // any content not yet read.
            try {
                cleanupCurrentRecord();
            } catch (IOException e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }
        
        try {
            return this.in.available() > 0;
        } catch (IOException e) {
            throw new RuntimeException("Failed: " + e.getMessage());
        }
    }

    /**
     * Return the next record.
     *
     * @return Next ARCRecord else null if no more records left.  You need to
     * cast result to ARCRecord.
     */
    public Object next() {
        try {
            return get(((PositionableStream)this.in).getFilePointer());
        } catch (IOException e) {
            throw new NoSuchElementException(e.getClass() + ": " +
                e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Create new arc record.
     *
     * Encapsulate housekeeping that has to do w/ creating a new record.
     *
     * <p>Call this method at end of constructor to read in the
     * arcfile header.  Will be problems reading subsequent arc records
     * if you don't since arcfile header has the list of metadata fields for
     * all records that follow.
     *
     * @param is InputStream to use.
     * @param offset Absolute offset into arc file.
     * @return An arc record.
     * @throws IOException
     */
    protected ARCRecord createARCRecord(InputStream is, long offset)
                throws IOException {
        ArrayList firstLineValues = new ArrayList(20);
        getTokenizedHeaderLine(is, firstLineValues);
        int bodyOffset = 0;
        if (offset == 0) {
            // If offset is zero, then no records have been read yet
            // and we're reading our first one, the record of ARC file meta
            // info.  Its special.  Has three lines of meta info. We've just
            // read the first line.  There are two more.   The second line
            // has misc. info.  We're only interested in the first field,
            // the version number.  The third line is the list of field
            // names. Here's what ARC file version 1 meta content looks like:
            //
            // filedesc://testIsBoundary-JunitIAH200401070157520.arc 0.0.0.0 \\
            //      20040107015752 text/plain 77
            // 1 0 InternetArchive
            // URL IP-address Archive-date Content-type Archive-length
            //
            // Cannot read other ARCRecords till this first field has been
            // read because it has the field names for subsequent record
            // metadata.
            //
            ArrayList secondLineValues = new ArrayList(20);
            bodyOffset += getTokenizedHeaderLine(is, secondLineValues);
            this.version = (String)secondLineValues.get(0) +
                "." + (String)secondLineValues.get(1) ;
            ArrayList thirdLineValues = new ArrayList(20);
            bodyOffset += getTokenizedHeaderLine(is, thirdLineValues);
            // Lowercase the field names found.
            for (int i = 0; i < thirdLineValues.size(); i++) {
                thirdLineValues.set(i,
                    ((String)thirdLineValues.get(i)).toLowerCase());
            }
            this.headerFieldNameKeys = thirdLineValues;
        }

        return this.currentRecord = new ARCRecord(is,
            computeMetaData(this.headerFieldNameKeys, firstLineValues,
                this.version, offset), bodyOffset);
    }

    /**
     * Get a record header line as list of tokens.
     *
     * We keep reading till we find a LINE_SEPARATOR or we reach the end
     * of file w/o finding a LINE_SEPARATOR or the line length is crazy.
     *
     * @param stream InputStream to read from.
     * @param list Empty list that gets filled w/ string tokens.
     * @return Count of characters read.
     * @exception IOException If problem reading stream or no line separator
     * found or EOF before EOL or we didn't get minimum header fields.
     */
    private int getTokenizedHeaderLine(final InputStream stream,
            List list) throws IOException {
        // Preallocate max with some padding.
        StringBuffer buffer = new StringBuffer(MAX_HEADER_LINE_LENGTH + 20);
        int read = 0;
        for (int c = -1; true;) {
            c = stream.read() & 0xff;
            if (c == -1) {
                throw new IOException("Hit EOF before header EOL.");
            }

            read++;
            if (read > MAX_HEADER_LINE_LENGTH) {
                throw new IOException("Header line longer than max allowed " +
                        " -- " + String.valueOf(MAX_HEADER_LINE_LENGTH) +
                " -- or passed buffer doesn't contain a line.");
            }

            if (c == LINE_SEPARATOR) {
                if (list.size() == 0 && buffer.length() == 0) {
                    // Empty line at start of buffer.  Skip it and try again.
                    continue;
                }

                list.add(buffer.toString());
                // LOOP TERMINATION.
                break;
            } else if (c == HEADER_FIELD_SEPARATOR) {
                list.add(buffer.toString());
                buffer = new StringBuffer();
            } else {
                buffer.append((char)c);
            }
        }

        // List must have at least 3 elements in it and no more than 10.  If
        // it has other than this, then bogus parse.
        if (list.size() < 3 || list.size() > 10) {
            throw new IOException("Empty header line.");
        }

        return read;
    }

    /**
     * Compute metadata fields.
     *
     * Here we check the meta field has right number of items in it.
     *
     * @param headerFieldNameKeys Keys to use composing headerFields map.
     * @param values Values to set into the headerFields map.
     * @param version The version of this ARC file.
     * @param offset Offset into arc file.
     *
     * @return Metadata structure for this record.
     *
     * @exception IOException  If no. of keys doesn't match no. of values.
     */
    private ARCRecordMetaData computeMetaData(ArrayList headerFieldNameKeys,
                ArrayList values, String version, long offset)
            throws IOException {
        if (headerFieldNameKeys.size() != values.size()) {
            throw new IOException("Size of field name keys does " +
            " not match count of field values.");
        }

        HashMap headerFields = new HashMap();
        for (int i = 0; i < headerFieldNameKeys.size(); i++) {
            headerFields.put(headerFieldNameKeys.get(i), values.get(i));
        }

        headerFields.put(VERSION_HEADER_FIELD_KEY, version);
        headerFields.put(ABSOLUTE_OFFSET_KEY, new  Long(offset));

        return new ARCRecordMetaData(this.arcFile, headerFields);
    }

    /**
     * Validate the arcFile.
     *
     * This method iterates over the file throwing exception if it fails
     * to successfully parse.
     *
     * @return List of all read metadatas. As we validate records, we add
     * a reference to the read metadata.
     *
     * <p>Assumes the stream is at the start of the file.
     *
     * @throws IOException
     */
    public List validate() throws IOException {
        return validate(-1);
    }

    /**
     * Validate the arcFile.
     *
     * This method iterates over the file throwing exception if it fails
     * to successfully parse.
     *
     * <p>Assumes the stream is at the start of the file.
     *
     * @param noRecords Number of records expected.  Pass -1 if number is
     * unknown.
     *
     * @return List of all read metadatas. As we validate records, we add
     * a reference to the read metadata.
     *
     * @throws IOException
     */
    public List validate(int noRecords) throws IOException {
        List metaDatas = new ArrayList();
        int count = 0;
        for (Iterator i = iterator(); hasNext();) {
            count++;
            ARCRecord r = (ARCRecord)i.next();
            if (r.getMetaData().getLength() <= 0
                && r.getMetaData().getMimetype().equals(NO_TYPE_MIMETYPE)) {
                throw new IOException("ARCRecord content is empty.");
            }
            r.close();
            // Add reference to metadata into a list of metadatas.
            metaDatas.add(r.getMetaData());
        }

        if (noRecords != -1) {
            if (count != noRecords) {
                throw new IOException("Count of records, " +
                    Integer.toString(count) + " is less than expected " +
                    Integer.toString(noRecords));
            }
        }

        return metaDatas;
    }

    /**
     * @return True if file can be successfully parsed.
     * Assumes the stream is at the start of the file.
     */
    public boolean isValid() {
        boolean valid = false;
        try {
            validate();
            valid = true;
        } catch(Exception e) {
            // File is not valid if exception thrown parsing.
            valid = false;
        }

        return valid;
    }

    /**
     * @return The current ARC record or null if none.
     * After construction has the arcfile header record.
     */
    public ARCRecord getCurrentRecord() {
        return this.currentRecord;
    }

    /**
     *
     * @param formatter Help formatter instance.
     * @param options Usage options.
     */
    private static void usage(HelpFormatter formatter, Options options,
            int exitCode) {
        formatter.printHelp("java org.archive.io.arc.ARCReader" +
            " [--offset=# [--nohead]] ARCFILE",  options);
        System.exit(exitCode);
    }

    /**
     * @param rec ARCRecord.
     * @param nohead Whether to output the header or just skip over.
     * @throws IOException
     */
    private static void processRequestHeaders(ARCRecord rec, boolean nohead)
            throws IOException {
        int c = -1;
        int lastChar = -1;
        boolean newline = false;
        for (boolean finished = false; !finished && ((c = rec.read()) != -1);) {
            if ((byte)c == '\n' && (byte)lastChar == '\r') {
                if (newline) {
                    // If already a newline, then this is the second
                    // newline and so we're at end of header.
                    finished = true;
                }
                newline = true;
            } else {
                if ((byte)c != '\r') {
                    newline = false;
                }
                lastChar = c;
            }
            if (!nohead) {
                System.out.write(c & 0xff);
            }
        }
    }

    /**
     * Write the arc meta data in pseudo-CDX format.
     * 
     * @param f Arc file to read.
     * @throws IOException
     */
    protected static void index(File f) throws IOException {
        boolean compressed = ARCReaderFactory.isCompressed(f);
        ARCReader arc = ARCReaderFactory.get(f);
        ARCRecord headerRecord = arc.getCurrentRecord();
        String arcFileName = headerRecord.getMetaData().getArcFile().getName();
        if (arcFileName.endsWith("." + COMPRESSED_FILE_EXTENSION)) {
            int stripLen = COMPRESSED_FILE_EXTENSION.length() + 1;
            arcFileName = arcFileName.substring(0,
                    arcFileName.length() - stripLen);
        }
        // Write output as pseudo-CDX file.  See
        // http://www.archive.org/web/researcher/cdx_legend.php
        // and http://www.archive.org/web/researcher/example_cdx.php.
        // Hash is hard-coded straight SHA-1 hash of content.
        System.out.println("CDX b e a m s c " +
                ((compressed)? "V": "v") + " n g");
        for (Iterator ii = arc.iterator(); ii.hasNext();) {
            ARCRecord r = (ARCRecord)ii.next();
            // Read the whole record so we get out a hash.
            r.close();
            ARCRecordMetaData meta = r.getMetaData();
            String statusCode = (meta.getStatusCode() == null)?
                    "-": meta.getStatusCode();
            System.out.println(meta.getDate() + " " +	// 'b' date.
                    meta.getIp() + " " +		// 'e' IP
                    meta.getUrl() + " " +		// 'a' Original URI
                    meta.getMimetype() + " " +	// 'm' Mimetype
                    statusCode + " " +			// 's' Mimetype
                    meta.getDigest() + " " +	// 'c' "Old-style" checksum
                    meta.getOffset() + " " +	// 'V' or 'v' offset.
                    meta.getLength() + " " +	// 'n' Arc doc length.
                    arcFileName		 			// 'g' Arc doc length.
            );
        }
        System.out.flush();
        // System.out.println(System.currentTimeMillis() - start);
    }   
    
    /**
     * Command-line interface to ARCReader.
     *
     * Here is the command-line interface:
     * <pre>
     * usage: java org.archive.io.arc.ARCReader [--offset=# [--nohead]] ARCFILE
     *  -h,--help      Prints this message and exits.
     *  -n,--nohead    Do not output request header as part of record.
     *  -o,--offset    Outputs record at this offset into arc file.</pre>
     *
     * <p>See in <code>$HERITRIX_HOME/bin/arcreader</code> for a script that'll
     * take care of classpaths and the calling of ARCReader.
     *
     * <p>Outputs using a pseudo-CDX format as described here:
     * <a href="http://www.archive.org/web/researcher/cdx_legend.php">CDX
     * Legent</a> and here
     * <a href="http://www.archive.org/web/researcher/example_cdx.php">Example</a>.
     * Legend used in below is: 'CDX b e a m s c V (or v if uncompressed) n g'.
     * Hash is hard-coded straight SHA-1 hash of content.
     *
     * @param args Command-line arguments.
     * @throws IOException Failed read of passed arc files.
     * @throws ParseException Failed parse of the command line.
     */
    public static void main(String [] args)
        throws IOException, ParseException {

        Options options = new Options();
        options.addOption(new Option("h","help", false,
            "Prints this message and exits."));
        options.addOption(new Option("o","offset", true,
            "Outputs record at this offset into arc file."));
        options.addOption(new Option("n","nohead", false,
            "Do not output request header as part of record."));
        PosixParser parser = new PosixParser();
        CommandLine cmdline = parser.parse(options, args, false);
        List cmdlineArgs = cmdline.getArgList();
        Option [] cmdlineOptions = cmdline.getOptions();
        HelpFormatter formatter = new HelpFormatter();

        // If no args, print help.
        if (cmdlineArgs.size() <= 0) {
            usage(formatter, options, 0);
        }

        // Now look at options passed.
        long offset = -1;
        boolean nohead = false;
        for (int i = 0; i < cmdlineOptions.length; i++) {
            switch(cmdlineOptions[i].getId()) {
                case 'h':
                    usage(formatter, options, 0);
                    break;

                case 'o':
                        offset =
                        Long.parseLong(cmdlineOptions[i].getValue());
                        break;

                case 'n':
                    nohead = true;
                    break;

                default:
                    throw new RuntimeException("Unexpected option: " +
                        + cmdlineOptions[i].getId());
            }
        }

        if (offset >= 0) {
            if (cmdlineArgs.size() != 1) {
                System.out.println("Error: Pass one arcfile only.");
                usage(formatter, options, 1);
            }
            ARCReader arc = ARCReaderFactory.
                get(new File((String)cmdlineArgs.get(0)));
            ARCRecord rec = arc.get(offset);
            processRequestHeaders(rec, nohead);
            for (int c = -1; (c = rec.read()) != -1;) {
                System.out.write(c & 0xff);
            }
            System.out.flush();
        } else if (cmdlineOptions.length > 0) {
            System.out.println("Error: Unexpected option.");
            usage(formatter, options, 1);
        } else {
            for (Iterator i = cmdlineArgs.iterator(); i.hasNext();) {
                // long start = System.currentTimeMillis();
                index(new File((String)i.next()));
            }
        }
    }
}
