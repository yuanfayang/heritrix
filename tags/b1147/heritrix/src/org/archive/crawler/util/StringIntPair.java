/*
 * Created on Jul 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.util;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StringIntPair {
	private String stringValue;
	private int intValue;
	
	public StringIntPair(String s, int i){
		stringValue = s;
		intValue = i;
	}
	/**
	 * @return
	 */
	public int getIntValue() {
		return intValue;
	}

	/**
	 * @return
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * @param i
	 */
	public void setIntValue(int i) {
		intValue = i;
	}

	/**
	 * @param string
	 */
	public void setStringValue(String string) {
		stringValue = string;
	}

}
