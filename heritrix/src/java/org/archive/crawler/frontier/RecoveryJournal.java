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
import org.archive.crawler.framework.URIFrontier;

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
public class RecoveryJournal {
    protected final static String F_ADD = "F+ ";

    protected final static String F_EMIT = "Fe ";

    protected final static String F_RESCHEDULE = "Fr ";

    protected final static String F_SUCCESS = "Fs ";

    protected final static String F_FAILURE = "Ff ";

    OutputStreamWriter out;

    /**
     * Create a new recovery journal at the given location
     * 
     * @param path
     * @param filename
     * @throws IOException
     */
    public RecoveryJournal(String path, String filename) throws IOException {
        out = new OutputStreamWriter(new GZIPOutputStream(
                new BufferedOutputStream(new FileOutputStream(new File(path,
                        filename + ".gz")))));
    }

    /**
     * Note that a CrawlURI was added (scheduled) to the Frontier
     * 
     * @param curi
     */
    public void added(CrawlURI curi) {
        write("\n" + F_ADD + curi.getURIString() + " " + curi.flattenVia()
                + " " + curi.getPathFromSeed());
    }

    /**
     * Note that a CrawlURI was finished, successfully
     * 
     * @param curi
     */
    public void finishedSuccess(CrawlURI curi) {
        write("\n" + F_SUCCESS + curi.getURIString());
    }

    /**
     * Note that a CrawlURI was emitted for processing. (If not followed
     * by a finished or rescheduled notation in the journal, the CrawlURI
     * was still in-process when the journal ended.)
     * 
     * @param curi
     */
    public void emitted(CrawlURI curi) {
        write("\n" + F_EMIT + curi.getURIString());

    }

    /**
     * Note that a CrawlURI was finished, unsuccessfully. 
     * @param curi
     */
    public void finishedFailure(CrawlURI curi) {
        write("\n" + F_FAILURE + curi.getURIString());
    }

    /**
     * Note that a CrawlURI was returned to the Frontier for 
     * another try.
     * 
     * @param curi
     */
    public void rescheduled(CrawlURI curi) {
        write("\n" + F_RESCHEDULE + curi.getURIString());
    }

    private void write(String string) {
        try {
            out.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method for scanning a recovery journal and applying it to
     * a Frontier. 
     * 
     * @see org.archive.crawler.framework.URIFrontier#importRecoverLog(java.lang.String)
     */
    public static void importRecoverLog(String pathToLog, URIFrontier frontier)
            throws IOException {
        // scan log for all 'Fs' lines: add as 'alreadyIncluded'
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(pathToLog))));
        String read;
        try {
            while ((read = reader.readLine()) != null) {
                if (read.startsWith(F_SUCCESS)) {
                    UURI u;
                    String args[] = read.split("\\s+");
                    try {
                        u = UURIFactory.getInstance(args[1]);
                        frontier.considerIncluded(u);
                    } catch (URIException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (EOFException e1) {
            // no problem; end of recovery journal
        }
        reader.close();
        // scan log for all 'F+' lines: if not alreadyIncluded, schedule for
        // visitation
        reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                new FileInputStream(pathToLog))));
        try {
            while ((read = reader.readLine()) != null) {
                if (read.startsWith(F_ADD)) {
                    UURI u;
                    String args[] = read.split("\\s+");
                    try {
                        u = UURIFactory.getInstance(args[1]);
                        CandidateURI caUri = new CandidateURI(u);
                        if (args.length > 2) {
                            caUri.setVia(args[2]);
                        } else {
                            caUri.setVia(pathToLog);
                        }
                        if (args.length > 3) {
                            caUri.setPathFromSeed(args[3]);
                        } else {
                            // filler
                            caUri.setPathFromSeed("L");
                        }
                        frontier.schedule(caUri);
                    } catch (URIException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (EOFException e) {
            // no problem: untidy end of recovery journal
        }
        reader.close();

    }

    /**
     *  Flush and close the underlying IO objects.
     */
    public void close() {
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}