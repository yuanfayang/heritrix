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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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
    public static final SimpleDateFormat TIMESTAMP12;
    /**
     * Arc-style date stamp in the format yyyyMMddHHmmss and UTC time zone.
     */
    public static final SimpleDateFormat TIMESTAMP14;
    /**
     * Arc-style date stamp in the format yyyyMMddHHmmssSSS and UTC time zone.
     */
    public static final SimpleDateFormat TIMESTAMP17;
    /**
     * Log-style date stamp in the format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * UTC time zone is assumed.
     */
    public static final SimpleDateFormat TIMESTAMP17ISO8601Z;
    /**
     * Log-style date stamp in the format yyyy-MM-dd'T'HH:mm:ss'Z'
     * UTC time zone is assumed.
     */
    public static final SimpleDateFormat TIMESTAMP14ISO8601Z;
    /**
     * Default character to use padding strings.
     */
    private static final char DEFAULT_PAD_CHAR = ' ';

    
    // Initialize fomatters with pattern and time zone
    static {
        TimeZone TZ = TimeZone.getTimeZone("GMT");
        TIMESTAMP12 = new SimpleDateFormat("yyyyMMddHHmm");
        TIMESTAMP12.setTimeZone(TZ);
        TIMESTAMP14 = new SimpleDateFormat("yyyyMMddHHmmss");
        TIMESTAMP14.setTimeZone(TZ);
        TIMESTAMP17 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        TIMESTAMP17.setTimeZone(TZ);
        TIMESTAMP17ISO8601Z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        TIMESTAMP14ISO8601Z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
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
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. Use current time. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * 
     * @return the date stamp
     */
    public static String getLog17Date(){
        return TIMESTAMP17ISO8601Z.format(new Date());
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     * @param date Date to format.
     * 
     * @return the date stamp
     */
    public static String getLog17Date(long date){
        return TIMESTAMP17ISO8601Z.format(new Date(date));
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. Use current time. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss'Z'
     * 
     * @return the date stamp
     */
    public static String getLog14Date(){
        return TIMESTAMP14ISO8601Z.format(new Date());
    }
    
    /**
     * Utility function for creating log timestamps, in
     * W3C/ISO8601 format, assuming UTC. 
     * 
     * Format is yyyy-MM-dd'T'HH:mm:ss'Z'
     * @param date Date to format.
     * 
     * @return the date stamp
     */
    public static String getLog14Date(long date){
        return TIMESTAMP14ISO8601Z.format(new Date(date));
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
    
    public static String get17DigitDate(Date date){
        return TIMESTAMP17.format(date);
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

    public static String get14DigitDate(Date d) {
        return TIMESTAMP14.format(d);
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
    
    public static String get12DigitDate(Date d) {
        return TIMESTAMP12.format(d);
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
    
    /**
     * Convert 17-digit date format timestamps (as found in crawl.log, for
     * example) into a GregorianCalendar object. + * Useful so you can convert
     * into milliseconds-since-epoch. Note: it is possible to compute
     * milliseconds-since-epoch + * using {@link #parse17DigitDate}.UTC(), but
     * that method is deprecated in favor of using Calendar.getTimeInMillis(). + *
     * <p/>I probably should have dug into all the utility methods in
     * DateFormat.java to parse the timestamp, but this was + * easier. If
     * someone wants to fix this to use those methods, please have at it! <p/>
     * Mike Schwartz, schwartz at CodeOnTheRoad dot com.
     * 
     * @param timestamp17String
     * @return Calendar set to <code>timestamp17String</code>.
     */
    public static Calendar timestamp17ToCalendar(String timestamp17String) {
        GregorianCalendar calendar = new GregorianCalendar();
        int year = Integer.parseInt(timestamp17String.substring(0, 4));
        int dayOfMonth = Integer.parseInt(timestamp17String.substring(6, 8));
        // Month is 0-based
        int month = Integer.parseInt(timestamp17String.substring(4, 6)) - 1;
        int hourOfDay = Integer.parseInt(timestamp17String.substring(8, 10));
        int minute = Integer.parseInt(timestamp17String.substring(10, 12));
        int second = Integer.parseInt(timestamp17String.substring(12, 14));
        int milliseconds = Integer
                .parseInt(timestamp17String.substring(14, 17));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, milliseconds);
        return calendar;
    }

    /** 
     * Convert an <code>int</code> to a <code>String</code>, and pad it to
     * <code>pad</code> spaces.
     * @param i the int
     * @param pad the width to pad to.
     * @return String w/ padding.
     */
    public static String padTo(final int i, final int pad) {
        String n = Integer.toString(i);
        return padTo(n, pad);
    }
    
    /** 
     * Pad the given <code>String</code> to <code>pad</code> characters wide
     * by pre-pending spaces.  <code>s</code> should not be <code>null</code>.
     * If <code>s</code> is already wider than <code>pad</code> no change is
     * done.
     *
     * @param s the String to pad
     * @param pad the width to pad to.
     * @return String w/ padding.
     */
    public static String padTo(final String s, final int pad) {
        return padTo(s, pad, DEFAULT_PAD_CHAR);
    }

    /** 
     * Pad the given <code>String</code> to <code>pad</code> characters wide
     * by pre-pending <code>padChar</code>.
     * 
     * <code>s</code> should not be <code>null</code>. If <code>s</code> is
     * already wider than <code>pad</code> no change is done.
     *
     * @param s the String to pad
     * @param pad the width to pad to.
     * @param padChar The pad character to use.
     * @return String w/ padding.
     */
    public static String padTo(final String s, final int pad,
            final char padChar) {
        String result = s;
        int l = s.length();
        if (l < pad) {
            StringBuffer sb = new StringBuffer(pad);
            while(l < pad) {
                sb.append(padChar);
                l++;
            }
            sb.append(s);
            result = sb.toString();
        }
        return result;
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
     * Converts a double to a string.
     * @param val The double to convert
     * @param precision How many characters to include after '.'
     * @return the double as a string.
     */
    public static String doubleToString(double val, int precision){
        String tmp = Double.toString(val);
        if(tmp.indexOf(".")!=-1){
            // Need to handle the part after '.'
            if(precision<=0){
                tmp = tmp.substring(0,tmp.indexOf('.'));
            } else {
                if(tmp.length()>tmp.indexOf('.')+precision+1){
                    // Need to trim
                    tmp = tmp.substring(0,tmp.indexOf('.')+precision+1);
                }
            }
        }
        return tmp;
    }

    /**
     * Takes an amount of bytes and formats it for display. This involves
     * converting it to Kb, Mb or Gb if the amount is enough to qualify for
     * the next level.
     * <p>
     * Displays as bytes (B): 0-1023
     * Displays as kilobytes (KB): 1024 - 2097151 (~2Mb)
     * Displays as megabytes (MB): 2097152 - 4294967295 (~4Gb)
     * Displays as gigabytes (GB): 4294967296 - infinity
     * <p>
     * Negative numbers will be returned as '0 B'.
     * <p>
     * All values will be approximated down (i.e. 2047 bytes are 1 KB)
     *
     * @param amount the amount of bytes
     * @return A string containing the amount, properly formated.
     */
    public static String formatBytesForDisplay(long amount){
        long kbStartAt = 1024;
        long mbStartAt = 1024*1024*2;
        long gbStartAt = ((long)1024*1024)*1024*4;

        if(amount < 0){
            return "0 B";
        }
        else if(amount < kbStartAt){
            // Display as bytes.
            return amount + " B";
        } else if(amount < mbStartAt) {
            // Display as kilobytes
            return Long.toString((long)(((double)amount/1024)))+" KB";
        } else if(amount < gbStartAt) {
            // Display as megabytes
            return Long.toString((long)(((double)amount/(1024*1024))))+" MB";
        } else {
            // Display as gigabytes
            return Long.toString((long)(((double)amount/(1024*1024*1024))))+" GB";
        }
    }

    /**
     * Convert milliseconds value to a human-readable duration
     * @param time
     * @return Human readable string version of passed <code>time</code>
     */
    public static String formatMillisecondsToConventional(long time) {
        StringBuffer sb = new StringBuffer();
        if (time > 3600000) {
            //got hours.
            sb.append(time / 3600000 + "h");
            time = time % 3600000;
        }
        if (time > 60000) {
            sb.append(time / 60000 + "m");
            time = time % 60000;
        }
        if (time > 1000) {
            sb.append(time / 1000 + "s");
            time = time % 1000;
        }
        sb.append(time + "ms");
        return sb.toString();
    }


    /**
     * Generate a long UID based on the given class and version number.
     * Using this instead of the default will assume serialization
     * compatibility across class changes unless version number is
     * intentionally bumped.
     *
     * @param class1
     * @param version
     * @return UID based off class and version number.
     */
    public static long classnameBasedUID(Class class1, int version) {
        String callingClassname = class1.getName();
        return (long)callingClassname.hashCode() << 32 + version;
    }
    
    /**
     * Copy the raw bytes of a long into a byte array, starting at
     * the specified offset.
     * 
     * @param l
     * @param array
     * @param offset
     */
    public static void longIntoByteArray(long l, byte[] array, int offset) {
        int i, shift;
                  
        for(i = 0, shift = 56; i < 8; i++, shift -= 8)
        array[offset+i] = (byte)(0xFF & (l >> shift));
    }
    
    public static long byteArrayIntoLong(byte [] bytearray) {
        return byteArrayIntoLong(bytearray, 0);
    }
    
    /**
     * Byte array into long.
     * @param bytearray Array to convert to a long.
     * @param offset Offset into array at which we start decoding the long.
     * @return Long made of the bytes of <code>array</code> beginning at
     * offset <code>offset</code>.
     * @see #longIntoByteArray(long, byte[], int)
     */
    public static long byteArrayIntoLong(byte [] bytearray,
            int offset) {
        long result = 0;
        for (int i = offset; i < 8 /*Bytes in long*/; i++) {
            result = (result << 8 /*Bits in byte*/) |
                (0xff & (byte)(bytearray[i] & 0xff));
        }
        return result;
    }

    /**
     * Given a string that may be a plain host or host/path (without
     * URI scheme), add an implied http:// if necessary. 
     * 
     * @param u string to evaluate
     * @return string with http:// added if no scheme already present
     */
    public static String addImpliedHttpIfNecessary(String u) {
        if(u.indexOf(':') == -1 || u.indexOf('.') < u.indexOf(':')) {
            // No scheme present; prepend "http://"
            u = "http://" + u;
        }
        return u;
    }
}

