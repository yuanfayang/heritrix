/*
 * Created on Jul 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.util;

import java.util.Comparator;

/**
 * @author Igor Ranitovic
 *
 * 
 * */
public class StringIntPairComparator implements Comparator {

	public StringIntPairComparator (){
		super();
	}
	
	public int compare(Object o1, Object o2) {
		StringIntPair p1 = (StringIntPair) o1;
		StringIntPair p2 = (StringIntPair) o2;
		if (p1.getIntValue() < p2.getIntValue())
			return -1;
		if (p1.getIntValue() > p2.getIntValue())
			return 1;
			
		return 0;
	}

}
