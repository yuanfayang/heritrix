package org.archive.monkeys.controller;

import org.json.simple.JSONObject;

public class Task {

	public enum Status { FREE, ASSIGNED, FAILED, COMPLETE };
	
	private long id;
	
	private Status status;
	
	private JSONObject taskData;

	public Task(JSONObject taskData, long id) {
		this.taskData = taskData;
		this.id = id;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public JSONObject getTaskData() {
		return taskData;
	}
	
}
