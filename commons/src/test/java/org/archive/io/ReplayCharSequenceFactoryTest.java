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
import java.io.OutputStream;
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
        this.regularBuffer =
            fillBufferWithRegularContent(new byte [BUFFER_SIZE]);
        this.factory = ReplayCharSequenceFactory.getInstance();
    }

    public void testShiftjis() throws IOException {

        // Here's the bytes for the JIS encoding of the Japanese form of Nihongo
        byte[] bytes_nihongo = {
            (byte) 0x1B, (byte) 0x24, (byte) 0x42, (byte) 0x46,
            (byte) 0x7C, (byte) 0x4B, (byte) 0x5C, (byte) 0x38,
            (byte) 0x6C, (byte) 0x1B, (byte) 0x28, (byte) 0x42,
            (byte) 0x1B, (byte) 0x28, (byte) 0x42 };
        final String ENCODING = "SJIS";
        // Here is nihongo converted to JVM encoding.
        String nihongo = new String(bytes_nihongo, ENCODING);

        String fileName =
            writeShiftjisFile("testShiftjis", bytes_nihongo).
                getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                bytes_nihongo, bytes_nihongo.length +
                    (bytes_nihongo.length * MULTIPLIER),
                0, fileName, ENCODING);

        // Now check that start of the rcs comes back in as nihongo string.
        String rcsStr = rcs.subSequence(0, nihongo.length()).toString();
        assertTrue("Nihongo " + nihongo + " does not equal converted string" +
                " from rcs " + rcsStr,
            nihongo.equals(rcsStr));
        // And assert next string is also properly nihongo.
        if (rcs.length() >= (nihongo.length() * 2)) {
            rcsStr = rcs.subSequence(nihongo.length(),
                nihongo.length() + nihongo.length()).toString();
            assertTrue("Nihongo " + nihongo + " does not equal converted " +
                " string from rcs (2nd time)" + rcsStr,
                nihongo.equals(rcsStr));
        }
    }

    public void testGetReplayCharSequenceByteZeroOffset() throws IOException {

        String fileName =
            writeRegularFile("testGetReplayCharSequenceByteZeroOffset").
                getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                0, fileName, null);

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }

    public void testGetReplayCharSequenceByteOffset() throws IOException {

        String fileName =
            writeRegularFile("testGetReplayCharSequenceByteOffset").
                getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                SEQUENCE_LENGTH, fileName, null);

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }

    public void testGetReplayCharSequenceMultiByteZeroOffset()
        throws IOException {

        String fileName =
            writeRegularFile("testGetReplayCharSequenceMultiByteZeroOffset").
                getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                0, fileName, "UTF-8");

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }

    public void testGetReplayCharSequenceMultiByteOffset() throws IOException {

        String fileName =
            writeRegularFile("testGetReplayCharSequenceMultiByteOffset").
                getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
                this.regularBuffer,
                this.regularBuffer.length +
                    (this.regularBuffer.length * MULTIPLIER),
                SEQUENCE_LENGTH, fileName, "UTF-8");

        for (int i = 0; i < MULTIPLIER; i++) {
            accessingCharacters(rcs);
        }
    }
    
    public void testReplayCharSequenceByteToString() throws IOException {
        String fileContent = "Some file content";
        byte [] buffer = fileContent.getBytes();
        File f = new File(getTmpDir(),
            "testReplayCharSequenceByteToString.txt");
        String fileName =
            writeFile(f, buffer, buffer.length).getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
            buffer, buffer.length, 0, fileName, null);
        String result = rcs.toString();
        assertTrue("Strings don't match " + result + " " + fileContent,
            fileContent.equals(result));
    }
    
    public void testReplayCharSequenceByteToStringMulti() throws IOException {
        String fileContent = "Some file content";
        byte [] buffer = fileContent.getBytes("UTF-8");
        File f = new File(getTmpDir(),
            "testReplayCharSequenceByteToStringMulti.txt");
        String fileName =
            writeFile(f, buffer, buffer.length).getAbsolutePath();
        ReplayCharSequence rcs = this.factory.getReplayCharSequence(
            buffer, buffer.length, 0, fileName, "UTF-8");
        String result = rcs.toString();
        assertTrue("Strings don't match " + result + " " + fileContent,
                fileContent.equals(result));
    }
    
    /**
     * Accessing characters test.
     *
     * Checks that characters in the rcs are in sequence.
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
        // Note that printing out below breaks cruisecontrols drawing
        // of the xml unit test results because it outputs disallowed
        // xml characters.
        logger.fine(rcs + " seeks count " + seeks + " in " +
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
     * @param baseName
     * @param buffer
     * @return Write a shiftjis file w/ japanese characters.
     * @throws IOException
     */
    private File writeShiftjisFile(String baseName, byte [] buffer)
            throws IOException {
        File file = new File(getTmpDir(), baseName + ".shiftjisContent.txt");
        writeFile(file, buffer, MULTIPLIER);
        return file;
    }

    /**
     * @param baseName
     * @return Regular file reference.
     * @throws IOException
     */
    private File writeRegularFile(String baseName) throws IOException {
        // Write a single-byte file of regular content.
        File file = new File(getTmpDir(), baseName + ".regularContent.txt");
        writeFile(file, this.regularBuffer, MULTIPLIER);
        return file;
    }

    /**
     * Write a file.
     *
     * @param file File to write.
     * @param buffer Regular content to write.
     * @param count Number of times to write the buffer.
     * @return <code>file</code>.
     * @throws IOException
     */
    private File writeFile(File file, byte [] buffer, int count)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStream bos = new BufferedOutputStream(fos);
        for( int i = 0; i < count; i++) {
            bos.write(buffer);
        }
        bos.close();
        return file;
    }

    /**
     * Fill a buffer w/ regular progression of characters.
     * @param buffer Buffer to fill.
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
