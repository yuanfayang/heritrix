/*
 * SimplePolitenessEnforcer.java
 * Created on May 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class SimplePolitenessEnforcer extends Processor {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimplePolitenessEnforcer");

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		// for all curis, set appropriate delay factor
		// TODO: someday, allow per-host factors
		curi.setDelayFactor(getDefaultDelayFactor());
		// treat /robots.txt fetches specially
		if (curi.getUURI().getUri().getPath().equals("/robots.txt")) {
			// allow processing to continue
			return; 
		}
		// require /robots.txt if not present
		if (curi.getHost().getRobotsExpires()<System.currentTimeMillis()) {
			logger.info("No valid robots for "+curi.getHost()+"; deferring "+curi);
			curi.setPrerequisiteUri("/robots.txt");
			curi.cancelFurtherProcessing();
			return;
		}
		// test against robots.txt if available
		String ua = controller.getOrder().getBehavior().getUserAgent();
		if( curi.getHost().getRobots().disallows(curi.getUURI().getUri().getPath(),ua)) {
			// don't fetch
			curi.cancelFurtherProcessing();  // turn off later stages
			curi.getAList().putString("error","robots.txt exclusion");
			logger.info("robots.txt precluded "+curi);
			return;
		}
		// OK, it's allowed
		return;
	}

	/**
	 * 
	 */
	private int getDefaultDelayFactor() {
		return getIntAt("//params/@delay-factor");
	}

}
