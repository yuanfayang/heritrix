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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.archive.io.PositionableStream;
import org.archive.io.RandomAccessInputStream;
import org.archive.util.ArchiveUtils;
import org.archive.util.InetAddressUtil;
import org.archive.util.MimetypeUtils;
import org.archive.util.TextUtils;


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
    Logger logger = Logger.getLogger(ARCReader.class.getName());
    
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
     * ARC file version.
     */
    private String version = null;
    

    /**
     * Array of field names.
     * 
     * Used to initialize <code>headerFieldNameKeys</code>.
     */
    private final String [] headerFieldNameKeysArray = {
        ARCConstants.URL_HEADER_FIELD_KEY,
        ARCConstants.IP_HEADER_FIELD_KEY,
        ARCConstants.DATE_HEADER_FIELD_KEY,
        ARCConstants.MIMETYPE_HEADER_FIELD_KEY,
        ARCConstants.LENGTH_HEADER_FIELD_KEY
    };
    
    /**
     * An array of the header field names found in the ARC file header on
     * the 3rd line.
     * 
     * We used to read these in from the arc file first record 3rd line but
     * now we hardcode them for sake of improved performance.
     */
    private final List headerFieldNameKeys =
        Arrays.asList(headerFieldNameKeysArray);
    
    /**
     * The file this arcreader is going against.
     */
    protected File arcFile = null;

    private boolean digest = true;
    
    private boolean strict = false;
    
    private boolean parseHttpHeaders = true;
    
    private static final byte [] outputBuffer = new byte[8 * 1024];
    
    private static final String CDX_OUTPUT = "cdx";
    private static final String DUMP_OUTPUT = "dump";
    private static final String GZIP_DUMP_OUTPUT = "gzipdump";
    private static final String NOHEAD_OUTPUT = "nohead";
    private static final String CDX_FILE_OUTPUT = "cdxfile";

    private static final String [] SUPPORTED_OUTPUT_FORMATS =
        {CDX_OUTPUT, DUMP_OUTPUT, GZIP_DUMP_OUTPUT, NOHEAD_OUTPUT,
            CDX_FILE_OUTPUT};
    
    private static final char SPACE = ' ';

    /**
     * Size used to preallocate stringbuffer used outputting a cdx line.
     * The numbers below are guesses at sizes of each of the cdx field.
     * The ones in the below are spaces. Here is the legend used outputting
     * the cdx line: CDX b e a m s c V n g.  Consult cdx documentation on
     * meaning of each of these fields.
     */
    private static final int CDX_LINE_BUFFER_SIZE = 14 + 1 + 15 + 1 + 1024 +
        1 + 24 + 1 + + 3 + 1 + 32 + 1 + 20 + 1 + 20 + 1 + 64;
    
    private static String cachedShortArcFileName = null;

    /**
     * Convenience method used by subclass constructors.
     * @param f ARC that this reader goes against.
     */
    protected void initialize(File f) {
        this.arcFile = f;
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
        return new PositionableBufferedInputStream(
            new RandomAccessInputStream(arcfile));
    }

    /**
     * Class that adds PositionableStream methods to a BufferedInputStream.
     */
    private class PositionableBufferedInputStream extends BufferedInputStream
    		implements PositionableStream {

        public PositionableBufferedInputStream(InputStream in)
        		throws IOException {
            super(in);
            doStreamCheck();
        }

        public PositionableBufferedInputStream(InputStream in, int size)
        		throws IOException {
            super(in, size);
            doStreamCheck();
        }
        
        private void doStreamCheck() throws IOException {
            if (!(this.in instanceof PositionableStream)) {
                throw new IOException(
                    "Passed stream must implement PositionableStream");
            }
        }

        public long getFilePointer() throws IOException {
            // Current position is the underlying files position
            // minus the amount thats in the buffer yet to be read.
            return ((PositionableStream)this.in).getFilePointer() -
            	(this.count - this.pos);
        }

        public void seek(long position) throws IOException {
            // Force refill of buffer whenever there's been a seek.
            this.pos = 0;
            this.count = 0;
            ((PositionableStream)this.in).seek(position);
        }
    }
    
    /**
     * Cleanout the current record if there is one.
     * @throws IOException
     */
    protected void cleanupCurrentRecord() throws IOException {
        if (this.currentRecord != null) {
            this.currentRecord.close();
            gotoEOR(this.currentRecord);
            this.currentRecord = null;
        }
    }
    
    /**
     * Skip over any trailing new lines at end of the record so we're lined up
     * ready to read the next.
     * @param record
     * @throws IOException
     */
    protected void gotoEOR(ARCRecord record) throws IOException {
        if (this.in.available() <= 0) {
            return;
        }
        
        // Remove any trailing LINE_SEPARATOR
        int c = -1;
        while (this.in.available() > 0) {
            if (this.in.markSupported()) {
                this.in.mark(1);
            }
            c = this.in.read();
            if (c != -1) {
                if (c == LINE_SEPARATOR) {
                    continue;
                }
                if (this.in.markSupported()) {
                    // We've overread.  We're probably in next record.  There is
                    // no way of telling for sure. It may be dross at end of
                    // current record. Backup.
                    this.in.reset();
                    break;
                }
                ARCRecordMetaData meta = (this.currentRecord != null)?
                    record.getMetaData(): null;
                throw new IOException("Read " + (char)c +
                    " when only " + LINE_SEPARATOR + " expected. " + 
                    this.arcFile + ((meta != null)?
                        meta.getHeaderFields().toString(): ""));
            }
        }
    }
    
    /**
     * Call close when done so we can cleanup after ourselves.
     * When parsing through an ARC writing out CDX info, we spend
     * 45% of CPU in here skipping over ARC Record body (~34% is
     * spent in the ARCRecord#read).
     * @throws IOException
     */
    public void close() throws IOException {
        cleanupCurrentRecord();
        if (this.in != null) {
            this.in.close();
            this.in = null;
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
        return this;
    }
    
    /**
     * @return True if we have more ARC records to read.
     */
    public boolean hasNext() {
        // Call close on any extant record.  This will scoot us past
        // any content not yet read.
        try {
            cleanupCurrentRecord();
        } catch (IOException e) {
            throw new NoSuchElementException(e.getMessage());
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
        } catch (RecoverableIOException e) {
            logger.warning("Recoverable error: " + e.getMessage());
            if (hasNext()) {
                return next();
            }
            return null;
        } catch (IOException e) {
            throw new NoSuchElementException(e.getClass() + ": " +
                e.getMessage());
        }
    }

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
     * <p>When parsing through ARCs writing out CDX info, we spend about
     * 38% of CPU in here -- about 30% of which is in getTokenizedHeaderLine
     * -- of which 16% is reading.
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
            // Just read over the 3rd line.  We used to parse it and use
            // values found here but now we just hardcode them to avoid
            // having to read this 3rd line even for random arc file accesses.
            bodyOffset += getTokenizedHeaderLine(is, null);
        }

        try {
            this.currentRecord = new ARCRecord(is,
                computeMetaData(this.headerFieldNameKeys, firstLineValues,
                    this.version, offset), bodyOffset, this.digest,
                    isStrict(), isParseHttpHeaders());
        } catch (IOException e) {
            IOException newE = new IOException(e.getMessage() + " (Offset " +
                    offset + ").");
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
        return this.currentRecord;
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
        // Preallocate usual line size.
        // TODO: Replace StringBuffer with more lightweight.  We burn
        // alot of our parse CPU in this method.
        StringBuffer buffer = new StringBuffer(2048 + 20);
        int read = 0;
        for (int c = -1; true;) {
            c = stream.read();
            if (c == -1) {
                throw new RecoverableIOException("Hit EOF before header EOL.");
            }
            c &= 0xff; 
            read++;
            if (read > MAX_HEADER_LINE_LENGTH) {
                throw new IOException("Header line longer than max allowed " +
                    " -- " + String.valueOf(MAX_HEADER_LINE_LENGTH) +
                    " -- or passed buffer doesn't contain a line (Read: " +
                    buffer.length() + ").  Here's" +
                    " some of what was read: " +
                    buffer.substring(0, Math.min(buffer.length(), 256)));
            }

            if (c == LINE_SEPARATOR) {
                if (buffer.length() == 0) {
                    // Empty line at start of buffer.  Skip it and try again.
                    continue;
                }

                if (list != null) {
                    list.add(buffer.toString());
                }
                // LOOP TERMINATION.
                break;
            } else if (c == HEADER_FIELD_SEPARATOR) {
                if (list != null) {
                    list.add(buffer.toString());
                }
                buffer = new StringBuffer();
            } else {
                buffer.append((char)c);
            }
        }

        // List must have at least 3 elements in it and no more than 10.  If
        // it has other than this, then bogus parse.
        if (list != null && (list.size() < 3 || list.size() > 100)) {
            throw new IOException("Unparseable header line: " + list);
        }

        return read;
    }

    /**
     * Compute metadata fields.
     *
     * Here we check the meta field has right number of items in it.
     *
     * @param keys Keys to use composing headerFields map.
     * @param values Values to set into the headerFields map.
     * @param v The version of this ARC file.
     * @param offset Offset into arc file.
     *
     * @return Metadata structure for this record.
     *
     * @exception IOException  If no. of keys doesn't match no. of values.
     */
    private ARCRecordMetaData computeMetaData(List keys, List values, String v,
            long offset)
    throws IOException {
        if (keys.size() != values.size()) {
            List originalValues = values;
            if (!isStrict()) {
                values = fixSpaceInMetadataLine(values, keys.size());
            }
            if (keys.size() != values.size()) {
                throw new IOException("Size of field name keys does" +
                    " not match count of field values: " + values);
            }
            // Note that field was fixed on stderr.
            logStdErr(Level.WARNING, "Fixed spaces in metadata URL." +
                " Original: " + originalValues + ", New: " + values);
        }
        
        Map headerFields = new HashMap(keys.size() + 2);
        for (int i = 0; i < keys.size(); i++) {
            headerFields.put(keys.get(i), values.get(i));
        }
        
        // Add a check for tabs in URLs.  If any, replace with '%09'.
        // See https://sourceforge.net/tracker/?group_id=73833&atid=539099&func=detail&aid=1010966,
        // [ 1010966 ] crawl.log has URIs with spaces in them.
        String url = (String)headerFields.get(URL_HEADER_FIELD_KEY);
        if (url != null && url.indexOf('\t') >= 0) {
            headerFields.put(URL_HEADER_FIELD_KEY,
                TextUtils.replaceAll("\t", url, "&09"));
        }

        headerFields.put(VERSION_HEADER_FIELD_KEY, v);
        headerFields.put(ABSOLUTE_OFFSET_KEY, new  Long(offset));

        return new ARCRecordMetaData(this.arcFile, headerFields);
    }
    
    /**
     * Log on stderr.
     * Logging should go via the logging system.  This method
     * bypasses the logging system going direct to stderr.
     * Should not generally be used.  Its used for rare messages
     * that come of cmdline usage of ARCReader ERRORs and WARNINGs.
     * Override if using ARCReader in a context where no stderr or
     * where you'd like to redirect stderr to other than System.err.
     * @param level Level to log message at.
     * @param message Message to log.
     */
    public static void logStdErr(Level level, String message) {
        System.err.println(level.toString() + " " + message);
    }
    
    /**
     * Fix space in URLs.
     * The ARCWriter used to write into the ARC URLs with spaces in them.
     * See <a
     * href="https://sourceforge.net/tracker/?group_id=73833&atid=539099&func=detail&aid=1010966">[ 1010966 ]
     * crawl.log has URIs with spaces in them</a>.
     * This method does fix up on such headers converting all spaces found
     * to '%20'.
     * @param values List of metadata values.
     * @param requiredSize Expected size of resultant values list.
     * @return New list if we successfully fixed up values or original if
     * fixup failed.
     */
    protected List fixSpaceInMetadataLine(List values, int requiredSize) {
        // Do validity check. 3rd from last is a date of 14 numeric
        // characters.  The 4th from last is IP, all before the IP
        // should be concatenated together with a '%20' joiner.
        // In the below, '4' is 4th field from end which has the IP.
        if (!(values.size() > requiredSize) || values.size() < 4) {
            return values;
        }
        // Test 3rd field is valid date.
        String date = (String)values.get(values.size() - 3);
        if (date.length() != 14) {
            return values;
        }
        for (int i = 0; i < date.length(); i++) {
            if (!Character.isDigit(date.charAt(i))) {
                return values;
            }
        }
        // Test 4th field is valid IP.
        String ip = (String)values.get(values.size() - 4);
        Matcher m = InetAddressUtil.IPV4_QUADS.matcher(ip);
        if (ip == "-" || m.matches()) {
            List newValues = new ArrayList(requiredSize);
            StringBuffer url = new StringBuffer();
            for (int i = 0; i < (values.size() - 4); i++) {
                if (i > 0) {
                    url.append("%20");
                }
                url.append(values.get(i));
            } 
            newValues.add(url.toString());
            for (int i = values.size() - 4; i < values.size(); i++) {
                newValues.add(values.get(i));
            }
            values =  newValues;
        }
        return values;
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
                && r.getMetaData().getMimetype().
                    equals(MimetypeUtils.NO_TYPE_MIMETYPE)) {
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
     * @return Returns the strict.
     */
    public boolean isStrict() {
        return strict;
    }
    /**
     * @param strict The strict to set.
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }
    
    // Static methods follow.

    /**
     *
     * @param formatter Help formatter instance.
     * @param options Usage options.
     * @param exitCode Exit code.
     */
    private static void usage(HelpFormatter formatter, Options options,
            int exitCode) {
        formatter.printHelp("java org.archive.io.arc.ARCReader" +
            " [--digest=true|false] \\\n" +
            " [--format=cdx|cdxfile|dump|gzipdump|nohead]" +
            " [--offset=#] \\\n[--strict] ARCFILE",
                options);
        System.exit(exitCode);
    }
    
    protected static String stripExtension(String name, String ext) {
        if (name.endsWith(ext)) {
            name = name.substring(0, name.length() - ext.length());
        }
        return name;
    }

    /**
     * Write out the arcfile.
     * 
     * @param f Arc file to read.
     * @param digest Digest yes or no.
     * @param strict True if we are to run in strict mode.
     * @param format Format to use outputting.
     * @throws IOException
     * @throws java.text.ParseException
     */
    protected static void output(File f, boolean digest, String format,
            boolean strict)
    throws IOException, java.text.ParseException {
        // long start = System.currentTimeMillis();
        boolean compressed = ARCReaderFactory.isCompressed(f);
        ARCReader arc = ARCReaderFactory.get(f);
        arc.setStrict(strict);
        // Clear cache of calculated arc file name.
        cachedShortArcFileName = null;
        
        // Write output as pseudo-CDX file.  See
        // http://www.archive.org/web/researcher/cdx_legend.php
        // and http://www.archive.org/web/researcher/example_cdx.php.
        // Hash is hard-coded straight SHA-1 hash of content.
        if (format.equals(CDX_OUTPUT)) {
            arc.setDigest(digest);
            cdxOutput(arc, compressed, false);
        } else if (format.equals(DUMP_OUTPUT)) {
            dumpOutput(arc, false);
        } else if (format.equals(GZIP_DUMP_OUTPUT)) {
            dumpOutput(arc, true);
        } else if (format.equals(CDX_FILE_OUTPUT)) {
            arc.setDigest(digest);
            cdxOutput(arc, compressed, true);
        } else {
            throw new IOException("Unsupported format: " + format);
        }
    }
    
    protected static void dumpOutput(ARCReader arc, boolean compressed)
    throws IOException, java.text.ParseException {
        // No point digesting if we're doing a dump.
        arc.setDigest(false);
        boolean firstRecord = true;
        ARCWriter writer = null;
        for (Iterator ii = arc.iterator(); ii.hasNext();) {
            ARCRecord r = (ARCRecord)ii.next();
            // We're to dump the arc on stdout.
            // Get the first record's data if any.
            ARCRecordMetaData meta = r.getMetaData();
            if (firstRecord) {
                firstRecord = false;
                // Get an ARCWriter.
                ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(r.available());
                // This is slow but done only once at top of ARC.
                while (r.available() > 0) {
                    baos.write(r.read());
                }
                List listOfMetadata = new ArrayList();
                listOfMetadata.add(baos.toString(ARCWriter.UTF8));
                writer = new ARCWriter(System.out, meta.getArcFile(),
                    compressed, listOfMetadata, meta.getDate());
                continue;
            }
            
            writer.write(meta.getUrl(), meta.getMimetype(), meta.getIp(),
                ArchiveUtils.parse14DigitDate(meta.getDate()).getTime(),
                (int)meta.getLength(), r);
        }
        // System.out.println(System.currentTimeMillis() - start);
    }
    
    protected static void cdxOutput(ARCReader arc, boolean compressed,
            boolean toFile)
    throws IOException {
        BufferedWriter cdxWriter = null;
        if (toFile) {
            String cdxFilename = stripExtension(arc.arcFile.getAbsolutePath(),
                    DOT_COMPRESSED_FILE_EXTENSION);
            cdxFilename = stripExtension(cdxFilename, '.' + ARC_FILE_EXTENSION);
            cdxFilename += ".cdx";
            cdxWriter = new BufferedWriter(new FileWriter(cdxFilename));
        }
        
        String header = "CDX b e a m s c " + ((compressed) ? "V" : "v")
            + " n g";
        if (toFile) {
            cdxWriter.write(header);
            cdxWriter.newLine();
        } else {
            System.out.println(header);
        }
        
        try {
            // Parsing http headers is costly and not needed dumping cdx.
            arc.setParseHttpHeaders(false);
            for (Iterator ii = arc.iterator(); ii.hasNext();) {
                ARCRecord r = (ARCRecord) ii.next();
                if (toFile) {
                    cdxWriter.write(outputARCRecordCdx(r));
                    cdxWriter.newLine();
                } else {
                    System.out.println(outputARCRecordCdx(r));
                }
            }
        } finally {
            if (toFile) {
                cdxWriter.close();
            }
        }
    }

    /**
     * @param meta ARCRecordMetaData instance.
     * @return short name of arc.
     */
    protected static String getShortArcFileName(ARCRecordMetaData meta) {
        if (cachedShortArcFileName == null) {
            String arcFileName = meta.getArcFile().getName();
            arcFileName = stripExtension(arcFileName,
                    DOT_COMPRESSED_FILE_EXTENSION);
            cachedShortArcFileName = stripExtension(arcFileName,
                    '.' + ARC_FILE_EXTENSION);
        }
        return cachedShortArcFileName;
    }
    
    /**
     * Output passed record using passed format specifier.
     * @param r ARCRecord instance to output.
     * @param format What format to use outputting.
     * @throws IOException
     */
    protected static void outputARCRecord(ARCRecord r, String format)
    throws IOException {
        if (format.equals(CDX_OUTPUT)) {
            outputARCRecordCdx(r);
        } else if(format.equals(DUMP_OUTPUT)) {
            outputARCRecordDump(r);
        } else if(format.equals(NOHEAD_OUTPUT)) {
            outputARCRecordNohead(r);
        } else {
            throw new IOException("Unsupported format" +
                " (or unsupported on a single record): " + format);
        }
    }
    
    /**
     * @param r ARCRecord instance to output.
     * @throws IOException
     */
    private static void outputARCRecordNohead(ARCRecord r)
    throws IOException {
        r.skipHttpHeader();
        outputARCRecordDump(r);
    }

    /**
     * @param r ARCRecord instance to output.
     * @throws IOException
     */
    private static void outputARCRecordDump(ARCRecord r)
    throws IOException {
        int read = outputBuffer.length;
        while ((read = r.read(outputBuffer, 0, outputBuffer.length)) != -1) {
            System.out.write(outputBuffer, 0, read);
        }
        System.out.flush();
    }

    protected static String outputARCRecordCdx(ARCRecord r)
    throws IOException {
        // Read the whole record so we get out a hash.
        r.close();
        ARCRecordMetaData meta = r.getMetaData();
        String statusCode = (meta.getStatusCode() == null)?
            "-": meta.getStatusCode();
        StringBuffer buffer = new StringBuffer(CDX_LINE_BUFFER_SIZE);
        buffer.append(meta.getDate());
        buffer.append(SPACE);
        buffer.append(meta.getIp());
        buffer.append(SPACE);
        buffer.append(meta.getUrl());
        buffer.append(SPACE);
        buffer.append(meta.getMimetype());
        buffer.append(SPACE);
        buffer.append(statusCode);
        buffer.append(SPACE);
        buffer.append((meta.getDigest() == null)? "-": meta.getDigest());
        buffer.append(SPACE);
        buffer.append(meta.getOffset());
        buffer.append(SPACE);
        buffer.append(meta.getLength());
        buffer.append(SPACE);
        buffer.append(getShortArcFileName(meta));

        return buffer.toString();
    }
    
    /**
     * @param d True if we're to digest.
     */
    public void setDigest(boolean d) {
        this.digest = d;
    }
    
    /**
     * @return True if we're digesting as we read.
     */
    public boolean getDigest() {
        return this.digest;
    }

    /**
     * @return Returns the parseHttpHeaders.
     */
    public boolean isParseHttpHeaders() {
        return this.parseHttpHeaders;
    }
    
    /**
     * @param parseHttpHeaders The parseHttpHeaders to set.
     */
    public void setParseHttpHeaders(boolean parseHttpHeaders) {
        this.parseHttpHeaders = parseHttpHeaders;
    }

    /**
     * Generate a CDX index file for an ARC file.
     *
     * @param arcFilename The ARC file to generate a CDX index for
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void createCDXIndexFile(String arcFilename)
    throws IOException, java.text.ParseException {
        output(new File(arcFilename), true, CDX_FILE_OUTPUT, false);
    }

    /**
     * Command-line interface to ARCReader.
     *
     * Here is the command-line interface:
     * <pre>
     * usage: java org.archive.io.arc.ARCReader [--offset=#] ARCFILE
     *  -h,--help      Prints this message and exits.
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
     * @throws ParseException Failed parse of the command line.
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void main(String [] args)
    throws ParseException, IOException, java.text.ParseException {

        Options options = new Options();
        options.addOption(new Option("h","help", false,
            "Prints this message and exits."));
        options.addOption(new Option("o","offset", true,
            "Outputs record at this offset into arc file."));
        options.addOption(new Option("d","digest", true,
            "Calculate digest. Expensive. Default: true."));
        options.addOption(new Option("s","strict", false,
            "Strict mode. Fails parse if incorrectly formatted ARC."));
        options.addOption(new Option("f","format", true,
            "Output options: 'cdx', cdxfile', 'dump', 'gzipdump'," +
            "'or 'nohead'. Default: 'cdx'."));
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
        boolean digest = true;
        boolean strict = false;
        String format = "cdx";
        for (int i = 0; i < cmdlineOptions.length; i++) {
            switch(cmdlineOptions[i].getId()) {
                case 'h':
                    usage(formatter, options, 0);
                    break;

                case 'o':
                    offset =
                        Long.parseLong(cmdlineOptions[i].getValue());
                    break;
                    
                case 's':
                    strict = true;
                    break;
                    
                case 'd':
                    if (cmdlineOptions[i].getValue() != null) {
                        if (Boolean.FALSE.toString().
                                equals(cmdlineOptions[i].getValue().
                                    toLowerCase())) {
                            digest = false;
                        }
                    }
                    break;
                    
                case 'f':
                    format = cmdlineOptions[i].getValue().toLowerCase();
                    boolean match = false;
                    for (int ii = 0; ii < SUPPORTED_OUTPUT_FORMATS.length; ii++) {
                        if (SUPPORTED_OUTPUT_FORMATS[ii].equals(format)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        usage(formatter, options, 1);
                    }
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
            arc.setStrict(strict);
            ARCRecord rec = arc.get(offset);
            outputARCRecord(rec, format);
        } else {
            for (Iterator i = cmdlineArgs.iterator(); i.hasNext();) {
                File f = new File((String)i.next());
                try {
                    output(f, digest, format, strict);
                } catch (RuntimeException e) {
                    // Write out name of file we failed on to help with
                    // debugging.  Then print stack trace and try to keep
                    // going.  We do this for case where we're being fed
                    // a bunch of ARCs; just note the bad one and move
                    // on to the next.
                    System.err.println("Exception processing " + f);
                    e.printStackTrace(System.err);
                }
            }
        }
    }
    
    /**
     * A subclass of IOException that is not fatal.
     * If iterating over records, should be able to move to the next record and 
     * continue processing.
     * @author stack
     * @version $Date$, $Revision$
     */
    public class RecoverableIOException
    extends IOException {
        protected RecoverableIOException() {
            super();
        }
        protected RecoverableIOException(String message) {
            super(message);
        }
    }
}
