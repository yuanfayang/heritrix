package org.archive.util;

import java.io.*;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

	/**
	 * Implementation of a unix-like 'tail' command
	 * @param aFileName a file name String
	 * @return the String representation of at most 10 last lines
	 */
	public static String tail(String aFileName) {
		return tail(aFileName, 10);
	}

	/**
	 * Implementation of a unix-like 'tail -n' command
	 * @param aFileName a file name String
	 * @param n int number of lines to be returned
	 * @return the String representation of at most n last lines 
	 */
	public static String tail(String aFileName, int n) {
		int BUFFERSIZE = 1024;
		long pos;
		long endPos;
		long lastPos;
		int numOfLines = 0;
		byte[] buffer = new byte[BUFFERSIZE];
		StringBuffer sb = new StringBuffer();
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(new File(aFileName), "r");
			endPos = raf.length();
			lastPos = endPos;

			// Check for non-empty file 
			// Check for newline at EOF
			if (endPos > 0) {
				byte[] oneByte = new byte[1];
				raf.seek(endPos - 1);
				raf.read(oneByte);
				if ((char) oneByte[0] != '\n') {
					numOfLines++;
				}
			}

			do {
				// seek back BUFFERSIZE bytes
				// if length of the file if less then BUFFERSIZE start from BOF
				pos = 0;
				if ((lastPos - BUFFERSIZE) > 0) {
					pos = lastPos - BUFFERSIZE;
				}
				raf.seek(pos);
				// If less then BUFFERSIZE avaliable read the remaining bytes
				if ((lastPos - pos) < BUFFERSIZE) {
					int remainer = (int) (lastPos - pos);
					buffer = new byte[remainer];
				}
				raf.readFully(buffer);
				// in the buffer seek back for newlines
				for (int i = buffer.length - 1; i >= 0; i--) {
					if ((char) buffer[i] == '\n') {
						numOfLines++;
						// break if we have last n lines
						if (numOfLines > n) {
							pos += (i + 1);
							break;
						}
					}
				}
				// reset last postion
				lastPos = pos;
			} while ((numOfLines <= n) && (pos != 0));

			// print last n line starting from last postion
			for (pos = lastPos; pos < endPos; pos += buffer.length) {
				raf.seek(pos);
				if ((endPos - pos) < BUFFERSIZE) {
					int remainer = (int) (endPos - pos);
					buffer = new byte[remainer];
				}
				raf.readFully(buffer);
				sb.append(new String(buffer));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return sb.toString();
		}
	}

	
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
