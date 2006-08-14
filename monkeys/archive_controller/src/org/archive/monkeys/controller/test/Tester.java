package org.archive.monkeys.controller.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.archive.monkeys.controller.Controller;
import org.archive.monkeys.controller.DefaultController;
import org.json.simple.JSONObject;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHandler;

public class Tester {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Controller controller = new DefaultController();

		HttpServer server = new HttpServer();
		try {
			server.addListener(":8081");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HttpContext context = server.getContext("/");
		ServletHandler handler= new ServletHandler();
		handler.addServlet("monkey","/monkey",
		                   "org.archive.monkeys.controller.interfaces.ControllerMonkeyServlet");
		handler.addServlet("admin","/admin",
        					"org.archive.monkeys.controller.interfaces.ControllerAdminServlet");
		context.addHandler(handler);

		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			HttpURLConnection c = (HttpURLConnection) (new URL(
					"http://localhost:8081/monkey?method=initController&cid="
							+ controller.getNanoId())).openConnection();
			c.connect();
			while (c.getResponseCode() != 200) {
				c.connect();
			}
			
			c = (HttpURLConnection) (new URL(
					"http://localhost:8081/admin?method=initController&cid="
							+ controller.getNanoId())).openConnection();
			c.connect();
			while (c.getResponseCode() != 200) {
				c.connect();
			}
			
			// Trying to submit a new task
			
			createTask();
			
			server.join();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void createTask() throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection c;
		c = (HttpURLConnection) (new URL(
				"http://localhost:8081/admin?method=submitTask").openConnection());
		c.setDoOutput(true);
		c.setRequestMethod("POST");
		c.setUseCaches(false);
		c.setRequestProperty("Content-Type", "text/plain");

		DataOutputStream po = new DataOutputStream(c.getOutputStream());
		JSONObject taskData = new JSONObject();
		taskData.put("URL", "http://www.google.com");
		String content = taskData.toString();
		po.writeBytes(content);
		po.flush();
		po.close();
		
		DataInputStream di = new DataInputStream(c.getInputStream());
		String str;
		while ((str = di.readLine()) != null) {
			System.out.println(str);
		}
		
	}
}
