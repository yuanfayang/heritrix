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
import org.archive.crawler.datamodel.FetchStatusCodes;

/**
 * Ensures the preconditions for a fetch -- such as 
 * DNS lookup or acquiring a robots.txt policy -- are
 * satisfied before a URI is passed to subsequent
 * stages.
 * 
 * @author gojomo
 *
 */
public class SimplePreconditionEnforcer extends Processor implements FetchStatusCodes {
	private static String XP_DELAY_FACTOR = "//params/@delay-factor";
	private static String XP_MINIMUM_DELAY = "//params/@minimum-delay";
	private static int DEFAULT_DELAY_FACTOR = 10;
	private static int DEFAULT_MINIMUM_DELAY = 2000;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimplePolitenessEnforcer");

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		
		if (considerDnsPreconditions(curi)) {
			return;
		}
		
		// make sure we only process schemes we understand (i.e. not dns)
		if(!curi.getUURI().getUri().getScheme().equals("http")){
			logger.fine("PolitenessEnforcer doesn't understand uri's of type "+curi.getUURI().getUri().getScheme() + " (ignoring)");
			return;
		}
		
		if (considerRobotsPreconditions(curi)) {
			return;
		}

		// OK, it's allowed
		
		// for all curis that will in fact be fetched, set appropriate delays
		// TODOSOMEDAY: allow per-host, per-protocol, etc. factors
		curi.setDelayFactor(getDelayFactorFor(curi));
		curi.setMinimumDelay(getMinimumDelayFor(curi));

		return;
	}

	/**
	 * @return
	 */
	private boolean considerRobotsPreconditions(CrawlURI curi) {
		// treat /robots.txt fetches specially
		if (curi.getUURI().getUri().getPath().equals("/robots.txt")) {
			// allow processing to continue
			return false; 
		}
		// require /robots.txt if not present
		if (curi.getHost().getRobotsExpires()<System.currentTimeMillis()) {
			logger.fine("No valid robots for "+curi.getHost()+"; deferring "+curi);
			curi.setPrerequisiteUri("/robots.txt");
			curi.incrementDeferrals();
			curi.cancelFurtherProcessing();
			return true;
		}
		// test against robots.txt if available
		String ua = controller.getOrder().getBehavior().getUserAgent();
		if( curi.getHost().getRobots().disallows(curi.getUURI().getUri().getPath(),ua)) {
			// don't fetch
			curi.cancelFurtherProcessing();  // turn off later stages
			curi.setFetchStatus(S_ROBOTS_PRECLUDED);
			curi.getAList().putString("error","robots.txt exclusion");
			logger.fine("robots.txt precluded "+curi);
			return true;
		}
		return false;
	}

	/**
	 * @param curi
	 * @return true if no further processing in this module shoudl occur
	 */
	private boolean considerDnsPreconditions(CrawlURI curi) {
		
		if(curi.getHost()==null) {
			curi.setFetchStatus(S_UNFETCHABLE_URI);
			curi.cancelFurtherProcessing();
			return true;
		}
		// if we haven't done a dns lookup  and this isn't a dns uri 
		// shoot that off and defer further processing
		if( !curi.getHost().hasBeenLookedUp() && 
			!curi.getUURI().getUri().getScheme().equals("dns")
		){
			logger.fine("deferring processing of " + curi.toString() + " for dns lookup." );

			String hostname = curi.getHost().getHostname();
			curi.setPrerequisiteUri("dns:" + hostname);
			curi.incrementDeferrals();
			curi.cancelFurtherProcessing();			
			return true;
		}
		
		// if we've done a dns lookup and it didn't resolve a host
		// cancel all processing of this URI
		if(curi.getHost().hasBeenLookedUp() && curi.getHost().getIP() == null){
			logger.fine("no dns for " + curi.getHost().toString() + " cancelling processing for " + curi.toString() );

			//TODO currently we're using FetchAttempts to denote both fetch attempts and
			// the choice to not attempt (here).  Eventually these will probably have to be treated seperately
			// to allow us to treat dns failures and connections failures (downed hosts, route failures, etc) seperately.
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
			curi.incrementFetchAttempts();
			curi.cancelFurtherProcessing();
			return true;
		}
		return false;
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
