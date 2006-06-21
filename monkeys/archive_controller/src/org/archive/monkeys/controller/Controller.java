package org.archive.monkeys.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.json.simple.JSONObject;

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
	}
	
	// Methods used by the operator-controller interface
	
	public void submitTask(JSONObject taskData) throws Exception {
		long tId = nextTaskId();
		Task t = new Task(taskData, this, tId);
		freeTasks.add(t);
		allTasks.put(tId, t);
	}
	
	public Iterator<Task> getFreeTasksIterator() {
		return freeTasks.iterator();
	}
	
	public Iterator<Task> getAssignedTasksIterator() {
		return assignedTasks.values().iterator();
	}
	
	public Task.Status getTaskStatus(long taskId) throws IllegalArgumentException {
		Task t = allTasks.get(taskId);
		if (t == null) {
			throw new IllegalArgumentException("Task with given ID doesn't exist");
		} else {
			return t.getStatus();
		}
	}
	
	public void cancelTask(long taskId) throws Exception {
		Task t = allTasks.get(taskId);
		if (t == null) {
			throw new IllegalArgumentException("Task with given ID doesn't exist");
		}
		
		t.cancel();
	}
	
	public Iterator<Task> getFailedTasksIterator() {
		return failedTasks.iterator();
	}
	
	// methods used by the monkey-controller interface
	
	public JSONObject getTask(String monkeyId) throws Exception {
		if (monkeyToAssignedTask.get(monkeyId) != null) {
			monkeyToAssignedTask.get(monkeyId).fail();
		}
		
		Task t = freeTasks.poll();
		
		if (t == null) {
			throw new Exception("There are no free tasks.");
		}
		
		t.assign(monkeyId);
		
		return t.getTaskData();
	}
	
	public void completeTask(String monkeyId, long taskId) throws Exception {
		Task t = monkeyToAssignedTask.get(monkeyId);
		
		//TODO handle null
		if (t.getId() != taskId) {
			t.fail();
			throw new Exception("Completed the wrong task.");
		} else {
			
		}
	}
	
	// helper methods 
	
	protected void taskAssigned(Task t) {
		assignedTasks.put(t.getId(), t);
		monkeyToAssignedTask.put(t.getMonkeyId(), t);
	}
	
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
	
	private long nextTaskId() {
		return taskCounter++;
	}
}
