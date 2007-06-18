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
public class DefaultController implements Serializable, Controller {

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
  public DefaultController() {
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
    
    
    //log.setAdditivity(false);
    
    
    //log.info("Controller created successfully, nano id: " + nanoId);
  }

  
  
  
  
  // Methods used by the operator-controller interface

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#submitTask(org.json.simple.JSONObject)
 */
  public synchronized long submitTask(JSONObject taskData) throws Exception {
    long tId = nextTaskId();
    Task t = new Task(taskData, this, tId);
    freeTasks.add(t);
    allTasks.put(tId, t);
    return tId;
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getFreeTasksIterator()
 */
  public synchronized Iterator<Task> getFreeTasksIterator() {
    return freeTasks.iterator();
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getAssignedTasksIterator()
 */
  public synchronized Iterator<Task> getAssignedTasksIterator() {
    return assignedTasks.values().iterator();
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getTaskStatus(long)
 */
  public synchronized Task.Status getTaskStatus(long taskId) throws Exception {
    Task t = allTasks.get(taskId);
    if (t == null) {
      throw new Exception("Task with given ID doesn't exist");
    } else {
      return t.getStatus();
    }
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#cancelTask(long)
 */
  public synchronized void cancelTask(long taskId) throws Exception {
    Task t = allTasks.get(taskId);
    if (t == null) {
      throw new Exception("Task with given ID doesn't exist");
    }

    t.cancel();
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getFailedTasksIterator()
 */
  public synchronized Iterator<Task> getFailedTasksIterator() {
    return failedTasks.iterator();
  }

  // methods used by the monkey-controller interface

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getTask(java.lang.String)
 */
  public synchronized JSONObject getTask(String monkeyId) throws Exception {
    if (monkeyToAssignedTask.get(monkeyId) != null) {
      monkeyToAssignedTask.get(monkeyId).fail();
    }

    Task t = freeTasks.poll();

    if (t == null) {
      throw new Exception("There are no free tasks.");
    }

    t.assign(monkeyId);
    
    t.setNumAttempts(t.getNumAttempts() + 1);
    
    t.setAssignmentTime(System.currentTimeMillis());
    t.setCompletionTime(-1);
    
    taskAssigned(t);

    return t.getTaskData();
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#completeTask(java.lang.String, long)
 */
  public synchronized void completeTask(String monkeyId, long taskId) throws Exception {
    Task t = monkeyToAssignedTask.get(monkeyId);

    if (t == null) {
      throw new Exception("Monkey does not have an assigned task");
    }

    if (t.getId() != taskId) {
      t.fail();
      throw new Exception("Completed the wrong task.");
    } else {
     
    	//completed the right task
    	
    	t.setCompletionTime(System.currentTimeMillis());
    	
    	
      taskCompleted(t);
      t.success();
    }
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#failTask(java.lang.String, long)
 */
public synchronized void failTask(String monkeyId, long taskId) throws Exception {
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

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#hungMonkey(java.lang.String)
 */
public synchronized void hungMonkey(String monkeyId) throws Exception {
    Task t = monkeyToAssignedTask.get(monkeyId);
    if (t != null) {
      failTask(monkeyId, t.getId());
    }
  }

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getTaskReport()
 */
/*
public synchronized HashMap<Long, Task.Status> getTaskReport() {
    HashMap<Long, Task.Status> res = new HashMap<Long, Task.Status>();
      for (Task t : allTasks.values()) {
        res.put(t.getId(), t.getStatus());
      }
    return res;
  }
*/

public synchronized HashMap<Long, String> getTaskReport() {
    HashMap<Long, String> res = new HashMap<Long, String>();
    
    long currentTime = System.currentTimeMillis();
    for (Task t : allTasks.values()) {
    	
    	String str = ((Object)t.getStatus()).toString() + " " + t.getNumAttempts();
    	str+=" " + t.getAssignmentTime() + " " + t.getCompletionTime() + " " + currentTime;
    	str+=" " + t.getMonkeyId();
    	
    	
        res.put(t.getId(), str);
      }
    return res;
  }



  // helper methods 

  /* (non-Javadoc)
 * @see org.archive.monkeys.controller.Controller#getNanoId()
 */
public synchronized long getNanoId() {
    return nanoId;
  }

  protected synchronized void taskFailed(Task t) {
    assignedTasks.remove(t.getId());
    monkeyToAssignedTask.remove(t.getMonkeyId());
    failedTasks.add(t);
  }

  
  /**
   * Reissues a task from the failed list to the free task list
   * allowing another go for the task to be assigned and processed
   */
 
  public synchronized void reIssueFailedTask(long taskId) throws Exception {
    	
	  	Task t = allTasks.get(taskId);
	    failedTasks.remove(t);
	    
	    t.setStatusFree();
	    t.setAssignmentTime(-1);
	    t.setCompletionTime(-1);
	    
	    freeTasks.add(t);
  }
  
  /**
   * Performs the necessary data structure changes when a task is assigned.
   */
  protected synchronized void taskAssigned(Task t) {
    assignedTasks.put(t.getId(), t);
    monkeyToAssignedTask.put(t.getMonkeyId(), t);
  }

  protected synchronized void taskCompleted(Task t) {
    assignedTasks.remove(t.getId());
    monkeyToAssignedTask.remove(t.getMonkeyId());
    completedTasks.add(t);
  }

  /**
   * Removes a task from all the data structures. This is used when a task
   * is cancelled.
   * @param t The task to be removed
   */
  protected synchronized void removeTaskFromCollections(Task t) {
    freeTasks.remove(t);
    assignedTasks.remove(t.getId());
    completedTasks.remove(t);
    failedTasks.remove(t);
    allTasks.remove(t.getId());

    if (t.equals(monkeyToAssignedTask.get(t.getMonkeyId()))) {
      monkeyToAssignedTask.remove(t.getMonkeyId());
    }
  }

  /**
   * Returns the next free task ID.
   */
  private synchronized long nextTaskId() {
    return taskCounter++;
  }
}
