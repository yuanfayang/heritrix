/* $Id$
 *
 * Created Aug 23, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.io.warc;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.WriterPoolMember;

/**
 * WARCReader.
 * Go via {@link WARCReaderFactory} to get instance.
 * @author stack
 * @version $Date$ $Version$
 */
public class WARCReader extends ArchiveReader implements WARCConstants {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    private static final byte [] outputBuffer = new byte[16 * 1024];
    
    private static String cachedShortFileName = null;
    
    private static final String CDX = "cdx";
    private static final String DUMP = "dump";
    private static final String GZIP_DUMP = "gzipdump";
    private static final String NOHEAD = "nohead";
    private static final String CDX_FILE = "cdxfile";

    private static final String [] SUPPORTED_OUTPUT_FORMATS =
        {CDX, DUMP, GZIP_DUMP, NOHEAD, CDX_FILE};
    
    
    WARCReader() {
        super();
    }
    
    @Override
    protected void initialize(String i) {
        super.initialize(i);
        setVersion(WARC_VERSION);
    }
    
    /**
     * Skip over any trailing new lines at end of the record so we're lined up
     * ready to read the next.
     * @param record
     * @throws IOException
     */
    protected void gotoEOR(ArchiveRecord record) throws IOException {
        if (getIn().available() <= 0) {
            return;
        }
        throw new IOException("Shouldn't be any trailing bytes in a Record");
    }
    
    /**
     * Create new WARC record.
     * Encapsulate housekeeping that has to do w/ creating new Record.
     * @param is InputStream to use.
     * @param offset Absolute offset into WARC file.
     * @return A WARCRecord.
     * @throws IOException
     */
    protected WARCRecord createArchiveRecord(InputStream is, long offset)
    throws IOException {
        return (WARCRecord)currentRecord(new WARCRecord(is, offset, isDigest(),
            isStrict()));
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
        formatter.printHelp("java org.archive.io.arc.WARCReader" +
            " [--digest=true|false] \\\n" +
            " [--format=cdx|cdxfile|dump|gzipdump|nohead]" +
            " [--offset=#] \\\n[--strict] WARC_FILE|WARC_URL",
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
     * @param urlOrPath Arc file to read.
     * @param digest Digest yes or no.
     * @param strict True if we are to run in strict mode.
     * @param format Format to use outputting.
     * @throws IOException
     * @throws java.text.ParseException
     */
    protected static void output(String urlOrPath, boolean digest,
            String format, boolean strict)
    throws IOException, java.text.ParseException {
        // long start = System.currentTimeMillis();
        WARCReader arc = WARCReaderFactory.get(urlOrPath);
        arc.setStrict(strict);
        // Clear cache of calculated arc file name.
        cachedShortFileName = null;
        
        // Write output as pseudo-CDX file.  See
        // http://www.archive.org/web/researcher/cdx_legend.php
        // and http://www.archive.org/web/researcher/example_cdx.php.
        // Hash is hard-coded straight SHA-1 hash of content.
        if (format.equals(CDX)) {
            arc.setDigest(digest);
            cdxOutput(arc, false);
        } else if (format.equals(DUMP)) {
            dumpOutput(arc, false);
        } else if (format.equals(GZIP_DUMP)) {
            dumpOutput(arc, true);
        } else if (format.equals(CDX_FILE)) {
            arc.setDigest(digest);
            cdxOutput(arc, true);
        } else {
            throw new IOException("Unsupported format: " + format);
        }
    }
    
    protected static void dumpOutput(WARCReader arc, boolean compressed)
    throws IOException, java.text.ParseException {
        // No point digesting if we're doing a dump.
        arc.setDigest(false);
        boolean firstRecord = true;
        ExperimentalWARCWriter w = null;
        for (Iterator ii = arc.iterator(); ii.hasNext();) {
            WARCRecord r = (WARCRecord)ii.next();
            // We're to dump the arc on stdout.
            // Get the first record's data if any.
            ArchiveRecordHeader h = r.getHeader();
            if (firstRecord) {
                firstRecord = false;
                // Get an ARCWriter.
                ByteArrayOutputStream baos =
                    new ByteArrayOutputStream(r.available());
                // This is slow but done only once at top of ARC.
                while (r.available() > 0) {
                    baos.write(r.read());
                }
                List<String> listOfMetadata = new ArrayList<String>();
                listOfMetadata.add(baos.toString(WriterPoolMember.UTF8));
                // Assume getArc returns full path to file.  ARCWriter
                // or new File will complain if it is otherwise.
                w = new ExperimentalWARCWriter(System.out,
                    new File(h.getFileIdentifier()),
                    compressed, h.getDate(), listOfMetadata);
                continue;
            }
            /* TODO
            w.write(h.getUrl(), meta.getMimetype(), meta.getIp(),
                ArchiveUtils.parse14DigitDate(meta.getDate()).getTime(),
                (int)meta.getLength(), r);
                */
        }
        // System.out.println(System.currentTimeMillis() - start);
    }
    
    protected static void cdxOutput(WARCReader arc, boolean toFile)
    throws IOException {
        BufferedWriter cdxWriter = null;
        if (toFile) {
            String cdxFilename = stripExtension(arc.getReaderIdentifier(),
                DOT_COMPRESSED_FILE_EXTENSION);
            cdxFilename = stripExtension(cdxFilename, DOT_WARC_FILE_EXTENSION);
            cdxFilename += ".cdx";
            cdxWriter = new BufferedWriter(new FileWriter(cdxFilename));
        }
        
        String header = "CDX b e a m s c " + ((arc.isCompressed()) ? "V" : "v")
            + " n g";
        if (toFile) {
            cdxWriter.write(header);
            cdxWriter.newLine();
        } else {
            System.out.println(header);
        }
        
        try {
            // Parsing http headers is costly and not needed dumping cdx.
            // TODO: arc.setParseHttpHeaders(false);
            for (Iterator ii = arc.iterator(); ii.hasNext();) {
                WARCRecord r = (WARCRecord) ii.next();
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
    protected static String getShortWarcFileName(ArchiveRecordHeader h) {
        if (cachedShortFileName == null) {
            String arcFileName = (new File(h.getFileIdentifier()).getName());
            arcFileName = stripExtension(arcFileName,
                DOT_COMPRESSED_FILE_EXTENSION);
            cachedShortFileName = stripExtension(arcFileName,
                DOT_WARC_FILE_EXTENSION);
        }
        return cachedShortFileName;
    }
    
    /**
     * Output passed record using passed format specifier.
     * @param r ARCRecord instance to output.
     * @param format What format to use outputting.
     * @throws IOException
     */
    protected static void outputARCRecord(final WARCReader arc,
            final WARCRecord r,
        final String format)
    throws IOException {
        if (format.equals(CDX)) {
            System.out.println(outputARCRecordCdx(r));
        } else if(format.equals(DUMP)) {
            // No point digesting if dumping content.
            arc.setDigest(false);
            outputARCRecordDump(r);
        } else if(format.equals(NOHEAD)) {
            // No point digesting if dumping content.
            arc.setDigest(false);
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
    private static void outputARCRecordNohead(WARCRecord r)
    throws IOException {
        // TODO: r.skipHttpHeader();
        outputARCRecordDump(r);
    }

    /**
     * @param r ARCRecord instance to output.
     * @throws IOException
     */
    private static void outputARCRecordDump(WARCRecord r)
    throws IOException {
        int read = outputBuffer.length;
        while ((read = r.read(outputBuffer, 0, outputBuffer.length)) != -1) {
            System.out.write(outputBuffer, 0, read);
        }
        System.out.flush();
    }

    protected static String outputARCRecordCdx(WARCRecord r)
    throws IOException {
        // Read the whole record so we get out a hash.
        r.close();
        ArchiveRecordHeader meta = r.getHeader();
        String statusCode = "-"; // TODO (meta.getStatusCode() == null)?
            // "-": meta.getStatusCode();
        StringBuffer buffer = new StringBuffer(CDX_LINE_BUFFER_SIZE);
        buffer.append(meta.getDate());
        buffer.append(' ');
        buffer.append('-' /* TODO meta.getIp()*/);
        buffer.append(' ');
        buffer.append(meta.getUrl());
        buffer.append(' ');
        buffer.append(meta.getMimetype());
        buffer.append(' ');
        buffer.append(statusCode);
        buffer.append(' ');
        buffer.append((meta.getDigest() == null)? "-": meta.getDigest());
        buffer.append(' ');
        buffer.append(meta.getOffset());
        buffer.append(' ');
        buffer.append(meta.getLength());
        buffer.append(' ');
        buffer.append(getShortWarcFileName(meta));

        return buffer.toString();
    }

    /**
     * Generate a CDX index file for an ARC file.
     *
     * @param urlOrPath The ARC file to generate a CDX index for
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void createCDXIndexFile(String urlOrPath)
    throws IOException, java.text.ParseException {
        output(urlOrPath, true, CDX_FILE, false);
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
            "Pass true|false. Expensive. Default: true (SHA-1)."));
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
                        String tmp =
                            cmdlineOptions[i].getValue().toLowerCase();
                        if (Boolean.FALSE.toString().equals(tmp)) {
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
            WARCReader arc =
                WARCReaderFactory.get(new File((String)cmdlineArgs.get(0)), offset);
            arc.setStrict(strict);
            outputARCRecord(arc, (WARCRecord)arc.get(), format);
        } else {
            for (Iterator i = cmdlineArgs.iterator(); i.hasNext();) {
                String urlOrPath = (String)i.next();
                try {
                    output(urlOrPath, digest, format, strict);
                } catch (RuntimeException e) {
                    // Write out name of file we failed on to help with
                    // debugging.  Then print stack trace and try to keep
                    // going.  We do this for case where we're being fed
                    // a bunch of ARCs; just note the bad one and move
                    // on to the next.
                    System.err.println("Exception processing " + urlOrPath +
                        ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        }
    }
}
