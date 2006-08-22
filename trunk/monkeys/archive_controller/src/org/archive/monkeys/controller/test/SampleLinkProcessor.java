package org.archive.monkeys.controller.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

public class SampleLinkProcessor extends HttpServlet{
	private static final long serialVersionUID = -641369717541626833L;

	private Queue<String> urls;
	private HashSet<String> pastUrls;

	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);
		urls = new LinkedList<String>();
		pastUrls = new HashSet<String>();
		urls.add("http://www.dmoz.org");
	}

	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// get the special method name if any
		String method = request.getParameter("method");
		if (method == null || method.equals("")) {
			// if it's not a special method then handle the regular
			// POST / GET request
			super.service(request, response);
		} else {
			// otherwise try to call the appropriate instance method
			method = Character.toUpperCase(method.charAt(0))
					+ method.substring(1);
			Class[] argTypes = { HttpServletRequest.class,
					HttpServletResponse.class };
			Object[] args = { request, response };
			try {
				Method handler = this.getClass().getMethod("do" + method,
						argTypes);
				handler.invoke(this, args);
			} catch (NoSuchMethodException e) {
				// If an instance method wasn't found
				// return an error code
				response.setContentType("text/html");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println(
						"The method " + method + " is not handled.");
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
	}
	
	public void doNewUrls(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
	}
	
}
