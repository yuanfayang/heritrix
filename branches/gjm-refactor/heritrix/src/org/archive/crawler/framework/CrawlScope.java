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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.util.DevUtils;

/**
 * A CrawlScope instance defines which URIs are "in"
 * a particular crawl. 
 * 
 * It is essentially a Filter which determines, looking at 
 * the totality of information available about a 
 * CandidateURI/CrawlURI instamce, if that URI should be 
 * scheduled for crawling.
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
	// the xpath to extract seeds from the scope configuration element
	public static final String XP_SEEDS = "seeds";
	//  regexp for identifying URIs in seed input data
	public static final Pattern DEFAULT_SEED_EXTRACTOR = 
		Pattern.compile("(?i:(http(s)?://)?\\w+\\.\\w+(\\.\\w+)*(:\\d+)?(/\\S*)?)");
	// a monotonically increasing version number, for scopes that may change
	int version = 0;
	// a list of all seeds
	List seeds;
	// pattern to extract seeds
	Pattern seedExtractor = DEFAULT_SEED_EXTRACTOR;
	// associated CrawlController
	CrawlController controller;
	
	public void initialize(CrawlController controller) {
		super.initialize(controller);
		this.controller = controller;
		// TODO let configuration info specify seedExtractor
	}

	/**
	 * Return the scope version. Increments if the scope changes,
	 * for example by operator edits during a crawl. A CandidateURI
	 * remembers any scope version it was previously accepted by,
	 * which helps avoid redundant scope checks. 
	 * 
	 * @return
	 */
	public int getVersion() {
		return version;
	}

	public String toString() {
		return "CrawlScope<"+name+">";
	}	
	
	
	/**
	 * Return the full list of seeds in this scope. 
	 * 
	 * @return
	 */
	public List getSeeds() {
		if (seeds != null) {
			return seeds;
		}
		readSeeds();
		return seeds;
	}

	/**
	 * Read seeds from either the configuration file, or the 
	 * external seed file it specifies. Any lines where the
	 * first non-whitespace character is '#' are considered
	 * comments and ignored. 
	 * 
	 * Otherwise, anything on the line that looks like a URI or 
	 * URI fragment (such as a dotted hostname, with or without 
	 * a path-fragment) will be taken as a URI. If necessary,
	 * "http://" will be prepended to URI-like strings lacking 
	 * it. 
	 */
	private void readSeeds() {
		seeds = new ArrayList();
		try {
			BufferedReader reader = nodeValueOrSrcReader(XP_SEEDS);
			String read;
			while ((read = reader.readLine()) != null) {
				read = read.trim();
				if (read.startsWith("#")) {
					continue;
				}
				Matcher m = seedExtractor.matcher(read);
				while(m.find()) {
					String candidate = m.group();
					if(m.group(1)==null) {
						// naked hostname without scheme
						candidate = "http://" + candidate;
					}
					try {
						seeds.add(UURI.createUURI(candidate));
					} catch (URISyntaxException e1) {
						Object[] array = { null, candidate };
						controller.uriErrors.log(Level.INFO,"reading seeds: "+e1.getMessage(), array );
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			DevUtils.warnHandle(e, "throw runtime error? log something?");
		}
	}

	/**
	 * Clear the list of seeds. 
	 * @param list
	 */
	public void clearSeeds() {
		seeds = new ArrayList();
	}

	/**
	 * Adds a seed to this scope -- and the associated crawl/frontier.
	 * 
	 * TODO determine if this is appropriate place for this
	 * @param u
	 */
	public void addSeed(UURI u){
		seeds.add(u);
		CandidateURI caUri = new CandidateURI(u);
		caUri.setIsSeed(true);
		controller.getFrontier().schedule(caUri);
	}
	
	/** 
	 * Returns whether the given object (typically a CandidateURI) falls
	 * within this scope. If so, stamps the object with the current scope
	 * version, so that subsequent checks are expedited. IMPORTANT NOTE: 
	 * assumes the same CandidateURI object will not be tested  against 
	 * different CrawlScope objects. 
	 */
	public boolean accepts(Object o) {
		// expedited check
		if (o instanceof CandidateURI
			&& ((CandidateURI) o).getScopeVersion() == version) {
			return true;
		}
		boolean result = super.accepts(o);
		// stamp with version for expedited check
		if (result == true && o instanceof CandidateURI) {
			((CandidateURI) o).setScopeVersion(version);
		}
		return result;
	}
}
