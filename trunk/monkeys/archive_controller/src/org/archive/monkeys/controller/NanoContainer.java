package org.archive.monkeys.controller;

import java.util.HashMap;
import java.util.Random;

public class NanoContainer {
	private static NanoContainer nano;
	private HashMap<Long, Object> map;
	private Random rand;
	
	private NanoContainer() {
		map = new HashMap<Long, Object>();
		rand = new Random();
	}
	
	public static NanoContainer getInstance() {
		if (nano == null) {
			nano = new NanoContainer();
		}
		return nano;
	}
	
	public long put(Object o) {
		long id = rand.nextLong();
		map.put(id, o);
		return id;
	}
	
	public Object get(long id) {
		return map.get(id);
	}
	
	public boolean remove(long id) {
		return map.remove(id) == null;
	}
}
