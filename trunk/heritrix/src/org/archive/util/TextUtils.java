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

//	/**
//	 * Implementation of a unix-like 'tail' command
//	 * @param aFileName a file name String
//	 * @return the String representation of at most 10 last lines
//	 * 
//	 * @deprecated Use @link LogReader#tail(String) instead.
//	 */
//	public static String tail(String aFileName) {
//		return LogReader.tail(aFileName, 10);
//	}

//	/**
//	 * Implementation of a unix-like 'tail -n' command
//	 * @param aFileName a file name String
//	 * @param n int number of lines to be returned
//	 * @return the String representation of at most n last lines 
//	 * 
//	 * @deprecated Use @link LogReader#tail(String, int) instead.
//	 */
//	public static String tail(String aFileName, int n) {
//		return LogReader.tail(aFileName, n);
//	}

	
	final static Hashtable patternMatchers = new Hashtable(50); // Resuable match objects
	
	/**
	 * Get a matcher object for a precompiled regex pattern.
	 * This method tries to reuse Matcher objects for efficiency.
	 * 
	 * @param p the precompiled Pattern
	 * @param input the character sequence the matcher should be using
	 * @return a matcher object loaded with the submitted character sequence
	 */
	public static Matcher getMatcher(Pattern p, CharSequence input) {
		Stack matchers;
		if((matchers = (Stack) patternMatchers.get(p)) == null) {
			matchers = new Stack();
			patternMatchers.put(p, matchers);
		}
		Matcher matcher;
		try {
			matcher = (Matcher) matchers.pop();
			matcher.reset(input);
		} catch(EmptyStackException e) {
			matcher = p.matcher(input);
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
		if((matchers = (Stack) patternMatchers.get(m.pattern())) == null) {
			// This matcher wasn't created by any pattern in the map, throw it away
			return;
		}
		matchers.push(m);
	}

	/**
	 * Utility method using a precompiled pattern instead of using the replaceAll method of
	 * the String class. This method will also be reusing Matcher objects.
	 * 
	 * @see java.lang.String#replaceAll
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @param replacement the String to substitute every match with
	 * @return the String with all the matches substituted
	 */
	public static String replaceAll(Pattern p, CharSequence input, String replacement) {
		Stack matchers;
		if((matchers = (Stack) patternMatchers.get(p)) == null) {
			matchers = new Stack();
			patternMatchers.put(p, matchers);
		}
		Matcher matcher;
		try {
			matcher = (Matcher) matchers.pop();
			matcher.reset(input);
		} catch(EmptyStackException e) {
			matcher = p.matcher(input);
		}
		String res = matcher.replaceAll(replacement);
		matchers.push(matcher);
		return res;
	}

	/**
	 * Utility method using a precompiled pattern instead of using the replaceFirst method of
	 * the String class. This method will also be reusing Matcher objects.
	 * 
	 * @see java.lang.String#replaceFirst
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @param replacement the String to substitute the first match with
	 * @return the String with the first match substituted
	 */
	public static String replaceFirst(Pattern p, CharSequence input, String replacement) {
		Stack matchers;
		if((matchers = (Stack) patternMatchers.get(p)) == null) {
			matchers = new Stack();
			patternMatchers.put(p, matchers);
		}
		Matcher matcher;
		try {
			matcher = (Matcher) matchers.pop();
			matcher.reset(input);
		} catch(EmptyStackException e) {
			matcher = p.matcher(input);
		}
		String res = matcher.replaceFirst(replacement);
		matchers.push(matcher);
		return res;
	}

	/**
	 * Utility method using a precompiled pattern instead of using the matches method of
	 * the String class. This method will also be reusing Matcher objects.
	 * 
	 * @see java.lang.String#matches
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @return true if character sequence matches
	 */
	public static boolean matches(Pattern p, CharSequence input) {
		Stack matchers;
		if((matchers = (Stack) patternMatchers.get(p)) == null) {
			matchers = new Stack();
			patternMatchers.put(p, matchers);
		}
		Matcher matcher;
		try {
			matcher = (Matcher) matchers.pop();
			matcher.reset(input);
		} catch(EmptyStackException e) {
			matcher = p.matcher(input);
		}
		boolean res = matcher.matches();
		matchers.push(matcher);
		return res;
	}

	/**
	 * Utility method using a precompiled pattern instead of using the split method of
	 * the String class.
	 * 
	 * @see java.lang.String#split
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to split by
	 * @param input the character sequence to split
	 * @return array of Strings split by pattern
	 */
	public static String[] split(Pattern p, CharSequence input) {
		return p.split(input);
	}
}
