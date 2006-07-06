package org.archive.monkeys.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * The core class of the Controller part, as defined in the design documentation.
 * A Controller object contains all the data structures of the controller part and the
 * necessary methods needed to manipulate them.
 * @author Eugene Vahlis
 */
public class Controller implements Serializable {

	private static final long serialVersionUID = -5287018642282073804L;

	private Queue<Task> freeTasks;

	private Map<Long, Task> assignedTasks;
	
	private Map<String, Task> monkeyToAssignedTask;

	private List<Task> completedTasks;
	
	private List<Task> failedTasks;
	
	private Map<Long, Task> allTasks;

	private NanoContainer nano;

	private long nanoId;
	
	private long taskCounter;
	
	private Logger log;

	/**
	 * Creates a new controller with all data structures empty.
	 */
	public Controller() {
		freeTasks = new LinkedList<Task>();
		assignedTasks = new HashMap<Long, Task>();
		monkeyToAssignedTask = new HashMap<String, Task>();
		completedTasks = new LinkedList<Task>();
		failedTasks = new LinkedList<Task>();
		allTasks = new HashMap<Long, Task>();
		nano = NanoContainer.getInstance();
		nanoId = nano.put(this);
		taskCounter = 0;
		log = Logger.getLogger(this.getClass());
		BasicConfigurator.configure();
		//log.info("Controller created successfully, nano id: " + nanoId);
	}
	
	// Methods used by the operator-controller interface
	
	/**
	 * Creates a new task using the given task data, if the data is valid,
	 * and inserts it into the free tasks queue.
	 * @param taskData The task data of the new task
	 * @return the new task's ID
	 */
	public long submitTask(JSONObject taskData) throws Exception {
		long tId = nextTaskId();
		Task t = new Task(taskData, this, tId);
		freeTasks.add(t);
		allTasks.put(tId, t);
		return tId;
	}
	
	/**
	 * Returns an iterator over free (unassigned) tasks.
	 */
	public Iterator<Task> getFreeTasksIterator() {
		return freeTasks.iterator();
	}
	
	/**
	 * Returns an iterator over assigned tasks.
	 */
	public Iterator<Task> getAssignedTasksIterator() {
		return assignedTasks.values().iterator();
	}
	
	/**
	 * Returns the status of a task.
	 * @param taskId The id of the task
	 * @return The status of the requested task
	 * @throws IllegalArgumentException If a task with the specified id
	 * doesn't exist
	 */
	public Task.Status getTaskStatus(long taskId) throws IllegalArgumentException {
		Task t = allTasks.get(taskId);
		if (t == null) {
			throw new IllegalArgumentException("Task with given ID doesn't exist");
		} else {
			return t.getStatus();
		}
	}
	
	/**
	 * Attempts to cancel a task
	 * @param taskId The task id
	 * @throws Exception If the task doesn't exist or cannot be cancelled
	 */
	public void cancelTask(long taskId) throws Exception {
		Task t = allTasks.get(taskId);
		if (t == null) {
			throw new IllegalArgumentException("Task with given ID doesn't exist");
		}
		
		t.cancel();
	}
	
	/**
	 * Returns an iterator over the failed tasks
	 */
	public Iterator<Task> getFailedTasksIterator() {
		return failedTasks.iterator();
	}
	
	// methods used by the monkey-controller interface

	/**
	 * Retrieves a task from the free tasks queue, assigns it to the
	 * monkey with the specified ID and returns the associated task data.
	 * @throws Exception If there are no free tasks.
	 */
	public JSONObject getTask(String monkeyId) throws Exception {
		if (monkeyToAssignedTask.get(monkeyId) != null) {
			monkeyToAssignedTask.get(monkeyId).fail();
		}
		
		Task t = freeTasks.poll();
		
		if (t == null) {
			throw new Exception("There are no free tasks.");
		}
		
		t.assign(monkeyId);
		taskAssigned(t);
		
		return t.getTaskData();
	}
	
	/**
	 * Processes a task as complete. 
	 * @param monkeyId id of the monkey that completed the task
	 * @param taskId id of the task
	 * @throws Exception
	 */
	public void completeTask(String monkeyId, long taskId) throws Exception {
		Task t = monkeyToAssignedTask.get(monkeyId);
		
		if (t == null) {
			throw new Exception("Monkey does not have an assigned task");
		}
		
		if (t.getId() != taskId) {
			t.fail();
			throw new Exception("Completed the wrong task.");
		} else {
			taskCompleted(t);
			t.success();
		}
	}
	
	public void failTask(String monkeyId, long taskId) throws Exception {
		Task t = monkeyToAssignedTask.get(monkeyId);
		
		if (t == null) {
			throw new Exception("Monkey does not have an assigned task");
		}
		
		if (t.getId() != taskId) {
			t.fail();
			throw new Exception("Failesd the wrong task.");
		} else {
			taskFailed(t);
			t.fail();
		}
	}
	
	public void hungMonkey(String monkeyId) throws Exception {
		Task t = monkeyToAssignedTask.get(monkeyId);
		if (t != null) {
			failTask(monkeyId, t.getId());
		}
	}

	// helper methods 

	public long getNanoId() {
		return nanoId;
	}

	protected void taskFailed(Task t) {
		assignedTasks.remove(t.getId());
		monkeyToAssignedTask.remove(t.getMonkeyId());
		failedTasks.add(t);
	}

	/**
	 * Performs the necessary data structure changes when a task is assigned.
	 */
	protected void taskAssigned(Task t) {
		assignedTasks.put(t.getId(), t);
		monkeyToAssignedTask.put(t.getMonkeyId(), t);
	}
	
	protected void taskCompleted(Task t) {
		assignedTasks.remove(t.getId());
		monkeyToAssignedTask.remove(t.getMonkeyId());
		completedTasks.add(t);
	}
	
	/**
	 * Removes a task from all the data structures. This is used when a task
	 * is cancelled.
	 * @param t The task to be removed
	 */
	protected void removeTaskFromCollections(Task t) {
		freeTasks.remove(t);
		assignedTasks.remove(t.getId());
		completedTasks.remove(t);
		failedTasks.remove(t);
		allTasks.remove(t.getId());
		
		if (monkeyToAssignedTask.get(t.getMonkeyId()).equals(t)) {
			monkeyToAssignedTask.remove(t.getMonkeyId());
		}
	}
	
	/**
	 * Returns the next free task ID.
	 */
	private long nextTaskId() {
		return taskCounter++;
	}
}
