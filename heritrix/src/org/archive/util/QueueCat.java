/*
 * QueueCat.java
 * Created on Nov 12, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.SequenceInputStream;

/**
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
		byte[] fakeStart = { (byte)0xac, (byte)0xed, (byte)0x00, (byte)0x05 };
		inStream = new SequenceInputStream(
			new ByteArrayInputStream(fakeStart),
			inStream);
			
		ObjectInputStream oin = new ObjectInputStream(inStream);

		Object o;
		while(true) {
			o=oin.readObject();
			if(o instanceof Lineable) {
				System.out.println(((Lineable)o).getLine());
			} else {
				// TODO: flatten multiple-line strings!
				System.out.println(o.toString());
			}
		}
	}
}
