/*
 * ArchiveUtils.java
 * Created on Jul 7, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.text.SimpleDateFormat;
import java.util.Date;

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
	

}
