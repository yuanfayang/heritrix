/*
 * QueueCat.java
 * Created on Nov 12, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.SequenceInputStream;

/**
 * Allows display of serialized object streams in a line-oriented format. 
 * 
 * @author gojomo
 *
 */
public class QueueCat {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		InputStream inStream;
		if (args.length == 0) {
			inStream = System.in;
		} else {
			inStream = new FileInputStream(args[0]);
		}
		
		// need to handle the case where the stream lacks the usual
		// objectstream prefix
		byte[] serialStart = { (byte)0xac, (byte)0xed, (byte)0x00, (byte)0x05 };
		byte[] actualStart = new byte[4];
		byte[] pseudoStart;
		inStream.read(actualStart);
		if (ArchiveUtils.byteArrayEquals(serialStart,actualStart)) {
			pseudoStart = serialStart;
		} else {
			// have to fake serialStart and original 4 bytes
			pseudoStart = new byte[8];
			System.arraycopy(serialStart,0,pseudoStart,0,4);
			System.arraycopy(actualStart,0,pseudoStart,4,4);
		}
		inStream = new SequenceInputStream(
			new ByteArrayInputStream(pseudoStart),
			inStream);
			
		ObjectInputStream oin = new ObjectInputStream(inStream);

		Object o;
		while(true) {
			try {
				o=oin.readObject();
			} catch (EOFException e) {
				return;
			} 
			if(o instanceof Lineable) {
				System.out.println(((Lineable)o).getLine());
			} else {
				// TODO: flatten multiple-line strings!
				System.out.println(o.toString());
			}
		}
	}
}
