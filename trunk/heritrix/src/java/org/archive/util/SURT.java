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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Matcher;

/**
 * Sort-friendly URI Reordering Transform.
 * 
 * Converts URIs of the form:
 * 
 *   scheme://userinfo@domain.tld:port/path?query#fragment
 * 
 * ...into...
 * 
 *   scheme://(tld,domain,:port@userinfo)/path?query#fragment
 * 
 * The '(' ')' characters serve as an unambiguous notice that the so-called 
 * 'authority' portion of the URI ([userinfo@]host[:port] in http URIs) has 
 * been transformed; the commas prevent confusion with regular hostnames.
 * 
 * This remedies the 'problem' with standard URIs that the host portion of a 
 * regular URI, with its dotted-domains, is actually in reverse order from 
 * the natural hierarchy that's usually helpful for grouping and sorting.
 * 
 * The value of respecting URI case variance is considered negligible: it
 * is vanishingly rare for case-variance to be meaningful, while URI case-
 * variance often arises from people's confusion or sloppiness, and they
 * only correct it insofar as necessary to avoid blatant problems. Thus 
 * SURT form is considered to be flattened to all lowercase, and thus not
 * completely reversible. 
 * 
 * @author gojomo
 */
public class SURT {
    static char DOT = '.';
    static String BEGIN_TRANSFORMED_AUTHORITY = "(";
    static String TRANSFORMED_HOST_DELIM = ",";
    static String END_TRANSFORMED_AUTHORITY = ")";
    
    // 1: scheme://
    // 2: userinfo (if present)
    // 3: @ (if present)
    // 4: host
    // 5: :port
    // 6: path
    static String URI_SPLITTER = 
            "^(\\w+://)(?:([-\\w\\.!~\\*'\\(\\)%;:&=+$,]+?)(@))?(\\S+?)(:\\d+)?(/\\S*)?$";
    
    // RFC2396 
    //       reserved    = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
    //                     "$" | ","
    //       unreserved  = alphanum | mark
    //       mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
    //       userinfo    = *( unreserved | escaped |
    //                     ";" | ":" | "&" | "=" | "+" | "$" | "," )
    //       escaped     = "%" hex hex


    
    
    /**
     * Utility method for creating the SURT form of the URI in the
     * given String.
     * 
     * If it appears a bit convoluted in its approach, note that it was
     * optimized to minimiz object-creation after allocation-sites profiling 
     * indicated this method was a top source of garbage in long-running crawls.
     * 
     * @param s String URI to be converted to SURT form
     * @return SURT form 
     */
    public static String fromURI(String s) {
        Matcher m = TextUtils.getMatcher(URI_SPLITTER,s);
        if(!m.matches()) {
            // not an authority-based URI scheme; return unchanged
            return s;
        }
        // preallocate enough space for SURT form, which includes
        // 3 extra characters ('(', ')', and one more ',' than '.'s
        // in original)
        StringBuffer builder = new StringBuffer(s.length()+3);
        PreJ15Utils.append(builder,s,m.start(1),m.end(1)); // scheme://
        builder.append(BEGIN_TRANSFORMED_AUTHORITY); // '('
        
        int hostSegEnd = m.end(4);
        int hostStart = m.start(4); 
        for(int i = m.end(4)-1; i>=hostStart; i--) {
            if(s.charAt(i-1)!=DOT && i > hostStart) {
                continue;
            }
            PreJ15Utils.append(builder,s,i,hostSegEnd); // rev host segment
            builder.append(TRANSFORMED_HOST_DELIM);     // ','
            hostSegEnd = i-1;
        }

        PreJ15Utils.append(builder,s,m.start(5),m.end(5)); // :port
        PreJ15Utils.append(builder,s,m.start(3),m.end(3)); // at
        PreJ15Utils.append(builder,s,m.start(2),m.end(2)); // userinfo
        builder.append(END_TRANSFORMED_AUTHORITY); // ')'
        PreJ15Utils.append(builder,s,m.start(6),m.end(6)); // path
        for(int i = 0; i < builder.length(); i++) {
            builder.setCharAt(i,Character.toLowerCase(builder.charAt((i))));
        }
        return builder.toString();
    }
    
    private static String emptyIfNull(String string) {
        return string == null ? "" : string;
    }
    
    /**
     * Allow class to be used as a command-line tool for converting 
     * URL lists (or naked host or host/path fragments implied
     * to be HTTP URLs) to SURT form. Lines that cannot be converted
     * are returned unchanged. 
     * 
     *
     * Read from stdin or first file argument. Writes to stdout or 
     * second argument filename
     * 
     * @param args cmd-line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        InputStream in = args.length > 0 ? new BufferedInputStream(
                new FileInputStream(args[0])) : System.in;
        PrintStream out = args.length > 1 ? new PrintStream(
                new BufferedOutputStream(new FileOutputStream(args[1])))
                : System.out;
        BufferedReader br =
            new BufferedReader(new InputStreamReader(in));
        String line;
        while((line = br.readLine())!=null) {
            if(line.indexOf("#")>0) line=line.substring(0,line.indexOf("#"));
            line = line.trim();
            if(line.length()==0) continue;
            line = ArchiveUtils.addImpliedHttpIfNecessary(line);
            out.println(SURT.fromURI(line));
        }
        br.close();
        out.close();
    }
}
