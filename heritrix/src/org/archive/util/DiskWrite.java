/*
 * Created on Jul 17, 2003
 *
 */
package org.archive.util;

/** Keep track of discrete disk writes.  This is basically a struct
 *  that holds timestamp -> file size pairs.
 * 
 * @author Parker Thompson
 */
public class DiskWrite {

	int bytes;
	long time;


	public DiskWrite() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public DiskWrite(int b, long t){
		bytes = b;
		time = t;
	}
	
	public long getTime(){
		return time;
	}
	
	public int getByteCount(){
		return bytes;
	}
}
