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
 * FetchStatusCodes.java
 * Created on Jun 19, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * Constant flag codes to be used, in lieu of per-protocol
 * codes (like HTTP's 200, 404, etc.), when network/internal/
 * out-of-band conditions occur. 
 * 
 * The URISelector may use such codes, along with user-configured
 * options, to determine whether, when, and how many times
 * a CrawlURI might be reattempted.
 * 
 * @author gojomo
 *
 */
public interface FetchStatusCodes {
	public static final int S_UNATTEMPTED = 0;
	public static final int S_DOMAIN_UNRESOLVABLE = -1;
	public static final int S_CONNECT_FAILED = -2;
	public static final int S_CONNECT_LOST = -3;
	public static final int S_TIMEOUT = -4;
	public static final int S_RUNTIME_EXCEPTION = -5;
	public static final int S_PREREQUISITE_FAILURE = -6;
	public static final int S_UNFETCHABLE_URI = -7;
	public static final int S_TOO_MANY_RETRIES = -8;

	public static final int S_SERIOUS_ERROR = -3000;
	public static final int S_DEFERRED = -50;

	public static final int S_ROBOTS_PRECLUDED = -9998;
	public static final int S_DEEMED_CHAFF = -4000;
	public static final int S_TOO_MANY_LINK_HOPS = -4001;
	public static final int S_TOO_MANY_EMBED_HOPS = -4002;
	public static final int S_OUT_OF_SCOPE = -5000;
	
	public static final int S_DNS_SUCCESS = 1;

}


