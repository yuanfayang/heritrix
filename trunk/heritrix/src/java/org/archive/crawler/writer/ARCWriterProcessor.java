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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCWriter;
import org.archive.io.arc.ARCWriterPool;
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
        implements CoreAttributeConstants, ARCConstants, CrawlStatusListener {
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
     * Key to use asking settings for arc path value.
     */
    public static final String ATTR_PATH ="path";

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
     * Max size we want ARC files to be (bytes).
     *
     * Default is ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE.  Note that ARC
     * files will usually be bigger than maxSize; they'll be maxSize + length
     * to next boundary.
     */
    private int arcMaxSize = DEFAULT_MAX_ARC_FILE_SIZE;

    /**
     * File prefix for ARCs.
     *
     * Default is ARCConstants.DEFAULT_ARC_FILE_PREFIX.
     */
    private String arcPrefix = DEFAULT_ARC_FILE_PREFIX;

    /**
     * File suffix for arcs.
     */
    private String arcSuffix = null;

    /**
     * Use compression flag.
     *
     * Default is ARCConstants.DEFAULT_COMPRESS.
     */
    private boolean useCompression = DEFAULT_COMPRESS;

    /**
     * Where to drop ARC files.
     */
    private File outputDir;

    /**
     * Maximum active elements in the ARCWriterPool.
     */
    private int poolMaximumActive = ARCWriterPool.DEFAULT_MAX_ACTIVE;

    /**
     * Maximum time to wait on pool elements.
     */
    private int poolMaximumWait = ARCWriterPool.DEFAULT_MAXIMUM_WAIT;

    /**
     * Reference to an ARCWriter.
     */
    transient private ARCWriterPool pool = null;

    /**
     * @param name
     */
    public ARCWriterProcessor(String name) {
        super(name, "ARCWriter processor");
        Type e = addElementToDefinition(
            new SimpleType(ATTR_COMPRESS, "Compress arc files",
                new Boolean(this.useCompression)));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_PREFIX, 
                "ARC file prefix\n" +
                "The text supplied here will be used as a prefix naming " +
                "ARC files.  For example if the prefix is 'IAH', then " +
                "ARC names will look like " +
                "IAH-20040808101010-0001-HOSTNAME.arc.gz " +
                "(The prefix will be separated from the date by a hyphen).",
                this.arcPrefix));
        e = addElementToDefinition(
            new SimpleType(ATTR_SUFFIX, "Suffix to tag onto arc files.\n" +
                "If value is '${HOSTNAME}', will use hostname for suffix.",
                "${HOSTNAME}.  If empty, no suffix will be added."));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_MAX_SIZE_BYTES, "Max size of arc file",
                new Integer(this.arcMaxSize)));
        e.setOverrideable(false);
        e = addElementToDefinition(
            new SimpleType(ATTR_PATH, "Where to store arc files", "arcs"));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_ACTIVE,
            "Maximum active ARC writers in pool",
            new Integer(this.poolMaximumActive)));
        e.setOverrideable(false);
        e = addElementToDefinition(new SimpleType(ATTR_POOL_MAX_WAIT,
            "Maximum time to wait on ARC writer pool element (milliseconds)",
            new Integer(this.poolMaximumWait)));
        e.setOverrideable(false);
    }

    public synchronized void initialTasks() {
        // Add this class to crawl state listeners
        getSettingsHandler().getOrder().getController().
            addCrawlStatusListener(this);

        // ReadConfiguration populates settings used creating ARCWriter.
        try {
            readConfiguration();
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        }

        try {
            setupPool();

        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }

    void setupPool() throws IOException {
		// Set up the pool of ARCWriters.
		this.pool =
                new ARCWriterPool(this.outputDir,
                    this.arcPrefix,
                    this.arcSuffix,
                    this.useCompression,
                    this.arcMaxSize,
                    getMetadata(),
                    this.poolMaximumActive,
                    this.poolMaximumWait);
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
    protected List getMetadata() {
        List settingsFiles = getSettingsHandler().getListOfAllFiles();
        if (settingsFiles ==  null || settingsFiles.size() <= 0) {
            // Early return.
            return null;
        }
        List metadata = null;
        final String XML_TAIL = ".xml";
        for (Iterator i = settingsFiles.iterator(); i.hasNext();) {
            String str = (String)i.next();
            if (str == null || str.length() <= 0) {
                continue;
            }
            if (str.length() <= XML_TAIL.length() ||
                !str.toLowerCase().endsWith(XML_TAIL)) {
                continue;
            }
            File f = new File(str);
            if (!f.exists() || !f.canRead()) {
                logger.severe("File " + str + " is does not exist or is" +
                " not readable.");
                continue;
            }
            if (metadata == null) {
                metadata = new ArrayList(settingsFiles.size());
            }
            metadata.add(getMetadataBody(f));
        }
        return metadata;
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
     *
     * @throws TransformerConfigurationException
     * @throws FileNotFoundException
     * @throws UnknownHostException
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

	protected void readConfiguration()
        throws AttributeNotFoundException, MBeanException, ReflectionException {

        // set up output directory
        setUseCompression(
            ((Boolean) getAttribute(ATTR_COMPRESS)).booleanValue());
        setArcPrefix((String) getAttribute(ATTR_PREFIX));
        setArcSuffix((String) getAttribute(ATTR_SUFFIX));
        setArcMaxSize(((Integer) getAttribute(ATTR_MAX_SIZE_BYTES)).intValue());
        setOutputDir((String) getAttribute(ATTR_PATH));
        setPoolMaximumActive(
            ((Integer)getAttribute(ATTR_POOL_MAX_ACTIVE)).intValue());
        setPoolMaximumWait(
            ((Integer)getAttribute(ATTR_POOL_MAX_WAIT)).intValue());
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

        // Find the write protocol and write this sucker
        String scheme = curi.getUURI().getScheme().toLowerCase();

        try {
            if (scheme.equals("dns")) {
                writeDns(curi);
            } else if (scheme.equals("http") || scheme.equals("https")) {
                writeHttp(curi);
            }
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "WriteARCRecord");
        }
    }

    protected void writeHttp(CrawlURI curi)
        throws IOException
    {
        if (curi.getFetchStatus() <= 0 && curi.isHttpTransaction())
        {
            // Error; do not write to ARC (for now)
            return;
        }

        int recordLength = (int)curi.getContentSize();
        if (recordLength == 0)
        {
            // Write nothing.
            return;
        }

        ARCWriter writer = this.pool.borrowARCWriter();
        try
        {
            writer.write(curi.getURIString(), curi.getContentType(),
               curi.getServer().getHost().getIP().getHostAddress(),
               curi.getAList().getLong(A_FETCH_BEGAN_TIME), recordLength,
               curi.getHttpRecorder().getRecordedInput().
                   getReplayInputStream());
        }
        finally
        {
            this.pool.returnARCWriter(writer);
        }
    }

    protected void writeDns(CrawlURI curi)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with a 14-digit date per RFC 2540
        long ts = curi.getAList().getLong(A_FETCH_BEGAN_TIME);
        byte[] fetchDate = ArchiveUtils.get14DigitDate(ts).getBytes();
        baos.write(fetchDate);
        // Don't forget the newline
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1;
        Record[] rrSet = (Record[]) curi.getAList().
            getObject(A_RRECORD_SET_LABEL);
        if (rrSet != null)
        {
            for (int i = 0; i < rrSet.length; i++)
            {
                byte[] record = rrSet[i].toString().getBytes();
                recordLength += record.length;
                baos.write(record);
                // Add the newline between records back in
                baos.write("\n".getBytes());
                recordLength += 1;
            }
        }

        ARCWriter writer = this.pool.borrowARCWriter();
        try
        {
            writer.write(curi.getURIString(), curi.getContentType(),
               curi.getServer().getHost().getIP().getHostAddress(),
               curi.getAList().getLong(A_FETCH_BEGAN_TIME), recordLength, baos);
        }
        finally
        {
            this.pool.returnARCWriter(writer);
        }

        // Save the calculated contentSize for logging purposes
        // TODO handle this need more sensibly
        curi.setContentSize(recordLength);
    }

    // Getters and setters
    public int getArcMaxSize() {
        return this.arcMaxSize;
    }

    public String getArcPrefix() {
        return this.arcPrefix;
    }

    public File getOutputDir() {
        return this.outputDir;
    }

    public void setArcMaxSize(int i) {
        this.arcMaxSize = i;
    }

    public void setArcPrefix(String buffer) {
        this.arcPrefix = buffer;
    }

    public void setArcSuffix(String suffix) {
        this.arcSuffix = suffix;
    }

    public void setUseCompression(boolean use) {
        this.useCompression = use;
    }

    public boolean useCompression() {
        return this.useCompression;
    }

    public void setOutputDir(String buffer) {
        this.outputDir = new File(buffer);
        if (!this.outputDir.isAbsolute()) {
            // OutputDir should be relative to "disk"
            this.outputDir = new File(getController().getDisk(), buffer);
        }

        if (!this.outputDir.exists()) {
            try {
                this.outputDir.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return Returns the poolMaximumActive.
     */
    public int getPoolMaximumActive()
    {
        return this.poolMaximumActive;
    }

    /**
     * @param poolMaximumActive The poolMaximumActive to set.
     */
    public void setPoolMaximumActive(int poolMaximumActive)
    {
        this.poolMaximumActive = poolMaximumActive;
    }

    /**
     * @return Returns the poolMaximumWait.
     */
    public int getPoolMaximumWait()
    {
        return this.poolMaximumWait;
    }

    /**
     * @param poolMaximumWait The poolMaximumWait to set.
     */
    public void setPoolMaximumWait(int poolMaximumWait)
    {
        this.poolMaximumWait = poolMaximumWait;
    }

	public void crawlEnding(String sExitMessage) {
		// sExitMessage is unused.
		ARCWriter.resetSerialNo();
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
	 */
	public void crawlEnded(String sExitMessage) {
        // sExitMessage is unused.
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
        // sExitMessage is unused.
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
        // sExitMessage is unused.
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
        // sExitMessage is unused.
	}
	
    // custom serialization
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;
        coistream.registerFinishTask( new Runnable() {
            public void run() {
                try {
                    setupPool();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
