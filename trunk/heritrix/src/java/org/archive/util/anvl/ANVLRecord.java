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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * Read as a single-byte stream until we get to a CRLFCRLF which
     * signifies End-of-ANVLRecord. Then parse all read as a UTF-8 Stream.
     * Doing it this way, while requiring a double-scan, it  makes it so do not
     * need to be passed a RepositionableStream or a Stream that supports
     * marking.  Also no danger of over-reading which can happen when we
     * wrap passed Stream with an InputStreamReader for doing UTF-8
     * character conversion (See the ISR class comment). Could also be leaking
     * issues when don't close ISR because we dont' want to close underlying
     * Stream. I looked at writing javacc grammer but seems like even here,
     * preprocessing is required to handle folding: See
     * https://javacc.dev.java.net/servlets/BrowseList?list=users&by=thread&from=56173).
     * @param is InputStream
     * @return An ANVLRecord instance.
     * @throws IOException
     */
    public static ANVLRecord load(final InputStream is)
    throws IOException {
        // It doesn't look like a CRLF sequence is possible in UTF-8 without
    	// it signifying CRLF: The top bits are set in multibyte characters.
    	// Was thinking of recording CRLF as I was running through this first
    	// parse but the offsets would then be incorrect if any multibyte
    	// characters in the intervening gaps between CRLF.
        boolean isCRLF = false;
        boolean recordStart = false;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        boolean done = false;
        int read = 0;
        for (int c  = -1, previousCharacter; !done;) {
            if (read++ >= MAXIMUM_SIZE) {
                throw new IOException("Read " + MAXIMUM_SIZE +
                    " bytes without finding  \\r\\n\\r\\n " +
                    "End-Of-ANVLRecord");
            }
            previousCharacter = c;
            c = is.read();
            if (c == -1) {
                throw new IOException("End-Of-Stream before \\r\\n\\r\\n " +
                    "End-Of-ANVLRecord");
            }
            if (isLF((char)c) && isCR((char)previousCharacter)) {
                if (isCRLF) {
                    // If we just had a CRLF, then its two CRLFs and its end of
                    // record.  We're done.
                    done = true;
                } else {
                    isCRLF = true;
                }
            } else if (!recordStart && Character.isWhitespace(c)) {
                // Skip any whitespace at start of ANVLRecord.
                continue;
            } else {
                // Clear isCRLF flag if this character is NOT a '\r'.
                if (isCRLF && !isCR((char)c)) {
                    isCRLF = false;
                }
                // Not whitespace so start record if we haven't already.
                if (!recordStart) {
                    recordStart = true;
                }
            }
            baos.write(c);
        }
        return load(new String(baos.toByteArray(), UTF8));
    }
    
    /**
     * @param s String with an ANVLRecord.
     * @return ANVLRecord parsed from passed String.
     * @throws IOException 
     */
    public static ANVLRecord load(final String s)
    throws IOException {
        ANVLRecord record = new ANVLRecord();
        boolean inValue = false, inLabel = false, inComment = false, 
            inNewLine = false;
        String label = null;
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0;  i < len; i++) {
            char c = s.charAt(i);
           
            // Assert I can do look-ahead.
            if ((i + 1) > len) {
                throw new IOException("Premature End-of-ANVLRecord at " +
                    s.substring(i));
            }
            
            // If at LF of a CRLF, just go around again.
            if (inNewLine && isLF(c)) {
                // Eat up the LF at end of CRLF.
                continue;
            }
            
            // If we're at a CRLF and we were just on one, then exit.
            if (inNewLine && isCR(c) && isLF(s.charAt(i + 1))) {
                break;
            }
            
            // Check if we're on a fold inside a value. Skip multiple white
            // space after CRLF replacing with a single ' '. 
            if (inNewLine && inValue) {
                if (Character.isWhitespace(c)) {
                    continue;
                }
                sb.append(' ');
            }
            
            // Else set flag if we're at start of a CRLF.
            inNewLine = isCR(c) && isLF(s.charAt(i + 1));
            
            // Eat up comments.
            if (inComment) {
                if (inNewLine) {
                    inComment = false;
                }
                continue;
            }
            
            if (inNewLine) {
                if (label != null && !inValue) {
                    // Label only 'data element'.
                    record.addLabel(label);
                    label = null;
                    sb.setLength(0);
                } else if (inValue) {
                    // Assert I can do look-ahead.
                    if ((i + 3) > len) {
                        throw new IOException("Premature End-of-ANVLRecord " +
                            "(2) at " + s.substring(i));
                    }
                    if (!isCR(s.charAt(i + 2)) && !isLF(s.charAt(i + 3)) &&
                            Character.isWhitespace(s.charAt(i + 2))) {
                        // Its a fold.  Let it go around. But add in a CRLF and
                        // do it here.  We don't let CRLF fall through to
                        // the sb.append on the end of this loop.
                        sb.append(CRLF);
                    } else {
                        // Next line is a new SubElement, a new Comment or
                        // Label.
                        record.addLabelValue(label, sb.toString());
                        sb.setLength(0);
                        label = null;
                        inValue = false;
                    }
                }
                // Don't let the '\r' through.
                continue;
            } else {
                if (!inLabel && !inValue && !inComment) {
                    // Start recording a comment, label or value.
                    if (Character.isWhitespace(c)) {
                        continue;
                    } else if (label == null && c == '#') {
                        inComment = true;
                        // Don't record comments.
                        continue;
                    } else if (label == null) {
                        inLabel = true;
                    } else {
                        inValue = true;
                    }
                } else {
                    // Label is odd.  It doesn't end on a CRLF, but on a ':'.
                    if (inLabel) {
                        if (c == ':') {
                            label = sb.toString();
                            sb.setLength(0);
                            inLabel = false;
                            continue;
                        }
                    }
                }
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