/* FetchFTP.java
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
package org.archive.crawler.fetcher;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.net.ftp.FTPCommand;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.io.RecordingInputStream;
import org.archive.io.ReplayCharSequence;
import org.archive.net.ClientFTP;
import org.archive.net.FTPException;
import org.archive.net.UURI;
import org.archive.util.ArchiveUtils;
import org.archive.util.HttpRecorder;


/**
 * Fetches documents and directory listings using FTP.
 * 
 * @author pjack
 *
 */
public class FetchFTP extends Processor implements CoreAttributeConstants {

    
    /** Serialization ID; robust against trivial API changes. */
    private static final long serialVersionUID =
     ArchiveUtils.classnameBasedUID(FetchFTP.class,1);

    /** Logger for this class. */
    private static Logger logger = Logger.getLogger(FetchFTP.class.getName());

    /** Pattern for matching directory entries. */
    private static Pattern DIR = 
     Pattern.compile("(.+)$", Pattern.MULTILINE);
//    Pattern.compile("([^\\s]+)\\s*$", Pattern.MULTILINE);

    
    /** The name for the <code>username</code> attribute. */
    final public static String ATTR_USERNAME = "username";
   
    /** The description for the <code>username</code> attribute. */
    final private static String DESC_USERNAME = "The username to send to " +
     "FTP servers.  By convention, the default value of \"anonymous\" is " +
     "used for publicly available FTP sites.";
    
    /** The default value for the <code>username</code> attribute. */
    final private static String DEFAULT_USERNAME = "anonymous";


    /** The name for the <code>password</code> attribute. */
    final public static String ATTR_PASSWORD = "password";
   
    /** The description for the <code>password</code> attribute. */
    final private static String DESC_PASSWORD = "The password to send to " +
    "FTP servers.  By convention, anonymous users send their email address " +
    "in this field.";
    
    /** The default value for the <code>password</code> attribute. */
    final private static String DEFAULT_PASSWORD = "pjack@archive.org"; // FIXME

    
    /** The name for the <code>extract-from-dirs</code> attribute. */
    final private static String ATTR_EXTRACT = "extract-from-dirs";
    
    /** The description for the <code>extract-from-dirs</code> attribute. */
    final private static String DESC_EXTRACT = "Set to true to extract "
     + "further URIs from FTP directories.  Default is true.";
    
    /** The default value for the <code>extract-from-dirs</code> attribute. */
    final private static boolean DEFAULT_EXTRACT = true;


    /**
     * Constructs a new <code>FetchFTP</code>.
     * 
     * @param name  the name of this processor
     */
    public FetchFTP(String name) {
        super(name, "FTP Fetcher.");
        add(ATTR_USERNAME, DESC_USERNAME, DEFAULT_USERNAME);
        add(ATTR_PASSWORD, DESC_PASSWORD, DEFAULT_PASSWORD);
        add(ATTR_EXTRACT, DESC_EXTRACT, DEFAULT_EXTRACT);
    }

    
    /**
     * Convenience method for adding an attribute.
     * 
     * @param name   The name of the attribute
     * @param desc   The description of the attribute
     * @param def    The default value for the attribute
     */
    private void add(String name, String desc, Object def) {
        SimpleType st = new SimpleType(name, desc, def);
        addElementToDefinition(st);
    }
    
    
    /**
     * Convenience method for extracting an attribute.
     * If a value for the specified name cannot be found,
     * a warning is written to the log and the specified
     * default value is returned instead.
     * 
     * @param context  The context for the attribute fetch
     * @param name     The name of the attribute to fetch
     * @param def      The value to return if the attribute isn't found
     * @return         The value of that attribute
     */
    private Object get(Object context, String name, Object def) {
        try {
            return getAttribute(context, name);
        } catch (AttributeNotFoundException e) {
            logger.warning("Attribute not found (using default): " + name);
            return def;
        }
    }
    

    /**
     * Processes the given URI.  If the given URI is not an FTP URI, then
     * this method does nothing.  Otherwise an attempt is made to connect
     * to the FTP server.
     * 
     * <p>If the connection is successful, an attempt will be made to CD to 
     * the path specified in the URI.  If the remote CD command succeeds, 
     * then it is assumed that the URI represents a directory.  If the
     * CD command fails, then it is assumed that the URI represents
     * a file.
     * 
     * <p>For directories, the directory listing will be fetched using
     * the FTP LIST command, and saved to the HttpRecorder.  If the
     * <code>extract.from.dirs</code> attribute is set to true, then
     * the files in the fetched list will be added to the curi as
     * extracted FTP links.  (It was easier to do that here, rather
     * than writing a separate FTPExtractor.)
     * 
     * <p>For files, the file will be fetched using the FTP RETR
     * command, and saved to the HttpRecorder.
     * 
     * <p>All file transfers (including directory listings) occur using
     * Binary mode transfer.  Also, the local passive transfer mode
     * is always used, to play well with firewalls.
     * 
     * @param curi  the curi to process
     * @throws InterruptedException  if the thread is interrupted during
     *   processing
     */
    public void innerProcess(CrawlURI curi) throws InterruptedException {
        if (!curi.getUURI().getScheme().equals("ftp")) {
            return;
        }
        
        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());
        HttpRecorder recorder = HttpRecorder.getHttpRecorder();
        ClientFTP client = new ClientFTP();
        
        try {
            fetch(curi, client, recorder);
        } catch (FTPException e) {
            logger.log(Level.SEVERE, "FTP server reported problem.", e);
            curi.setFetchStatus(e.getReplyCode());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO Error during FTP fetch.", e);
            curi.setFetchStatus(FetchStatusCodes.S_CONNECT_LOST);
        } finally {
            disconnect(client);
            curi.setContentSize(recorder.getRecordedInput().getSize());
            curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
        }
    }


    /**
     * Fetches a document from an FTP server.
     * 
     * @param curi      the URI of the document to fetch
     * @param client    the FTPClient to use for the fetch
     * @param recorder  the recorder to preserve the document in
     * @throws IOException  if a network or protocol error occurs
     * @throws InterruptedException  if the thread is interrupted
     */
    private void fetch(CrawlURI curi, ClientFTP client, HttpRecorder recorder) 
    throws IOException, InterruptedException {
        // Connect to the FTP server.
        UURI uuri = curi.getUURI();
        int port = uuri.getPort();
        if (port == -1) {
            port = 21;
        }
        client.connectStrict(uuri.getHost(), port);
        
        // Authenticate.
        String username = (String)get(curi, ATTR_USERNAME, DEFAULT_USERNAME);
        String password = (String)get(curi, ATTR_PASSWORD, DEFAULT_PASSWORD);
        client.loginStrict(username, password);
        
        // The given resource may or may not be a directory.
        // To figure out which is which, execute a CD command to
        // the UURI's path.  If CD works, it's a directory.
        boolean dir = client.changeWorkingDirectory(uuri.getPath());
        if (dir) {
            curi.setContentType("text/plain");
        }
        
        // TODO: A future version of this class could use the system string to
        // set up custom directory parsing if the FTP server doesn't support 
        // the nlist command.
        if (logger.isLoggable(Level.FINE)) {
            String system = client.getSystemName();
            logger.fine(system);
        }
        
        // Get a data socket.  This will either be the result of a NLIST
        // command for a directory, or a RETR command for a file.
        int command = dir ? FTPCommand.NLST : FTPCommand.RETR;
        String path = dir ? "." : uuri.getPath();
        client.enterLocalPassiveMode();
        client.setBinary();
        Socket socket = client.openDataConnection(command, path);
        curi.setFetchStatus(client.getReplyCode());

        // Save the streams in the CURI, where downstream processors
        // expect to find them.
        curi.setHttpRecorder(recorder);
        try {
            saveToRecorder(socket, recorder);
        } finally {
            recorder.close();
            close(socket);
        }

        curi.setFetchStatus(200);
        if (dir) {
            extract(curi, recorder);
        }
    }
    
    
    /**
     * Saves the given socket to the given recorder.
     * 
     * @param socket    the socket whose streams to save
     * @param recorder  the recorder to save them to
     * @throws IOException  if a network or file error occurs
     * @throws InterruptedException  if the thread is interrupted
     */
    private void saveToRecorder(Socket socket, HttpRecorder recorder) 
    throws IOException, InterruptedException {
        recorder.inputWrap(socket.getInputStream());
        recorder.outputWrap(socket.getOutputStream());

        // Read the remote file/dir listing in its entirety.
        // FIXME: Make these attributes!  
        // FetchHTTP and FetchFTP should inherit a common superclass.
        long softMax = 0;
        long hardMax = 0;
        long timeout = 0;
        int maxRate = 0;
        RecordingInputStream input = recorder.getRecordedInput();
        input.readFullyOrUntil(softMax, hardMax, timeout, maxRate);
     }
    
    
    /**
     * Extract FTP links in a directory listing.
     * The listing must already be saved to the given recorder.
     * 
     * @param curi      The curi to save extracted links to
     * @param recorder  The recorder containing the directory listing
     */
    private void extract(CrawlURI curi, HttpRecorder recorder) {
        if (!getExtractFromDirs(curi)) {
            return;
        }
        
        ReplayCharSequence seq = null;
        try {
            seq = recorder.getReplayCharSequence();
            extract(curi, seq);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error during extraction.", e);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "IO error during extraction.", e);
        } finally {
            close(seq);
        }
    }
    
    
    /**
     * Extracts FTP links in a directory listing.
     * 
     * @param curi  The curi to save extracted links to
     * @param dir   The directory listing to extract links from
     * @throws URIException  if an extracted link is invalid
     */
    private void extract(CrawlURI curi, ReplayCharSequence dir) {
        logger.log(Level.FINEST, "Extracting URIs from FTP directory.");
        Matcher matcher = DIR.matcher(dir);
        while (matcher.find()) {
            String file = matcher.group(1);
            addExtracted(curi, file);
        }
    }


    /**
     * Adds an extracted filename to the curi.  A new URI will be formed
     * by taking the given curi (which should represent the directory the
     * file lives in) and appending the file.
     * 
     * @param curi  the curi to store the discovered link in
     * @param file  the filename of the discovered link
     */
    private void addExtracted(CrawlURI curi, String file) {
        try {
            file = URLEncoder.encode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Found " + file);
        }
        String base = curi.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        try {
            String uri = curi.toString() + "/" + file;
            curi.createAndAddLink(uri, file, Link.NAVLINK_HOP);            
        } catch (URIException e) {
            logger.log(Level.WARNING, "URI error during extraction.", e);            
        }
    }
    
    
    /**
     * Returns the <code>extract.from.dirs</code> attribute for this
     * <code>FetchFTP</code> and the given curi.
     * 
     * @param curi  the curi whose attribute to return
     * @return  that curi's <code>extract.from.dirs</code>
     */
    public boolean getExtractFromDirs(CrawlURI curi) {
        return (Boolean)get(curi, ATTR_EXTRACT, DEFAULT_EXTRACT);
    }


    /**
     * Quietly closes the given socket.
     * 
     * @param socket  the socket to close
     */
    private static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO error closing socket.", e);
        }
    }


    /**
     * Quietly closes the given sequence.
     * If an IOException is raised, this method logs it as a warning.
     * 
     * @param seq  the sequence to close
     */
    private static void close(ReplayCharSequence seq) {
        if (seq == null) {
            return;
        }
        try {
            seq.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO error closing ReplayCharSequence.", e);
        }
    }

    
    /**
     * Quietly disconnects from the given FTP client.
     * If an IOException is raised, this method logs it as a warning.
     * 
     * @param client  the client to disconnect
     */
    private static void disconnect(ClientFTP client) {
        if (client.isConnected()) try {
            client.disconnect();
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Could not disconnect from FTP client: " + e.getMessage());
            }
        }        
    }


}
