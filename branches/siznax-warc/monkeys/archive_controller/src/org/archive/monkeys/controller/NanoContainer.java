package org.archive.monkeys.controller;

import java.util.HashMap;
import java.util.Random;

/**
 * A small which allows objects to have a shared repository or dependencies.
 * This class is a singleton so all the using objects will have the same repository.
 * New objects are added with the put method which returns an ID number that can be 
 * used to retrieve the object from the repository.
 * 
 * @author Eugene Vahlis
 */
public class NanoContainer {
	private static NanoContainer nano;
	private HashMap<Long, Object> map;
	private Random rand;
	
	private NanoContainer() {
		map = new HashMap<Long, Object>();
		rand = new Random();
	}
	
	/**
	 * Returns a nano container. Only one container is created per JVM.
	 * The same container will be returned to all callers within a single JVM.
	 */
	public static NanoContainer getInstance() {
		if (nano == null) {
			nano = new NanoContainer();
		}
		return nano;
	}
	
	/**
	 * Inserts a new object into the repository.
	 * @param o The object to be inserted
	 * @return An ID number which can be used, with the get method, to retrieve o
	 */
	public long put(Object o) {
		long id = rand.nextLong();
		map.put(id, o);
		return id;
	}
	
	/**
	 * Retrieves an object from the repository.
	 * @param id The ID of the object that was returned by put
	 * @return The object associated with the given ID or null if no such object exists
	 */
	public Object get(long id) {
		return map.get(id);
	}
	
	/**
	 * Removes an object from the repository.
	 * @param id the ID of the object that was returned by put
	 * @return true if the object was erased, false if it was not in the repository
	 */
	public boolean remove(long id) {
		return map.remove(id) == null;
	}
}
