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

	private NanoContainer nano;

	private long nanoId;
	
	private long taskCounter;

	public Controller() {
		freeTasks = new LinkedList<Task>();
		assignedTasks = new HashMap<Long, Task>();
		completedTasks = new LinkedList<Task>();
		nano = NanoContainer.getInstance();
		nanoId = nano.put(this);
		taskCounter = 0;
	}
	
	public void submitTask(JSONObject taskData) throws Exception {
		Task t = new Task(taskData, nextTaskId());
		freeTasks.add(t);
	}
	
	public Iterator<Task> getFreeTasksIterator() {
		return freeTasks.iterator();
	}
	
	public Iterator<Task> getAssignedTasksIterator() {
		return assignedTasks.values().iterator();
	}
	
	private long nextTaskId() {
		return taskCounter++;
	}
}
