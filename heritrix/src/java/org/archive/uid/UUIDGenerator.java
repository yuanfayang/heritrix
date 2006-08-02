/* $Id$
 *
 * Created on July 27th, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
 */
package org.archive.uid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Generates <a href="http://en.wikipedia.org/wiki/UUID">UUID</a>s, using
 * {@link java.util.UUID}, formatted as URIs.
 * @author stack
 * @version $Revision$ $Date$
 * @see <a href="http://ietf.org/rfc/rfc4122.txt">RFC4122</a>
 */
class UUIDGenerator implements Generator {
	private static final String SCHEME = "uuri";
	private static final String SCHEME_COLON = SCHEME + ":";
	private static final int UUID_WIDTH = 36;
	
	UUIDGenerator() {
		super();
	}

	public synchronized URI qualifyRecordID(URI recordId,
			final Map<String, String> qualifiers)
	throws URISyntaxException {
		if (qualifiers == null || qualifiers.size() == 0) {
			throw new IllegalArgumentException("Cannot pass null or empty " +
					"qualifiers");
		}
		if (!recordId.getScheme().toLowerCase().equals(SCHEME)) {
			throw new URISyntaxException(recordId.toString(), "Wrong scheme");
		}
		String part = recordId.getSchemeSpecificPart();
		if (part.length() < UUID_WIDTH) {
			throw new URISyntaxException(recordId.toString(), "Too short to " +
				"be a UURI URI");
		}

		int index = part.indexOf(";");
		if (index >= 0 && index < UUID_WIDTH) {
			throw new URISyntaxException(recordId.toString(), "';' before " +
				"position " + UUID_WIDTH);
		}
		String uuid = null;
		Map<String, String> newQualifiers = null;
		if (index < 0) {
			uuid = part;
			newQualifiers = qualifiers;
		} else {
			uuid = part.substring(0, index);
			String qSuffix = part.substring(index + 1);
			Map<String, String> existingQualifiers = parseSuffix(qSuffix);
			if (existingQualifiers == null || existingQualifiers.size() <= 0) {
				throw new URISyntaxException(recordId.toString(), "Failed " +
						"parse of qualifiers suffix");
			}
			// Merge with new qualifiers be careful to ensure that new
			// qualifiers do not already exist in existing qualifiers.
			boolean different = false;
			newQualifiers = existingQualifiers;
			for (final Iterator<String> i = qualifiers.keySet().iterator();
					i.hasNext();) {
				String k = i.next();
				String v = qualifiers.get(k);
				if (newQualifiers.containsKey(k)
						&& newQualifiers.get(k).equals(v)) {
					continue;
				}
				different = true;
				newQualifiers.put(k, v);
			}
			if (!different) {
				throw new URISyntaxException(recordId.toString() + "+" +
					qualifiers.toString(), "Does not make a new unique URI");
			}
		}
			
		return qualifyRecordID(uuid, newQualifiers);
	}
	
	private Map<String, String> parseSuffix(final String s)
	throws URISyntaxException {
		if (s == null || s.length() == 0) {
			return (Map<String, String>)null;
		}
		Map<String, String> q = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		String key = null;
		String value = null;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '=') {
				key = sb.toString();
				sb.setLength(0);
			} else if (c == ';') {
				value = sb.toString();
			} else if (i == s.length() - 1) {
				sb.append(c);
				value = sb.toString();
			} else {
				sb.append(c);
			}
			if (key != null && value != null) {
				q.put(key, value);
				key = null;
				value = null;
			}
		}
		return q;
	}

	private String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	public synchronized URI getRecordID() throws URISyntaxException {
		return new URI(SCHEME_COLON + getUUID());
	}
	
	public synchronized URI getQualifiedRecordID(
			final String key, final String value)
	throws URISyntaxException {
		StringBuilder sb = new StringBuilder(SCHEME_COLON);
		sb.append(getUUID());
		return new URI(appendQualifier(sb, key, value).toString());
	}
	
	public synchronized URI getQualifiedRecordID(
			final Map<String, String> qualifiers)
	throws URISyntaxException {
		return qualifyRecordID(getUUID(), qualifiers);
	}
	
	private URI qualifyRecordID(final String uuid,
			final Map<String, String> qualifiers)
	throws URISyntaxException {
		StringBuilder sb = new StringBuilder(SCHEME_COLON);
		sb.append(uuid);
		for (final Iterator<String> i = qualifiers.keySet().iterator();
				i.hasNext();) {
			String key = i.next();
			if (key == null || key.length() <= 0) {
				throw new URISyntaxException(qualifiers.toString(),
				    "Fragment key is empty");
			}
			String value = qualifiers.get(key);
			if (value == null || value.length() <= 0) {
				throw new URISyntaxException(qualifiers.toString(),
				    "Fragment value is empty");
			}
			sb = appendQualifier(sb, key, value);
		}
		return new URI(sb.toString());
	}
	
	private StringBuilder appendQualifier(final StringBuilder sb,
			final String key, final String value) {
		if (key == null || key.length() <= 0 || value == null ||
				value.length() <= 0) {
			throw new IllegalArgumentException("Cannot pass null key " +
				"or value qualifications. Key: " + key + ", Value: " + value);
		}
		sb.append(';');
		sb.append(key);
		sb.append('=');
		sb.append(value);
		return sb;
	}
}
