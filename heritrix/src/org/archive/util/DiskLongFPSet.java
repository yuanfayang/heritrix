/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * DiskLongFPSet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gojomo
 *
 */
public class DiskLongFPSet extends AbstractLongFPSet implements LongFPSet {
	private static Logger logger = Logger.getLogger("org.archive.util.DiskLongFPSet");
	static final int DEFAULT_CAPACITY_POWER_OF_TWO = 4;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	File disk;
	RandomAccessFile rawRafile;
	
	/**
	 * 
	 */
	public DiskLongFPSet(File dir, String name) throws IOException {
		this(dir, name, DEFAULT_CAPACITY_POWER_OF_TWO,DEFAULT_LOAD_FACTOR);
	}

	/**
	 * @param i
	 */
	public DiskLongFPSet(File dir, String name, int capacityPowerOfTwo, float loadFactor) throws IOException {
		this.capacityPowerOfTwo = capacityPowerOfTwo;
		this.loadFactor = loadFactor;
		disk = new File(dir, name+".fps");
		if(disk.exists()) {
			disk.delete();
		}
		rawRafile = new RandomAccessFile(disk,"rw");
		for(long l=0;l<(1<<capacityPowerOfTwo);l++) {
			rawRafile.writeByte(EMPTY);
			rawRafile.writeLong(0);
		}
		count = 0;
	}

	/**
	 * 
	 */
	protected void makeSpace() {
		grow();
	}

	private void grow() {
		try {
			RandomAccessFile oldRaw = rawRafile;
			capacityPowerOfTwo++;
			File tmpDisk = new File(disk.getAbsolutePath()+".tmp");
			if(tmpDisk.exists()) {
				tmpDisk.delete();
			}
			rawRafile = new RandomAccessFile(tmpDisk,"rw");
			for(long l=0;l<(1<<capacityPowerOfTwo);l++) {
				rawRafile.writeByte(EMPTY);
				rawRafile.writeLong(0);
			}
			count=0;
			oldRaw.seek(0);
			while(true) {
				try {
					byte slot = oldRaw.readByte();
					long val = oldRaw.readLong();
					if (slot!=EMPTY) {
						add(val);
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
			} else {
				logger.warning("RENAME SUCCESSFUL");
			}
			// disk = new File(disk.getAbsolutePath());
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
			rawRafile.seek(i*(1+8));
			rawRafile.writeByte(0); // non-empty
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
			rawRafile.seek((i*(1+8))+1);
			return rawRafile.readLong();
		} catch (IOException e) {
			// TODO Convert to runtime exception
			e.printStackTrace();
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#relocate(long, long, long)
	 */
	void relocate(long value, long fromIndex, long toIndex) {
		clearAt(fromIndex);
		setAt(toIndex,value);
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#getSlotState(long)
	 */
	protected int getSlotState(long i) {
		try {
			rawRafile.seek(i*(1+8));
			return rawRafile.readByte();
		} catch (IOException e) {
			// TODO convert to runtime exception
			e.printStackTrace();
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#clearAt(long)
	 */
	protected void clearAt(long index) {
		try {
			rawRafile.seek(index*(1+8));
			rawRafile.writeByte(EMPTY);
		} catch (IOException e) {
			// TODO convert to runtime exception
			DevUtils.logger.log(Level.SEVERE,"clearAt("+index+")"+DevUtils.extraInfo(),e);
		}
	}


}
