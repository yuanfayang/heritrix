/*
 * Created on Jun 23, 2003
 *
 */
package org.archive.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Parker Thompson
 *
 * This class takes input and does nothing with it,
 * sort of a /dev/null for the rest of us.
 */
public class NullOutputStream extends OutputStream {

	public NullOutputStream(){
	}

	public void write(int b) throws IOException {
	}

	public void write(byte[] b){	
	}
	
	public void write(byte[] b, int off, int len){
	}
}
