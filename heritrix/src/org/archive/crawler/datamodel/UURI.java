/*
 * URI.java
 * Created on Apr 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Usable URI: a legal URI for our purposes.
 * 
 * These instances will always have been normalized 
 * (massaged in ways that by spec and in practice,
 * do not change the URI's meaning or function) and
 * rehabilitated (patched in riskless or necessary
 * ways to be legal, eg escaping spaces). 
 * 
 * @author gojomo
 *
 */
public class UURI {
	private static Logger logger = Logger.getLogger("org.archive.crawler.datamodel.UURI");

	java.net.URI uri;
	
	public static UURI createUURI(String s) {
		URI u;
		try {
			u = new URI(normalize(s));
			return new UURI(u);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @param u
	 */
	public UURI(URI u) {
		uri = u;
	}

	/**
	 * Return a "normalized" String for the given String.
	 * This is NOT the same as Alexa's canonicalization.
	 * 
	 * Normalization cleans a URI to the maximum extent
	 * possible without regard to what it would return
	 * if fetched, or any special-casing based on past 
	 * observed behavior.
	 * 
	 * For example, the URI scheme is case-flattened, 
	 * hostnames are case-flattened, default ports are
	 * removed, and path-info is regularized. 
	 * 
	 * @param u
	 * @return
	 */
	 public static String normalize(String u) throws URISyntaxException {
		return normalize(u,null);
	}
	
	/**
	 * Normalize and derelativize
	 * 
	 * @param s absolute or relative URI string
	 * @param parent URI to use for derelativizing; may be null
	 * @return String
	 */
	public static String normalize(String s, URI parent)
		throws URISyntaxException {
		// TODO: stop creating temporary instances like a drunken sailor
		String es = patchEscape(s);
		URI u = new URI(es);
		if (!u.isAbsolute()) {
			u = parent.resolve(es);
		}
		if (u.getSchemeSpecificPart().startsWith("/")) {
			// hierarchical URI
			if ("".equals(u.getPath())) {
				u = u.resolve("/"); // ensure root URLs end with '/'
			}
			u = u.normalize(); // factor out path cruft
			String canonizedAuthority = u.getAuthority();
			if(canonizedAuthority==null) {
				logger.warning("bad URI: "+s+" relative to "+parent);
				return null;
			}
		
			// TODO fix the fact that this might clobber case-sensitive user-info
			if (u.getScheme().equals("http")) {
				// case-flatten host, remove default port
				canonizedAuthority = canonizedAuthority.toLowerCase();
				if (canonizedAuthority.endsWith(":80")) {
					canonizedAuthority =
						canonizedAuthority.substring(
							0,
							canonizedAuthority.length() - 3);
				}
			} else if (u.getScheme().equals("https")) {
				// case-flatten host, remove default port
				canonizedAuthority = canonizedAuthority.toLowerCase();
				if (canonizedAuthority.endsWith(":443")) {
					canonizedAuthority =
						canonizedAuthority.substring(
							0,
							canonizedAuthority.length() - 4);
				}
			}
			u = new URI(u.getScheme().toLowerCase(), // case-flatten scheme
			            canonizedAuthority, // case and port flatten
			            u.getPath(), // leave alone
			            u.getQuery(), // leave alone
		                null); // drop fragment
		} else {
			// opaque URI
			u = new URI(u.getScheme().toLowerCase(), // case-flatten scheme
			            u.getSchemeSpecificPart(), // leave alone
		                null); // drop fragment
		}

		try {
			return u.toASCIIString();
		} catch (NullPointerException npe) {
			throw new URISyntaxException(u.toString(),npe.toString());
		}
	}
	
	/** apply URI escaping where necessary
	 * 
	 * @param s
	 * @return
	 */
	private static String patchEscape(String s) {
		// in a perfect world, s would already be escaped
		// but it may only be partially escaped, so patch it 
		// up where necessary

		// for now, just patch spaces -- but in the
		// future, it could make sense to look for any
		// of the rfc2396 'delims' or 'unwise' characters
		// and escape them, as well as other special 
		// handling of '%' and '+'
		if (s.indexOf(" ") < 0)
			return s;

		return s.replaceAll(" ", "%20");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "UURI<"+uri+">";
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		if(! (arg0 instanceof UURI)) {
			return false;
		}
		return uri.equals(((UURI)arg0).getUri());
	}

	/**
	 * @return
	 */
	public URI getUri() {
		return uri;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return uri.hashCode();
	}

	/**
	 * @param string
	 * @param uri
	 * @return
	 */
	public static UURI createUURI(String string, URI uri) {
		try {
			return createUURI(normalize(string,uri));
		} catch (URISyntaxException e) {
			logger.info("BAD URI: "+string+" from "+uri);
			return null;
		}
	}

}
