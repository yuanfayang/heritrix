/*
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.LinkedList;

/**
 * @author gojomo
 *
 */
public class KeyedQueue extends LinkedList {
	public static Object READY = new Object();
	public static Object HOLDING = new Object();
	public static Object SLEEPING = new Object();
	long wakeTime;
	String key;
	Object state;
}
