/* ReplayCharSequenceTest
 * 
 * Created on Mar 2, 2004
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
package org.archive.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.archive.util.TmpDirTestCase;

/**
 * Test ReplayCharSequence class.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class ReplayCharSequenceTest extends TmpDirTestCase {   
    
    /**
     * Logger.
     */
    private static Logger logger =
        Logger.getLogger("org.archive.io.ReplayCharSequenceTest");
    

    private static final int SEQUENCE_LENGTH = 256;
    private static final int MULTIPLIER = 3;
    private static final int BUFFER_SIZE = SEQUENCE_LENGTH * MULTIPLIER;
    private static final int INCREMENT = 16;

        
    /**
     * Test constructor.
     * 
     * Ensure we charAt gets expected character.  Assumes default encodings.
     */
    public void testReplayCharSequencebyteArraylonglongString()
            throws IOException {
        byte [] buffer = fillBufferWithRegularContent(new byte [BUFFER_SIZE]);
        File backingFile = new File(getTmpDir(),
            "testReplayCharSequencebyteArraylonglongString.bkng");
        writeFile(backingFile, buffer, MULTIPLIER);
        ReplayCharSequence rcs = new ReplayCharSequence(buffer,
            buffer.length + (buffer.length * MULTIPLIER), 0,
                backingFile.getAbsolutePath());
        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    
    /**
     * Accessing characters test.
     * 
     * @param rcs The ReplayCharSequence to try out.
     */
    private void accessingCharacters(ReplayCharSequence rcs) {
        long timestamp = (new Date()).getTime();
        int seeks = 0;
        for (int i = (INCREMENT * 2); 
                (i + INCREMENT) < (BUFFER_SIZE + (BUFFER_SIZE * MULTIPLIER));
                i += INCREMENT) {
            checkCharacter(rcs, i);
            seeks++;
            for (int j = i - INCREMENT; j < i; j++) {
                checkCharacter(rcs, j);
                seeks++;
            }
        }
        logger.info("Seeks count " + seeks + " in " +
            ((new Date().getTime()) - timestamp) + " milliseconds.");
    }
    
    /**
     * Check the character read.
     * 
     * Throws assertion if not expected result. 
     * 
     * @param rcs ReplayCharSequence to read from.
     * @param i Character offset.
     */
    private void checkCharacter(ReplayCharSequence rcs, int i) {
        int c = rcs.charAt(i);
        assertTrue("Character " + Integer.toString(c) +
            " unexpected", (c % SEQUENCE_LENGTH) == (i % SEQUENCE_LENGTH));
    }
    
    /**
     * Write a file.
     * 
     * @param file File to write.
     * @param buffer Regular content to write.
     * @param count Number of times to write the buffer.
     */
    private void writeFile(File file, byte [] buffer, int count)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        for( int i = 0; i < count; i++) {
            bos.write(buffer);
        }
        bos.close();
    }
    
    /**
     * Fill a buffer w/ regular progression of characters.
     * 
     * @para buffer Buffer to fill.
     * @return The buffer we filled.
     */
    private byte [] fillBufferWithRegularContent(byte [] buffer) {
        int index = 0;
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (index & 0x00ff);
            if (index >= 255) {
                // Reset the index.
                index = 0;
            } else {
                index++;
            }
        }
        return buffer;
    }
}
