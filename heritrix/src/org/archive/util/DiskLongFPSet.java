/*
 * DiskLongFPSet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * @author gojomo
 *
 */
public class DiskLongFPSet extends AbstractLongFPSet implements LongFPSet {
	private static Logger logger = Logger.getLogger("org.archive.util.DiskLongFPSet");
	File disk;
	RandomAccessFile rawRafile;
	
	/**
	 * 
	 */
	public DiskLongFPSet(File dir, String name) throws IOException {
		this(dir, name, 20);
	}

	/**
	 * @param i
	 */
	public DiskLongFPSet(File dir, String name, int capacityPowerOfTwo) throws IOException {
		this.capacityPowerOfTwo = capacityPowerOfTwo;
		disk = new File(dir, name+".fps");
		rawRafile = new RandomAccessFile(disk,"rw");
		for(long l=0;l<(1<<capacityPowerOfTwo);l++) {
			rawRafile.writeLong(0);
		}
		count = 0;
	}

	/**
	 * 
	 */
	protected void makeSpace() {
		try {
			RandomAccessFile oldRaw = rawRafile;
			capacityPowerOfTwo++;
			File tmpDisk = new File(disk.getAbsolutePath()+".tmp");
			rawRafile = new RandomAccessFile(tmpDisk,"rw");
			for(long l=0;l<(1<<capacityPowerOfTwo);l++) {
				rawRafile.writeLong(0);
			}
			count=0;
			oldRaw.seek(0);
			while(true) {
				try {
					long lo = oldRaw.readLong();
					if(lo!=0) {
						add(lo);
					} 
				} catch (EOFException e) {
					break;
				}
			}
			oldRaw.close();
			rawRafile.close();
			disk.delete();
			if(!tmpDisk.renameTo(disk)) {
				logger.warning("unable to switch to expanded disk file");
			}
			rawRafile=new RandomAccessFile(disk,"rw");
		} catch (IOException e) {
			// TODO Convert to runtime exception
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#setAt(long, long)
	 */
	protected void setAt(long i, long val) {
		try {
			rawRafile.seek(i*8);
			rawRafile.writeLong(val);
		} catch (IOException e) {
			// TODO Convert to runtime exception
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#getAt(long)
	 */
	protected long getAt(long i) {
		try {
			rawRafile.seek(i*8);
			return rawRafile.readLong();
		} catch (IOException e) {
			// TODO Convert to runtime exception
			e.printStackTrace();
			return 0;
		}
	}

}
