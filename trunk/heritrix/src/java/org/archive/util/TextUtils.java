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
import java.util.Hashtable;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

//    /**
//     * Implementation of a unix-like 'tail' command
//     * @param aFileName a file name String
//     * @return the String representation of at most 10 last lines
//     *
//     * @deprecated Use @link LogReader#tail(String) instead.
//     */
//    public static String tail(String aFileName) {
//    	return LogReader.tail(aFileName, 10);
//    }

//    /**
//     * Implementation of a unix-like 'tail -n' command
//     * @param aFileName a file name String
//     * @param n int number of lines to be returned
//     * @return the String representation of at most n last lines
//     *
//     * @deprecated Use @link LogReader#tail(String, int) instead.
//     */
//    public static String tail(String aFileName, int n) {
//    	return LogReader.tail(aFileName, n);
//    }

    /** Reusable precompiled Pattern objects indexed on pattern string. */
    final static Hashtable patterns = new Hashtable(50);

    /** Resuable match objects indexed on pattern string. Each element is a
     * stack of Matcher objects that can be reused.
     */
    final static Hashtable patternMatchers = new Hashtable(50);

    /**
     * Get a matcher object for a precompiled regex pattern.
     * This method tries to reuse Matcher objects for efficiency.
     *
     * @param p the precompiled Pattern
     * @param input the character sequence the matcher should be using
     * @return a matcher object loaded with the submitted character sequence
     */
    public static Matcher getMatcher(String pattern, CharSequence input) {
        Matcher matcher;
        Pattern p = (Pattern) patterns.get(pattern);
        if (p == null) {
            p = Pattern.compile(pattern);
            patterns.put(pattern, p);
            patternMatchers.put(pattern, new Stack());
            matcher = p.matcher(input);
        } else {
            try {
                matcher = (Matcher) ((Stack)patternMatchers.get(pattern)).pop();
                matcher.reset(input);
            } catch(EmptyStackException e) {
                matcher = ((Pattern)patterns.get(pattern)).matcher(input);
            }
        }
        return matcher;
    }

    /**
     * Use this method to indicate that you are finnished with a Matcher object so
     * that it can be recycled. It is up to the user to make sure that this object really isn't
     * used anymore. If used after it is marked as freed behaviour is unknown because
     * the Matcher object is not thread safe.
     *
     * @param m the Matcher object that is no longer needed.
     */
    public static void freeMatcher(Matcher m) {
    	Stack matchers;
    	if((matchers = (Stack) patternMatchers.get(m.pattern().pattern()))
            == null) {

    		// This matcher wasn't created by any pattern in the map, throw it away
    		return;
    	}
    	matchers.push(m);
    }

    /**
     * Utility method using a precompiled pattern instead of using the replaceAll method of
     * the String class. This method will also be reusing Matcher objects.
     *
     * @see java.util.regex.Pattern
     * @param p precompiled Pattern to match against
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
     * Utility method using a precompiled pattern instead of using the replaceFirst method of
     * the String class. This method will also be reusing Matcher objects.
     *
     * @see java.util.regex.Pattern
     * @param p precompiled Pattern to match against
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
     * Utility method using a precompiled pattern instead of using the matches method of
     * the String class. This method will also be reusing Matcher objects.
     *
     * @see java.util.regex.Pattern
     * @param p precompiled Pattern to match against
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
     * Utility method using a precompiled pattern instead of using the split method of
     * the String class.
     *
     * @see java.util.regex.Pattern
     * @param p precompiled Pattern to split by
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
     * Escapes a string so that it can be passed as an argument to a javscript in
     * a JSP page. This method takes a string and returns the same string
     * with any single quote escaped by prepending the character with a backslash.
     * Linebreaks are also replaced with '\n'.
     * @param s The string to escape
     * @return The same string escaped.
     */
    public static String escapeForJavascript(String s) {
        if(s.indexOf('\'') < 0 ){
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
