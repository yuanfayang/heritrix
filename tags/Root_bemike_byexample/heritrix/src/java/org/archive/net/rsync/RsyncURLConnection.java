/* RsyncURLConnection.java
 *
 * $Id$
 *
 * Created Jul 19, 2005
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.ProcessUtils;

/**
 * Rsync URL connection.
 * @author stack
 * @version $Date$, $Revision$
 */
public class RsyncURLConnection extends URLConnection {
    private static final String CLASSNAME = RsyncURLConnection.class.getName();
    private final Logger LOGGER = Logger.getLogger(CLASSNAME);
    private static final File TMPDIR =
        new File(System.getProperty("java.io.tmpdir", "/tmp"));
    private static final String RSYNC =
        System.getProperty(CLASSNAME + ".path", "rsync");
    private static final String RSYNC_TIMEOUT =
        System.getProperty(CLASSNAME + ".timeout", "300");
    private File downloadFile = null;

    protected RsyncURLConnection(URL u) {
        super(u);
    }

    /**
     * Do rsync copy to local file.
     * File is available via {@link #getFile()}.
     * @throws IOException 
     */
    public void connect() throws IOException {
        if (this.connected) {
            return;
        }
        
        this.downloadFile = File.createTempFile(CLASSNAME, null, TMPDIR);
        try {
            String [] cmd = new String[] {RSYNC, "--timeout=" + RSYNC_TIMEOUT,
                this.url.toExternalForm(), this.downloadFile.getAbsolutePath()};
            if (LOGGER.isLoggable(Level.FINE)) {
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < cmd.length; i++) {
                    if (i > 0) {
                        buffer.append(" ");
                    }
                    buffer.append(cmd[i]);
                }
                LOGGER.fine("Rsyc command: " + buffer.toString());
            }
            ProcessUtils.exec(cmd);
            // Assume download went smoothly.
            this.connected = true;
        } catch (IOException ioe) {
            // Clean up my tmp file.
            this.downloadFile.delete();
            this.downloadFile = null;
            // Rethrow.
            throw ioe;
        }
    }
    
    public File getFile() {
        return this.downloadFile;
    }
    
    protected void setFile(final File f) {
        this.downloadFile = f;
    }

    public InputStream getInputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        
        // Return BufferedInputStream so 'delegation' is done for me, so
        // I don't have to implement all IS methods and pass to my
        // 'delegate' instance.
        final RsyncURLConnection connection = this;
        return new BufferedInputStream(new FileInputStream(this.downloadFile)) {
            private RsyncURLConnection ruc = connection;

            public void close() throws IOException {
                super.close();
                if (this.ruc != null && this.ruc.getFile()!= null &&
                    this.ruc.getFile().exists()) {
                    this.ruc.getFile().delete();
                    this.ruc.setFile(null);
                }
            }
        };
    }
}
