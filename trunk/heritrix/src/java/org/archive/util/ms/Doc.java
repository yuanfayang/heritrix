/* Doc
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


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.io.Endian;
import org.archive.io.RandomAccessInputStream;
import org.archive.io.SeekInputStream;


/**
 * Reads .doc files.
 * 
 * @author pjack
 */
public class Doc {
    
    
    final private static Logger LOGGER = Logger.getLogger(Doc.class.getName());
    

    /**
     * Static utility library, do not instantiate.
     */
    private Doc() {
    }


    /**
     * Returns the text of the .doc file with the given file name.
     * 
     * @param docFilename   the name of the file whose text to return
     * @return  the text of that file
     * @throws IOException  if an IO error occurs
     */
    public static Reader getText(String docFilename) throws IOException {
        return getText(new File(docFilename));
    }


    /**
     * Returns the text of the given .doc file.
     * 
     * @param doc   the .doc file whose text to return
     * @return   the text of that file
     * @throws IOException   if an IO error occurs
     */
    public static Reader getText(File doc) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(doc, "r");
        RandomAccessInputStream rais = new RandomAccessInputStream(raf);
        return getText(rais);
    }

    
    
    public static Reader getText(SeekInputStream input) throws IOException {
        BlockFileSystem bfs = new DefaultBlockFileSystem(input);
        return getText(bfs);
    }

    
    
    public static Reader getText(BlockFileSystem wordDoc) throws IOException {
        List<Entry> entries = wordDoc.getRoot().list();
        Entry main = find(entries, "WordDocument");
        SeekInputStream mainStream = main.open();
        
        mainStream.position(10);
        int flags = Endian.littleChar(mainStream);
        boolean complex = (flags & 0x0004) == 0x0004;
        boolean tableOne = (flags & 0x0200) == 0x0200;
        String tableName = tableOne ? "1Table" : "0Table";
        Entry table = find(entries, tableName);
        SeekInputStream tableStream = table.open();
        
        mainStream.position(24);
        int fcMin = Endian.littleInt(mainStream);
        int fcMax = Endian.littleInt(mainStream);
        
        mainStream.position(76);
        int cppText = Endian.littleInt(mainStream);
        
        mainStream.position(418);
        int fcClx = Endian.littleInt(mainStream);
        int fcSz = Endian.littleInt(mainStream);
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("fcMin: " + fcMin);
            LOGGER.fine("fcMax: " + fcMax);
            LOGGER.fine("FcClx: " + fcClx);
            LOGGER.fine("szClx: " + fcSz);
            LOGGER.fine("complex: " + complex);
            LOGGER.fine("cppText: " + cppText);
        }
        PieceTable pt = new PieceTable(tableStream, fcClx, fcMax - fcMin);
        return new PieceReader(pt, mainStream);
    }


    private static Entry find(List<Entry> entries, String name) {
        for (Entry e: entries) {
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

}
