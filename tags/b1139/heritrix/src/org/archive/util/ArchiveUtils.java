/*
 * ArchiveUtils.java
 * Created on Jul 7, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.File;

/**
 * Miscellaneous useful methods.
 * 
 * 
 * @author gojomo
 */
public class ArchiveUtils {
    public static SimpleDateFormat TIMESTAMP12;	
	public static SimpleDateFormat TIMESTAMP14;	
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
	 * Date stamps are in the UTC time zone
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
	 * Date stamps are in the UTC time zone
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
	 * Date stamps are in the UTC time zone
	 * 
	 * @param date an arc-style formatted date stamp
	 * @return the Date corresponding to the date stamp string
	 * @throws ParseException if the inputstring was malformed
	 */
	public static Date parse12DigitDate(String date) throws ParseException{
		return TIMESTAMP12.parse(date);
	}
	
	/**
	 * @param i
	 * @param j
	 * @return
	 */
	public static String padTo(int i, int pad) {
		String n = Integer.toString(i);
		return padTo(n,pad);
	}
	
	/**
	 * @param length
	 * @param i
	 * @return
	 */
	public static String padTo(String s, int pad) {
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
		return (new File(aFileName)).getPath();
	}

	public static String getFilePath(String aFileName){
		String tmpFileName = systemFileName(aFileName);
		int pathEnd = tmpFileName.lastIndexOf(File.separatorChar);
		if(pathEnd >=0 ){
			return tmpFileName.substring(0, pathEnd+1);
		}else{
			return "./";
		}
	}
	
	public static boolean byteArrayEquals(byte[] lhs, byte[] rhs) {
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
}
