/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.math.LongRange;


/** Utility methods for manipulating files and directories.
 *
 * @contributor John Erik Halse
 * @contributor gojomo
 */
public class FileUtils {
    private static final Logger LOGGER =
        Logger.getLogger(FileUtils.class.getName());
            
    /**
     * Constructor made private because all methods of this class are static.
     */
    private FileUtils() {
        super();
    }

    /** Recursively copy all files from one directory to another.
     *
     * @param src file or directory to copy from.
     * @param dest file or directory to copy to.
     * @throws IOException
     * @deprecated use org.apache.commons.io.FileUtils.copyDirectory()
     */
    public static void copyFiles(File src, File dest)
    throws IOException {
        org.apache.commons.io.FileUtils.copyDirectory(src, dest);
    }
    
    /**
     * Copy the src file to the destination. Deletes any preexisting
     * file at destination. 
     * 
     * @param src
     * @param dest
     * @return True if the extent was greater than actual bytes copied.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean copyFile(final File src, final File dest)
    throws FileNotFoundException, IOException {
        return copyFile(src, dest, -1, true);
    }
    
    /**
     * Copy the src file to the destination.
     * 
     * @param src
     * @param dest
     * @param overwrite If target file already exits, and this parameter is
     * true, overwrite target file (We do this by first deleting the target
     * file before we begin the copy).
     * @return True if the extent was greater than actual bytes copied.
     * @throws FileNotFoundException
     * @throws IOException
     * @deprecated use org.apache.commons.io.FileUtils.co
     */
    public static boolean copyFile(final File srcFile, final File destFile,
        final boolean overwrite)
    throws FileNotFoundException, IOException {
        org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
        return false; 
    }
    
    /**
     * Copy up to extent bytes of the source file to the destination.
     * Deletes any preexisting file at destination.
     *
     * @param src
     * @param dest
     * @param extent Maximum number of bytes to copy
     * @return True if the extent was greater than actual bytes copied.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean copyFile(final File src, final File dest,
        long extent)
    throws FileNotFoundException, IOException {
        return copyFile(src, dest, extent, true);
    }

	/**
     * Copy up to extent bytes of the source file to the destination
     *
     * @param src
     * @param dest
     * @param extent Maximum number of bytes to copy
	 * @param overwrite If target file already exits, and this parameter is
     * true, overwrite target file (We do this by first deleting the target
     * file before we begin the copy).
	 * @return True if the extent was greater than actual bytes copied.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean copyFile(final File src, final File dest,
        long extent, final boolean overwrite)
    throws FileNotFoundException, IOException {
        boolean result = false;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Copying file " + src + " to " + dest + " extent " +
                extent + " exists " + dest.exists());
        }
        if (dest.exists()) {
            if (overwrite) {
                dest.delete();
                LOGGER.finer(dest.getAbsolutePath() + " removed before copy.");
            } else {
                // Already in place and we're not to overwrite.  Return.
                return result;
            }
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel fcin = null;
        FileChannel fcout = null;
        try {
            // Get channels
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            fcin = fis.getChannel();
            fcout = fos.getChannel();
            if (extent < 0) {
                extent = fcin.size();
            }

            // Do the file copy
            long trans = fcin.transferTo(0, extent, fcout);
            if (trans < extent) {
                result = false;
            }
            result = true; 
        } catch (IOException e) {
            // Add more info to the exception. Preserve old stacktrace.
            // We get 'Invalid argument' on some file copies. See
            // http://intellij.net/forums/thread.jsp?forum=13&thread=63027&message=853123
            // for related issue.
            String message = "Copying " + src.getAbsolutePath() + " to " +
                dest.getAbsolutePath() + " with extent " + extent +
                " got IOE: " + e.getMessage();
            if ((e instanceof ClosedByInterruptException) ||
                    ((e.getMessage()!=null)
                            &&e.getMessage().equals("Invalid argument"))) {
                LOGGER.severe("Failed copy, trying workaround: " + message);
                workaroundCopyFile(src, dest);
            } else {
                IOException newE = new IOException(message);
                newE.initCause(e);
                throw newE;
            }
        } finally {
            // finish up
            if (fcin != null) {
                fcin.close();
            }
            if (fcout != null) {
                fcout.close();
            }
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        return result;
    }
    
    protected static void workaroundCopyFile(final File src,
            final File dest)
    throws IOException {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(src);
            to = new FileOutputStream(dest);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead);
            }
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

	/** Deletes all files and subdirectories under dir.
     * @param dir
     * @return true if all deletions were successful. If a deletion fails, the
     *          method stops attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }



    /**
     * Utility method to read an entire file as a String.
     *
     * @param file
     * @return File as String.
     * @throws IOException
     * @deprecated use org.apache.commons.io.FileUtils.readFileToString()
     */
    public static String readFileAsString(File file) throws IOException {
        return org.apache.commons.io.FileUtils.readFileToString(file);
    }

    /**
     * Get a list of all files in directory that have passed prefix.
     *
     * @param dir Dir to look in.
     * @param prefix Basename of files to look for. Compare is case insensitive.
     *
     * @return List of files in dir that start w/ passed basename.
     */
    public static File [] getFilesWithPrefix(File dir, final String prefix) {
        FileFilter prefixFilter = new FileFilter() {
                public boolean accept(File pathname)
                {
                    return pathname.getName().toLowerCase().
                        startsWith(prefix.toLowerCase());
                }
            };
        return dir.listFiles(prefixFilter);
    }

    /** Get a @link java.io.FileFilter that filters files based on a regular
     * expression.
     *
     * @param regex the regular expression the files must match.
     * @return the newly created filter.
     */
    public static IOFileFilter getRegexFileFilter(String regex) {
        // Inner class defining the RegexpFileFilter
        class RegexFileFilter implements IOFileFilter {
            Pattern pattern;

            protected RegexFileFilter(String re) {
                pattern = Pattern.compile(re);
            }

            public boolean accept(File pathname) {
                return pattern.matcher(pathname.getName()).matches();
            }

            public boolean accept(File dir, String name) {
                return accept(new File(dir,name));
            }
        }

        return new RegexFileFilter(regex);
    }
    
    /**
     * Test file exists and is readable.
     * @param f File to test.
     * @exception FileNotFoundException If file does not exist or is not unreadable.
     */
    public static File assertReadable(final File f) throws FileNotFoundException {
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath() +
                " does not exist.");
        }

        if (!f.canRead()) {
            throw new FileNotFoundException(f.getAbsolutePath() +
                " is not readable.");
        }
        
        return f;
    }
    
    /**
     * @param f File to test.
     * @return True if file is readable, has uncompressed extension,
     * and magic string at file start.
     * @exception IOException If file not readable or other problem.
     */
    public static boolean isReadableWithExtensionAndMagic(final File f, 
            final String uncompressedExtension, final String magic)
    throws IOException {
        boolean result = false;
        FileUtils.assertReadable(f);
        if(f.getName().toLowerCase().endsWith(uncompressedExtension)) {
            FileInputStream fis = new FileInputStream(f);
            try {
                byte [] b = new byte[magic.length()];
                int read = fis.read(b, 0, magic.length());
                fis.close();
                if (read == magic.length()) {
                    StringBuffer beginStr
                        = new StringBuffer(magic.length());
                    for (int i = 0; i < magic.length(); i++) {
                        beginStr.append((char)b[i]);
                    }
                    
                    if (beginStr.toString().
                            equalsIgnoreCase(magic)) {
                        result = true;
                    }
                }
            } finally {
                fis.close();
            }
        }

        return result;
    }
    
    /**
     * Turn path into a File, relative to context (which may be ignored 
     * if path is absolute). 
     * 
     * @param context File context if path is relative
     * @param path String path to make into a File
     * @return File created
     */
    public static File maybeRelative(File context, String path) {
        File f = new File(path);
        if(f.isAbsolute()) {
            return f;
        }
        return new File(context, path);
    }
    
    /**
     * Load Properties instance from a File
     * 
     * @param file
     * @return Properties
     * @throws IOException
     */
    public static Properties loadProperties(File file) throws IOException {
        FileInputStream finp = new FileInputStream(file);
        try {
            Properties p = new Properties();
            p.load(finp);
            return p;
        } finally {
            IoUtils.close(finp);
        }
    }
    
    /**
     * Store Properties instance to a File
     * @param p
     * @param file destination File
     * @throws IOException
     */
    public static void storeProperties(Properties p, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            p.store(fos,"");
        } finally {
            IoUtils.close(fos);
        }
    }
    
    public static long tailLines(File file, int lineCount, List<String> lines ) throws IOException {
        return tailLines(file, lineCount, lines, 0, 0);
    }

    /**
     * Add lines found in given File to provided List. Up to lineCount
     * lines will be returned, through the endPosition location. (An
     * endPosition of 0 means end-of-file.) Return another file position 
     * which may be passed in a subsequent call to get lines strictly 
     * preceding this set. 
     * 
     * Lines are returned in <i>reverse-order</i>, latest in file first. 
     * 
     * @param file File to get lines
     * @param desiredLineCount number of lines to return (if sufficient lines 
     * precede the endPosition)
     * @param lines List<String> that lines are added to
     * @param endPosition return lines up through the line containing 
     * this position in the file
     * @param lineEstimate estimated size in bytes of lines. If zero, a 
     * guess will be used
     * @return long file position which can serve as the endPosition for
     * a subsequent call
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static long tailLines(File file, int desiredLineCount, List<String> lines, long endPosition, int lineEstimate) throws IOException {
        if(lineEstimate == 0) {
            lineEstimate = 80; 
        }
        int bufferSize = (desiredLineCount + 5) * lineEstimate; 
        if(endPosition == 0) {
            endPosition = file.length();
        }
        long startPosition = endPosition - bufferSize; 
        if(startPosition<0) {
            startPosition = 0; 
            bufferSize = (int) (endPosition - startPosition); 
        }

        // read reasonable chunk
        FileInputStream fis = new FileInputStream(file);
        fis.getChannel().position(startPosition); 
        byte[] buf = new byte[bufferSize];
        IoUtils.readFully(fis, buf);
        IOUtils.closeQuietly(fis);
        
        // find all line starts fully in buffer
        // (position after a line-end, per definition in 
        // BufferedReader.readLine
        LinkedList<Integer> lineStarts = new LinkedList<Integer>();
        if(startPosition==0) {
            lineStarts.add(0);
        }
        boolean atLineEnd = false; 
        boolean eatLF = false; 
        for(int i = 0; i < bufferSize; i++) {
            if ((char) buf[i] == '\n' && eatLF) {
                eatLF = false;
                continue;
            }
            if(atLineEnd) {
                lineStarts.add(i);
                atLineEnd = false; 
            }
            if ((char) buf[i] == '\r') {
                atLineEnd = true; 
                eatLF = true; 
                continue;
            }
            if ((char) buf[i] == '\n') {
                atLineEnd = true; 
            }
        }
        
        // discard extra lineStarts
        while (lineStarts.size()>desiredLineCount) {
            lineStarts.removeFirst();
        }
        int foundLinesCount = lineStarts.size();
        long continueEndPosition = endPosition; 
        if(foundLinesCount > 0) {
            int firstLine = lineStarts.getFirst();
            List<String> foundLines =
                IOUtils.readLines(new ByteArrayInputStream(buf,firstLine,bufferSize-firstLine));
            Collections.reverse(foundLines);
            lines.addAll(foundLines);
            continueEndPosition = startPosition + firstLine;
        }
 
        if(foundLinesCount<desiredLineCount && startPosition > 0) {
            int newLineEstimate =  bufferSize / (foundLinesCount+1);
            return tailLines(file,desiredLineCount-foundLinesCount,lines,continueEndPosition,newLineEstimate);
        }
        return continueEndPosition; 
    }

    public static void moveAsideIfExists(File file) throws IOException {
        if(file.exists()) {
            String newName = 
                file.getCanonicalPath() + "." 
                + ArchiveUtils.get14DigitDate(file.lastModified());
            file.renameTo(new File(newName));
        }
    }

    /**
     * Retrieve a number of lines from the file around the given 
     * position, as when paging forward or backward through a file. 
     * 
     * @param file File to retrieve lines
     * @param position offset to anchor lines
     * @param signedDesiredLineCount lines requested; if negative, 
     *        want this number of lines ending with a line containing
     *        the position; if positive, want this number of lines,
     *        all starting at or after position. 
     * @param lines List<String> to insert found lines
     * @param lineEstimate int estimate of line size, 0 means use default
     *        of 128
     * @return LongRange indicating the file offsets corresponding to 
     *         the beginning of the first line returned, and the point
     *         after the end of the last line returned
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static LongRange pagedLines(File file, long position,
            int signedDesiredLineCount, List<String> lines, int lineEstimate)
            throws IOException {
        // pick a reasonably size chunk likely to have all desired lines
        if(lineEstimate == 0) {
            lineEstimate = 128; 
        }
        // consider negative positions as from end of file; -1 = last byte
        if (position < 0) {
            position = file.length() + position; 
        }
        int desiredLineCount = Math.abs(signedDesiredLineCount);
        long startPosition;
        long fileEnd = file.length();
        int bufferSize = (desiredLineCount + 5) * lineEstimate; 
        if(signedDesiredLineCount>0) {
            // reading forward; include previous char in case line-end
            startPosition = position - 1;
        } else {
            // reading backward
            startPosition = position - bufferSize + (2 * lineEstimate);
        }
        if(startPosition<0) {
            startPosition = 0; 
        }
        if(startPosition+bufferSize > fileEnd) {
            bufferSize = (int)(fileEnd - startPosition); 
        }

        // read that reasonable chunk
        FileInputStream fis = new FileInputStream(file);
        fis.getChannel().position(startPosition); 
        byte[] buf = new byte[bufferSize];
        IoUtils.readFully(fis, buf);
        IOUtils.closeQuietly(fis);
        
        // find all line starts fully in buffer
        // (positions after a line-end, per line-end definition in 
        // BufferedReader.readLine)
        LinkedList<Integer> lineStarts = new LinkedList<Integer>();
        if(startPosition==0) {
            lineStarts.add(0);
        }
        boolean atLineEnd = false; 
        boolean eatLF = false; 
        for(int i = 0; i < bufferSize; i++) {
            if ((char) buf[i] == '\n' && eatLF) {
                eatLF = false;
                continue;
            }
            if(atLineEnd) {
                lineStarts.add(i);
                atLineEnd = false; 
                if(signedDesiredLineCount<0 && startPosition+i > position) {
                    // reached next line past position, read no more
                    break;
                }
            }
            if ((char) buf[i] == '\r') {
                atLineEnd = true; 
                eatLF = true; 
                continue;
            }
            if ((char) buf[i] == '\n') {
                atLineEnd = true; 
            }
        }
        if(startPosition+bufferSize == fileEnd) {
            // add phantom lineStart after end
            lineStarts.add(bufferSize);
        }
        int foundFullLines = lineStarts.size()-1;

        // if found no lines
        if(foundFullLines<1) {
            if(signedDesiredLineCount>0) {
                if(startPosition+bufferSize == fileEnd) {
                    // nothing more to read: return nothing
                    return new LongRange(fileEnd,fileEnd);
                } else {
                    // continue forward from end of buffer, perhaps larger lineEstimate
                    return pagedLines(file, startPosition+bufferSize, desiredLineCount, lines, Math.max(bufferSize,lineEstimate));
                }
                
            } else {
                // try again with much larger line estimate
                // TODO: fail gracefully before growing to multi-MB buffers
                return pagedLines(file, position, desiredLineCount, lines, bufferSize);
            }
        }
                
        // trim unneeded lines
        while(lineStarts.size()>desiredLineCount+1) {
            if (signedDesiredLineCount < 0) { 
                lineStarts.removeFirst();
            } else {
                lineStarts.removeLast();
            }
        }
        int firstLine =  lineStarts.getFirst();
        int partialLine =  lineStarts.getLast(); 
        LongRange range = new LongRange(startPosition + firstLine, startPosition + partialLine); 
        List<String> foundLines = 
            IOUtils.readLines(new ByteArrayInputStream(buf,firstLine,partialLine-firstLine));

        if(foundFullLines<desiredLineCount && signedDesiredLineCount < 0 && startPosition > 0) {
            // if needed and reading backward, read more lines from earlier
            range = expandRange(
                        range,
                        pagedLines(file, 
                                   startPosition, 
                                   signedDesiredLineCount+foundFullLines, 
                                   lines, 
                                   bufferSize/foundFullLines));
            
        }
        
        lines.addAll(foundLines); 
        
        if(signedDesiredLineCount < 0 && range.getMaximumLong() < position) {
            // did not get line containining start position
            range = expandRange(
                        range,
                        pagedLines(file,
                                   partialLine,
                                   1,
                                   lines,
                                   bufferSize/foundFullLines));
        }
        
        if(signedDesiredLineCount > 0 && foundFullLines < desiredLineCount && range.getMaximumLong() < fileEnd) {
            // need more forward lines
            range = expandRange(
                    range,
                    pagedLines(file,
                               range.getMaximumLong(),
                               desiredLineCount - foundFullLines,
                               lines,
                               bufferSize/foundFullLines));
        }
        
        return range; 
    }

    public static LongRange expandRange(LongRange range1, LongRange range2) {
        return new LongRange(Math.min(range1.getMinimumLong(), range2.getMinimumLong()),
                             Math.max(range1.getMaximumLong(), range2.getMaximumLong()));
        
    }
}