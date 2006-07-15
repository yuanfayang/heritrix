package org.archive.monkeys.controller.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.archive.monkeys.controller.Controller;
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
			log.debug("Resuest URL was: " + request.getRequestURI());
			log.debug(request.getInputStream().available());
			log.debug("before reader");
			
			// read POST data
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			log.debug("reading");
			String content = "";
			while (br.ready()) {
				content += br.readLine();
			}
			log.debug("!!! " + content);
			
			// parse the JSON data read from the stream
			JSONObject taskData = (JSONObject) JSONValue
					.parse(content);

			try {
				// try to submit the task to the controller
				long taskId = controller.submitTask(taskData);
				log.debug("Submitted task status: " + controller.getTaskStatus(taskId));
				
				response.setStatus(200);
				response.getWriter().println(taskId);
			} catch (Exception e) {
				log.error("Request failed. Reason was: " + e.getMessage() );
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
			String status = controller.getTaskStatus(taskId).toString();
			response.setStatus(200);
			response.getWriter().println(status);
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
			for (Iterator<Task> it = controller.getFreeTasksIterator(); it.hasNext();) {
				res.add(it.next().getTaskData());
			}
			response.getWriter().println(res.toString());
		}
	}

}
