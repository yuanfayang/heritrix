/*
 * Created on Jul 17, 2003
 *
 */
package org.archive.util;

import java.util.LinkedList;
import java.util.Iterator;

/** Provide a simple fixed-size queue that handles ordering and
 *  makes sure it's a first-in first-out kind of thing.
 * 
 * @author Parker Thompson
 */
public class Queue {

	protected int maxSize = 10;
	protected LinkedList store = new LinkedList();
	
	public Queue() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public Queue(int size){
		maxSize = size;
	}
	
	public void add(Object item){

		if(store.size() >= maxSize){
			store.removeFirst();
		}
		store.addLast(item);
	}
	
	public void remove(Object item){
		store.remove(item);
	}
	
	public Iterator iterator(){
		return store.iterator();
	}
	
	public Object getFirst(){
		return store.getFirst();
	}
	
	public Object getLast(){
		return store.getLast();
	}
	
	public int size(){
		return store.size();
	}
}
