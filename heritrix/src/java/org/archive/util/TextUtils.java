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

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

public class TextUtils {
    /**
     * Upper-bound on Matcher Stacks.
     * Profiling has the size of these Stacks tending upward over
     * the life of a crawl.
     */
    private final static int MAXIMUM_STACK_SIZE = 10;

    /** Reusable precompiled Pattern objects indexed on pattern string.
     * Profiling has this Map growing to ~18 elements total circa
     * 1.3.0 Heritrix.
     */
    private final static ConcurrentReaderHashMap patterns =
        new ConcurrentReaderHashMap(50);

    /** Resuable match objects indexed on pattern string. Each element is a
     * stack of Matcher objects that can be reused.
     */
    private final static ConcurrentReaderHashMap patternMatchers =
        new ConcurrentReaderHashMap(50);

    /**
     * Get a matcher object for a precompiled regex pattern.
     * This method tries to reuse Matcher objects for efficiency.
     * 
     * This method is a hotspot frequently accessed.
     *
     * @param pattern the precompiled Pattern
     * @param input the character sequence the matcher should be using
     * @return a matcher object loaded with the submitted character sequence
     */
    public static Matcher getMatcher(String pattern, CharSequence input) {
        pattern = pattern == null ? "" : pattern;
        input = input == null ? "" : input;
        Matcher matcher = null;
        Pattern p = (Pattern)patterns.get(pattern);
        if (p == null) {
            p = Pattern.compile(pattern);
            patterns.put(pattern, p);
            patternMatchers.put(pattern, new Stack());
            matcher = p.matcher(input);
        } else {
            try {
                Stack s = (Stack)patternMatchers.get(pattern);
                if (s != null) {
                    matcher = (Matcher)s.pop();
                    matcher.reset(input);
                }
            } catch(EmptyStackException e) {
                // The finally clause will 'recover' from this exception.
            } finally {
                if (matcher == null) {
                    matcher = ((Pattern)patterns.get(pattern)).matcher(input);
                }
            }
        }
        return matcher;
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
        Stack matchers;
        if((matchers = (Stack) patternMatchers.get(m.pattern().pattern()))
                == null == matchers.size() > MAXIMUM_STACK_SIZE) {
            // This matcher wasn't created by any pattern in the map, throw it
            // away
            return;
        }
        matchers.push(m);
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
        Pattern p = (Pattern) patterns.get(pattern);
        if (p == null) {
            p = Pattern.compile(pattern);
            patterns.put(pattern, p);
            patternMatchers.put(pattern, new Stack());
        }
        return p.split(input);
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
}
