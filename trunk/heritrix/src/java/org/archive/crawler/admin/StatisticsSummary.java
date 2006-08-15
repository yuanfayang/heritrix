/* StatisticsSummary
 * 
 * $Id$$
 * 
 * Created on July 27, 2006
 * 
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.crawler.admin;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.LongWrapper;


/**
 * This class provides descriptive statistics of a finished crawl job by
 * using the crawl report files generated by StatisticsTracker.  Any formatting
 * changes to the way StatisticsTracker writes to the summary crawl reports will
 * require changes to this class.
 * <p>
 * The following statistics are accessible from this class:
 * <ul>
 *   <li> Successfully downloaded documents per fetch status code
 *   <li> Successfully downloaded documents per document mime type
 *   <li> Amount of data per mime type
 *   <li> Successfully downloaded documents per host
 *   <li> Amount of data per host
 *   <li> Successfully downloaded documents per top-level domain name (TLD)
 *   <li> Disposition of all seeds 
 *   <li> Successfully downloaded documents per host per source
 * </ul>
 *
 * <p>TODO: Make it so summarizing is not done all in RAM so we avoid
 * OOME.
 *
 * @author Frank McCown
 *
 * @see org.archive.crawler.admin.StatisticsTracker
 */
public class StatisticsSummary {
    /**
     * Messages from the StatisticsSummary.
     */
    private final static Logger logger =
        Logger.getLogger(StatisticsSummary.class.getName());
    
    private boolean stats = true;
    
    /** Crawl job whose summary we want to view */
    private CrawlJob cjob;
        
    protected long totalDnsStatusCodeDocuments = 0;
    protected long totalStatusCodeDocuments = 0;
    protected long totalFileTypeDocuments = 0;
    protected long totalMimeTypeDocuments = 0;
    protected long totalDnsMimeTypeDocuments = 0;
    protected long totalDnsHostDocuments = 0;
    protected long totalHostDocuments = 0;
    protected long totalMimeSize = 0;
    protected long totalDnsMimeSize = 0;
    protected long totalHostSize = 0;
    protected long totalDnsHostSize = 0;
    protected long totalTldDocuments = 0;
    protected long totalTldSize = 0;
    protected long totalHosts = 0;
    
    protected String durationTime;
    protected String processedDocsPerSec;
    protected String bandwidthKbytesPerSec;
    protected String totalDataWritten;
    
    /** Keep track of the file types we see (mime type -> count) */
    protected Hashtable mimeTypeDistribution = new Hashtable();
    protected Hashtable mimeTypeBytes = new Hashtable();
    protected Hashtable mimeTypeDnsDistribution = new Hashtable();
    protected Hashtable mimeTypeDnsBytes = new Hashtable();
    
    /** Keep track of status codes */
    protected Hashtable statusCodeDistribution = new Hashtable();
    protected Hashtable dnsStatusCodeDistribution = new Hashtable();
    
    /** Keep track of hosts */
    protected Hashtable hostsDistribution = new Hashtable(); 
    protected Hashtable hostsBytes = new Hashtable(); 
    protected Hashtable hostsDnsDistribution = new Hashtable();
    protected Hashtable hostsDnsBytes = new Hashtable(); 

    /** Keep track of TLDs */
    protected Hashtable tldDistribution = new Hashtable();
    protected Hashtable tldBytes = new Hashtable();
    protected Hashtable tldHostDistribution = new Hashtable();

    /** Keep track of processed seeds */
    protected transient Map processedSeedsRecords = new Hashtable();

    /**
     * Constructor
     * 
     * @param cjob
     * 				Completed crawl job
     */
    public StatisticsSummary(CrawlJob cjob) {
    	this.cjob = cjob;
    	
    	// Read all stats for this crawl job
    	this.stats = calculateStatusCodeDistribution();
    	if (calculateMimeTypeDistribution()) {
    		this.stats = true;
    	}
    	if (calculateHostsDistribution()) {
    		this.stats = true;
    	}
    	if (readCrawlReport()) {
    		this.stats = true;
    	}
    	if (readSeedReport()) {
    		this.stats = true;
    	}
    }
    
    
    /**
     * Increment a counter for a key in a given HashMap. Used for various
     * aggregate data.
     *
     * @param map The HashMap
     * @param key The key for the counter to be incremented, if it does not
     *               exist it will be added (set to 1).  If null it will
     *            increment the counter "unknown".
     */
    protected static void incrementMapCount(Map map, String key) {
    	incrementMapCount(map,key,1);
    }

    /**
     * Increment a counter for a key in a given HashMap by an arbitrary amount.
     * Used for various aggregate data. The increment amount can be negative.
     *
     * @param map
     *            The HashMap
     * @param key
     *            The key for the counter to be incremented, if it does not
     *            exist it will be added (set to equal to
     *            <code>increment</code>).
     *            If null it will increment the counter "unknown".
     * @param increment
     *            The amount to increment counter related to the
     *            <code>key</code>.
     */
    protected static void incrementMapCount(Map map, String key,
            long increment) {
        if (key == null) {
            key = "unknown";
        }
        LongWrapper lw = (LongWrapper)map.get(key);
        if(lw == null) {
            map.put(key, new LongWrapper(increment));
        } else {
            lw.longValue += increment;
        }
    }
  
    /** Returns a HashMap that contains information about distributions of
     *  encountered mime types.  Key/value pairs represent
     *  mime type -> count.
     * <p>
     * <b>Note:</b> All the values are wrapped with a
     * {@link LongWrapper LongWrapper}
     * @return mimeTypeDistribution
     */
    public Hashtable getMimeDistribution() {
        return mimeTypeDistribution;
    }
    
    public long getTotalMimeTypeDocuments() {
       	return totalMimeTypeDocuments;
    }
    
    public long getTotalDnsMimeTypeDocuments() {
       	return totalDnsMimeTypeDocuments;
    }
    
    public long getTotalMimeSize() {
    	return totalMimeSize;
    }
    
    public long getTotalDnsMimeSize() {
    	return totalDnsMimeSize;
    }
   
    /**
     * Return a HashMap representing the distribution of HTTP status codes for
     * successfully fetched curis, as represented by a hashmap where key -&gt;
     * val represents (string)code -&gt; (integer)count.
     * 
     * <b>Note: </b> All the values are wrapped with a
     * {@link LongWrapper LongWrapper}
     * 
     * @return statusCodeDistribution
     */
    public Hashtable getStatusCodeDistribution() {    	
        return statusCodeDistribution;
    }
   
    /**
     * Return a HashMap representing the distribution of DNS status codes for
     * successfully fetched curis, as represented by a hashmap where key -&gt;
     * val represents (string)code -&gt; (integer)count.
     * 
     * <b>Note: </b> All the values are wrapped with a
     * {@link LongWrapper LongWrapper}
     * 
     * @return dnsStatusCodeDistribution
     */
    public Hashtable getDnsStatusCodeDistribution() {
    	return dnsStatusCodeDistribution;
    }
    
    public Hashtable getDnsMimeDistribution() {
        return mimeTypeDnsDistribution;
    }

    public long getTotalDnsStatusCodeDocuments() {
    	return totalDnsStatusCodeDocuments;
    }
    
    public long getTotalStatusCodeDocuments() {
    	return totalStatusCodeDocuments;
    }  
    
    public long getTotalHostDocuments() {
       	return totalHostDocuments;
    }
    
    public long getTotalDnsHostDocuments() {
       	return totalDnsHostDocuments;
    }
    
    public Hashtable getHostsDnsDistribution() {
    	return hostsDnsDistribution;
    }
    
    public long getTotalHostDnsDocuments() {
    	return totalDnsHostDocuments;
    }
    
    public long getTotalHostSize() {
    	return totalHostSize;
    }
    
    public long getTotalDnsHostSize() {
    	return totalDnsHostSize;
    }
    
    public Hashtable getTldDistribution() {
    	return tldDistribution;
    }
    
    public Hashtable getTldBytes() {
    	return tldBytes;
    }
    
    public long getTotalTldDocuments() {
    	return totalTldDocuments;
    }
    
    public long getTotalTldSize() {
    	return totalTldSize;
    }
    
    public Hashtable getTldHostDistribution() {
    	return tldHostDistribution;
    }
    
    public long getTotalHosts() {
    	return totalHosts;
    }
    
    public String getDurationTime() {
    	return durationTime;
    }
    
    public String getProcessedDocsPerSec() {
    	return processedDocsPerSec;
    }
    
    public String getBandwidthKbytesPerSec() {
    	return bandwidthKbytesPerSec;
    }
    
    public String getTotalDataWritten() {
    	return totalDataWritten;
    }

    /**
     * Sort the entries of the given HashMap in descending order by their
     * values, which must be longs wrapped with <code>LongWrapper</code>.
     * <p>
     * Elements are sorted by value from largest to smallest. Equal values are
     * sorted in an arbitrary, but consistent manner by their keys. Only items
     * with identical value and key are considered equal.
     *
     * If the passed-in map requires access to be synchronized, the caller
     * should ensure this synchronization. 
     * 
     * @param mapOfLongWrapperValues
     *            Assumes values are wrapped with LongWrapper.
     * @return a sorted set containing the same elements as the map.
     */
    public TreeMap getReverseSortedCopy(final Map mapOfLongWrapperValues) {
        TreeMap sortedMap = new TreeMap(new Comparator() {
            public int compare(Object e1, Object e2) {
                long firstVal = ((LongWrapper)mapOfLongWrapperValues.get(e1)).
                    longValue;
                long secondVal = ((LongWrapper)mapOfLongWrapperValues.get(e2)).
                    longValue;
                if (firstVal < secondVal) {
                    return 1;
                }
                if (secondVal < firstVal) {
                    return -1;
                }
                // If the values are the same, sort by keys.
                return ((String)e1).compareTo((String)e2);
            }
        });
        try {
            sortedMap.putAll(mapOfLongWrapperValues);
        } catch (UnsupportedOperationException e) {
            Iterator i = mapOfLongWrapperValues.keySet().iterator();
            for (;i.hasNext();) {
                // Ok. Try doing it the slow way then.
                Object key = i.next();
                sortedMap.put(key, mapOfLongWrapperValues.get(key));
            }
        }
        return sortedMap;
    }
     
    /**
     * Get the number of hosts with a particular TLD.
     * @param tld
     * 				top-level domain name
     * @return		Total crawled hosts
     */
    public long getHostsPerTld(String tld) {
    	LongWrapper lw = (LongWrapper)tldHostDistribution.get(tld);
    	return (lw == null ? 0 : lw.longValue);
    }
    
    /**
     * Read status code distribution from responsecode-report.txt.
     * DNS and HTTP status codes are separated when read.
     * @return True if we found some stats.
     */
    private boolean calculateStatusCodeDistribution() {
    	// Read from responsecode-report.txt
    	File f = new File(cjob.getDirectory(), "responsecode-report.txt");
    	if (!f.exists()) {
    		return false;
    	}
    	BufferedReader br = null;
    	try {
	    	FileReader reader = new FileReader(f);
	    	br = new BufferedReader(reader);
	    	String line = br.readLine();  // Ignore heading
	    	line = br.readLine();
	    	while (line != null) {  	  
	    	  // Get status code and # urls which are seperated by a space
	    	  
	    	  String[] items = line.split(" ");
	    	  if (items.length < 2) {
	    		  logger.log(Level.WARNING,
                          "Unexpected formatting on line [" + line + "]");
	    	  }
	    	  else {
	    		  // See if DNS or HTTP status code
	    		  if (items[0].length() < 3) {
	    			  // DNS status code
	    			  long total = Long.parseLong(items[1]);
	    			  dnsStatusCodeDistribution.put(items[0], 
	    					  new LongWrapper(total));
	    			  totalDnsStatusCodeDocuments += total;
	    		  }
	    		  else {
	    			  // HTTP status code
	    			  long total = Long.parseLong(items[1]);
	    			  statusCodeDistribution.put(items[0], 
	    					  new LongWrapper(total));
	    			  totalStatusCodeDocuments += total;
	    		  }
	    	  }
	    	  line = br.readLine();
	    	}
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Unable to read " + f.getAbsolutePath(),
    			e);
    	} finally {
    		if (br != null) {
    			try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE,
						"Closing " + f.getAbsolutePath(), e);
				}
    		}
    	}
    	return true;
    }
    
    /**
     * Read MIME type data from mimetype-report.txt.
     * MIME type of text/dns is separated from other MIME types.
     * @return True if we found some stats.
     */
    private boolean calculateMimeTypeDistribution() {    	
    	File f = new File(cjob.getDirectory(), "mimetype-report.txt");
    	if (!f.exists()) {
    		return false;
    	}
    	BufferedReader br = null;
    	try {
	    	FileReader reader = new FileReader(f);
	    	br = new BufferedReader(reader);
	    	String line = br.readLine();  // Ignore heading
	    	line = br.readLine();
	    	while (line != null) {	    			    	  
	    		// Get num urls, num bytes, and MIME type (seperated by a space)
	    		// Example: 12 134279 text/html
  
	    		String[] items = line.split(" ");
	    		if (items.length < 3) {
	    			logger.log(Level.WARNING,
                            "Unexpected formatting on line [" + line + "]");
	    		}
	    		else {
	    			long total = Long.parseLong(items[0]);
	    			long bytes = Long.parseLong(items[1]);
	    			String mime = items[2];

	    			// Seperate DNS reconrds from HTTP
	    			if (mime.equalsIgnoreCase("text/dns")) {
	    				mimeTypeDnsDistribution.put(mime,
                                new LongWrapper(total));
	    				mimeTypeDnsBytes.put(mime, new LongWrapper(bytes));
	    				totalDnsMimeTypeDocuments += total;
	    				totalDnsMimeSize += bytes;
	    			}
	    			else {
	    				mimeTypeDistribution.put(mime, new LongWrapper(total));
	    				mimeTypeBytes.put(mime, new LongWrapper(bytes));
	    				totalMimeTypeDocuments += total;
	    				totalMimeSize += bytes;
	    			}
	    		}
	    		line = br.readLine();
	    	}
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Reading " + f.getAbsolutePath(), e);
    	} finally {
    		if (br != null) {
    			try {
    				br.close();
    			} catch (IOException e) {
    				logger.log(Level.SEVERE,
    					"Closing " + f.getAbsolutePath(), e);
    			}
    		}
    	}
    	return true;
    }
    
    /**
     * Read number of URLs and total bytes for each host name from
     * hosts-report.txt.
     * Host name of "dns:" is separated from others.
     * @return true if stats found.
     */
    private boolean calculateHostsDistribution() {
    	File f = new File(cjob.getDirectory(), "hosts-report.txt");
    	if (!f.exists()) {
    		return false;
    	}
    	BufferedReader br = null;
    	try {
	    	FileReader reader = new FileReader(f);
	    	br = new BufferedReader(reader);
	    	String line = br.readLine();  // Ignore heading
	    	line = br.readLine();
	    	while (line != null) {    	  
	    		// Get num urls, num bytes, and host name (seperated by a space)
	    		// Example: 9 7468 www.blogger.com

	    		String[] items = line.split(" ");
	    		if (items.length < 3) {
	    			logger.log(Level.WARNING,
                            "Unexpected formatting on line [" + line + "]");
	    		}
	    		else {
	    			long total = Long.parseLong(items[0]);
	    			long bytes = Long.parseLong(items[1]);
	    			String host = items[2];

	    			// Seperate DNS reconrds from HTTP
	    			if (host.startsWith("dns:", 0)) {
	    				hostsDnsDistribution.put(host, new LongWrapper(total));
	    				hostsDnsBytes.put(host, new LongWrapper(bytes));
	    				totalDnsHostDocuments += total;
	    				totalDnsHostSize += bytes;
	    			}
	    			else {
	    				hostsDistribution.put(host, new LongWrapper(total));
	    				hostsBytes.put(host, new LongWrapper(bytes));
	    				totalHostDocuments += total;
	    				totalHostSize += bytes;

	    				// Count top level domain (TLD)
	    				String tld = host.substring(host.lastIndexOf('.')+1);
	    				incrementMapCount(tldDistribution, tld, total);   
	    				incrementMapCount(tldBytes, tld, bytes);
	    				incrementMapCount(tldHostDistribution, tld);
	    				totalTldDocuments += total;
	    				totalTldSize += bytes;

	    				totalHosts++;
	    			}
	    		}
	    		line = br.readLine();
	    	}
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Reading " + f.getAbsolutePath(), e);
    	} finally {
    		if (br != null) {
    			try {
    				br.close();
    			} catch (IOException e) {
    				logger.log(Level.SEVERE,
    					"Closing " + f.getAbsolutePath(), e);
    			}
    		}
    	}
    	return true;
    }

    /**
     * Returns the accumulated number of bytes downloaded from a given host.
     * @param host name of the host
     * @return the accumulated number of bytes downloaded from a given host
     */
    public long getBytesPerHost(String host) { 
    	long bytes = -1;
    	
    	bytes = host != null && host.startsWith("dns:", 0) ? 
	    	((LongWrapper)hostsDnsBytes.get(host)).longValue :
	    	((LongWrapper)hostsBytes.get(host)).longValue;	    
    	
    	return bytes;
    }
    
    /**
     * Returns the total number of bytes downloaded for a given TLD.
     * @param tld TLD
     * @return the total number of bytes downloaded for a given TLD
     */
    public long getBytesPerTld(String tld) {
    	LongWrapper lw = (LongWrapper)tldBytes.get(tld);
    	return (lw == null ? 0 : lw.longValue);
    }

    /**
     * Returns the accumulated number of bytes from files of a given file type.
     * @param filetype Filetype to check.
     * @return the accumulated number of bytes from files of a given mime type
     */
    public long getBytesPerMimeType(String filetype) {
    	long bytes = -1;
    	
    	if (filetype != null) {    	
	    	if (filetype.equals("text/dns")) {	    		
	    		bytes = mimeTypeDnsBytes.get(filetype) == null ? 0 :
	    			((LongWrapper)mimeTypeDnsBytes.get(filetype)).longValue;
	    	}
	    	else {
	    		bytes = mimeTypeBytes.get(filetype) == null ? 0 :
	    			((LongWrapper)mimeTypeBytes.get(filetype)).longValue;
	    	}
    	}
    	return bytes;
    }
    
    /**
     * Reads duration time, processed docs/sec, bandwidth, and total size
     * of crawl from crawl-report.txt.
     * @return true if stats found.
     */
    public boolean readCrawlReport() {
    	File f = new File(cjob.getDirectory(), "crawl-report.txt");
    	if (!f.exists()) {
    		return false;
    	}
    	BufferedReader br = null;
    	try {
	    	FileReader reader = new FileReader(f);
	    	br = new BufferedReader(reader);
	    	String line = br.readLine();  
	    	while (line != null) {
	    		if (line.startsWith("Duration Time")) {
	    			durationTime = line.substring(line.indexOf(':')+1);
	    		}
	    		else if (line.startsWith("Processed docs/sec")) {
	    			processedDocsPerSec = line.substring(line.indexOf(':')+1);
	    		}
	    		else if (line.startsWith("Bandwidth in Kbytes/sec")) {
	    			bandwidthKbytesPerSec = line.substring(line.indexOf(':')+1);
	    		}
	    		else if (line.startsWith("Total Raw Data Size in Bytes")) {
	    			totalDataWritten = line.substring(line.indexOf(':')+1);
	    		}

	    		line = br.readLine();
	    	}
    	}
    	catch (IOException e) {
    		logger.log(Level.SEVERE, "Reading " + f.getAbsolutePath(), e);		
    	} finally {
    		if (br != null) {
    			try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE,
					    "Failed close of " + f.getAbsolutePath(), e);
				}
    		}
    	}
    	return true;
    }
  
    /**
     * Returns sorted Iterator of seeds records based on status code.
     * @return sorted Iterator of seeds records
     */
    public Iterator getSeedRecordsSortedByStatusCode() {
        TreeSet sortedSet = new TreeSet(new Comparator() {
            public int compare(Object e1, Object e2) {
                SeedRecord sr1 = (SeedRecord)e1;
                SeedRecord sr2 = (SeedRecord)e2;
                int code1 = sr1.getStatusCode();
                int code2 = sr2.getStatusCode();
                if (code1 == code2) {
                    // If the values are equal, sort by URIs.
                    return sr1.getUri().compareTo(sr2.getUri());
                }
                // mirror and shift the nubmer line so as to
                // place zero at the beginning, then all negatives 
                // in order of ascending absolute value, then all 
                // positives descending
                code1 = -code1 - Integer.MAX_VALUE;
                code2 = -code2 - Integer.MAX_VALUE;
                
                return new Integer(code1).compareTo(new Integer(code2));
            }
        });
        for (Iterator iterator = processedSeedsRecords.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            SeedRecord sr = (SeedRecord)entry.getValue();
            sortedSet.add(sr);
        }
        
        return sortedSet.iterator();
    }
    
    /**
     * Reads seed data from seeds-report.txt.
     * @return True if stats found.
     */
    private boolean readSeedReport() {
    	File f = new File(cjob.getDirectory(), "seeds-report.txt");
    	if (!f.exists()) {
    		return false;
    	}
    	BufferedReader br = null;
    	try {
	    	FileReader reader = new FileReader(f);
	    	br = new BufferedReader(reader);
	    	
	    	// Ignore heading: [code] [status] [seed] [redirect]
	    	String line = br.readLine();  
	    	line = br.readLine();
	    	while (line != null) {
	    		// Example lines:
	    		// 302 CRAWLED http://www.ashlandcitytimes.com/ http://www.ashlandcitytimes.com/apps/pbcs.dll/section?Category=MTCN01
	    		// 200 CRAWLED http://noleeo.com/

	    		String[] items = line.split(" ");

	    		if (items.length < 3) {
	    			logger.log(Level.WARNING,
                            "Unexpected formatting on line [" + line + "]");
	    		}
	    		else {
	    			String statusCode = items[0];
	    			String crawlStatus = items[1];
	    			String seed = items[2];
	    			String redirect = items.length > 3 ? items[3] : null;

	    			// All values should be CRAWLED or NOTCRAWLED
	    			if (crawlStatus.equals("CRAWLED")) {
	    				crawlStatus =org.archive.crawler.framework.StatisticsTracking.SEED_DISPOSITION_SUCCESS;	    		  
	    			}
	    			else {
	    				crawlStatus = org.archive.crawler.framework.StatisticsTracking.SEED_DISPOSITION_FAILURE;
	    			}
	    			SeedRecord sr = new SeedRecord(seed, crawlStatus, 
	    					Integer.parseInt(statusCode), redirect);
	    			processedSeedsRecords.put(seed, sr);
	    		}

	    		line = br.readLine();
	    	}
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Reading " + f.getAbsolutePath(), e);   		
    	} finally {
    		if (br != null) {
    			try {
					br.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE,
						"Closing " + f.getAbsolutePath(), e);
				}
    		}
    	}
    	return true;
    }
        
    /**
     * Return a copy of the hosts distribution in reverse-sorted
     * (largest first) order.
     *  
     * @return SortedMap of hosts distribution
     */
    public SortedMap getReverseSortedHostsDistribution() {
        return getReverseSortedCopy(hostsDistribution);  
    }    
    
    /**
     * @return True if we compiled stats, false if none to compile (e.g.
     * there are no reports files on disk).
     */
    public boolean isStats() {
    	return this.stats;
    }
}
