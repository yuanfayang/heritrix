package org.archive.util;

import java.io.*;
import java.util.regex.Pattern;

public class TextUtils {

	public static String getText(String aFileName) {
		try {
			BufferedReader reader =
				new BufferedReader(new FileReader(aFileName));
			if (reader == null) {
				return "";
			}
			StringBuffer sb = new StringBuffer();
			String aLine;
			while ((aLine = reader.readLine()) != null) {
				sb.append(aLine + "\n");
			}
			reader.close();
			return sb.toString();
		} catch (Exception e) {
			// TODO: report error
			e.printStackTrace();
		}
		return "";
	}

	public static String tail(String aFileName) {
		return tail(aFileName, 10);
	}

	public static String tail(String aFileName, int n) {
		int BUFFERSIZE = 1024;
		long pos;
		long endPos;
		long lastPos;
		int numOfLines = 0;
		byte[] buffer = new byte[BUFFERSIZE];

		try {
			RandomAccessFile raf = new RandomAccessFile(new File(aFileName), "r");
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
			StringBuffer sb = new StringBuffer();
			for (pos = lastPos; pos < endPos; pos += buffer.length) {
				raf.seek(pos);
				if ((endPos - pos) < BUFFERSIZE) {
					int remainer = (int) (endPos - pos);
					buffer = new byte[remainer];
				}
				raf.readFully(buffer);
				// remove runing white spaces
				sb.append(new String(buffer));
			}
			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Utility method using a precompiled pattern instead of using the replaceAll method of
	 * the String class.
	 * 
	 * @see java.lang.String#replaceAll
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @param replacement the String to substitute every match with
	 * @return the String with all the matches substituted
	 */
	public static String replaceAll(Pattern p, CharSequence input, String replacement) {
		return p.matcher(input).replaceAll(replacement);
	}

	/**
	 * Utility method using a precompiled pattern instead of using the replaceFirst method of
	 * the String class.
	 * 
	 * @see java.lang.String#replaceFirst
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @param replacement the String to substitute the first match with
	 * @return the String with the first match substituted
	 */
	public static String replaceFirst(Pattern p, CharSequence input, String replacement) {
		return p.matcher(input).replaceFirst(replacement);
	}
	
	/**
	 * Utility method using a precompiled pattern instead of using the matches method of
	 * the String class.
	 * 
	 * @see java.lang.String#matches
	 * @see java.util.regex.Pattern
	 * @param p precompiled Pattern to match against
	 * @param input the character sequence to check
	 * @return true if character sequence matches
	 */
	public static boolean matches(Pattern p, CharSequence input) {
		return p.matcher(input).matches();
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
