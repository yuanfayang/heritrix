/* RecoveryJournal
 *
 * $Id$
 *
 * Created on Jul 20, 2004
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
package org.archive.crawler.frontier;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.framework.Frontier;

/**
 * Helper class for managing a simple Frontier change-events journal which is
 * useful for recovering from crawl problems.
 * 
 * By replaying the journal into a new Frontier, its state (at least with
 * respect to URIs alreadyIncluded and in pending queues) will match that of the
 * original Frontier, allowing a pseudo-resume of a previous crawl, at least as
 * far as URI visitation/coverage is concerned.
 * 
 * @author gojomo
 */
public class RecoveryJournal
implements FrontierJournal {
    protected final static String F_ADD = "F+ ";
    protected final static String F_EMIT = "Fe ";
    protected final static String F_RESCHEDULE = "Fr ";
    protected final static String F_SUCCESS = "Fs ";
    protected final static String F_FAILURE = "Ff ";

    /**
     * Stream on which we record frontier events.
     */
    private OutputStreamWriter out = null;


    private static final String GZIP_SUFFIX = ".gz";

    
    /**
     * Create a new recovery journal at the given location
     * 
     * @param path
     * @param filename
     * @throws IOException
     */
    public RecoveryJournal(String path, String filename)
    throws IOException {
        this.out = new OutputStreamWriter(new GZIPOutputStream(
            new BufferedOutputStream(new FileOutputStream(new File(path,
                filename + GZIP_SUFFIX)))));
    }

    public synchronized void added(CrawlURI curi) {
        write("\n" + F_ADD + curi.getURIString() + " " 
            + curi.getPathFromSeed() + " " + curi.flattenVia());
    }

    public void finishedSuccess(CrawlURI curi) {
        finishedSuccess(curi.getURIString());
    }
    
    public void finishedSuccess(UURI uuri) {
        finishedSuccess(uuri.toString());
    }
    
    protected void finishedSuccess(String uuri) {
        write("\n" + F_SUCCESS + uuri);
    }

    public void emitted(CrawlURI curi) {
        write("\n" + F_EMIT + curi.getURIString());

    }

    public void finishedFailure(CrawlURI curi) {
        write("\n" + F_FAILURE + curi.getURIString());
    }

    public void rescheduled(CrawlURI curi) {
        write("\n" + F_RESCHEDULE + curi.getURIString());
    }

    private void write(String string) {
        try {
            this.out.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method for scanning a recovery journal and applying it to
     * a Frontier.
     * 
     * @param source Recover log path.
     * @param frontier Frontier reference.
     * 
     * @see org.archive.crawler.framework.Frontier#importRecoverLog(java.lang.String)
     */
    public static void importRecoverLog(File source, Frontier frontier)
            throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("Passed source file is null.");
        }
        // Scan log for all 'Fs' lines: add as 'alreadyIncluded'
        BufferedReader reader = getBufferedReader(source);
        String read;
        try {
            while ((read = reader.readLine()) != null) {
                if (read.startsWith(F_SUCCESS)) {
                    String args[] = read.split("\\s+");
                    try {
                        UURI u = UURIFactory.getInstance(args[1]);
                        frontier.considerIncluded(u);
                        frontier.getFrontierJournal().finishedSuccess(u);
                    } catch (URIException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (EOFException e1) {
            // no problem; end of recovery journal
        } finally {
            reader.close();
        }
        
        // Scan log for all 'F+' lines: if not alreadyIncluded, schedule for
        // visitation
        reader = getBufferedReader(source);
        try {
            while ((read = reader.readLine()) != null) {
                if (read.startsWith(F_ADD)) {
                    UURI u;
                    String args[] = read.split("\\s+");
                    try {
                        u = UURIFactory.getInstance(args[1]);
                        CandidateURI caUri = new CandidateURI(u);
                        if (args.length > 2) {
                            caUri.setPathFromSeed(args[2]);
                        } else {
                            caUri.setPathFromSeed("");
                        }
                        if (args.length > 3) {
                            caUri.setVia(args[3]);
                        } else {
                            // filler
                            caUri.setVia(source.getPath());
                        }
                        frontier.schedule(caUri);
                    } catch (URIException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (EOFException e) {
            // no problem: untidy end of recovery journal
        } finally {
        	    reader.close(); 
        }
    }
    
    /**
     * @return Recover log buffered reader.
     * @throws IOException
     */
    protected static BufferedReader getBufferedReader(File source)
    throws IOException {
        boolean isGzipped = source.getName().toLowerCase().
            endsWith(GZIP_SUFFIX);
        // Scan log for all 'Fs' lines: add as 'alreadyIncluded'
        FileInputStream fis = new FileInputStream(source);
        return new BufferedReader(isGzipped?
            new InputStreamReader(new GZIPInputStream(fis)):
            new InputStreamReader(fis));   
    }

    /**
     *  Flush and close the underlying IO objects.
     */
    public void close() {
        if (this.out == null) {
            return;
        }
        try {
            this.out.flush();
            this.out.close();
            this.out = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
