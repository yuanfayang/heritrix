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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class contains a variety of methods for reading log files (or other text files containing
 * repeated lines with similar information).
 * <p>
 * All methods are static.
 *
 * @author Kristinn Sigurdsson
 */

public class LogReader
{
    /**
     * Returns the entire file. Useful for smaller files.
     *
     * @param aFileName a file name
     * @return The String representation of the entire file.
     *         Null is returned if errors occur (file not found or io exception)
     */
    public static String get(String aFileName){
    	StringBuffer ret = new StringBuffer();
    	try{
    		BufferedReader bf = new BufferedReader(new FileReader(aFileName), 8192);

    		String line = null;
    		while ((line = bf.readLine()) != null) {
    			ret.append(line);
    			ret.append("\n");
    		}
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    		return null;
    	}catch(IOException e){
    		e.printStackTrace();
    		return null;
    	}
    	return ret.toString();
    }

    /**
     * Gets a portion of a log file. Starting at a given line number and the n-1 lines
     * following that one or until the end of the log if that is reached first.
     *
     * @param aFileName The filename of the log/file
     * @param lineNumber The number of the first line to get (if larger then the file an
     *                   empty string will be returned)
     * @param n How many lines to return (total, including the one indicated by lineNumber).
     *                   If smaller then 1 then an empty string will be returned.
     *
     * @return A portion of the file starting at lineNumber and reaching lineNumber+n.
     *         Null is returned if errors occur (file not found or io exception)
     */
    public static String get(String aFileName, int lineNumber, int n)
    {
    	StringBuffer ret = new StringBuffer();
    	try{
    		BufferedReader bf = new BufferedReader(new FileReader(aFileName), 8192);

    		String line = null;
    		int i=1;
    		while ((line = bf.readLine()) != null) {
    			if(i >= lineNumber && i < (lineNumber+n))
    			{
    				ret.append(line);
    				ret.append("\n");
    			} else if( i >= (lineNumber+n)){
    				break;
    			}
    			i++;
    		}
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    		return null;
    	}catch(IOException e){
    		e.printStackTrace();
    		return null;
    	}
    	return ret.toString();
    }

    /**
     * Return the line number of the first line in the
     * log/file that matches a given regular expression.
     *
     * @param aFileName The filename of the log/file
     * @param regExpr The regular expression that is to be used
     * @return The line number (counting from 1, not zero) of the first line
     *         that matches the given regular expression. -1 is returned if no
     *         line matches the regular expression.
     *         -1 also is returned if errors occur (file not found, io exception etc.)
     */
    public static int findFirstLineContaining(String aFileName, String regExpr)
    {
    	Pattern p = Pattern.compile(regExpr);

    	try{
    		BufferedReader bf = new BufferedReader(new FileReader(aFileName), 8192);

    		String line = null;
    		int i = 1;
    		while ((line = bf.readLine()) != null) {
    			if(p.matcher(line).matches()){
    				// Found a match
    				return i;
    			}
    			i++;
    		}
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    	return -1;
    }

    /**
     * Returns all lines in a log/file matching a given regular expression.  Possible to get lines
     * immediately following the matched line.  Also possible to have each line prepended by it's
     * line number.
     *
     * @param aFileName The filename of the log/file
     * @param regExpr The regular expression that is to be used
     * @param addLines How many lines (in addition to the matched line) to add. A value less then 1
     *                 will mean that only the matched line will be included. If another matched
     *                 line is hit before we reach this limit it will be included and this counter
     *                 effectively reset for it.
     * @param prependLineNumbers If true, then each line will be prepended by it's line number in
     *                           the file.
     * @return Returns all lines in a log/file matching a given regular expression.
     *         Null is returned if file not found or io exception occur. If a PatternSyntaxException
     *         occurs, it's error message will be returned.
     */
    public static String getByRegExpr(String aFileName, String regExpr, int addLines, boolean prependLineNumbers)
    {
    	// TODO: Optimize how this is done?
    	StringBuffer ret = new StringBuffer();

    	try{
    		Pattern p = Pattern.compile(regExpr);
    		BufferedReader bf = new BufferedReader(new FileReader(aFileName), 8192);

    		String line = null;
    		int i = 1;
    		boolean doAdd = false;
    		int addCount = 0;
    		while ((line = bf.readLine()) != null) {
    			if(p.matcher(line).matches()){
    				// Found a match
    				if(prependLineNumbers){
    					ret.append(i);
    					ret.append(". ");
    				}
    				ret.append(line);
    				ret.append("\n");
    				doAdd = true;
    				addCount = 0;
    			} else if(doAdd) {
    				if(addCount < addLines){
    					//Ok, still within addLines
    					if(prependLineNumbers){
    						ret.append(i);
    						ret.append(". ");
    					}
    					ret.append(line);
    					ret.append("\n");
    				}else{
    					doAdd = false;
    					addCount = 0;
    				}
    			}
    			i++;
    		}
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    		return null;
    	}catch(IOException e){
    		e.printStackTrace();
    		return null;
    	}catch(PatternSyntaxException e){
    		return e.getMessage();
    	}
    	return ret.toString();
    }

    /**
     * Returns all lines in a log/file matching a given regular expression.  Possible to get lines
     * immediately following the matched line.  Also possible to have each line prepended by it's
     * line number.
     *
     * @param aFileName The filename of the log/file
     * @param regExpr The regular expression that is to be used
     * @param addLines Any lines following a match that <b>begin</b> with this string will also be included.
     *                 We will stop including new lines once we hit the first that does not match.
     * @param prependLineNumbers If true, then each line will be prepended by it's line number in
     *                           the file.
     * @return Returns all lines in a log/file matching a given regular expression.
     *         Null is returned if file not found or io exception occur. If a PatternSyntaxException
     *         occurs, it's error message will be returned.
     */
    public static String getByRegExpr(String aFileName, String regExpr, String addLines, boolean prependLineNumbers){
    	StringBuffer ret = new StringBuffer();

    	try{
    		Matcher m = Pattern.compile(regExpr).matcher("");
    		BufferedReader bf = new BufferedReader(new FileReader(aFileName), 8192);

    		String line = null;
    		int i = 1;
    		boolean doAdd = false;
    		while ((line = bf.readLine()) != null) {
                m.reset(line);
    			if(m.matches()){
    				// Found a match
    				if(prependLineNumbers){
    					ret.append(i);
    					ret.append(". ");
    				}
    				ret.append(line);
    				ret.append("\n");
    				doAdd = true;
    			} else if(doAdd) {
    				if(line.indexOf(addLines)==0){
    					//Ok, line begins with 'addLines'
    					if(prependLineNumbers){
    						ret.append(i);
    						ret.append(". ");
    					}
    					ret.append(line);
    					ret.append("\n");
    				}else{
    					doAdd = false;
    				}
    			}
    			i++;
    		}
    	}catch(FileNotFoundException e){
    		e.printStackTrace();
    		return null;
    	}catch(IOException e){
    		e.printStackTrace();
    		return null;
    	}catch(PatternSyntaxException e){
    		return e.getMessage();
    	}
    	return ret.toString();
    }

    /**
     * Implementation of a unix-like 'tail' command
     *
     * @param aFileName a file name String
     * @return the String representation of at most 10 last lines
     */
    public static String tail(String aFileName) {
    	return tail(aFileName, 10);
    }

    /**
     * Implementation of a unix-like 'tail -n' command
     *
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
    	}
        return sb.toString();
    }
}
