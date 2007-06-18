package org.archive.monkeys.controller.interfaces;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.archive.monkeys.controller.Controller;
import org.archive.monkeys.controller.ControllerFactory;

public abstract class ControllerInterfaceServlet extends HttpServlet{
	protected Controller controller;
	
	protected Logger log;
	
	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);
		
		
		log = Logger.getLogger(this.getClass());
		
		//BasicConfigurator.configure();
		//log.setAdditivity(false);
		
		controller = ControllerFactory.getController();
		
		
	}

	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// get the special method name if any
		String method = request.getParameter("method");
		log.debug("Got method " + method);
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

//	public void doInitController(HttpServletRequest request,
//			HttpServletResponse response) throws ServletException, IOException {
//		long id = Long.parseLong(request.getParameter("cid"));
//		controller = (Controller) NanoContainer.getInstance().get(id);
//		response.setContentType("text/html");
//		response.setStatus(200);
//	}
}
