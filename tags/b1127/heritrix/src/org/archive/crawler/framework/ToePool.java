/*
 * ToePool.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.ArrayList;

/**
 * A collection of ToeThreads.
 * 
 * @author gojomo
 *
 */
public class ToePool {
	public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;
	
	protected CrawlController controller;
	protected ArrayList toes;
	/**
	 * @param i
	 */
	public ToePool(CrawlController c, int count) {
		controller = c;
		toes = new ArrayList(count);
		// TODO make number of threads self-optimizing
		for(int i = 0; i<count; i++) {
			ToeThread newThread = new ToeThread(c,this,i);
			newThread.setPriority(DEFAULT_TOE_PRIORITY);
			toes.add(newThread);
			newThread.start();
		}
	}

	/**
	 * 
	 */
	public synchronized ToeThread available() {	
		while(true) {
			for(int i=0; i < toes.size();i++){
				if(((ToeThread)toes.get(i)).isAvailable()) {
					return (ToeThread) toes.get(i);
				}
			}
			// nothing available
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param thread
	 */
	public synchronized void noteAvailable(ToeThread thread) {
		notify();
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public int getActiveToeCount() {
		int count = 0; 
		// will be an approximation
		for(int i=0; i < toes.size();i++){
			if(!((ToeThread)toes.get(i)).isAvailable()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return
	 */
	public int getToeCount() {
		return toes.size();
	}

}