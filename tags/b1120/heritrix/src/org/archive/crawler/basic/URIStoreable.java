/*
 * KeyedItem.java
 * Created on Jun 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

/**
 * Represents objects with a lifecycle, including snoozed times,
 * inside the Frontier: individual CandidateURIs and queues. 
 * 
 * @author gojomo
 *
 */
public interface URIStoreable {

	public static final Object FORGOTTEN = "FORGOTTEN".intern();
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

	/**
	 * a fallback string to use when wake times are equal
	 */
	String getSortFallback();
}
