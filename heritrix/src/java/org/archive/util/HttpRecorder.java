/* HTTPRecorder
 *
 * $Id$
 *
 * Created on Sep 22, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.archive.io.RecordingInputStream;
import org.archive.io.RecordingOutputStream;


/**
 * Pairs together a RecordingInputStream and RecordingOutputStream
 * to capture exactly a single HTTP transaction.
 *
 * Initially only supports HTTP/1.0 (one request, one response per stream)
 *
 * @author gojomo
 */
public class HttpRecorder
{
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 4096;
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 65536;

    private RecordingInputStream ris = null;
    private RecordingOutputStream ros = null;

    /**
     * Backing file basename.
     *
     * Keep it around so can clean up backing files left on disk.
     */
    private String backingFileBasename = null;

    /**
     * Backing file output stream suffix.
     */
    private static final String RECORDING_OUTPUT_STREAM_SUFFIX = ".ros";

   /**
    * Backing file input stream suffix.
    */
    private static final String RECORDING_INPUT_STREAM_SUFFIX = ".ris";


    /**
     * Create an HttpRecorder.
     *
     * @param tempDir Directory into which we drop backing files for
     * recorded input and output.
     * @param backingFilenameBase Backing filename base to which we'll append
     * suffices <code>ris</code> for recorded input stream and
     * <code>ros</code> for recorded output stream.
     */
    public HttpRecorder(File tempDir, String backingFilenameBase) {
    	tempDir.mkdirs();
        this.backingFileBasename =
            (new File(tempDir.getPath(), backingFilenameBase))
                .getAbsolutePath();
    	this.ris = new RecordingInputStream(DEFAULT_INPUT_BUFFER_SIZE,
            this.backingFileBasename + RECORDING_INPUT_STREAM_SUFFIX);
    	this.ros = new RecordingOutputStream(DEFAULT_OUTPUT_BUFFER_SIZE,
            this.backingFileBasename + RECORDING_OUTPUT_STREAM_SUFFIX);
    }

    /**
     * Wrap the provided stream with the internal RecordingInputStream
     *
     * @param is InputStream to wrap.
     *
     * @return The input stream wrapper which itself is an input stream.
     * Pass this in place of the passed stream so input can be recorded.
     *
     * @throws IOException
     */
    public InputStream inputWrap(InputStream is) throws IOException {
    	ris.open(is);
    	return ris;
    }

    /**
     * Wrap the provided stream with the internal RecordingOutputStream
     *
     * @param os The output stream to wrap.
     *
     * @return The output stream wrapper which is itself an output stream.
     * Pass this in place of the passed stream so output can be recorded.
     *
     * @throws IOException
     */
    public OutputStream outputWrap(OutputStream os) throws IOException {
    	ros.open(os);
    	return ros;
    }

    /**
     * Close all streams.
     */
    public void close() {
    	try {
    		ris.close();
    	} catch (IOException e) {
            // TODO: Can we not let the exception out of here and report it
            // higher up in the caller?
    		DevUtils.logger.log(Level.SEVERE, "close() ris" +
                DevUtils.extraInfo(), e);
    	}
    	try {
    		ros.close();
    	} catch (IOException e) {
    		DevUtils.logger.log(Level.SEVERE, "close() ros" +
                DevUtils.extraInfo(), e);
    	}
    }

    /**
     * Return the internal RecordingInputStream
     *
     * @return A RIS.
     */
    public RecordingInputStream getRecordedInput() {
    	return this.ris;
    }

    /**
     * @return The RecordingOutputStream.
     */
    public RecordingOutputStream getRecordedOutput() {
        return this.ros;
    }

    /**
     * Mark current position as the point where the HTTP headers end.
     */
    public void markContentBegin() {
    	ris.markContentBegin();
    }

    public long getResponseContentLength() {
    	return ris.getResponseContentLength();
    }

    /**
     * Close both input and output recorders.
     *
     * Recorders are the output streams to which we are recording.
     * {@link #close} closes the stream that is being recorded and the
     * recorder. This method explicitly closes the recorder only.
     */
    public void closeRecorders() {
    	try {
    		ris.closeRecorder();
    		ros.closeRecorder();
    	} catch (IOException e) {
    		DevUtils.warnHandle(e, "Convert to runtime exception?");
    	}
    }

    /**
     * Clean up backing files.
     *
     * @see java.lang.Object#finalize()
     */
    protected void finalize()
        throws Throwable
    {
        try
        {
            this.cleanup();
        }

        catch(Exception e)
        {
            // We're in finalize.  Not much we can do about this.
            e.printStackTrace(System.out);
        }

        super.finalize();
    }

    /**
     * Cleanup backing files.
     *
     * Call when completely done w/ recorder.  Removes any backing files that
     * may have been dropped.
     */
    public void cleanup()
    {
        this.close();
        this.delete(this.backingFileBasename + RECORDING_OUTPUT_STREAM_SUFFIX);
        this.delete(this.backingFileBasename + RECORDING_INPUT_STREAM_SUFFIX);
    }

    /**
     * Delete file if exists.
     *
     * @param name Filename to delete.
     */
    private void delete(String name)
    {
        File f = new File(name);
        if (f.exists())
        {
            f.delete();
        }
    }
}
