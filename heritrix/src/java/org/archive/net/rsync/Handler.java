/* RsyncProtocolHandler.java
 *
 * $Id$
 *
 * Created Jul 15, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.net.rsync;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A protocol handler that uses native rsync client to do copy.
 * You need to define the system property
 * <code>-Djava.protocol.handler.pkgs=org.archive.net</code> to add this handler
 * to the java.net.URL set.  Assumes rsync is in path.  Define
 * system property
 * <code>-Dorg.archive.net.rsync.Handler.path=PATH_TO_RSYNC</code> to pass path
 * to rsync.
 * @author stack
 */
public class Handler extends URLStreamHandler {
    public static final String PREFIX = Handler.class.getName();
    public static final File TMPDIR =
        new File(System.getProperty("java.io.tmpdir", "/tmp"));
    public static final String RSYNC =
        System.getProperty(Handler.class.getName() + ".path", "rsync");

    protected URLConnection openConnection(URL u) {
        return new RsyncURLConnection(u);
    }

    private class RsyncURLConnection extends URLConnection {
        private final Logger LOGGER =
            Logger.getLogger(RsyncURLConnection.class.getName());
        protected RsyncURLConnection(URL u) {
            super(u);
        }

        public void connect() {
            // Do nothing. Do all when they go to get an input stream.
        }
        
        public InputStream getInputStream() throws IOException {
            final File rsyncCopy = File.createTempFile(PREFIX, null, TMPDIR);
            try {
                Process p = Runtime.getRuntime().exec(
                    new String[] {RSYNC, this.url.toExternalForm(),
                    rsyncCopy.getAbsolutePath()});
                // Gobble up any output.
                StreamGobbler err = new StreamGobbler(p.getErrorStream());
                err.start();
                StreamGobbler out = new StreamGobbler(p.getInputStream());
                out.start();
                int exitVal;
                try {
                    exitVal = p.waitFor();
                } catch (InterruptedException e) {
                    throw new IOException("Wait on process interrupted: "
                        + e.getMessage());
                }
                if (exitVal != 0) {
                    throw new IOException(getSummary(exitVal, err.getSink(), out
                        .getSink()));
                } else if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(getSummary(exitVal, err.getSink(), out
                        .getSink()));
                }
            } catch(IOException ioe) {
                // Clean up my tmp file.
                rsyncCopy.delete();
                // Rethrow.
                throw ioe;
            }
            
            // Return BufferedInputStream so 'delegation' is done for me, so
            // I don't have to implement all IS methods and pass to my
            // 'delegate' instance.
            return new BufferedInputStream(new FileInputStream(rsyncCopy)) {
                private File fileToRemoveOnClose = rsyncCopy;
                
                public void close() throws IOException {
                    super.close();
                    if (this.fileToRemoveOnClose != null &&
                            this.fileToRemoveOnClose.exists()) {
                        this.fileToRemoveOnClose.delete();
                        this.fileToRemoveOnClose = null;
                    }
                }
            };
        }
    }
    
    protected String getSummary(int exitValue, String err, String out) {
        return "Exit code: " + exitValue +
            ((err != null && err.length() > 0)? "\nSTDERR: " + err: "") +
            ((out != null && out.length() > 0)? "\nSTDOUT: " + out: "");
    }
    
    /**
     * See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
     */
    class StreamGobbler extends Thread {
        private final InputStream is;
        private final StringBuffer sink = new StringBuffer();

        StreamGobbler(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(this.is));
                for (String line = null; (line = br.readLine()) != null;) {
                    this.sink.append(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
        public String getSink() {
            return this.sink.toString();
        }
    }
    
    /**
     * Main dumps rsync file to STDOUT.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args)
    throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java java " +
                "-Djava.protocol.handler.pkgs=org.archive.net " +
                "org.archive.net.rsync.Handler RSYNC_URL");
            System.exit(1);
        }
        URL u = new URL(args[0]);
        URLConnection connect = u.openConnection();
        // Write download to stdout.
        final int bufferlength = 4096;
        byte [] buffer = new byte [bufferlength];
        InputStream is = connect.getInputStream();
        try {
            for (int count = is.read(buffer, 0, bufferlength);
                    (count = is.read(buffer, 0, bufferlength)) != -1;) {
                System.out.write(buffer, 0, count);
            }
            System.out.flush();
        } finally {
            is.close();
        }
    }
}
