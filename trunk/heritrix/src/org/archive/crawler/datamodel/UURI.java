/*
 * URI.java
 * Created on Apr 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class UURI implements Serializable {
	private static Logger logger = Logger.getLogger("org.archive.crawler.datamodel.UURI");

	protected java.net.URI uri;
	protected String uriString;
	
	public static UURI createUURI(String s) throws URISyntaxException {
		URI u;
		u = new URI(normalize(s));
		return new UURI(u);
	}
	
	/**
	 * @param u
	 */
	private UURI(URI u) {
		uri = u;
		uriString = u.toASCIIString();
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
		
		if (s==null) {
			throw new URISyntaxException("n/a","Is null");
		}
		// TODO: stop creating temporary instances like a drunken sailor
		String es = patchEscape(s);
		URI u = new URI(es);
		if (!u.isAbsolute()) {
			if (parent==null) {
				throw new URISyntaxException(s,"No parent supplied for relative URI.");
			}
			u = parent.resolve(es);
		}
		if (u.getSchemeSpecificPart().startsWith("/")) {
			// hierarchical URI
			u = u.normalize(); // factor out path cruft, according to official spec
			// now, go further and eliminate extra '..' segments
			String fixedPath = u.getRawPath().replaceFirst("^(/\\.\\.)+","");
			if ("".equals(fixedPath)) {
//				ensure root URLs end with '/'
				fixedPath = "/"; 
			}
			String canonizedAuthority = u.getAuthority();
			if(canonizedAuthority==null) {
				logger.warning("bad URI: "+s+" relative to "+parent);
				return null;
			}
		
			// TODO fix the fact that this might clobber case-sensitive user-info
			if (u.getScheme().equals("http")) {
				// case-flatten host, remove default port
				canonizedAuthority = canonizedAuthority.toLowerCase();
				// strip default port
				if (canonizedAuthority.endsWith(":80")) {
					canonizedAuthority =
						canonizedAuthority.substring(
							0,
							canonizedAuthority.length() - 3);
				}
				// chop trailing '.'
				if (canonizedAuthority.endsWith(".")) {
					canonizedAuthority =
						canonizedAuthority.substring(
							0,
							canonizedAuthority.length() - 1);
				}
				// chop leading '.'
				if (canonizedAuthority.startsWith(".")) {
					canonizedAuthority =
						canonizedAuthority.substring(
							1,
							canonizedAuthority.length());
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
			            fixedPath, // leave alone
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

		// replace nbsp with normal spaces (so that they get
		// stripped if at ends, or encoded if in middle)
		s = s.replaceAll("\\xA0"," ");
		// strip ends whitespaces
		s = s.trim();
		// patch spaces
		if (s.indexOf(" ") >= 0) {
			s = s.replaceAll(" ", "%20");
		}
		// escape  | ^ " ' ` [ ] { } \
		// (IE actually sends these unescaped, but they can't
		// be put into a java.net.URI instance)
		if (s.indexOf("|") >= 0) {
			s = s.replaceAll("\\|","%7C");
		}
		if (s.indexOf("^") >= 0) {
			s = s.replaceAll("\\^","%5E");
		}
		if (s.indexOf("\"") >= 0) {
			s = s.replaceAll("\"","%22");
		}
		if (s.indexOf("'") >= 0) {
			s = s.replaceAll("'","%27");
		}
		if (s.indexOf("`") >= 0) {
			s = s.replaceAll("`","%60");
		}
		if (s.indexOf("[") >= 0) {
			s = s.replaceAll("\\[","%5B");
		}
		if (s.indexOf("]") >= 0) {
			s = s.replaceAll("\\]","%5D");
		}
		if (s.indexOf("{") >= 0) {
			s = s.replaceAll("\\{","%7B");
		}
		if (s.indexOf("}") >= 0) {
			s = s.replaceAll("\\}","%7D");
		}
		if (s.indexOf("\\") >= 0) {
			s = s.replaceAll("\\\\","%5C");
		}
		// escape improper escape codes; eg any '%' followed
		// by non-hex-digits or 
		s = s.replaceAll("%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))","%25$1");
		// twice just to be sure (actually, to handle multiple %% in a row)
		s = s.replaceAll("%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))","%25$1");
		// kill newlines etc
		s = s.replaceAll("\n+|\r+","");
		
		return s; 
	}
	
	
	public String toExternalForm(){
		
	return uri.toString();
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
	protected URI getUri() {
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
	public static UURI createUURI(String string, URI uri) throws URISyntaxException {
		return createUURI(normalize(string,uri));
	}

	static Pattern UNUSABLE_SCHEMES = Pattern.compile("(?i)^(javascript:)|(aim:)");
	/**
	 * @param string
	 * @return
	 */
	private static boolean isUnusableScheme(String string) {
		Matcher m = UNUSABLE_SCHEMES.matcher(string);
		if (m.matches()) {
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public String getScheme() {
		return uri.getScheme();
	}

	/**
	 * 
	 */
	public String getPath() {
		return uri.getPath();
	}

	/**
	 * 
	 */
	public String getUriString() {
		return uriString;
	}

	/**
	 * Avoid casual use; java.net.URI may be phased out of crawler
	 * for memory performance reasons
	 * 
	 * @return
	 */
	public URI getRawUri() {
		return getUri();
	}

	/**
	 * @return
	 */
	public String getHost() {
		return uri.getHost();
	}

}
