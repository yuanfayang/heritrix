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
import java.util.regex.Pattern;

import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

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
	// for now, consider URIs too long for IE as illegal
	// TODO: move this policy elsewhere
	private static int DEFAULT_MAX_URI_LENGTH = 2083;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.datamodel.UURI");

	protected java.net.URI uri;
	protected String uriString;
	
	public static UURI createUURI(String s) throws URISyntaxException {
		return new UURI(normalize(s));
	}
	
	/**
	 * @param u
	 */
	private UURI(URI u) throws URISyntaxException {
		uri = u;
		try {
			uriString = u.toASCIIString();
		} catch (NullPointerException npe) {
			DevUtils.warnHandle(npe,"URI problem with "+u);
			throw new URISyntaxException(u.toString(),"URI.encode NPE");
		}
		if (uriString.length()>DEFAULT_MAX_URI_LENGTH) {
			throw new URISyntaxException(uriString,"Too Long");
		}
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
	 public static URI normalize(String u) throws URISyntaxException {
		return normalize(u,null);
	}
	
	static final Pattern DOTDOT = Pattern.compile("^(/\\.\\.)+");

	/**
	 * Normalize and derelativize
	 * 
	 * @param s absolute or relative URI string
	 * @param parent URI to use for derelativizing; may be null
	 * @return String
	 */
	public static URI normalize(String s, URI parent)
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
		if (u.getRawSchemeSpecificPart().startsWith("/")) {
			// hierarchical URI
			u = u.normalize(); // factor out path cruft, according to official spec
			// now, go further and eliminate extra '..' segments
			//String fixedPath = u.getRawPath().replaceFirst("^(/\\.\\.)+","");
			String fixedPath = TextUtils.replaceFirst(DOTDOT, u.getPath(), "");
			if ("".equals(fixedPath)) {
//				ensure root URLs end with '/'
				fixedPath = "/"; 
			}
			String canonizedAuthority = u.getAuthority();
			if(canonizedAuthority==null) {
				//logger.warning("bad URI: "+s+" relative to "+parent);
				//return null;
				throw new URISyntaxException(s,"uninterpretable relative to "+parent);
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

		return u;
	}
	
	static final Pattern NBSP = Pattern.compile("\\xA0");
	static final Pattern SPACE = Pattern.compile(" ");
	static final Pattern PIPE = Pattern.compile("\\|");
	static final Pattern CIRCUMFLEX = Pattern.compile("\\^");
	static final Pattern QUOT = Pattern.compile("\"");
	static final Pattern SQUOT = Pattern.compile("'");
	static final Pattern APOSTROPH = Pattern.compile("`");
	static final Pattern LSQRBRACKET = Pattern.compile("\\[");
	static final Pattern RSQRBRACKET = Pattern.compile("\\]");
	static final Pattern LCURBRACKET = Pattern.compile("\\{");
	static final Pattern RCURBRACKET = Pattern.compile("\\}");
	static final Pattern BACKSLASH = Pattern.compile("\\\\");
	static final Pattern IMPROPERESC = Pattern.compile("%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))");
	static final Pattern NEWLINE = Pattern.compile("\n+|\r+");
	
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
		//s = s.replaceAll("\\xA0"," ");
		s = TextUtils.replaceAll(NBSP, s, " ");
		// strip ends whitespaces
		s = s.trim();
		// patch spaces
		if (s.indexOf(" ") >= 0) {
			//s = s.replaceAll(" ", "%20");
			s = TextUtils.replaceAll(SPACE, s, "%20");
		}
		// escape  | ^ " ' ` [ ] { } \
		// (IE actually sends these unescaped, but they can't
		// be put into a java.net.URI instance)
		if (s.indexOf("|") >= 0) {
			//s = s.replaceAll("\\|","%7C");
			s = TextUtils.replaceAll(PIPE, s, "%7C");
		}
		if (s.indexOf("^") >= 0) {
			//s = s.replaceAll("\\^","%5E");
			s = TextUtils.replaceAll(CIRCUMFLEX, s, "%5E");
		}
		if (s.indexOf("\"") >= 0) {
			//s = s.replaceAll("\"","%22");
			s = TextUtils.replaceAll(QUOT, s, "%22");
		}
		if (s.indexOf("'") >= 0) {
			//s = s.replaceAll("'","%27");
			s = TextUtils.replaceAll(SQUOT, s, "%27");
		}
		if (s.indexOf("`") >= 0) {
			//s = s.replaceAll("`","%60");
			s = TextUtils.replaceAll(APOSTROPH, s, "%60");
		}
		if (s.indexOf("[") >= 0) {
			//s = s.replaceAll("\\[","%5B");
			s = TextUtils.replaceAll(LSQRBRACKET, s, "%5B");
		}
		if (s.indexOf("]") >= 0) {
			//s = s.replaceAll("\\]","%5D");
			s = TextUtils.replaceAll(RSQRBRACKET, s, "%5D");
		}
		if (s.indexOf("{") >= 0) {
			//s = s.replaceAll("\\{","%7B");
			s = TextUtils.replaceAll(LCURBRACKET, s, "%7B");
		}
		if (s.indexOf("}") >= 0) {
			//s = s.replaceAll("\\}","%7D");
			s = TextUtils.replaceAll(RCURBRACKET, s, "%7D");
		}
		if (s.indexOf("\\") >= 0) {
			//s = s.replaceAll("\\\\","%5C");
			s = TextUtils.replaceAll(BACKSLASH, s, "%5C");
		}
		// escape improper escape codes; eg any '%' followed
		// by non-hex-digits or 
		//s = s.replaceAll("%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))","%25$1");
		s = TextUtils.replaceAll(IMPROPERESC, s, "%25$1");
		// twice just to be sure (actually, to handle multiple %% in a row)
		//s = s.replaceAll("%((?:[^\\p{XDigit}])|(?:.[^\\p{XDigit}])|(?:\\z))","%25$1");
		s = TextUtils.replaceAll(IMPROPERESC, s, "%25$1");
		// kill newlines etc
		//s = s.replaceAll("\n+|\r+","");
		s = TextUtils.replaceAll(NEWLINE, s, "");
		
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
		return new UURI(normalize(string,uri));
	}

	static final Pattern UNUSABLE_SCHEMES = Pattern.compile("(?i)^(javascript:)|(aim:)");
	/**
	 * @param string
	 * @return
	 */
	private static boolean isUnusableScheme(String string) {
		if (TextUtils.matches(UNUSABLE_SCHEMES, string)) {
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
	 * @return URI
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
