/* ReplayCharSequenceFactoryTest
 * 
 * Created on Mar 8, 2004
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
 * Test the ReplayCharSequence factory.
 * 
 * @author stack
 * @version $Revision$, $Date$
 */
public class ReplayCharSequenceFactoryTest extends TmpDirTestCase
{
    /**
     * Logger.
     */
    private static Logger logger =
        Logger.getLogger("org.archive.io.ReplayCharSequenceFactoryTest");
    

    private static final int SEQUENCE_LENGTH = 127;
    private static final int MULTIPLIER = 3;
    private static final int BUFFER_SIZE = SEQUENCE_LENGTH * MULTIPLIER;
    private static final int INCREMENT = 1;
    
    /**
     * Name of the file with regular content.
     */
    private static final String REGULAR_CONTENT_FILE_NAME =
        "ReplayCharSequenceFactoryTest.regular.content.txt";
    
    /**
     * Regular content file.
     */
    private File regularFile = null;
    
    /**
     * Buffer of regular content.
     */
    private byte [] regularBuffer = null;
    
    /**
     * Instance of the replay char sequence factory.
     */
    private ReplayCharSequenceFactory factory = null;
    
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        // Write a single-byte file of regular content.
        this.regularBuffer =
            fillBufferWithRegularContent(new byte [BUFFER_SIZE]);
        this.regularFile = new File(getTmpDir(), REGULAR_CONTENT_FILE_NAME);
        writeFile(this.regularFile, this.regularBuffer, MULTIPLIER);
        this.factory = ReplayCharSequenceFactory.getInstance();
    }
/*
    public void testGetReplayCharSequenceByteZeroOffset() throws IOException {
        
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                0, this.regularFile.getAbsolutePath(), null);

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    
    public void testGetReplayCharSequenceByteOffset() throws IOException {
        
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                SEQUENCE_LENGTH, this.regularFile.getAbsolutePath(), null);

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    

    public void testGetReplayCharSequenceMultiByteZeroOffset()
        throws IOException {
        
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                0, this.regularFile.getAbsolutePath(), "UTF-8");

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    */
    public void testGetReplayCharSequenceMultiByteOffset() throws IOException {
        
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                SEQUENCE_LENGTH, this.regularFile.getAbsolutePath(),
                "UTF-8");

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    
    /**
     * Accessing characters test.
     * 
     * @param rcs The ReplayCharSequence to try out.
     */
    private void accessingCharacters(CharSequence rcs) {
        long timestamp = (new Date()).getTime();
        int seeks = 0;
        for (int i = (INCREMENT * 2); (i + INCREMENT) < rcs.length();
                i += INCREMENT) {
            checkCharacter(rcs, i);
            seeks++;
            for (int j = i - INCREMENT; j < i; j++) {
                checkCharacter(rcs, j);
                seeks++;
            }
        }
        logger.info(rcs + " seeks count " + seeks + " in " +
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
    private void checkCharacter(CharSequence rcs, int i) {
        int c = rcs.charAt(i);
        assertTrue("Character " + Integer.toString(c) + " at offset " + i +
            " unexpected.", (c % SEQUENCE_LENGTH) == (i % SEQUENCE_LENGTH));
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
            index++;
            if (index >= SEQUENCE_LENGTH) {
                // Reset the index.
                index = 0;
            }
        }
        return buffer;
    }
    
    public void testCheckParameters()
    {
        // TODO.
    }
}
