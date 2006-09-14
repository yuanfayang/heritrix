/* PieceTable
*
* Created on September 12, 2006
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
package org.archive.util.ms;

import java.io.IOException;

import org.archive.io.BufferedSeekInputStream;
import org.archive.io.Endian;
import org.archive.io.OriginSeekInputStream;
import org.archive.io.SafeSeekInputStream;
import org.archive.io.SeekInputStream;


/**
 * 
 * 
 * @author pjack
 */
class PieceTable {


    final private static int ANSI_INDICATOR = 1 << 30;
    final private static int ANSI_MASK = ~(3 << 30);

    private int count;
    private int maxSize;

    private int current;


    private SeekInputStream charPos;
    private SeekInputStream filePos;
    

    public PieceTable(SeekInputStream tableStream, int offset, 
            int maxSize, int cachedRecords) throws IOException {
        tableStream.position(offset);
        skipProperties(tableStream);
        int sizeInBytes = Endian.littleInt(tableStream);
        this.count = (sizeInBytes - 4) / 12 + 1;
        long tp = tableStream.position();
        long charPosStart = tp + 4;
        long filePosStart = tp + count * 4;
        
        this.charPos = wrap(tableStream, charPosStart, cachedRecords * 4);
        this.filePos = wrap(tableStream, filePosStart, cachedRecords * 8);
        this.maxSize = maxSize;
    }
    
    
    private SeekInputStream wrap(SeekInputStream input, long pos, int cache) 
    throws IOException {
        SeekInputStream r = new SafeSeekInputStream(input);
        r = new OriginSeekInputStream(r, pos);
        r = new BufferedSeekInputStream(r, 1024);
        return r;
    }
    
    
    private static void skipProperties(SeekInputStream input) throws IOException {
        int tag = input.read();
        while (tag == 1) {
            int size = Endian.littleChar(input);
            while (size > 0) {
                size -= input.skip(size);
            }
            tag = input.read();
        }
        if (tag != 2) {
            throw new IllegalStateException();
        }
    }

/*
    public Piece getPiece(int charPos) throws IOException {
        int index = getIndex(charPos);
        int boundary;
        if (index == count - 1) {
            boundary = maxSize;
        } else {
            tableStream.position(charPosStart + index * 4 + 4);
            boundary = decode(Endian.littleInt(tableStream));
        }
        int size = boundary - charPos;
        tableStream.position(filePosStart + index * 8 + 2);
        int encoded = Endian.littleInt(tableStream);
        if ((encoded & ANSI_INDICATOR) == 0) {
            return new Piece(encoded, size, true);
        } else {
            int filePos = (encoded & ANSI_MASK) / 2;
            return new Piece(filePos, size, false);
        }
    }


    private static int decode(int encoded) {
        if ((encoded & ANSI_INDICATOR) == 0) {
            return encoded;
        } else {
            return (encoded & ANSI_MASK) / 2;
        }
    }


    private int getIndex(int targetCharPos) throws IOException {
        // FIXME: Binary search instead of linear search.
        tableStream.position(charPosStart);
        int index = 0;
        int cp = Endian.littleInt(tableStream);
        while (index < count) {
            if (targetCharPos <= cp) {
                return index;
            }
            cp = Endian.littleInt(tableStream);
            index++;
        }
        return count - 1;
    }
*/

    public int getMaxSize() {
        return maxSize;
    }


    public Piece next() throws IOException {
        if (current >= count) {
            System.out.println("Cur: " + current);
            return null;
        }
        
        charPos.position(current * 4);
        int cp = Endian.littleInt(charPos);
        
        filePos.position(current * 8 + 2);
        int encoded = Endian.littleInt(filePos);

        if ((encoded & ANSI_INDICATOR) == 0) {
            Piece piece = new Piece(encoded, cp, true);
            return piece;
        } else {
            int filePos = (encoded & ANSI_MASK) / 2;
            Piece piece = new Piece(filePos, cp, false);
            return piece;
        }
    }


}
