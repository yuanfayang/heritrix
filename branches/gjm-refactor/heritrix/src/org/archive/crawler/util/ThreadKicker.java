/*
 * ThreadKicker.java
 * Created on Jun 24, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;


/**
 * Interrupts threads at the requested time. Useful
 * for aborting hung network IO. 
 * 
 * @author gojomo
 *
 */
public class ThreadKicker extends Thread {
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.ThreadKicker");

	SortedSet scheduledKicks = new TreeSet(); // of ScheduledKicks
	HashMap pendingKicks = new HashMap(); // of Thread -> ScheduledKick
	
	/**
	 * 
	 */
	public ThreadKicker() {
		super();
		setName("ThreadKicker");
	}


	/**
	 * Arrange to be kicked (interrupted) at the specified time. 
	 * 
	 * @param target
	 * @param time
	 */
	public synchronized void kickMeAt(Thread target, long time) {
		removeKicks(target);
		if(time==0) {
			return;
		}
		ScheduledKick kick = new ScheduledKick(target, time);
		scheduledKicks.add(kick);
		pendingKicks.put(target,kick); 
		if(scheduledKicks.first()==kick) {
			notify();
		}
	}


	private void removeKicks(Thread target) {
		ScheduledKick kick = (ScheduledKick)pendingKicks.remove(target);
		if(kick!=null) {
			scheduledKicks.remove(kick);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public synchronized void run() {
		while (true) {
			try {
				long now = System.currentTimeMillis();
				ScheduledKick top = scheduledKicks.isEmpty() ? null : (ScheduledKick)scheduledKicks.first();
				while (top!=null && top.getWhen()<now) {
					scheduledKicks.remove(top);
					pendingKicks.remove(top);
					logger.warning("kicking "+top.getTarget());
					top.getTarget().interrupt();
					top = scheduledKicks.isEmpty() ? null : (ScheduledKick)scheduledKicks.first();
				}
				if (top == null) {
					wait();
				} else {
					wait(top.getWhen()-now);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Cancel all scheduled kicks for the given thread.
	 * 
	 * @param thread
	 */
	public synchronized void cancelKick(Thread thread) {
		removeKicks(thread);
	}

}

class ScheduledKick implements Comparable {
		
	private Thread target;
	private long when;

	/**
	 * 
	 */
	public ScheduledKick(Thread th, long time) {
		target = th;
		when = time;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		ScheduledKick other = (ScheduledKick)o;
		if (this==other) {
			return 0;
		}
		if(when < other.getWhen()) {
			return -1;
		} 
		if(when > other.getWhen()) {
			return 1;
		}
		// equal times; arbitrary ordering ok
		if(target.hashCode()<other.getTarget().hashCode()) {
			return -1;
		} else {
			// TODOSOMEDAY: fix tiny chance of bug here
			// if two ScheduledKicks of same time and 
			// same target hashcode are compared, 
			// answer is order-dependent. 
			return 1;
		}
	}

	/**
	 * 
	 */
	Thread getTarget() {
		return target;
	}

	/**
	 * @return
	 */
	long getWhen() {
		return when;
	}

}