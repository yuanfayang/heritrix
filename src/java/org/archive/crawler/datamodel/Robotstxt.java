/* Robots.java
 *
 * $Id$
 *
 * Created Sep 1, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing 'robots.txt' format directives, into a list
 * of named user-agents and map from user-agents to disallowed paths. 
 */
public class Robotstxt {
    public static boolean parse(BufferedReader reader,
            final LinkedList<String> userAgents, 
            final Map<String,List<String>> disallows)
    throws IOException {
        boolean hasErrors = false;
        String read;
        // current is the disallowed paths for the preceding User-Agent(s)
        ArrayList<String> current = null;
        // whether a non-'User-Agent' directive has been encountered
        boolean hasDirectivesYet = false; 
        String catchall = null;
        while (reader != null) {
            do {
                read = reader.readLine();
                // Skip comments & blanks
            } while ((read != null) && ((read = read.trim()).startsWith("#") ||
                read.length() == 0));
            if (read == null) {
                reader.close();
                reader = null;
            } else {
                int commentIndex = read.indexOf("#");
                if (commentIndex > -1) {
                    // Strip trailing comment
                    read = read.substring(0, commentIndex);
                }
                read = read.trim();
                if (read.matches("(?i)^User-agent:.*")) {
                    String ua = read.substring(11).trim().toLowerCase();
                    if (current == null || hasDirectivesYet ) {
                        // only create new rules-list if necessary
                        // otherwise share with previous user-agent
                        current = new ArrayList<String>();
                        hasDirectivesYet = false; 
                    }
                    if (ua.equals("*")) {
                        ua = "";
                        catchall = ua;
                    } else {
                        userAgents.addLast(ua);
                    }
                    disallows.put(ua, current);
                    continue;
                }
                if (read.matches("(?i)Disallow:.*")) {
                    if (current == null) {
                        // buggy robots.txt
                        hasErrors = true;
                        continue;
                    }
                    String path = read.substring(9).trim();
                    current.add(path);
                    hasDirectivesYet = true; 
                    continue;
                }
                if (read.matches("(?i)Crawl-delay:.*")) {
                    if (current == null) {
                        // buggy robots.txt
                        hasErrors = true;
                        continue;
                    }
                    // consider a crawl-delay, even though we don't 
                    // yet understand it, as sufficient to end a 
                    // grouping of User-Agent lines
                    hasDirectivesYet = true;
                    // TODO: understand/save/respect 'Crawl-Delay' 
                    continue;
                }
                if (read.matches("(?i)Allow:.*")) {
                    if (current == null) {
                        // buggy robots.txt
                        hasErrors = true;
                        continue;
                    }
                    // consider an Allow, even though we don't 
                    // yet understand it, as sufficient to end a 
                    // grouping of User-Agent lines
                    hasDirectivesYet = true;
                    // TODO: understand/save/respect 'Allow' 
                    continue;
                }
                // unknown line; do nothing for now
            }
        }

        if (catchall != null) {
            userAgents.addLast(catchall);
        }
        return hasErrors;
    }

    /**
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    }
}
