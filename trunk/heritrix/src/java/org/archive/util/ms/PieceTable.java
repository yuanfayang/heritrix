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
 * The piece table of a .doc file.  
 * 
 * @author pjack
 */
class PieceTable {


    final static int ANSI_INDICATOR = 1 << 30;
    final static int ANSI_MASK = ~(3 << 30);

    private int count;
    private int maxSize;

    private int current;
    private Piece currentPiece;


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
        r = new BufferedSeekInputStream(r, cache);
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

    public int getMaxSize() {
        return maxSize;
    }


    public Piece next() throws IOException {
        if (current >= count) {
            currentPiece = null;
            return null;
        }
        
        charPos.position(current * 4);
        int cp = Endian.littleInt(charPos);
        
        filePos.position(current * 8 + 2);
        int encoded = Endian.littleInt(filePos);
        
        current++;

        int start;
        if (currentPiece == null) {
            start = 0;
        } else {
            start = currentPiece.getCharPosLimit();
        }
        if ((encoded & ANSI_INDICATOR) == 0) {
            Piece piece = new Piece(encoded, start, cp, true);
            currentPiece = piece;
            return piece;
        } else {
            int filePos = (encoded & ANSI_MASK) / 2;
            Piece piece = new Piece(filePos, start, cp, false);
            currentPiece = piece;
            return piece;
        }
    }

    
    public Piece pieceFor(int charPos) throws IOException {
        if (currentPiece.contains(charPos)) {
            return currentPiece;
        }
     
        // FIXME: Use binary search to find piece index
        
        current = 0;
        currentPiece = null;
        next();
        
        while (currentPiece != null) {
            if (currentPiece.contains(charPos)) {
                return currentPiece;
            }
            next();
        }
        
        return null;
    }

}
