/*
 * CrawlScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;

/**
 * Filter which determines, looking at the totality of 
 * information available about a CandidateURI/CrawlURI,
 * instamce, if that URI should be scheduled for crawling.
 * 
 * Dynamic information inherent in the discovery of the
 * URI -- such as the path by which it was discovered --
 * may be considered.
 * 
 * Dynamic information which requires the consultation 
 * of external and potentially volatile information --
 * such as current robots.txt requests and the history
 * of attempts to crawl the same URI -- should NOT be
 * considered. Those potentially high-latency decisions
 * should be made at another step. . 
 * 
 * @author gojomo
 *
 */
public abstract class CrawlScope extends Filter {
	private static final String XP_SEEDS = "/seeds";
	int version;
	List seeds;
	CrawlController controller;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController controller) {
		super.initialize(controller);
		this.controller = controller;
	}

	/**
	 * @return
	 */
	public int getVersion() {
		return version;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlScope<"+name+">";
	}	
	
	public List getSeeds() {
		if (seeds != null) {
			return seeds;
		}
		seeds = new ArrayList();
		try {
			BufferedReader reader = nodeValueOrSrcReader(XP_SEEDS);
			String read;
			while (reader != null) {
				do {
					read = reader.readLine();
				} while (
					(read != null)
						&& ((read = read.trim()).startsWith("#")
							|| read.length() == 0));

				if (read == null) {
					reader.close();
					reader = null;
				} else {
					try {
						seeds.add(UURI.createUURI(read));
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// TODO throw runtime error? log something?
		}
		return seeds;
   	
	}


	/**
	 * @param list
	 */
	public void clearSeeds() {
		seeds = new ArrayList();
	}

	/**
	 * TODO determine if this is appropriate place for this
	 * @param u
	 */
	public void addSeed(UURI u){
		seeds.add(u);
		CandidateURI caUri = new CandidateURI(u);
		caUri.setIsSeed(true);
		controller.getFrontier().schedule(caUri);
	}
}
