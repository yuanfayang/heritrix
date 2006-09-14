/* DefaultBlockFileSystem
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
import java.nio.ByteBuffer;

import org.apache.poi.util.LittleEndian;
import org.archive.io.SeekInputStream;
import org.archive.util.IoUtils;



public class DefaultBlockFileSystem implements BlockFileSystem {


    private SeekInputStream input;
    private HeaderBlock header;
    
    
    public DefaultBlockFileSystem(SeekInputStream input) throws IOException {
        this.input = input;
        byte[] temp = new byte[BLOCK_SIZE];
        IoUtils.readFully(input, temp);
        this.header = new HeaderBlock(ByteBuffer.wrap(temp));
    }


    public HeaderBlock getHeaderBlock() {
        return header;
    }


    public Entry getRoot() throws IOException {
        int block = header.getPropertiesStart();
        input.position((block + 1) * BLOCK_SIZE);
        return new DefaultEntry(this, input, 0);
    }


    public Entry getEntry(int propertyNumber) throws IOException {
        if (propertyNumber < 0) {
            return null;
        }
        int blockCount = propertyNumber / 4;
        int remainder = propertyNumber % 4;
        
        // FIXME: Support XBAT and SBAT
        int block = header.getPropertiesStart();
        for (int i = 0; i < blockCount; i++) {
            block = getNextBlock(block);
        }

        int filePos = (block + 1) * BLOCK_SIZE + remainder * 128;
        input.position(filePos);
        
        return new DefaultEntry(this, input, propertyNumber);
    }


    public int getNextBlock(int block) throws IOException {
        // FIXME: Support for XBAT 
        int headerBATIndex = block / 128;
        int batBlockIndex = block % 128;
        int batBlock = header.getBlockOffset(headerBATIndex);
        input.position((batBlock + 1) * BLOCK_SIZE + batBlockIndex * 4);
        return LittleEndian.readInt(input);
    }


    
    public SeekInputStream getRawInput() {
        return input;
    }
}
