/*
 * FullyBufferedInputStream.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.IOException;
import java.io.InputStream;


/**
 * An InputStream on data of discrete size which gives full
 * forward, backward, reset capabilities and hides from users
 * whether data is held in memory or disk-backed.
 * 
 * (A subclass for HTTP messages might offer easy access to
 * a substream for its content-body, or substreams for its
 * pipelined requests, etc.)
 * 
 * @author gojomo
 */
public class FullyBufferedInputStream extends InputStream {
	int checksum;
	
	public int getChecksum() {
		return checksum;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
