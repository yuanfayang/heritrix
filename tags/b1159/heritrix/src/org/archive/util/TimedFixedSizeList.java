/*
 * Created on Jul 22, 2003
 *
 */
package org.archive.util;

import java.util.Iterator;

import org.archive.crawler.datamodel.CoreAttributeConstants;

/** This class extends the FixedSizeList.  It expects to store
 *  only TimedItems and will enforce a notion of time expiration,
 *  so that in addition to size limitations this queue will have 
 *  a "freshness" limitation, removing items that are too old.
 * 
 * @author Parker Thompson
 */
public class TimedFixedSizeList extends FixedSizeList implements CoreAttributeConstants{

	// by default expire after a minute
	long expireAfterMili = 60*1000;
	
	public TimedFixedSizeList() {
		super();
	}

	public TimedFixedSizeList(int size) {
		super(size);
	}
	
	/** Constuctor for TimedQueue 
	 *  @param size - the number of recent CrawlURIs to keep track of
	 *  @param expire - the number of miliseconds after which to expire an entry
	 */ 
	public TimedFixedSizeList(int size, long expire){
		super(size);
		expireAfterMili = expire;
	}
	
	/** Remove expired items from the queue */
	protected void cleanUp(){
		long now = System.currentTimeMillis();
		cleanUp(now);
	}
	
	/** Remove expired items from the queue */
	protected void cleanUp(long now){
		
		long expireIfBefore = now - expireAfterMili;
		
		while ( (super.size() > 0)
			     && ((TimedItem) super.getFirst()).getTime() < expireIfBefore) {
			removeFirst();
		}
	}
		
	public void setExpireAfter(long miliseconds){
		expireAfterMili = miliseconds;
	}
	
	public Iterator iterator(){
		cleanUp();
		return super.iterator();
	}
	
	public Object getFirst(){
		cleanUp();
		return super.getFirst();
	}
	
	public Object getLast(){
		cleanUp();
		return super.getLast();
	}
	
	public int size(){
		cleanUp();
		return super.size();
	}
	
	public interface TimedItem {

		/**
		 * @return
		 */
		long getTime();
	
	}
}

