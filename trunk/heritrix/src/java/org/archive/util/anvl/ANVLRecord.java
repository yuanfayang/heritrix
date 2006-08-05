/* ANVLRecord
*
* $Id$
*
* Created on July 26, 2006.
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
package org.archive.util.anvl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.archive.io.UTF8Bytes;

/**
 * An ordered {@link List} with 'data' {@link Element} values.
 * ANVLRecords end with a blank line.
 * 
 * @see <a
 * href="http://www.cdlib.org/inside/diglib/ark/anvlspec.pdf">A Name-Value
 * Language (ANVL)</a>.
 * @author stack
 */
public class ANVLRecord extends ArrayList<Element> implements UTF8Bytes {
	private static final long serialVersionUID = -4610638888453052958L;
	
	public static final String MIMETYPE = "text/anvl";
	
	public static final ANVLRecord EMPTY_ANVL_RECORD = new ANVLRecord();
    
    /**
     * Arbitrary upper bound on maximum size of ANVL Record.
     * Will throw an IOException if exceed this size.
     */
    public static final long MAXIMUM_SIZE = 1024 * 10;
	
	/**
	 * An ANVL 'newline'.
	 * @see http://en.wikipedia.org/wiki/CRLF
	 */
    static final String CRLF = "\r\n";
    
    static final String FOLD_PREFIX = CRLF + ' ';
    
    public ANVLRecord() {
        super();
    }

    public ANVLRecord(Collection<? extends Element> c) {
        super(c);
    }

    public ANVLRecord(int initialCapacity) {
        super(initialCapacity);
    }
    
    // TODO: Remove support for 'Comment'?
    public boolean addComment(final String s) {
    	return super.add(new Element(new Comment(s)));
    }
    
    public boolean addLabel(final String l) {
    	return super.add(new Element(new Label(l)));
    }

    public boolean addLabelValue(final String l, final String v) {
    	return super.add(new Element(new Label(l), new Value(v)));
    }
    
    @Override
    public String toString() {
        // TODO: What to emit for empty ANVLRecord?
        StringBuilder sb = new StringBuilder();
        for (final Iterator i = iterator(); i.hasNext();) {
            sb.append(i.next());
            sb.append(CRLF);
        }
        // 'ANVL Records end in a blank line'.
        sb.append(CRLF);
        return sb.toString();
    }
    
    @Override
    public ANVLRecord clone() {
        return new ANVLRecord(this);
    }
    
    public byte [] getUTF8Bytes()
    throws UnsupportedEncodingException {
        return toString().getBytes(UTF8);
    }
    
    /**
     * Parses a single ANVLRecord from passed InputStream.
     * @param is InputStream
     * @return An ANVLRecord instance.
     * @throws IOException
     */
    public static ANVLRecord load(final InputStream is)
    throws IOException {
        // Read as a single-byte stream until we get to a CRLFCRLF
        // End-of-ANVLRecord then parse all read as a UTF-8 Stream.  Doing it
        // this way makes it so I don't need to be passed a RepositionableStream
        // or a Stream that supports marking.  Also no danger of over-reading
        // which can happen when we wrap passed Stream with a InputStreamReader
        // for doing character conversion (See the class comment). Also, no
        // danger of possible leaking because we don't want to close
        // InputStreamReader.
        // TODO: See if there is a UTF-8 character or sequence of
        // characters that could result inadvertently in a CRLFCRLF
        // byte sequence appearing in their midst
        char previousCharacter;
        char c = (char)-1;
        boolean wasCRLF = false;
        boolean recordStart = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean done = false;
        int read = 0;
        while (!done) {
            if (read++ >= MAXIMUM_SIZE) {
                throw new IOException("Read " + MAXIMUM_SIZE +
                    " bytes without finding End-Of-ANVLRecord");
            }
            previousCharacter = c;
            c = (char)is.read();
            if (c == -1) {
                throw new IOException("End-Of-Stream before End-Of-ANVLRecord");
            }
            if (isLF(c) && isCR(previousCharacter)) {
                if (wasCRLF) {
                    done = true;
                }
                wasCRLF = true;
            } else if (!recordStart && Character.isWhitespace(c)) {
                // Skip any whitespace at start of ANVLRecord.
                continue;
            } else {
                if (wasCRLF && !isCR(c)) {
                    wasCRLF = false;
                }
                if (!recordStart) {
                    recordStart = true;
                }
            }
            baos.write(c);
        }
        
        InputStreamReader utf8Stream = new InputStreamReader(
            new ByteArrayInputStream(baos.toByteArray()), UTF8);
        ANVLRecord result = null;
        try {
            result = load(utf8Stream);
        } finally {
            utf8Stream.close();
        }
        return result;
    }
    
    /**
     * @param utf8Stream Stream cued-up on an ANVLRecord.  We do not close
     * passed Stream when done.  Be careful, InputStreamReader may over-read
     * the ANVLRecord (according to its class comment). 
     * @return ANVLRecord read from parsed Stream.
     * @throws IOException 
     */
    public static ANVLRecord load(final InputStreamReader utf8Stream)
    throws IOException {
        ANVLRecord record = new ANVLRecord();
        StringBuilder sb = new StringBuilder();
        boolean inComment = false;
        boolean inValue = false;
        boolean wasCRLF = false;
        char previousCharacter;
        char c = (char)-1;
        String label = null;
        while (true) {
            previousCharacter = c;
            c = (char)utf8Stream.read();
            if (c == -1) {
                throw new IOException("Premature EOF");
            }
            if (isLF((char)c) && isCR((char)previousCharacter)) {
                if (wasCRLF) {
                    // Double CRLF means End-Of-ANVLRecord.
                    break;
                }
                wasCRLF = true;
            } else if (wasCRLF && !isCR(c) && Character.isWhitespace(c)) {
                // Skip all whitespace after CRLF.
                continue;
            } else if (wasCRLF) {
                if (!isCR(c)) {
                    wasCRLF = false;
                }
                if (!isCR((char)c) &&
                        Character.isWhitespace(previousCharacter)) {
                    if (!inValue) {
                        throw new IOException("Ambigious record format");
                    }
                    sb.append(' ');
                } else if (inValue) {
                    if (label == null) {
                        throw new IOException("Empty label when there " +
                            "should be one");
                    }
                    record.addLabelValue(label.toString(), sb.toString());
                    sb.setLength(0);
                    inValue = false;
                    label =  null;
                }
            }

            if (!inComment && c == '#') {
                inComment = true;
                continue;
            } else if (!inValue && c == ':') {
                if (sb.length() <= 0) {
                    throw new IOException("Empty label?");
                }
                label = sb.toString();
                sb.setLength(0);
                continue;
            } else if (!inValue && label != null && Character.isWhitespace(c)) {
                continue;
            } else if (label != null && !inValue) {
                inValue = true;
            }
            sb.append(c);
        }
        return record;
    }
    
    public static boolean isCROrLF(final char c) {
        return isCR(c) || isLF(c);
    }
    
    public static boolean isCR(final char c) {
        return c == ANVLRecord.CRLF.charAt(0);
    }
    
    public static boolean isLF(final char c) {
        return c == ANVLRecord.CRLF.charAt(1);
    }
}