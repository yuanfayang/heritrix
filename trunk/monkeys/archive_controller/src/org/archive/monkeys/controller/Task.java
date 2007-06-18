package org.archive.monkeys.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.json.simple.JSONObject;

/**
 * Task objects contain all the relevant information about a task. Each task
 * object contains the task data which is passed to the browser monkeys and metadata
 * (such as status) which is used by the controller. 
 * @author Eugene Vahlis
 */
public class Task {
	 
	/**
	 * Represents the current status of the task.
	 * @author Eugene Vahlis
	 */
	public enum Status {
		FREE, ASSIGNED, FAILED, COMPLETE, CANCELLED
	};

	private static final Status[] CANCELLABLE_STATES = { Status.FREE,
			Status.FAILED };

	private long id;
	
	private int numAttempts;

	private Status status;

	private JSONObject taskData;

	private DefaultController controller;

	private String monkeyId;
	
	private long assignmentTime;
	private long completionTime;

	@SuppressWarnings("unchecked")
	/**
	 * Creates a new Task.
	 * @param taskData The external task data of this task
	 * @param controller The controller which will be managing this task
	 * @param id The controller assigned id for this task
	 */
	public Task(JSONObject taskData, DefaultController controller, long id) throws IllegalArgumentException {
		if (taskData == null) {
			throw new IllegalArgumentException("Task data cannot be null");
		}
		this.taskData = taskData;
		this.controller = controller;
		this.id = id;
		this.taskData.put("id", id);
		this.status = Status.FREE;
		
		this.numAttempts = 0;
		
		this.assignmentTime = -1;
		this.completionTime = -1;
	}

	
	public long getAssignmentTime() {
		return assignmentTime;
	}
	
	public long getCompletionTime() {
		return completionTime;
	}
	
	public void setAssignmentTime(long time) {
		assignmentTime = time;
	}
	
	public void setCompletionTime(long time) {
		completionTime = time;
	}
	
	
	/**
	 * Returns the status of this task.
	 */
	public Status getStatus() {
		return status;
	}

	
	public int getNumAttempts() {
		return numAttempts;
	}
	
	public void setNumAttempts(int num) {
		numAttempts = num;
	}
	
	
	
	/**
	 * Sets the status of this task.
	 * @param status The new status
	 */
	public void setStatus(Status status) {
		if (status != Status.ASSIGNED) {
			; //setMonkeyId(null);
		}
		this.status = status;
	}

	/**
	 * Returns the id of this task.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the external task data for this task.
	 */
	public JSONObject getTaskData() {
		return taskData;
	}

	/**
	 * Returns the ID of the monkey to which this task is assigned.
	 * If this task is not in the ASSIGNED status then null is returned.
	 */
	public String getMonkeyId() {
		return monkeyId;
	}

	/**
	 * Sets the ID of the monkey to which this task is assigned.
	 */
	public void setMonkeyId(String monkeyId) {
		this.monkeyId = monkeyId;
	}

	/**
	 * Cancels this task.
	 * @throws Exception If the task cannot be cancelled.
	 */
	public void cancel() throws Exception {
		if (Arrays.binarySearch(CANCELLABLE_STATES, status) >= 0) {
			setStatus(Status.CANCELLED);
			controller.removeTaskFromCollections(this);
		} else {
			throw new Exception("Task state " + status
					+ " indicates it cannot be cancelled");
		}
	}
	
	/**
	 * Assigns this task to a monkey.
	 * @param monkeyId The monkey's ID.
	 */
	public void assign(String monkeyId) {
		setMonkeyId(monkeyId);
		setStatus(Status.ASSIGNED);
	}
	
	/**
	 * Marks this task as successful and calls the necessary
	 * controller methods.
	 */
	public void success() {
		this.setStatus(Status.COMPLETE);
	}
	
	/**
	 * Marks this task as free
	 */
	public void setStatusFree() {
		
		this.setStatus(Status.FREE);
		this.setMonkeyId(null);
	}
	
	/**
	 * Marks this task as a failure and calls the necessary controller
	 * methods.
	 */
	public void fail() {
		setStatus(Status.FAILED);
	}

	@Override
	/**
	 * Compares this task to an object. The result will be true if and only
	 * if the other object is a Task and both tasks have the same ID.
	 */
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Task)) {
			return false;
		} else {
			Task o = (Task) arg0;
			return this.id == o.getId();
		}
	}
	
	private void processAuthInfo() throws MalformedURLException {
		if (!(taskData.get("auth") == null)) {
			return;
		}
		
		URL url = new URL((String) taskData.get("URL"));
		JSONObject auth = ((JSONObject) taskData.get("auth"));
		auth.put("scheme", url.getProtocol());
		auth.put("host", url.getHost());
		auth.put("port", url.getPort());
		auth.put("path", url.getPath());
	}

}
