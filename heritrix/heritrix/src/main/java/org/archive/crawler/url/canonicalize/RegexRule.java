/* RegexRule
 * 
 * Created on Oct 6, 2004
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

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * General conversion rule.
 * @author stack
 * @version $Date$, $Revision$
 */
public class RegexRule
extends BaseRule {

    private static final long serialVersionUID = -3L;

    protected static Logger logger =
        Logger.getLogger(BaseRule.class.getName());

//    private static final String DESCRIPTION = "General regex rule. " +
//        "Specify a matching regex and a format string used outputting" +
//        " result if a match was found.  If problem compiling regex or" +
//        " interpreting format, problem is logged, and this rule does" +
//        " nothing.  See User Manual for example usage.";

    
    /**
     * The regular expression to use to match.
     */
    final public static Key<Pattern> REGEX = Key.make(Pattern.compile("(.*)"));    
    
    
    /**
     * The format string to use when a match is found.
     */
    final public static Key<String> FORMAT = Key.make("${1}");

    //final public static Key<String> COMMENT = Key.make("");
    
    static {
        KeyManager.addKeys(RegexRule.class);
    }
    
    public RegexRule() {
    }
    

    public String canonicalize(String url, StateProvider context) {
        Pattern pattern = context.get(this, REGEX);
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            return url;
        }
        
        String format = context.get(this, FORMAT);
        StringBuffer buffer = new StringBuffer(url.length() * 2);
        format(matcher, format, buffer);
        return buffer.toString();
    }
    
    /**
     * @param matcher Matched matcher.
     * @param format Output format specifier.
     * @param buffer Buffer to append output to.
     */
    protected void format(Matcher matcher, String format,
            StringBuffer buffer) {
        for (int i = 0; i < format.length(); i++) {
            switch(format.charAt(i)) {
                case '\\':
                    if ((i + 1) < format.length() &&
                            format.charAt(i + 1) == '$') {
                        // Don't write the escape character in output.
                        continue;
                    }
                    
                case '$':
                    // Check to see if its not been escaped.
                    if (i == 0 || (i > 0 && (format.charAt(i - 1) != '\\'))) {
                        // Looks like we have a matching group specifier in
                        // our format string, something like '$2' or '${2}'.
                        int start = i + 1;
                        boolean curlyBraceStart = false;
                        if (format.charAt(start) == '{') {
                            start++;
                            curlyBraceStart = true;
                        }
                        int j = start;
                        for (; j < format.length() &&
                                Character.isDigit(format.charAt(j)); j++) {
                            // While a digit, increment.
                        }
                        if (j > start) {
                            int groupIndex = Integer.
                                parseInt(format.substring(start, j));
                            if (groupIndex >= 0 && groupIndex < 256) {
                                String g = null;
                                try {
                                    g = matcher.group(groupIndex);
                                } catch (IndexOutOfBoundsException e) {
                                    logger.warning("IndexOutOfBoundsException" +
                                        " getting group " + groupIndex +
                                        " from " + matcher.group(0) +
                                        " with format of " + format);
                                }
                                if (g != null) {
                                    buffer.append(g);
                                }
                                // Skip closing curly bracket if one.
                                if (curlyBraceStart &&
                                        format.charAt(j) == '}') {
                                    j++;
                                }
                                // Update the loop index so that we skip over
                                // the ${x} group item.
                                i = (j - 1);
                                // Don't fall through to the default.
                                continue;
                            }
                        }
                        
                    }
                    // Let fall through to default rule.  The '$' was escaped.
                    
                default:
                    buffer.append(format.charAt(i));
            }
        }
    }

}
