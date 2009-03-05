package org.archive.monkeys.harness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
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
	private HttpServer server;

	private ServletHandler handler;

	private Logger log;

	private Properties conf;
	
	private static String monkeyId = "MonkeyOne";
	
	private static String browserCommand = "firefox -P dev";
	

	/**
	 * Creates a new harness. All necessary initializations are done but the
	 * browser and http servers are not started. User the run method to start
	 * them.
	 * 
	 * @throws IOException
	 *             in case the server fails to start.
	 * @throws FileNotFoundException
	 *             in case the config file cannot be created.
	 */
	
	public Harness() {
		
		
	}
	
	public Harness(String mId, String bCommand) throws FileNotFoundException, IOException {


		Harness.monkeyId =  mId;

		Harness.browserCommand = bCommand;
		
		log = Logger.getLogger(this.getClass());
		//log.setAdditivity(false);
		BasicConfigurator.configure();
		conf = loadOrCreateProperties();
		this.server = new HttpServer();
		this.server.addListener(":" + conf.getProperty("port"));

		
		HttpContext context = this.server.getContext("/");
		ServletHandler handler = new ServletHandler();
		handler.addServlet("harness", conf.getProperty("harness.mapping"),
				"org.archive.monkeys.harness.MonkeyServlet");
		context.addHandler(handler);
	}
	
	
	/**
	 * Starts the browser and http server. Will return when both processes exit.
	 * 
	 * @throws Exception
	 *             In case of an error in starting one of the processes.
	 */
	public void start() throws Exception {
		this.server.start();
		HttpURLConnection c = (HttpURLConnection) (new URL("http://localhost:"
				+ conf.getProperty("port")
				+ conf.getProperty("harness.mapping"))).openConnection();
		c.connect();
		while (c.getResponseCode() != 200) {
			c.connect();
		}
		this.server.join();
	}

	/**
	 * Creates a new harness and runs it.
	 */
	public static void main(String[] args) {
		try {
			Harness h = new Harness(args[0], args[1]);
			h.start();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected static Properties loadOrCreateProperties() throws FileNotFoundException,
			IOException {
		String confDirPath = System.getProperty("user.home")
				+ "/.archive_monkey";
		File confDir = new File(confDirPath);
		if (!confDir.exists()) {
			confDir.mkdir();
		}

		File conf = new File(confDirPath + "/harness_settings.properties");
		Properties p = new Properties();
		if (conf.exists()) {
			p.load(new FileInputStream(conf));
			
		} else {
			// load defaults
			
			p.setProperty("port", "8082");
			p.setProperty("harness.mapping", "/harness");
			
			p.setProperty("browser.timeout", "20000");
			p.setProperty("controller.url", "http://localhost:8081/monkey");
			
			p.store(new FileOutputStream(conf),
					"Archive Monkey Harness Config File");
			
		}

		// this will not be stored into the conf file
		p.setProperty("browser.command", browserCommand);
		p.setProperty("monkey.id", monkeyId);
		
		return p;
	}
}
