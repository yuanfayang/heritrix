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
 * HostCacheMaintainer.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;

/**
 * A step, late in the processing of a CrawlURI, for
 * updating the per-host information that may have
 * been affected by the fetch. This will initially
 * be robots and ip address info; it could include 
 * other per-host stats that would affect the crawl
 * (like total pages visited at the site) as well.
 * 
 * @author gojomo
 *
 */
public class CrawlStateUpdater extends Processor implements CoreAttributeConstants, FetchStatusCodes {

	public static int MAX_DNS_FETCH_ATTEMPTS = 3;
	
	public void initialize(CrawlController c){
		super.initialize(c);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
	
		// if it's a dns entry set the expire time
		if(curi.getUURI().getScheme().equals("dns")){

			// if we've looked up the host update the expire time
			if(curi.getServer().getHost().hasBeenLookedUp()){
				long expires = curi.getServer().getHost().getIpExpires();
				
				if(expires > 0){
					curi.setDontRetryBefore(expires);
				}
				
			}else{ 
				
				// TODO: resolve several issues here:
				//   (1) i don't think this else clause is ever reached;
				//       won't every DNS uri that gets this far imply
				//       hasBeenLookedUp will have been set?
				//   (2) we don't want repeated successful attempts to
				//       refetch a domain name, each time it expires,
				//       to eventually exhaust the retries... so in
				//       fact the retry count needs to be reset somewhere,
				//       maybe at each success
				
				
				
				// if we've tried too many times give up
				if(curi.getFetchAttempts() >= MAX_DNS_FETCH_ATTEMPTS){
					curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
				}
			}
		
		// if it's not dns make sure it's http, 'cause we don't know nuthin' else
		}else if(curi.getUURI().getScheme().equals("http")){
		
			if ( curi.getFetchStatus()>0 && (curi.getUURI().getPath() != null) && curi.getUURI().getPath().equals("/robots.txt")) {
				// update host with robots info
				if(curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
					GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
					try {
						curi.getServer().updateRobots(get, controller.getOrder().getRobotsHonoringPolicy());
					} catch (IOException e) {
						curi.addLocalizedError(getName(),e,"robots.txt parsing IOException");
					}
									
					// curi can be refetched once robots data expires
					curi.setDontRetryBefore(curi.getServer().getRobotsExpires());
				}
			}
		}
	}
}
