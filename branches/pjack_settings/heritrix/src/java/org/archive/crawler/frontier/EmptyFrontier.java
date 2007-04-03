package org.archive.crawler.frontier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.net.UURI;


/**
 * A frontier that is always empty.
 * 
 * @author pjack
 */
public class EmptyFrontier implements Frontier, Serializable {

    private static final long serialVersionUID = 1L;

    
    public long averageDepth() {
        return 0;
    }

    public float congestionRatio() {
        return 0;
    }

    public void considerIncluded(UURI u) {
    }

    public long deepestUri() {
        return 0;
    }

    public long deleteURIs(String match) {
        return 0;
    }

    public void deleted(CrawlURI curi) {
    }

    public long discoveredUriCount() {
        return 0;
    }

    public long disregardedUriCount() {
        return 0;
    }

    public long failedFetchCount() {
        return 0;
    }

    public void finished(CrawlURI cURI) {
    }

    public long finishedUriCount() {
        return 0;
    }

    public String getClassKey(CrawlURI cauri) {
        return null;
    }

    public FrontierJournal getFrontierJournal() {
        return null;
    }

    public FrontierGroup getGroup(CrawlURI curi) {
        return null;
    }

    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        return null;
    }

    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        return null;
    }

    public void importRecoverLog(String pathToLog, boolean retainFailures)
            throws IOException {
    }

    public void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
    }

    public boolean isEmpty() {
        return false;
    }

    public void kickUpdate() {
    }

    public void loadSeeds() {
    }

    public CrawlURI next() throws InterruptedException, EndedException {
        return null;
    }

    public void pause() {
    }

    public long queuedUriCount() {
        return 0;
    }

    public void schedule(CrawlURI caURI) {
    }

    public void start() {
    }

    public long succeededFetchCount() {
        return 0;
    }

    public void terminate() {
    }

    public long totalBytesWritten() {
        return 0;
    }

    public void unpause() {
    }

    public String[] getReports() {
        return new String[0];
    }

    public void reportTo(String name, PrintWriter writer) {
    }

    public void reportTo(PrintWriter writer) throws IOException {
        writer.println("EmptyFrontier");
    }

    public String singleLineLegend() {
        return "EmptyFrontier";
    }

    public String singleLineReport() {
        return "EmptyFontier";
    }

    public void singleLineReportTo(PrintWriter writer) throws IOException {
        writer.println("EmptyFrontier");
    }

}
