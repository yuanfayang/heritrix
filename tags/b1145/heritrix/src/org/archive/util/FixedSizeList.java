/*
 * Created on Jul 17, 2003
 *
 */
package org.archive.util;

import java.util.LinkedList;

/** Provide a simple fixed-size queue that handles ordering and
 *  makes sure it's a first-in first-out kind of thing.
 * 
 * @author Parker Thompson
 */
public class FixedSizeList extends LinkedList {

	public FixedSizeList() {
	}

	protected int maxSize = 10;
		
	public FixedSizeList(int size){
		maxSize = size;
	}
	
	public boolean add(Object item){
		makeSpace();
		return super.add(item);
	}
	
	public void add(int index, Object element) {
		makeSpace();
		super.add(index, element);
	}

	public void addLast(Object o) {
		makeSpace();
		super.addLast(o);
	}
	
	private void makeSpace() {
		if(size() >= maxSize){
			removeFirst();
		}
	}
}
