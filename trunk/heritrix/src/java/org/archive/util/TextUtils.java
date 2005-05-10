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
 */
package org.archive.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

public class TextUtils {
    private static final String FIRSTWORD = "^([^\\s]*).*$";
    
    /** PatternMatcherRecycler objects with reusable Pattern and Matcher
     * instaces, indexed on pattern string.
     * Profiling has this Map growing to ~18 elements total circa
     * 1.3.0 Heritrix.
     */
    private final static ConcurrentReaderHashMap recyclers =
        new ConcurrentReaderHashMap(50);

    /**
     * Get a matcher object for a precompiled regex pattern.
     * This method tries to reuse Matcher objects for efficiency.
     * 
     * This method is a hotspot frequently accessed.
     *
     * @param pattern the string pattern to use
     * @param input the character sequence the matcher should be using
     * @return a matcher object loaded with the submitted character sequence
     */
    public static Matcher getMatcher(String pattern, CharSequence input) {
        return getRecycler(pattern).getMatcher(input);
    }

    /**
     * Get a preexisting PatternMatcherRecycler for the given String pattern, 
     * or create (and remember) a new one if necessary. 
     * 
     * @param pattern String pattern 
     */
    private static PatternMatcherRecycler getRecycler(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("String 'pattern' must not be null");
        }
        PatternMatcherRecycler pmr = (PatternMatcherRecycler)recyclers.get(pattern);
        if (pmr == null) {
            /* harmless timing issue here:
             * ---> <---
             * if another thread sneaks in and beats this one to 
             * create and populate recyclers map, only effect is that
             * a redundant PatternMatcherRecycler is created and 
             * clobbers the first
             */
            Pattern p = Pattern.compile(pattern);
            pmr = new PatternMatcherRecycler(p);
            recyclers.put(pattern, pmr);
        }
        return pmr; 
    }

    /**
     * Use this method to indicate that you are finnished with a Matcher object
     * so that it can be recycled. It is up to the user to make sure that this
     * object really isn't used anymore. If used after it is marked as freed
     * behaviour is unknown since the Matcher object is not thread safe.
     * 
     * @param m the Matcher object that is no longer needed.
     */
    public static void freeMatcher(Matcher m) {
        getRecycler(m.pattern().pattern()).freeMatcher(m);
    }

    /**
     * Utility method using a precompiled pattern instead of using the
     * replaceAll method of the String class. This method will also be reusing
     * Matcher objects.
     * 
     * @see java.util.regex.Pattern
     * @param pattern precompiled Pattern to match against
     * @param input the character sequence to check
     * @param replacement the String to substitute every match with
     * @return the String with all the matches substituted
     */
    public static String replaceAll(
            String pattern, CharSequence input, String replacement) {
        Matcher m = getMatcher(pattern, input);
        String res = m.replaceAll(replacement);
        freeMatcher(m);
        return res;
    }

    /**
     * Utility method using a precompiled pattern instead of using the
     * replaceFirst method of the String class. This method will also be reusing
     * Matcher objects.
     * 
     * @see java.util.regex.Pattern
     * @param pattern precompiled Pattern to match against
     * @param input the character sequence to check
     * @param replacement the String to substitute the first match with
     * @return the String with the first match substituted
     */
    public static String replaceFirst(
            String pattern, CharSequence input, String replacement) {
        Matcher m = getMatcher(pattern, input);
        String res = m.replaceFirst(replacement);
        freeMatcher(m);
        return res;
    }

    /**
     * Utility method using a precompiled pattern instead of using the matches
     * method of the String class. This method will also be reusing Matcher
     * objects.
     * 
     * @see java.util.regex.Pattern
     * @param pattern precompiled Pattern to match against
     * @param input the character sequence to check
     * @return true if character sequence matches
     */
    public static boolean matches(String pattern, CharSequence input) {
        Matcher m = getMatcher(pattern, input);
        boolean res = m.matches();
        freeMatcher(m);
        return res;
    }

    /**
     * Utility method using a precompiled pattern instead of using the split
     * method of the String class.
     * 
     * @see java.util.regex.Pattern
     * @param pattern precompiled Pattern to split by
     * @param input the character sequence to split
     * @return array of Strings split by pattern
     */
    public static String[] split(String pattern, CharSequence input) {
        return getRecycler(pattern).getPattern().split(input); 
    }
    
    /**
     * @param s String to find first word in (Words are delimited by
     * whitespace).
     * @return First word in the passed string else null if no word found.
     */
    public static String getFirstWord(String s) {
        Matcher m = getMatcher(FIRSTWORD, s);
        return (m != null && m.matches())? m.group(1): null;
    }

    /**
     * Escapes a string so that it can be passed as an argument to a javscript
     * in a JSP page. This method takes a string and returns the same string
     * with any single quote escaped by prepending the character with a
     * backslash. Linebreaks are also replaced with '\n'.
     * 
     * @param s The string to escape
     * @return The same string escaped.
     */
    public static String escapeForJavascript(String s) {
        if(s.indexOf('\'') < 0 && s.indexOf('\n') < 0){
            return s;
        }
        StringBuffer buffer = new StringBuffer(s.length() + 10);
        char[] characters = s.toCharArray();
        for(int j=0 ; j<characters.length ; j++){
            if(characters[j] == '\''){
                buffer.append('\\');
            } else if(characters[j]=='\n') {
                buffer.append("\\n");
            }
            buffer.append(characters[j]);
        }
        return buffer.toString();
    }
    
    /**
     * Escapes a string so that it can be placed inside XML/HTML attribute.
     * Replaces ampersand, less-than, greater-than, single-quote, and 
     * double-quote with escaped versions. 
     * 
     * @param s The string to escape
     * @return The same string escaped.
     */
    public static String escapeForMarkupAttribute(String s) {
        // TODO: do this in a single pass instead of creating 5 junk strings
        String escaped = s.replaceAll("&","&amp;");
        escaped = escaped.replaceAll("<","&lt;");
        escaped = escaped.replaceAll(">","&gt;");
        escaped = escaped.replaceAll("\'","&apos;");
        escaped = escaped.replaceAll("\"","&quot;");
        return escaped; 
    }

    /**
     * @param message Message to put at top of the string returned. May be
     * null.
     * @param e Exception to write into a string.
     * @return Return formatted string made of passed message and stack trace
     * of passed exception.
     */
    public static String exceptionToString(String  message, Throwable e) {
        StringWriter sw = new StringWriter();
        if (message == null || message.length() == 0) {
            sw.write(message);
            sw.write("\n");
        }
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}