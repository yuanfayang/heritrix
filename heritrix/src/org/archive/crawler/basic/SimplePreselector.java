/*
 * SimplePreselector.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Processor;

/**
 * Gives a yes/no on whether a CrawlURI should be processed at all.
 * 
 * Usually, failing a processor filter causes that processor
 * to be skipped. Failing this processor's filter means a
 * CrawlURI will be marked OUT_OF_SCOPE.
 * 
 * 
 * @author gojomo
 *
 */
public class SimplePreselector extends Processor implements FetchStatusCodes {
	private boolean recheckScope;

	private static String XP_RECHECK_SCOPE="@scope";
	
//	private static String XP_MAX_LINK_DEPTH="params/@max-link-depth";
//	private static String XP_MAX_EMBED_DEPTH="params/@max-embed-depth";
//	private int maxLinkDepth = -1;
//	private int maxEmbedDepth = -1;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		if (recheckScope) {
			CrawlScope scope = controller.getScope();
			if (curi.getScopeVersion()==scope.getVersion()) {
				// already checked
				return;
			}
			if(scope.accepts(curi)) {
				curi.setScopeVersion(scope.getVersion());
				return;
			}
			// scope rejected
			curi.setFetchStatus(S_OUT_OF_SCOPE);
			curi.cancelFurtherProcessing();
		}
		
		
//		super.innerProcess(curi);
//		
//		// check for too-deep
//		if(maxLinkDepth>=0 && curi.getLinkHopCount()>maxLinkDepth) {
//			curi.setFetchStatus(S_TOO_MANY_LINK_HOPS);
//			curi.cancelFurtherProcessing();
//			return;
//		}
//		if(maxEmbedDepth>=0 && curi.getEmbedHopCount()>maxEmbedDepth) {
//			curi.setFetchStatus(S_TOO_MANY_EMBED_HOPS);
//			curi.cancelFurtherProcessing();
//			return;
//		}
	}

//	/* (non-Javadoc)
//	 * @see org.archive.crawler.framework.Processor#innerRejectProcess(org.archive.crawler.datamodel.CrawlURI)
//	 */
//	protected void innerRejectProcess(CrawlURI curi) {
//		super.innerRejectProcess(curi);
//		// filter-rejection means out-of-scope for everything but embeds
//		if (curi.getEmbedHopCount() < 1) {
//			curi.setFetchStatus(S_OUT_OF_SCOPE);
//			curi.cancelFurtherProcessing();
//		} else {
//			// never mind; scope filters don't apply
//		}
//	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
	    recheckScope = getBooleanAt("@scope",false);
		
		
		//maxLinkDepth = getIntAt(XP_MAX_LINK_DEPTH, maxLinkDepth);
		//maxEmbedDepth = getIntAt(XP_MAX_EMBED_DEPTH, maxEmbedDepth);
		
	}
	

}
