/*
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
		size = 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongSet#add(long)
	 */
	public boolean add(long l) {
		if (l == 0) {
			if(containsZero) {
				return false;
			} else {
				containsZero = true;
				size++;
				return true;
			}
		}
		try {
			int i = indexFor(l);
			if (i>=0) {
				// already in set
				return false;
			}
			size++;
			rawSet(-(i+1),l);
			return true;
		} catch (IOException e) {
			// TODO Convert to runtime exception?
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * @param i
	 * @param l
	 */
	private void rawSet(int index, long value) throws IOException {
		rawRafile.seek(index*8);
		rawRafile.writeLong(value);
	}

	/**
	 * @param l
	 * @return
	 */
	protected int indexFor(long l)  {
		try {
			if(size>(1<<(capacityPowerOfTwo-1))) {
				grow();
			}
			int candidateIndex = (int) (l >>> (64 - capacityPowerOfTwo));
			rawRafile.seek(candidateIndex*8);
			while (true) {
				long atIndex;
				try {
					atIndex = rawRafile.readLong();
				} catch (EOFException e) {
					rawRafile.seek(0);
					continue;
				}
				if (atIndex==0) {
					// not present: return negative insertion index -1 
					return -candidateIndex-1;
				}
				if (atIndex==l) {
					// present: return actual position
					return candidateIndex;
				}
				candidateIndex++;
			}
		} catch (IOException e) {
			// TODO Convert to runtime exception?
			e.printStackTrace();
			return 0;
		}
	}


	/* (non-Javadoc)
	 * @see org.archive.util.LongSet#remove(long)
	 */
	public boolean remove(long l) {
		if(l==0) {
			if (containsZero) {
				containsZero=false;
				size--;
				return true;
			} else {
				return false;
			}
		}
		int i;
		try {
			i = indexFor(l);
			if (i<0) {
				// not present, not changed
				return false;
			}
			removeAt(i);
			return true;
		} catch (IOException e) {
			// TODO Convert to runtime exception?
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param i
	 */
	private void removeAt(int index) throws IOException {
		// TODO: make this more efficient
		rawSet(index,0);
		size--;
		int probeIndex = index+1;
		while(true) {
			long atIndex = rawRafile.readLong();
			if (atIndex==0) {
				break;
			}
			rawSet(probeIndex,0);
			rawSet(indexFor(atIndex),atIndex);
			probeIndex++;
			rawRafile.seek(probeIndex*8);
		}
	}

	/**
	 * 
	 */
	private void grow() throws IOException {
		RandomAccessFile oldRaw = rawRafile;
		
		capacityPowerOfTwo++;
		File tmpDisk = new File(disk.getAbsolutePath()+".tmp");
		rawRafile = new RandomAccessFile(tmpDisk,"rw");
		for(long l=0;l<(1<<capacityPowerOfTwo);l++) {
			rawRafile.writeLong(0);
		}
		size=0;
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
	}

}
