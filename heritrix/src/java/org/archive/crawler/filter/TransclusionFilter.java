/* Copyright (C) 2003 Internet Archive.
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
 *
 * TransclusionFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * Filter which accepts CandidateURI/CrawlURI instances which contain more
 * than zero but fewer than max-trans-hops entries at the end of their 
 * discovery path. 
 * 
 * @author Gordon Mohr
 */
public class TransclusionFilter extends Filter {
	private static final String XP_MAX_TRANS_HOPS = "@max-trans-hops";
	private static final String XP_MAX_SPECULATIVE_HOPS = "@max-speculative-hops";
	private static final String XP_MAX_REFERRAL_HOPS = "@max-referral-hops";
	private static final String XP_MAX_EMBED_HOPS = "@max-embed-hops";
	private static final int DEFAULT_MAX_TRANS_HOPS = 4;
	private static final int DEFAULT_MAX_SPECULATIVE_HOPS = 1; // no more than 1
	private static final int DEFAULT_MAX_REFERRAL_HOPS = Integer.MAX_VALUE; // no limit beside the overall trans limit
	private static final int DEFAULT_MAX_EMBED_HOPS = Integer.MAX_VALUE; // no limit beside the overall trans limit
	int maxTransHops = DEFAULT_MAX_TRANS_HOPS;
	int maxSpeculativeHops = DEFAULT_MAX_SPECULATIVE_HOPS;
	int maxReferralHops = DEFAULT_MAX_REFERRAL_HOPS;
	int maxEmbedHops = DEFAULT_MAX_EMBED_HOPS;
		
//	// 1-3 trailing P(recondition)/R(eferral)/E(mbed)/X(speculative-embed) hops
//	private static final String TRANSCLUSION_PATH = ".*[PREX][PREX]?[PREX]?$";
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
//		if(o instanceof CandidateURI) {
//			return ((CandidateURI)o).getPathFromSeed().matches(TRANSCLUSION_PATH);
//		}
//		return false;
		if(! (o instanceof CandidateURI)) {
			return false;
		}
		String path = ((CandidateURI)o).getPathFromSeed();
		int transCount = 0;
		int specCount = 0;
		int refCount = 0;
		int embedCount = 0;
		loop: for(int i=path.length()-1;i>=0;i--) {
			// everything except 'L' is considered transitive
			switch (path.charAt(i)) {
				case 'L': {
					break loop;
				}
				case 'X': {
					specCount++;
					break;
				}
				case 'R': {
					refCount++;
					break;
				}
				case 'E': {
					embedCount++;
					break;
				}
				// 'D's get a free pass
			}
			transCount++;
		}
		return (transCount > 0) // this is a case of possible transclusion
				&& (transCount <= maxTransHops) // and the overall number of hops isn't too high
				&& (specCount <= maxSpeculativeHops) // and the number of spec-hops isn't too high
				&& (refCount <= maxReferralHops)  // and the number of referral-hops isn't too high
				&& (embedCount <= maxEmbedHops);  // and the number of embed-hops isn't too high
	}

	public void initialize(CrawlController c) {
		super.initialize(c);
		maxTransHops = getIntAt(XP_MAX_TRANS_HOPS,maxTransHops);
		maxSpeculativeHops = getIntAt(XP_MAX_SPECULATIVE_HOPS,maxSpeculativeHops);
		maxReferralHops = getIntAt(XP_MAX_REFERRAL_HOPS,maxReferralHops);
		maxEmbedHops = getIntAt(XP_MAX_EMBED_HOPS,maxEmbedHops);
	}

}
