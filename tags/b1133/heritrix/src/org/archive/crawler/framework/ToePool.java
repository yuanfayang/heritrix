/*
 * ToePool.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.ArrayList;
import java.util.logging.Level;

import org.archive.util.DevUtils;

/**
 * A collection of ToeThreads.
 * 
 * @author gojomo
 *
 */
public class ToePool implements CrawlListener {
	public static int DEFAULT_TOE_PRIORITY = Thread.NORM_PRIORITY - 1;
	
	protected CrawlController controller;
	protected ArrayList toes;
	/**
	 * @param i
	 */
	public ToePool(CrawlController c, int count) {
		controller = c;
		controller.addListener(this);
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
				wait(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				DevUtils.logger.log(Level.SEVERE,"available()"+DevUtils.extraInfo(),e);
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

	/**
	 * The crawl controller uses this method to notify the pool that the crawl has ended.
	 * All toe threads will be ordered to stop after current.
	 * All references in this object will be set to null to facilitate GC.
	 * Once the CrawlController has called this method, this object should be considered
	 * as having been destroyed.
	 */
	public void crawlEnding(String sExitMessage) {
		while(toes.size()>0)
		{
			ToeThread t = (ToeThread)toes.get(0);
			t.stopAfterCurrent();
			toes.remove(0);
		}
		controller = null;
		toes = null;
	}

	/**
	 * Gets a ToeThreads internal status report.
	 * 
	 * @param toe the number of the ToeThread to query.
	 * @return
	 */
	public String getReport(int toe)
	{
		return ((ToeThread)toes.get(toe)).report();
	}
}
