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
	private static String XP_DELAY_FACTOR = "//params/@delay-factor";
	private static String XP_MINIMUM_DELAY = "//params/@minimum-delay";
	private static int DEFAULT_DELAY_FACTOR = 10;
	private static int DEFAULT_MINIMUM_DELAY = 2000;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimplePolitenessEnforcer");

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		// for all curis, set appropriate delays
		// TODOSOMEDAY: allow per-host factors
		curi.setDelayFactor(getDelayFactorFor(curi));
		curi.setMinimumDelay(getMinimumDelayFor(curi));
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
	private int getMinimumDelayFor(CrawlURI curi) {
		return getIntAt(XP_MINIMUM_DELAY,DEFAULT_MINIMUM_DELAY);		
	}

	/**
	 * 
	 */
	private int getDelayFactorFor(CrawlURI curi) {
		return getIntAt(XP_DELAY_FACTOR, DEFAULT_DELAY_FACTOR);
	}

}
