/* 
 * ArchiveUtils
 * 
 * $Header$
 * 
 * Created on Jul 7, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 *
 */
package org.archive.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.File;
import java.io.IOException;

/**
 * Miscellaneous useful methods.
 * 
 * 
 * @author gojomo
 */
public class ArchiveUtils {
    
	/** 
	 * Arc-style date stamp in the format yyyyMMddHHmm and UTC time zone.
	 */
    public static SimpleDateFormat TIMESTAMP12;	
	/**
	 * Arc-style date stamp in the format yyyyMMddHHmmss and UTC time zone.
	 */
	public static SimpleDateFormat TIMESTAMP14;	
	/**
	 * Arc-style date stamp in the format yyyyMMddHHmmssSSS and UTC time zone.
	 */
	public static SimpleDateFormat TIMESTAMP17;	

	// Initialize fomatters with pattern and time zone
	static {
		TimeZone TZ = TimeZone.getTimeZone("GMT");
		TIMESTAMP12 = new SimpleDateFormat("yyyyMMddHHmm");
		TIMESTAMP12.setTimeZone(TZ);
		TIMESTAMP14 = new SimpleDateFormat("yyyyMMddHHmmss");
		TIMESTAMP14.setTimeZone(TZ);
		TIMESTAMP17 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		TIMESTAMP17.setTimeZone(TZ);
	}

	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmmssSSS.
	 * Date stamps are in the UTC time zone
	 * @return the date stamp
	 */
	public static String get17DigitDate(){
		return TIMESTAMP17.format(new Date());
	}

	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmmss.
	 * Date stamps are in the UTC time zone
	 * @return the date stamp
	 */
	public static String get14DigitDate(){
		return TIMESTAMP14.format(new Date());
	}

	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmm.
	 * Date stamps are in the UTC time zone
	 * @return the date stamp
	 */
	public static String get12DigitDate(){
		return TIMESTAMP12.format(new Date()); 
	}
	
	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmmssSSS.
	 * Date stamps are in the UTC time zone
	 * 
	 * @param date milliseconds since epoc
	 * @return the date stamp
	 */
	public static String get17DigitDate(long date){
		return TIMESTAMP17.format(new Date(date));
	}
	
	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmmss.
	 * Date stamps are in the UTC time zone
	 * 
	 * @param date milliseconds since epoc
	 * @return the date stamp
	 */
	public static String get14DigitDate(long date){
		return TIMESTAMP14.format(new Date(date));
	}
	
	/**
	 * Utility function for creating arc-style date stamps
	 * in the format yyyMMddHHmm.
	 * Date stamps are in the UTC time zone
	 * 
	 * @param date milliseconds since epoc
	 * @return the date stamp
	 */
	public static String get12DigitDate(long date){
		return TIMESTAMP12.format(new Date(date)); 
	}
	
	/**
	 * Utility function for parsing arc-style date stamps
	 * in the format yyyMMddHHmmssSSS.
	 * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 17 digits.
	 * 
	 * @param date an arc-style formatted date stamp
	 * @return the Date corresponding to the date stamp string
	 * @throws ParseException if the inputstring was malformed
	 */
	public static Date parse17DigitDate(String date) throws ParseException{
		return TIMESTAMP17.parse(date);
	}
	
	/**
	 * Utility function for parsing arc-style date stamps
	 * in the format yyyMMddHHmmss.
	 * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 14 digits.
	 * 
	 * @param date an arc-style formatted date stamp
	 * @return the Date corresponding to the date stamp string
	 * @throws ParseException if the inputstring was malformed
	 */
	public static Date parse14DigitDate(String date) throws ParseException{
		return TIMESTAMP14.parse(date);
	}
	
	/**
	 * Utility function for parsing arc-style date stamps
	 * in the format yyyMMddHHmm.
	 * Date stamps are in the UTC time zone.  The whole string will not be
     * parsed, only the first 12 digits.
	 * 
	 * @param date an arc-style formatted date stamp
	 * @return the Date corresponding to the date stamp string
	 * @throws ParseException if the inputstring was malformed
	 */
	public static Date parse12DigitDate(String date) throws ParseException{
		return TIMESTAMP12.parse(date);
	}
	
	/** Convert an <code>int</code> to a <code>String</code>, and pad it to
     * <code>pad</code> spaces.
	 * @param i the int
	 * @param pad the width to pad to.
	 * @return String w/ padding.
	 */
	public static String padTo(final int i, final int pad) {
		String n = Integer.toString(i);
		return padTo(n,pad);
	}
	
	/** Pad the given <code>String</code> to <code>pad</code> characters wide
     * by pre-pending spaces.  <code>s</code> should not be <code>null</code>.
     * If <code>s</code> is already wider than <code>pad</code> no change is
     * done.
     *
	 * @param s the String to pad
	 * @param pad the width to pad to.
	 * @return String w/ padding.
	 */
	public static String padTo(final String s, final int pad) {
		int l = s.length();
		StringBuffer sb = new StringBuffer();
		while(l<pad) {
			sb.append(" ");
			l++;
		}
		sb.append(s);
		return sb.toString();
	}
    
	/**
	 * Example: On Windows machine file test/test.txt is converted to test\test.txt 
	 * @param aFileName
	 * @return Retruns a file name apropriate for the system
	 */
	public static String systemFileName(String aFileName){
		return (aFileName != null ? (new File(aFileName)).getPath() : "null");
	}

	/**
	 * @param aFileName Filename to get a file path for.
	 * @return Returns a file's path.
	 */
	public static String getFilePath(String aFileName){
		String tmpFileName = systemFileName(aFileName);
		int pathEnd = tmpFileName.lastIndexOf(File.separatorChar);
		if(pathEnd >=0 ){
			return tmpFileName.substring(0, pathEnd+1);
		}else{
			return "." + File.separator;
		}
	}

	/**
	 * Tests if a file's path is absolute.
	 * 
	 * @param aFileName the filename to check
	 * @return <code>true</code> if it is an absolute file
	 */
	public static boolean isFilePathAbsolute(String aFileName){
        // deal with null argument to avoid npe
        if(aFileName == null) {
            return false;
        }
		return (new File(aFileName)).isAbsolute();
 	}

    /** check that two byte arrays are equal.  They may be <code>null</code>.
     *
     * @param lhs a byte array
     * @param rhs another byte array.
     * @return <code>true</code> if they are both equal (or both
     * <code>null</code>)
     */
	public static boolean byteArrayEquals(final byte[] lhs, final byte[] rhs) {
        if (lhs == null && rhs != null || lhs != null && rhs == null) {
            return false;
        }
		if (lhs==rhs) {
			return true;
		}
		if (lhs.length != rhs.length) {
			return false;
		}
		for(int i = 0; i<lhs.length; i++) {
			if (lhs[i]!=rhs[i]) {
				return false;
			}
		}
		return true;	
	}
  
    /**
     * Ensure writeable directory.
     * 
     * If doesn't exist, we attempt creation.
     * 
     * @param dir Directory to test for exitence and is writeable.
     * 
     * @return The passed <code>dir</code>.
     * 
     * @exception IOException If passed directory does not exist and is not 
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(String dir)
        throws IOException
    {
        return ensureWriteableDirectory(new File(dir));
    }
    
    /**
     * Ensure writeable directory.
     * 
     * If doesn't exist, we attempt creation.
     * 
     * @param dir Directory to test for exitence and is writeable.
     * 
     * @return The passed <code>dir</code>.
     * 
     * @exception IOException If passed directory does not exist and is not 
     * createable, or directory is not writeable or is not a directory.
     */
    public static File ensureWriteableDirectory(File dir)
        throws IOException
    {
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        else
        {
            if (!dir.canWrite())
            {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " not writeable.");
            }
            else if (!dir.isDirectory())
            {
                throw new IOException("Dir " + dir.getAbsolutePath() +
                    " is not a directory.");
            }
        }  
        
        return dir;
    }

    /**
     * If possible, create a hard link from the second File to 
     * the first. If not, create a copy. 
     * 
     * @param file
     * @param file2
     */
    public static void hardLinkOrCopy(File file, File file2) {
        // TODO Auto-generated method stub
        
    }
}

