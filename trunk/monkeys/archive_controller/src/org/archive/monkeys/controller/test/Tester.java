package org.archive.monkeys.controller.test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.archive.monkeys.controller.Controller;
import org.json.simple.JSONObject;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;

public class Tester {
	public static void main(String[] args) {
		Controller controller = new Controller();

		Server server = new Server();
		Connector connector = new SelectChannelConnector();
		connector.setPort(8081);
		server.setConnectors(new Connector[] { connector });

		ServletHandler handler = new ServletHandler();

		server.setHandler(handler);

		handler.addServletWithMapping(
						"org.archive.monkeys.controller.interfaces.ControllerMonkeyServlet",
						"/monkey");
		handler.addServletWithMapping(
				"org.archive.monkeys.controller.interfaces.ControllerAdminServlet",
				"/admin");
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
			
			c = (HttpURLConnection) (new URL(
					"http://localhost:8081/admin?method=submitTask").openConnection());
			c.setDoOutput(true);
			c.setRequestMethod("POST");
			JSONObject taskData = new JSONObject();
			taskData.put("URL", "http://www.google.com");
			(new OutputStreamWriter(c.getOutputStream())).write(taskData.toString());
			c.connect();
			while (c.getResponseCode() != 200) {
				c.connect();
			}
			
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
}
