package org.archive.monkeys.controller.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.archive.monkeys.controller.DefaultController;
import org.archive.monkeys.controller.NanoContainer;
import org.archive.monkeys.controller.Task;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ControllerAdminServlet extends ControllerInterfaceServlet {

	private static final long serialVersionUID = -4464454056276630172L;

	public void doSubmitTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		
		
		
		// check that we got the correct HTTP method in the request
		if (!request.getMethod().equals("POST")) {
			log.warn("Request wasn't POST, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a POST request.");
		} else {
			log.debug("Request OK, parsing data.");
			log.debug("Content length is: " + request.getContentLength());
			log.debug("Query string is: " + request.getQueryString());
			log.debug("Request URL was: " + request.getRequestURI());
			
			//log.debug(request.getInputStream().available());
			log.debug("before reader");

			// read POST data
		
			String content = "";
	
			content = request.getParameter("jsondata");

			log.debug("content is " + content);

			//content = "{\"URL\":\"www.google.com\",\"auth\":{},\"operation\":\"linksGetter\"}";	
		
			
			// parse the JSON data read from the stream
			JSONObject taskData = (JSONObject) JSONValue.parse(content);

			try {
				// try to submit the task to the controller
				long taskId = controller.submitTask(taskData);
				log.debug("Submitted task status: "
						+ controller.getTaskStatus(taskId));

				response.setStatus(200);
				response.getWriter().println(taskId);
			} catch (Exception e) {
				log.error("Request failed. Reason was: " + e.getMessage());
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			} finally {
				response.flushBuffer();
			}
		}
	}

	public void doGetTaskStatus(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			long taskId = Long.parseLong(request.getParameter("tid"));
			try {
				String status = controller.getTaskStatus(taskId).toString();
				response.setStatus(200);
				response.getWriter().println(status);
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doReIssueFailedTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			long taskId = Long.parseLong(request.getParameter("tid"));
			try {
				
				controller.reIssueFailedTask(taskId);
				response.setStatus(200);
				response.getWriter().println(taskId);
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	
	
	
	public void doShowQueue(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			response.setStatus(200);
			JSONArray res = new JSONArray();
			for (Iterator<Task> it = controller.getFreeTasksIterator(); it
					.hasNext();) {
				res.add(it.next().getTaskData());
			}
			response.getWriter().println(res.toString());
		}
	}

	public void doShowAssignedState(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			response.setStatus(200);
			JSONArray res = new JSONArray();
			for (Iterator<Task> it = controller.getAssignedTasksIterator(); it
					.hasNext();) {
				res.add(it.next().getTaskData());
			}
			response.getWriter().println(res.toString());
		}
	}

	public void doShowFailedTasks(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			response.setStatus(200);
			JSONArray res = new JSONArray();
			for (Iterator<Task> it = controller.getFailedTasksIterator(); it
					.hasNext();) {
				res.add(it.next().getTaskData());
			}
			response.getWriter().println(res.toString());
		}
	}

	public void doCancelTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			long taskId = Long.parseLong(request.getParameter("tid"));
			try {
				controller.cancelTask(taskId);
				response.setStatus(200);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doTaskReport(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			log.warn("Request wasn't GET, returning error.");
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			try {
				HashMap<Long, String> rep = controller.getTaskReport();
				response.setStatus(200);
				
				// modifying the following to not print html
				
				//response.getWriter().print("<table><tr><th>Task id</th><th>Status</th></tr>");
				/*for (long tid : rep.keySet()) {
					response.getWriter().print("<tr><td>" + tid + "</td><td>" + rep.get(tid) +
							"</td></tr>");
				}*/
				
				for (long tid : rep.keySet()) {
					
									
					response.getWriter().print(tid + " " + rep.get(tid) + "\n");
				}	
				
				response.getWriter().println();
				
				//response.getWriter().println("</table>");
				
//				
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

}
