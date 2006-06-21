package org.archive.monkeys.harness;

import java.net.HttpURLConnection;
import java.net.URL;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * The harness class is responsible for several tasks:
 * 
 * 1. Starting a browser instance 2. Starting a small HTTP servlet which
 * communicates with the browser and the controller.
 * 
 * @author Eugene Vahlis
 */
public class Harness {
	private Server server;
	private ServletHandler handler;

	/**
	 * Creates a new harness. All necessary initializations are done but the
	 * browser and http servers are not started. User the run method to start
	 * them.
	 */
	public Harness() {
		server = new Server();
		Connector connector = new SelectChannelConnector();
		connector.setPort(8080);
		server.setConnectors(new Connector[] { connector });

		handler = new ServletHandler();
		
		server.setHandler(handler);

		handler.addServletWithMapping(
				"org.archive.monkeys.harness.MonkeyServlet", "/");
	}

	/**
	 * Starts the browser and http server. Will return when both processes exit.
	 * 
	 * @throws Exception
	 *             In case of an error in starting one of the processes.
	 */
	public void start() throws Exception {
		server.start();
		HttpURLConnection c = (HttpURLConnection) (new URL("http://localhost:8080/")).openConnection();
		c.connect();
		while (c.getResponseCode() != 200) {
			c.connect();
		}
		server.join();
	}

	/**
	 * Creates a new harness and runs it.
	 */
	public static void main(String[] args) {
		Harness h = new Harness();
		try {
			h.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
