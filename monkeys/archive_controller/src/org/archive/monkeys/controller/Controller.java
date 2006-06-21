package org.archive.monkeys.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.json.simple.JSONObject;

public class Controller implements Serializable {

	private static final long serialVersionUID = -5287018642282073804L;

	private Queue<Task> freeTasks;

	private Map<Long, Task> assignedTasks;

	private List<Task> completedTasks;
	
	private Map<Long, Task> allTasks;

	private NanoContainer nano;

	private long nanoId;
	
	private long taskCounter;

	public Controller() {
		freeTasks = new LinkedList<Task>();
		assignedTasks = new HashMap<Long, Task>();
		completedTasks = new LinkedList<Task>();
		allTasks = new HashMap<Long, Task>();
		nano = NanoContainer.getInstance();
		nanoId = nano.put(this);
		taskCounter = 0;
	}
	
	public void submitTask(JSONObject taskData) throws Exception {
		long tId = nextTaskId();
		Task t = new Task(taskData, tId);
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
		
	}
	
	private long nextTaskId() {
		return taskCounter++;
	}
}
