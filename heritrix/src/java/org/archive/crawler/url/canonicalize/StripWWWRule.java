/* StripWWWRule
 * 
 * Created on Oct 5, 2004
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
package org.archive.crawler.url.canonicalize;

import java.util.regex.Pattern;



/**
 * Strip any 'www' found on http/https URLs.
 * @author stack
 * @version $Date$, $Revision$
 */
public class StripWWWRule extends BaseRule {
    private static final String DESCRIPTION = "Strip any 'www' found. " +
        "Use this rule to equate 'http://www.archive.org/index.html' and" +
        " 'http://archive.org/index.html'.  The resulting canonicalization" +
        " returns 'http://archive.org/index.html'.  Removes any www's" +
        " found.  Operates on http and https schemes only.";
    
    private static final Pattern REGEX =
        Pattern.compile("^(https?://)(?:www\\.)(.*)$",
            Pattern.CASE_INSENSITIVE);

    public StripWWWRule(String name) {
        super(name, DESCRIPTION);
    }

    public String canonicalize(String url, Object context) {
        return doStripRegexMatch(url, REGEX.matcher(url));
    }
}
