package org.archive.crawler.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CrawlerCluster {

	private List<Crawler> crawlers;

	/**
	 * Construct a CrawlerCluster for the local crawlers.
	 */
	public CrawlerCluster() {
		crawlers = Collections.synchronizedList(new LinkedList<Crawler>(Crawler
				.discoverLocal()));
	}

	/**
	 * Find a matching crawler.
	 * @param host
	 * @param port
	 * @param instanceNo
	 * @return
	 */
	public Crawler getCrawler(String host, int port, int instanceNo) {
		for (Crawler crawler : crawlers) {
			if (crawler.getHost().equals(host) && crawler.getPort() == port
					&& crawler.getInstanceNo() == instanceNo) {
				return crawler;
			}
		}
		return null;
	}

	public Collection<Crawler> getCrawlers() {
		return crawlers;
	}

}
