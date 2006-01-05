/*
 * ARCWriter
 *
 * $Id$
 *
 * Created on Jun 5, 2003
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
package org.archive.crawler.writer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.fetcher.FetchSTREAM;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.ReplayInputStream;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCWriter;
import org.archive.io.arc.ARCWriterPool;
import org.archive.io.arc.ARCWriterSettings;
import org.archive.util.ArchiveUtils;
import org.xbill.DNS.Record;


/**
 * Processor module for writing the results of successful fetches (and
 * perhaps someday, certain kinds of network failures) to the Internet Archive
 * ARC file format.
 *
 * Assumption is that there is only one of these ARCWriterProcessors per
 * Heritrix instance.
 *
 * @author Parker Thompson
 */
public class ARCWriterProcessor extends Processor
implements CoreAttributeConstants, ARCConstants, CrawlStatusListener,
ARCWriterSettings, FetchStatusCodes {
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(ARCWriterProcessor.class.getName());

    /**
     * Key to use asking settings for compression value.
     */
    public static final String ATTR_COMPRESS = "compress";

    /**
     * Key to use asking settings for prefix value.
     */
    public static final String ATTR_PREFIX = "prefix";

    /**
     * Key to use asking settings for suffix value.
     */
    public static final String ATTR_SUFFIX = "suffix";

    /**
     * Key to use asking settings for max size value.
     */
    public static final String ATTR_MAX_SIZE_BYTES = "max-size-bytes";
    
    /**
     * Key for the maximum ARC bytes to write attribute.
     */
    public static final String ATTR_MAX_BYTES_WRITTEN =
        "total-bytes-to-write";

    /**
     * Key to use asking settings for arc path value.
     */
    public static final String ATTR_PATH = "path";

    /**
     * Key to get maximum pool size.
     *
     * This key is for maximum ARC writers active in the pool of ARC writers.
     */
    public static final String ATTR_POOL_MAX_ACTIVE = "pool-max-active";

    /**
     * Key to get maximum wait on pool object before we give up and
     * throw IOException.
     */
    public static final String ATTR_POOL_MAX_WAIT = "pool-max-wait";
    
    /**
     * Value to interpolate with actual hostname.
     */
    public static final String HOSTNAME_VARIABLE = "${HOSTNAME}";
    
    /**
     * Default for arc suffix.
     */
    private static final String DEFAULT_SUFFIX = HOSTNAME_VARIABLE;
    
    /**
     * Default path list.
     */
    private static final String [] DEFAULT_PATH = {"arcs"};

    /**
     * Calculate metadata once only.
     */
    transient private List cachedMetadata = null;

    /**
     * Reference to an ARCWriter.
     */
    transient private ARCWriterPool pool = null;
    
    /**
     * Total number of bytes written to disc.
     */
    private long totalBytesWritten = 0;
    
    /**
     * Name of file to keep state in when checkpointing.
     */
    private static final String STATE_FILENAME =
        ARCWriterProcessor.class.getName() + ".state";
    

    /**
     * @param name Name of this writer.
     */
    public ARCWriterProcessor(String name) {
        super(name, "ARCWriter processor");
        Type e = addElementToDefinition(
            new SimpleType(ATTR_COMPRESS, "Compress ARC files when writing" +
                    " to disk.",
                new Boolean(DEFAULT_COMPRESS)));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_PREFIX, 
                "ARC file prefix. " +
                "The text supplied here will be used as a prefix naming " +
                "ARC files.  For example if the prefix is 'IAH', then " +
                "ARC names will look like " +
                "IAH-20040808101010-0001-HOSTNAME.arc.gz " +
                "(The prefix will be separated from the date by a hyphen).",
                DEFAULT_ARC_FILE_PREFIX));
        e = addElementToDefinition(
            new SimpleType(ATTR_SUFFIX, "Suffix to tag onto ARC files. " +
                "If value is '${HOSTNAME}', will use hostname for suffix." +
                " If empty, no suffix will be added.",
                DEFAULT_SUFFIX));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_MAX_SIZE_BYTES, "Max size of each ARC file",
                new Integer(DEFAULT_MAX_ARC_FILE_SIZE)));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new StringList(ATTR_PATH, "Where to store ARC files. " +
                "Supply absolute or relative path.  If relative, ARCs will" +
                " will be written relative to the 'disk-path' setting." +
                " If more than one path specified, we'll round-robin" +
                " dropping files to each.  This setting is safe" +
                " to change midcrawl (You can remove and add new dirs" +
                " as the crawler progresses).", DEFAULT_PATH));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_ACTIVE,
            "Maximum active ARC writers in pool. " +
            "This setting cannot be varied over the life of a crawl.",
            new Integer(ARCWriterPool.DEFAULT_MAX_ACTIVE)));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_WAIT,
            "Maximum time to wait on ARC writer pool element" +
            " (milliseconds). This setting cannot be varied over the life" +
            " of a crawl.",
            new Integer(ARCWriterPool.DEFAULT_MAXIMUM_WAIT)));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_MAX_BYTES_WRITTEN,
            "Total ARC bytes to write to disk." +
            " Once the size of all ARCs on disk has exceeded this limit," +
            " this processor will stop the crawler. " +
            "A value of zero means no upper limit.", new Long(0)));
        e.setOverrideable(false);
        e.setExpertSetting(true);
    }

    public synchronized void initialTasks() {
        // Add this class to crawl state listeners
        getSettingsHandler().getOrder().getController().
            addCrawlStatusListener(this);
        setupPool();
        if (getSettingsHandler().getOrder().getController().
                isCheckpointRecover()) {
            // If in recover mode, read in the ARCWriter serial number saved
            // off when we checkpointed.
            File stateFile = new File(getSettingsHandler().getOrder().
                getController().getCheckpointRecover().getDirectory(),
                    STATE_FILENAME);
            if (!stateFile.exists()) {
                logger.info(stateFile.getAbsolutePath() +
                    " doesn't exist so cannot restore ARC serial number.");
            } else {
                DataInputStream dis = null;
                try {
                    dis = new DataInputStream(new FileInputStream(stateFile));
                    ARCWriter.setSerialNo(dis.readShort());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (dis != null) {
                            dis.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    void setupPool() {
		// Set up the pool of ARCWriters.
		this.pool = new ARCWriterPool(this, getPoolMaximumActive(),
            getPoolMaximumWait());
    }

    /**
     * Return list of metadatas to add to first arc file metadata record.
     *
     * Get xml files from settingshandle.  Currently order file is the
     * only xml file.  We're NOT adding seeds to meta data.
     *
     * @return List of strings and/or files to add to arc file as metadata or
     * null.
     */
    public List getMetadata() {
        if (this.cachedMetadata != null) {
            return this.cachedMetadata;
        }
        return cacheMetadata();
    }
    
    protected synchronized List cacheMetadata() {
        if (this.cachedMetadata != null) {
            return this.cachedMetadata;
        }
        
        List result = null;
        if (!XMLSettingsHandler.class.isInstance(getSettingsHandler())) {
            logger.warning("Expected xml settings handler (No arcmetadata).");
            // Early return
            return result;
        }
        
        XMLSettingsHandler xsh = (XMLSettingsHandler)getSettingsHandler();
        File orderFile = xsh.getOrderFile();
        if (!orderFile.exists() || !orderFile.canRead()) {
                logger.severe("File " + orderFile.getAbsolutePath() +
                    " is does not exist or is not readable.");
        } else {
                result = new ArrayList(1);
                result.add(getMetadataBody(orderFile));
        }
        this.cachedMetadata = result;
        return this.cachedMetadata;
    }

    /**
     * Write the arc metadata body content.
     *
     * Its based on the order xml file but into this base we'll add other info
     * such as machine ip.
     *
     * @param orderFile Order file.
     *
     * @return String that holds the arc metaheader body.
     */
    protected String getMetadataBody(File orderFile) {
        String result = null;
        TransformerFactory factory = TransformerFactory.newInstance();
        Templates templates = null;
        Transformer xformer = null;
        try {
            templates = factory.newTemplates(new StreamSource(
                this.getClass().getResourceAsStream("/arcMetaheaderBody.xsl")));
            xformer = templates.newTransformer();
            // Below parameter names must match what is in the stylesheet.
            xformer.setParameter("software", "Heritrix " +
                Heritrix.getVersion() + " http://crawler.archive.org");
            xformer.setParameter("ip",
                InetAddress.getLocalHost().getHostAddress());
            xformer.setParameter("hostname",
                InetAddress.getLocalHost().getHostName());
            StreamSource source = new StreamSource(
                new FileInputStream(orderFile));
            StringWriter writer = new StringWriter();
            StreamResult target = new StreamResult(writer);
            xformer.transform(source, target);
            result= writer.toString();
        } catch (TransformerConfigurationException e) {
            logger.severe("Failed transform " + e);
        } catch (FileNotFoundException e) {
            logger.severe("Failed transform, file not found " + e);
        } catch (UnknownHostException e) {
            logger.severe("Failed transform, unknown host " + e);
        } catch(TransformerException e) {
            SourceLocator locator = e.getLocator();
            int col = locator.getColumnNumber();
            int line = locator.getLineNumber();
            String publicId = locator.getPublicId();
            String systemId = locator.getSystemId();
            logger.severe("Transform error " + e + ", col " + col + ", line " +
                line + ", publicId " + publicId + ", systemId " + systemId);
        }

        return result;
    }

    /**
     * Takes a CrawlURI and generates an arc record, writing it to disk.
     *
     * Currently this method understands the following uri types: dns, http.
     *
     * @param curi CrawlURI to process.
     */
    protected void innerProcess(CrawlURI curi) {
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return;
        }

        String scheme = curi.getUURI().getScheme().toLowerCase();
        try {
            if (scheme.equals("dns") && curi.getFetchStatus() ==
                    S_DNS_SUCCESS) {
                writeDns(curi);
            } else if (scheme.equals("http") || scheme.equals("https")) {
                writeHttp(curi);
            } else if (FetchSTREAM.SCHEME.contains(scheme)) {
            	writeStream(curi);
            }
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "WriteARCRecord: " +
                curi.toString());
            logger.log(Level.SEVERE, "Failed write of ARC Record: " +
                curi.toString(), e);
        }
    }

    protected void writeStream(final CrawlURI curi)
    throws IOException {
    	if (curi.getFetchStatus() <= 0) {
    		return;
    	}
    	File streamFile = (File)curi.getObject(FetchSTREAM.FILE_KEY);
    	if (!streamFile.exists() || !streamFile.canRead()) {
    		throw new IOException(streamFile.toString() +
    		    " does not exist or is not readable.");
    	}
        long recordLength = (int)curi.getContentSize();
        if (recordLength == 0) {
        	if (logger.isLoggable(Level.FINE)) {
        		logger.fine(streamFile.toString() + " is empty -- skipping");
        	}
            // Write nothing.
            return;
        }
        if (recordLength >= Integer.MAX_VALUE) {
        	throw new IOException(streamFile.toString() +
        	    " is too long.");
        	// HEY CLEAN IT UP....
        }
        
        write(curi, (int)recordLength,
            new BufferedInputStream(new FileInputStream(streamFile)),
        		getHostAddress(curi));
	}

	protected void writeHttp(CrawlURI curi)
    throws IOException {
        if (curi.getFetchStatus() <= 0 && curi.isHttpTransaction()) {
            // Error; do not write to ARC (for now)
            return;
        }

        int recordLength = (int)curi.getContentSize();
        if (recordLength == 0) {
            // Write nothing.
            return;
        }
        
        write(curi, recordLength,
            curi.getHttpRecorder().getRecordedInput().
                   getReplayInputStream(), getHostAddress(curi));
    }

    protected void writeDns(CrawlURI curi)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with a 14-digit date per RFC 2540
        long ts = curi.getLong(A_FETCH_BEGAN_TIME);
        byte[] fetchDate = ArchiveUtils.get14DigitDate(ts).getBytes();
        baos.write(fetchDate);
        // Don't forget the newline
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1;
        Record[] rrSet = (Record[]) curi.getObject(A_RRECORD_SET_LABEL);
        if (rrSet != null) {
            for (int i = 0; i < rrSet.length; i++) {
                byte[] record = rrSet[i].toString().getBytes();
                recordLength += record.length;
                baos.write(record);
                // Add the newline between records back in
                baos.write("\n".getBytes());
                recordLength += 1;
            }
        }
        
        write(curi, recordLength,
            new ByteArrayInputStream(baos.toByteArray()),
            curi.getString(A_DNS_SERVER_IP_LABEL));

        // Save the calculated contentSize for logging purposes
        // TODO handle this need more sensibly.  The length setting
        // should be done back in the DNSFetcher.
        curi.setContentSize(recordLength);
    }
    
    protected void write(CrawlURI curi, int recordLength, InputStream in,
        String ip)
    throws IOException {
        ARCWriter writer = this.pool.borrowARCWriter();
        long position = writer.getPosition();
        // See if we need to open a new ARC because we've exceeed maxBytes
        // per ARC.
        writer.checkARCFileSize();
        if (writer.getPosition() != position) {
            // We just closed the ARC because it was larger than maxBytes.
            // Add to the totalBytesWritten the size of the first record
            // in the ARC.
            this.totalBytesWritten += (writer.getPosition() - position);
            position = writer.getPosition();
        }
        if (writer == null) {
            throw new IOException("Writer is null");
        }
        
        try {
            if (in instanceof ReplayInputStream) {
                writer.write(curi.toString(), curi.getContentType(),
                    ip, curi.getLong(A_FETCH_BEGAN_TIME),
                    recordLength, (ReplayInputStream)in);
            } else {
                writer.write(curi.toString(), curi.getContentType(),
                    ip, curi.getLong(A_FETCH_BEGAN_TIME),
                    recordLength, in);
            }
        } catch (IOException e) {
            // Invalidate this arc file (It gets a '.invalid' suffix).
            this.pool.invalidateARCWriter(writer);
            // Set the writer to null otherwise the pool accounting
            // of how many active writers gets skewed if we subsequently
            // do a returnARCWriter call on this object in the finally block.
            writer = null;
            throw e;
        } finally {
            this.totalBytesWritten += (writer.getPosition() - position);
            if (writer != null) {
                this.pool.returnARCWriter(writer);
            }
        }
        checkBytesWritten();
    }
    
    protected void checkBytesWritten() {
        long max = getMaxToWrite();
        if (max <= 0) {
            return;
        }
        if (max <= this.totalBytesWritten) {
            getController().requestCrawlStop("Finished - Maximum bytes (" +
                Long.toString(max) + ") written");
        }
    }
    
    private String getHostAddress(CrawlURI curi) {
        CrawlHost h = getController().getServerCache().getHostFor(curi);
        if (h == null) {
            throw new NullPointerException("Crawlhost is null for " +
                curi + " " + curi.getVia());
        }
        InetAddress a = h.getIP();
        if (a == null) {
            throw new NullPointerException("Address is null for " +
                curi + " " + curi.getVia() + ". Address " +
                ((h.getIpFetched() == CrawlHost.IP_NEVER_LOOKED_UP)?
                     "was never looked up.":
                     (System.currentTimeMillis() - h.getIpFetched()) +
                         " ms ago."));
        }
        return h.getIP().getHostAddress();
    }
    
    /**
     * Version of getAttributes that catches and logs exceptions
     * and returns null if failure to fetch the attribute.
     * @param name Attribute name.
     * @return Attribute or null.
     */
    public Object getAttributeUnchecked(String name) {
        Object result = null;
        try {
            result = super.getAttribute(name);
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        }
        return result;
    }

   /**
    * Max size we want ARC files to be (bytes).
    *
    * Default is ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE.  Note that ARC
    * files will usually be bigger than maxSize; they'll be maxSize + length
    * to next boundary.
    * @return ARC maximum size.
    */
    public int getArcMaxSize() {
        Object obj = getAttributeUnchecked(ATTR_MAX_SIZE_BYTES);
        return (obj == null)? DEFAULT_MAX_ARC_FILE_SIZE:
            ((Integer)obj).intValue();
    }

    public String getArcPrefix() {
        Object obj = getAttributeUnchecked(ATTR_PREFIX);
        return (obj == null)? DEFAULT_ARC_FILE_PREFIX: (String)obj;
    }

    public List getOutputDirs() {
        Object obj = getAttributeUnchecked(ATTR_PATH);
        List list = (obj == null)? Arrays.asList(DEFAULT_PATH): (StringList)obj;
        ArrayList results = new ArrayList();
        for (Iterator i = list.iterator(); i.hasNext();) {
            String path = (String)i.next();
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(getController().getDisk(), path);
            }
            if (!f.exists()) {
                try {
                    f.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            results.add(f);
        }
        return results;
    }
    
    public boolean isCompressed() {
        Object obj = getAttributeUnchecked(ATTR_COMPRESS);
        return (obj == null)? DEFAULT_COMPRESS:
            ((Boolean)obj).booleanValue();
    }

    /**
     * @return Returns the poolMaximumActive.
     */
    public int getPoolMaximumActive() {
        Object obj = getAttributeUnchecked(ATTR_POOL_MAX_ACTIVE);
        return (obj == null)? ARCWriterPool.DEFAULT_MAX_ACTIVE:
            ((Integer)obj).intValue();
    }

    /**
     * @return Returns the poolMaximumWait.
     */
    public int getPoolMaximumWait() {
        Object obj = getAttributeUnchecked(ATTR_POOL_MAX_WAIT);
        return (obj == null)? ARCWriterPool.DEFAULT_MAXIMUM_WAIT:
            ((Integer)obj).intValue();
    }

    public String getArcSuffix() {
        Object obj = getAttributeUnchecked(ATTR_SUFFIX);
        String sfx = (obj == null)? DEFAULT_SUFFIX: (String)obj;
        if (sfx != null && sfx.trim().equals(HOSTNAME_VARIABLE)) {
            String str = "localhost.localdomain";
            try {
                str = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ue) {
                logger.severe("Failed getHostAddress for this host: " + ue);
            }
            sfx = str;
        }
        return sfx;
    }
    
    public long getMaxToWrite() {
        Object obj = getAttributeUnchecked(ATTR_MAX_BYTES_WRITTEN);
        return (obj == null)? 0: ((Long)obj).longValue();
    }

	public void crawlEnding(String sExitMessage) {
		// sExitMessage is unused.
		ARCWriter.resetSerialNo();
        this.pool.close();
	}

	public void crawlEnded(String sExitMessage) {
        // sExitMessage is unused.
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }
    
    public void crawlCheckpoint(File checkpointDir)
    throws IOException {
        // Write out the current state of the ARCWriter serial number.
        File f = new File(checkpointDir, STATE_FILENAME);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
        try {
            dos.writeShort(ARCWriter.getSerialNo());
        } finally {
            dos.close();
        }
        // Close all ARCs on checkpoint.
        try {
            this.pool.close();
        } finally {
            // Reopen on checkpoint.
            setupPool();
        }
    }
    
	public void crawlPausing(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlPaused(String statusMessage) {
        // sExitMessage is unused.
	}

	public void crawlResuming(String statusMessage) {
        // sExitMessage is unused.
	}
	
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream coistream =
            (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new Runnable() {
            public void run() {
                setupPool();
            }
        });
    }
}
