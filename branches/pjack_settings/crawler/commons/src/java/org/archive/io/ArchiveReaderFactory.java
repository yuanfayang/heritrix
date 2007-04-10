/* $Id$
 *
 * Created on August 18th, 2006
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.net.UURI;
import org.archive.net.md5.Md5URLConnection;
import org.archive.net.rsync.RsyncURLConnection;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;


/**
 * Factory that returns an Archive file Reader.
 * Returns Readers for ARCs or WARCs.
 * @author stack
 * @version $Date$ $Revision$
 */
public class ArchiveReaderFactory implements ArchiveFileConstants {
	private static final ArchiveReaderFactory factory =
		new ArchiveReaderFactory();
	
    /**
     * Shutdown any public access to default constructor.
     */
    protected ArchiveReaderFactory() {
        super();
    }
    
    /**
     * Get an Archive file Reader on passed path or url.
     * Does primitive heuristic figuring if path or URL.
     * @param arcFileOrUrl File path or URL pointing at an Archive file.
     * @return An Archive file Reader.
     * @throws IOException 
     * @throws MalformedURLException 
     * @throws IOException 
     */
    public static ArchiveReader get(final String arcFileOrUrl)
    throws MalformedURLException, IOException {
    	return ArchiveReaderFactory.factory.getArchiveReader(arcFileOrUrl);
    }
    
    protected ArchiveReader getArchiveReader(final String arcFileOrUrl)
    throws MalformedURLException, IOException {
    	return UURI.hasScheme(arcFileOrUrl)?
    		get(new URL(arcFileOrUrl)): get(new File(arcFileOrUrl));
    }
    
    /**
     * @param f An Archive file to read.
     * @return An ArchiveReader
     * @throws IOException 
     */
    public static ArchiveReader get(final File f) throws IOException {
    	return ArchiveReaderFactory.factory.getArchiveReader(f);
    }
    
    protected ArchiveReader getArchiveReader(final File f)
    throws IOException {
    	return getArchiveReader(f, 0);
    }
    
    /**
     * @param f An Archive file to read.
     * @param offset Have returned Reader set to start reading at this offset.
     * @return An ArchiveReader
     * @throws IOException 
     */
    public static ArchiveReader get(final File f, final long offset)
    throws IOException {
    	return ArchiveReaderFactory.factory.getArchiveReader(f, 0);
	}
    
    protected ArchiveReader getArchiveReader(final File f,
    	final long offset)
    throws IOException {
    	if (ARCReaderFactory.isARCSuffix(f.getName())) {
    		return ARCReaderFactory.get(f, true, offset);
    	} else if (WARCReaderFactory.isWARCSuffix(f.getName())) {
    		return WARCReaderFactory.get(f, offset);
    	}
    	throw new IOException("Unknown file extension (Not ARC nor WARC): "
    		+ f.getName());
    }
    
    /**
     * Wrap a Reader around passed Stream.
     * @param s Identifying String for this Stream used in error messages.
     * @param is Stream.
     * @param atFirstRecord Are we at first Record?
     * @return ArchiveReader.
     * @throws IOException
     */
    public static ArchiveReader get(final String s, final InputStream is,
        final boolean atFirstRecord)
    throws IOException {
        return ArchiveReaderFactory.factory.getArchiveReader(s, is,
        	atFirstRecord);
    }
    
    protected ArchiveReader getArchiveReader(final String id, 
    		final InputStream is, final boolean atFirstRecord)
    throws IOException {
    	// Default to compressed WARC.
    	return WARCReaderFactory.get(id, is, atFirstRecord);
    }
    
    /**
     * Get an Archive Reader aligned at <code>offset</code>.
     * This version of get will not bring the file local but will try to
     * stream across the net making an HTTP 1.1 Range request on remote
     * http server (RFC1435 Section 14.35).
     * @param u HTTP URL for an Archive file.
     * @param offset Offset into file at which to start fetching.
     * @return An ArchiveReader aligned at offset.
     * @throws IOException
     */
    public static ArchiveReader get(final URL u, final long offset)
    throws IOException {
    	return ArchiveReaderFactory.factory.getArchiveReader(u, offset);
    }
    
    protected ArchiveReader getArchiveReader(final URL f, final long offset)
    throws IOException {
        // Get URL connection.
        URLConnection connection = f.openConnection();
        if (!(connection instanceof HttpURLConnection)) {
            throw new IOException("This method only handles HTTP connections.");
        }
        addUserAgent((HttpURLConnection)connection);
        // Use a Range request (Assumes HTTP 1.1 on other end). If length <= 0,
        // add open-ended range header to the request.  Else, because end-byte
        // is inclusive, subtract 1.
        connection.addRequestProperty("Range", "bytes=" + offset + "-");
        // TODO: Get feedback on this ArchiveReader maker. If fetching single
        // record remotely, might make sense to do a slimmed down
        // ArchiveReader getter.
        // Good if size 2 * inflator buffer to avoid buffer boundaries
        // (TODO: Implement better handling across buffer boundaries).
        return getArchiveReader(f.toString(),
            new RepositionableInputStream(connection.getInputStream(),
                16 * 1024),
            (offset == 0));
    }
    
    /**
     * Get an ARCReader.
     * Pulls the ARC local into whereever the System Property
     * <code>java.io.tmpdir</code> points. It then hands back an ARCReader that
     * points at this local copy.  A close on this ARCReader instance will
     * remove the local copy.
     * @param u An URL that points at an ARC.
     * @return An ARCReader.
     * @throws IOException 
     */
    public static ArchiveReader get(final URL u)
    throws IOException {
    	return ArchiveReaderFactory.factory.getArchiveReader(u);
    }
    
    protected ArchiveReader getArchiveReader(final URL u)
    throws IOException {
        // If url represents a local file then return file it points to.
        if (u.getPath() != null) {
            // TODO: Add scheme check and host check.
            File f = new File(u.getPath());
            if (f.exists()) {
                return get(f, 0);
            }
        }
        
        return makeARCLocal(u.openConnection());
    }
    
    protected ArchiveReader makeARCLocal(final URLConnection connection)
    throws IOException {
        File localFile = null;
        if (connection instanceof HttpURLConnection) {
            // If http url connection, bring down the resouce local.
            localFile = File.createTempFile(ArchiveReader.class.getName(),
            	".tmp", FileUtils.TMPDIR);
            connection.connect();
            addUserAgent((HttpURLConnection)connection);
            try {
                IoUtils.readFullyToFile(connection.getInputStream(), localFile,
                        new byte[16 * 1024]);
            } catch (IOException ioe) {
                localFile.delete();
                throw ioe;
            }
        } else if (connection instanceof RsyncURLConnection) {
            // Then, connect and this will create a local file.
            // See implementation of the rsync handler.
            connection.connect();
            localFile = ((RsyncURLConnection)connection).getFile();
        } else if (connection instanceof Md5URLConnection) {
            // Then, connect and this will create a local file.
            // See implementation of the md5 handler.
            connection.connect();
            localFile = ((Md5URLConnection)connection).getFile();
        } else {
            throw new UnsupportedOperationException("No support for " +
                connection);
        }
        
        ArchiveReader reader = null;
        try {
            reader = get(localFile, 0);
        } catch (IOException e) {
            localFile.delete();
            throw e;
        }
        
        // Assign to final variables so can assign in inner class.
        final ArchiveReader r = reader;
        final File f = localFile;
        
        // Return a delegate that does cleanup of downloaded file on close.
        return new ArchiveReader() {
            private final ArchiveReader delegate = r;
            private File archiveFile = f;
            
            public void close() throws IOException {
                this.delegate.close();
                if (this.archiveFile != null) {
                    if (archiveFile.exists()) {
                    	archiveFile.delete();
                    }
                    this.archiveFile = null;
                }
            }
            
            public ArchiveRecord get(long o) throws IOException {
                return this.delegate.get(o);
            }
            
            public boolean isDigest() {
                return this.delegate.isDigest();
            }
            
            public boolean isStrict() {
                return this.delegate.isStrict();
            }
            
            public Iterator<ArchiveRecord> iterator() {
                return this.delegate.iterator();
            }
            
            public void setDigest(boolean d) {
                this.delegate.setDigest(d);
            }
            
            public void setStrict(boolean s) {
                this.delegate.setStrict(s);
            }
            
            public List validate() throws IOException {
                return this.delegate.validate();
            }

			@Override
			public ArchiveRecord get() throws IOException {
				return this.delegate.get();
			}

			@Override
			public String getVersion() {
				return this.delegate.getVersion();
			}

			@Override
			public List validate(int noRecords) throws IOException {
				return this.delegate.validate(noRecords);
			}

			@Override
			protected ArchiveRecord createArchiveRecord(InputStream is,
					long offset)
			throws IOException {
				return this.delegate.createArchiveRecord(is, offset);
			}

			@Override
			protected void gotoEOR(ArchiveRecord record) throws IOException {
				this.delegate.gotoEOR(record);
			}

			@Override
			public void dump(boolean compress)
			throws IOException, ParseException {
				this.delegate.dump(compress);
			}

			@Override
			public String getDotFileExtension() {
				return this.delegate.getDotFileExtension();
			}

			@Override
			public String getFileExtension() {
				return this.delegate.getFileExtension();
			}
        };
    }
    
    protected void addUserAgent(final HttpURLConnection connection) {
        connection.addRequestProperty("User-Agent", this.getClass().getName());
    }
    
    /**
     * @param f File to test.
     * @return True if <code>f</code> is compressed.
     * @throws IOException
     */
    protected boolean isCompressed(final File f) throws IOException {
        return f.getName().toLowerCase().
        	endsWith(DOT_COMPRESSED_FILE_EXTENSION);
    }
}