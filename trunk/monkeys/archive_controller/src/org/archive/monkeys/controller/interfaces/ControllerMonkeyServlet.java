package org.archive.monkeys.controller.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.archive.monkeys.controller.DefaultController;
import org.archive.monkeys.controller.NanoContainer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ControllerMonkeyServlet extends ControllerInterfaceServlet {

	private static final long serialVersionUID = -4388736212099937045L;

	public void doGetTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			String monkeyId = request.getParameter("mid");
			try {
				JSONObject taskData = controller.getTask(monkeyId);
				response.setStatus(200);
				response.getWriter().print(taskData.toString());
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doCompleteTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			String monkeyId = request.getParameter("mid");
			long taskId = Long.parseLong(request.getParameter("tid"));
			try {
				controller.completeTask(monkeyId, taskId);
				response.setStatus(200);
				response.getWriter().println(
						"Task registered as successful: " + taskId);
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doFailTask(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			String monkeyId = request.getParameter("mid");
			long taskId = Long.parseLong(request.getParameter("tid"));
			try {
				controller.failTask(monkeyId, taskId);
				response.setStatus(200);
				response.getWriter().println("OK");
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doHungMonkey(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		if (!request.getMethod().equals("GET")) {
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			String monkeyId = request.getParameter("mid");
			try {
				controller.hungMonkey(monkeyId);
				response.setStatus(200);
				response.getWriter().println("OK");
			} catch (Exception e) {
				response.setStatus(400);
				response.getWriter().println(e.getMessage());
			}
		}
	}

	public void doUrls(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		if (!request.getMethod().equals("POST")) {
			response.setStatus(400);
			response.getWriter().println(
					"This request should be a GET request.");
		} else {
			BufferedReader br = new BufferedReader(request.getReader());
			String line;
			System.err.println("HERE!!!!!");
			while ((line = br.readLine()) != null) {
				System.err.println(line);
			}
			System.err.println("here2!!!!!!!!!!!!!!");
			response.setStatus(200);
			response.getWriter().println("OK");
		}
	}
}
