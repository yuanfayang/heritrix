/* SURT
*
* $Id$
*
* Created on Jul 16, 2004
*
* Copyright (C) 2004 Internet Archive.
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
package org.archive.util;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sort-friendly Reversible URI Transform
 * 
 * Converts URIs of the form:
 * 
 *   scheme://userinfo@domain.tld:port/path?query#fragment
 * 
 * ...into...
 * 
 *   scheme://#tld,domain,:port@userinfo#/path?query#fragment
 * 
 * The '#' characters serve as an unambiguous notice that the so-called 
 * 'authority' portion of the URI ([userinfo@]host[:port] in http URIs) has 
 * been transformed; the commas prevent confusion with regular hostnames.
 * 
 * This remedies the 'problem' with standard URIs that the host portion of a 
 * regular URI, with its dotted-domains, is actually in reverse order from 
 * the natural hierarchy that's usually helpful for grouping and sorting.
 * 
 * @author gojomo
 */
public class SURT {
    static String DOT = ".";
    static String BEGIN_TRANSFORMED_AUTHORITY = "#";
    static String TRANSFORMED_HOST_DELIM = ",";
    static String END_TRANSFORMED_AUTHORITY = "#";
    
    // 1: scheme://
    // 2: userinfo (if present)
    // 3: @ (if present)
    // 4: host
    // 5: :port
    // 6: path
    static Pattern URI_SPLITTER = Pattern.compile(
            "^(\\w+://)(?:(\\S+?)(@))?(\\S+?)(:\\d+)?(/\\S*)?$");
    
    public static String transform(String s) {
        Matcher m = URI_SPLITTER.matcher(s);
        if(!m.matches()) {
            // not an authority-based URI scheme; return unchanged
            return s;
        }
        String scheme = m.group(1);
        String host = m.group(4);
        
        String reorderedHost = "";
        StringTokenizer stk = new StringTokenizer(host,DOT);
        while(stk.hasMoreTokens()) {
            reorderedHost = stk.nextToken() + TRANSFORMED_HOST_DELIM  + reorderedHost;
        }
        String port = emptyIfNull(m.group(5));
        String userinfo = emptyIfNull(m.group(2));
        String at = emptyIfNull(m.group(3));
        String path = emptyIfNull(m.group(6)); 
        return scheme + BEGIN_TRANSFORMED_AUTHORITY + reorderedHost 
            + port + at
            + userinfo + END_TRANSFORMED_AUTHORITY 
            + path;
    }
    
    private static String emptyIfNull(String string) {
        return string == null ? "" : string;
    }
}
