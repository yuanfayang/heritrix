/*
 * Created on Jul 22, 2003
 *
 */
package org.archive.util;

import java.util.LinkedList;
import java.util.Iterator;
import st.ata.util.AList;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;

/** This class extends our simple queue.  It expects to store
 *  only CrawlURIs and will enforce a notion of time expiration,
 *  so that in addition to size limitations this queue will have 
 *  a "freshness" limiation, removing items that are too old.
 * 
 * @author Parker Thompson
 */
public class TimedQueue extends Queue implements CoreAttributeConstants{

	// by default expire after a minute
	long expireAfterMili = 60*1000;
	
	public TimedQueue() {
		super();
	}

	public TimedQueue(int size) {
		super(size);
	}
	
	/** Constuctor for TimedQueue 
	 *  @param size - the number of recent CrawlURIs to keep track of
	 *  @param expire - the number of miliseconds after which to expire an entry
	 */ 
	public TimedQueue(int size, long expire){
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
		
		if(store.size() == 0){
			return;
		}
		
		CrawlURI oldest = (CrawlURI)store.getFirst();
		
		if(oldest == null){
			return;
		}
		
		long finishTime = oldest.getAList().getLong(A_FETCH_COMPLETED_TIME);
		
		if(finishTime + expireAfterMili <= now){
			store.remove(oldest);
			
			// go until we've removed all expired items
			cleanUp(now);
		}
		return;
	}
	
	public void add(Object item){
		super.add(item);
	}
	
	public void remove(Object item){
		super.remove(item);
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
		return store.size();
	}

}
