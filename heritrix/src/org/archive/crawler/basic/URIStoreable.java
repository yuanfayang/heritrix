/*
 * KeyedItem.java
 * Created on Jun 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

/**
 * @author gojomo
 *
 */
public interface URIStoreable {

	public static final Object FINISHED = "FINISHED".intern();;
	public static final Object HELD = "HELD".intern();
	public static final Object IN_PROCESS = "IN_PROCESS".intern();
	public static final Object PENDING = "PENDING".intern();
	public static final Object READY = "READY".intern();
	public static final Object SNOOZED = "SNOOZED".intern();

	/**
	 * @return
	 */
	Object getClassKey();

	Object getStoreState();
	
	void setStoreState(Object s);
	
	long getWakeTime();
	
	void setWakeTime(long w);
}
