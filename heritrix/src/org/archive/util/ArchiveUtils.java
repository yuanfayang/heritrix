/*
 * ArchiveUtils.java
 * Created on Jul 7, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

/**
 * Miscellaneous useful methods.
 * 
 * 
 * @author gojomo
 */
public class ArchiveUtils {
    public static SimpleDateFormat TIMESTAMP12 = new SimpleDateFormat("yyyyMMddHHmm");	
	public static SimpleDateFormat TIMESTAMP14 = new SimpleDateFormat("yyyyMMddHHmmss");	
	public static SimpleDateFormat TIMESTAMP17 = new SimpleDateFormat("yyyyMMddHHmmssSSS");	

	// utility functions for creating arc-style date stamps
	public static String get17DigitDate(){
		return TIMESTAMP17.format(new Date());
	}
	public static String get14DigitDate(){
		return TIMESTAMP14.format(new Date());
	}
	public static String get12DigitDate(){
		return TIMESTAMP12.format(new Date()); 
	}
	
	public static String get17DigitDate(long date){
		return TIMESTAMP17.format(new Date(date));
	}
	public static String get14DigitDate(long date){
		return TIMESTAMP14.format(new Date(date));
	}
	public static String get12DigitDate(long date){
		return TIMESTAMP12.format(new Date(date)); 
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
