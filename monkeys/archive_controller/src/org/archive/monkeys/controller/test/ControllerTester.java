package org.archive.monkeys.controller.test;

import java.util.Iterator;
import java.util.Vector;

import org.archive.monkeys.controller.Controller;
import org.archive.monkeys.controller.DefaultController;
import org.archive.monkeys.controller.Task;
import org.json.simple.JSONObject;

import junit.framework.TestCase;

public class ControllerTester extends TestCase {
	public void testConstructor() {
		@SuppressWarnings("unused") Controller c = new DefaultController();
	}
	
	@SuppressWarnings("unchecked")
	public void testTaskCreation() {
		Controller c = new DefaultController();
		JSONObject jo = new JSONObject();
		jo.put("testField", "testValue");
		try {
			c.submitTask(jo);
		} catch (Exception e) {
			fail("Shouldn't be exceptions here...");
		}
		
		try {
			c.submitTask(null);
			fail("Should not accept task with null data");
		} catch (Exception e) {
		}
		try {
			c.submitTask(jo);
			c.submitTask(jo);
		} catch (Exception e) {
			fail("Shouldn't be exceptions here...");
		}
	}
	
	public void testFreeTaskIterator() {
		Controller c = new DefaultController();
		JSONObject jo = new JSONObject();
		jo.put("testField", "testValue");
		Vector<Long> taskIds = new Vector<Long>(100);
		try {
			for (int i = 0; i < 100; i++) {
				taskIds.add(c.submitTask(jo));
			}
		} catch (Exception e) {
			fail();
		}
		for (Iterator<Task> it = c.getFreeTasksIterator(); it.hasNext();) {
			Task t = it.next();
			assertTrue(taskIds.remove(t.getId()));
		}
	}
	
	public void testAssignTask() {
		Controller c = new DefaultController();
		JSONObject jo = new JSONObject();
		jo.put("testField", "testValue");
		
		try {
			long t1 = c.submitTask(jo);
			long t2 = c.submitTask((JSONObject) jo.clone());
			JSONObject j1 = c.getTask("testMonkey");
			long assignedId = (Long) j1.get("id");
			if (assignedId != t1 && assignedId != t2) {
				fail();
			}
		} catch (Exception e) {
			fail();
		}
	}
}
