package org.archive.monkeys.controller;

import java.util.Arrays;

import org.json.simple.JSONObject;

public class Task {

	public enum Status {
		FREE, ASSIGNED, FAILED, COMPLETE, CANCELLED
	};

	private static final Status[] CANCELLABLE_STATES = { Status.FREE,
			Status.FAILED };

	private long id;

	private Status status;

	private JSONObject taskData;

	private Controller controller;

	private String monkeyId;

	@SuppressWarnings("unchecked")
	public Task(JSONObject taskData, Controller controller, long id) {
		this.taskData = taskData;
		this.controller = controller;
		this.id = id;
		this.taskData.put("id", id);
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		if (status != Status.ASSIGNED) {
			setMonkeyId(null);
		}
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public JSONObject getTaskData() {
		return taskData;
	}

	public String getMonkeyId() {
		return monkeyId;
	}

	public void setMonkeyId(String monkeyId) {
		this.monkeyId = monkeyId;
	}

	public void cancel() throws Exception {
		if (Arrays.binarySearch(CANCELLABLE_STATES, status) >= 0) {
			setStatus(Status.CANCELLED);
			controller.removeTaskFromCollections(this);
		} else {
			throw new Exception("Task state " + status
					+ " indicates it cannot be cancelled");
		}
	}
	
	public void assign(String monkeyId) {
		setMonkeyId(monkeyId);
		setStatus(Status.ASSIGNED);
		controller.taskAssigned(this);
	}
	
	public void success() {
		
	}
	
	public void fail() {
		//TODO implement
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Task)) {
			return false;
		} else {
			Task o = (Task) arg0;
			return this.id == o.getId();
		}
	}

}
