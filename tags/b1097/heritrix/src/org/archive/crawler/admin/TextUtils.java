package org.archive.crawler.admin;

import java.io.*;

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
				sb.append((new String(buffer)).replaceAll("[ ]+", " "));
			}
			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
