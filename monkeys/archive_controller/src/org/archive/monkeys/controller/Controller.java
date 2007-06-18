package org.archive.monkeys.controller;

import java.util.HashMap;
import java.util.Iterator;

import org.archive.monkeys.controller.Task;
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;

public interface Controller {

	/**
	 * Creates a new task using the given task data, if the data is valid,
	 * and inserts it into the free tasks queue.
	 * @param taskData The task data of the new task
	 * @return the new task's ID
	 */
	public abstract long submitTask(JSONObject taskData) throws Exception;

	/**
	 * Returns an iterator over free (unassigned) tasks.
	 */
	public abstract Iterator<Task> getFreeTasksIterator();

	/**
	 * Returns an iterator over assigned tasks.
	 */
	public abstract Iterator<Task> getAssignedTasksIterator();

	/**
	 * Returns the status of a task.
	 * @param taskId The id of the task
	 * @return The status of the requested task
	 * @throws IllegalArgumentException If a task with the specified id
	 * doesn't exist
	 */
	public abstract Task.Status getTaskStatus(long taskId) throws Exception;

	/**
	 * Attempts to cancel a task
	 * @param taskId The task id
	 * @throws Exception If the task doesn't exist or cannot be cancelled
	 */
	public abstract void cancelTask(long taskId) throws Exception;

	/**
	 * Returns an iterator over the failed tasks
	 */
	public abstract Iterator<Task> getFailedTasksIterator();

	/**
	 * Retrieves a task from the free tasks queue, assigns it to the
	 * monkey with the specified ID and returns the associated task data.
	 * @throws Exception If there are no free tasks.
	 */
	public abstract JSONObject getTask(String monkeyId) throws Exception;

	/**
	 * Processes a task as complete. 
	 * @param monkeyId id of the monkey that completed the task
	 * @param taskId id of the task
	 * @throws Exception
	 */
	public abstract void completeTask(String monkeyId, long taskId)
			throws Exception;

	public abstract void failTask(String monkeyId, long taskId)
			throws Exception;

	public abstract void hungMonkey(String monkeyId) throws Exception;
	
	public abstract void reIssueFailedTask(long taskId) throws Exception; 

	//public abstract HashMap<Long, Task.Status> getTaskReport();

	public abstract HashMap<Long, String> getTaskReport();
	
	public abstract long getNanoId();
	
	

}