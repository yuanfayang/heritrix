/*
 * SeedExtensionFilter.java
 * Created on Sep 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.Iterator;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * Accepts a new UURI if it is an "extension' of one of the URIs 
 * in the seed set. Most loosely, this could be any other URI 
 * under the same domain (as "calendar.yahoo.com' is to 'www.yahoo.com').
 * In other cases, only URIs on the exact same host sharing the
 * same path prefix (as "www.geocities.com/foouser/about" is to
 * "www.geocities.com/foouser/"). 
 * 
 * Configuration options determine how expansive the extension
 * definition is. By default, it is very strict: same host and
 * identical path up to the last '/' given in the seed.
 * 
 * 
 * 
 * @author gojomo
 *
 */
public class SeedExtensionFilter extends Filter {
	private CrawlController controller;
	static private int PATH = 0; // only accept same host, path-extensions
	static private int HOST = 1; // accept any URIs from same host
	static private int DOMAIN = 2; // accept any URIs from same domain
 
	private int extensionMode = PATH;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		UURI u = null;
		if(o instanceof UURI) {
			u = (UURI)o;
		} else if ( o instanceof CrawlURI) {
			u = ((CrawlURI)o).getUURI();
		}
		if(u==null) {
			return false;
		}
		Iterator iter = controller.getScope().getSeeds().iterator();
		while(iter.hasNext()) {
			UURI s = (UURI)iter.next();
			if(s.getUri().getHost().equals(u.getUri().getHost())) {
				// hosts match
				if (extensionMode == PATH) {
					if(s.getUri().getPath().regionMatches(0,u.getUri().getPath(),0,s.getUri().getPath().lastIndexOf('/'))) {
						// matches up to last '/'
						return true;
					}  else {
						// no match; try next seed
						continue;
					}
				} // else extensionMode == HOST or DOMAIN, match is good enough
				return true;
			}
			if (extensionMode == DOMAIN) {
				// might be a close-enough match
				String seedDomain = s.getUri().getHost();
				// strip www[#]
				seedDomain = seedDomain.replaceFirst("^www\\d*","");
				String candidateDomain = u.getUri().getHost();
				if (candidateDomain==null) {
					// either an opaque, unfetchable, or unparseable URI
					continue;
				}
				if(seedDomain.regionMatches(0,candidateDomain,candidateDomain.length()-seedDomain.length(),seedDomain.length())) {
					// domain suffix congruence
					return true;
				} // else keep trying other seeds
			}
		}
		// if none found, fail
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize()
	 */
	public void initialize(CrawlController c) {
		// TODO Auto-generated method stub
		super.initialize(c);
		controller = c;
		String mode = getStringAt("@mode");
		if(mode==null || "path".equals(mode)) {
			// default
			return;
		}
		if("host".equals(mode)) {
			extensionMode = HOST;
		}
		if("domain".equals(mode)) {
			extensionMode = DOMAIN;
		}
	}


}
