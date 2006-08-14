package org.archive.monkeys.controller.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.archive.monkeys.controller.Controller;
import org.archive.monkeys.controller.DefaultController;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * <p>
 * The Controller Driver starts a Jetty web server and the two interface
 * servlets: ControllerAdminServlet and ControllerMonkeyServlet.
 * </p>
 * <p>
 * The configuration for the Controller Driver is available at
 * ${HOME}/.archive_monkey/controller_settings.properties
 * </p>
 * <p>
 * If the configuration file does not exist, it will be created with the default
 * values when the driver is run for the first time.
 * </p>
 * 
 * @author Eugene Vahlis
 */
public class ControllerDriver {

	/**
	 * Loads the driver configuration file or creates one filled with the
	 * default values.
	 * 
	 * @return The configuration Properties object
	 * @throws IOException
	 *             If the config file is unreadable or cannot be created.
	 */
	private static Properties loadOrCreateProperties()
			throws FileNotFoundException, IOException {
		String confDirPath = System.getProperty("user.home")
				+ "/.archive_monkey";
		File confDir = new File(confDirPath);
		if (!confDir.exists()) {
			confDir.mkdir();
		}

		File conf = new File(confDirPath + "/controller_settings.properties");
		Properties p = new Properties();
		if (conf.exists()) {
			p.load(new FileInputStream(conf));
		} else {
			// load defaults
			p.setProperty("port", "8081");
			p.setProperty("admin.mapping", "/admin");
			p.setProperty("monkey.mapping", "/monkey");
			p.store(new FileOutputStream(conf),
					"Archive Monkey Controller Config File");
		}

		return p;
	}

	/**
	 * Creates the beginning of a url depending on the servlet we want to use.
	 * Loads information such as port and mapping from the configuration. Append
	 * the query string to the end of the string returned by this method
	 * (including the ?). For example: <code>
	 * String s = assembleUrlStart(conf, 'admin');
	 * String finalURL = s + "?method=getStatus";
	 * </code>
	 * 
	 * @param conf
	 *            Configuration properties.
	 * @param part
	 *            The name of the servlet we want to contact.
	 * @return A string which is the partial URL for the request.
	 */
	private static String assembleUrlStart(Properties conf, String part) {
		StringBuffer res = new StringBuffer();
		res.append("http://localhost:");
		res.append(conf.getProperty("port"));
		res.append(conf.getProperty(part + ".mapping"));
		return res.toString();
	}

	public static void main(String[] args) {
		try {
			// load the configuration
			Properties conf = loadOrCreateProperties();

			// create the server
			Controller controller = new DefaultController();
			HttpServer server = new HttpServer();
			server.addListener(":" + conf.getProperty("port"));

			HttpContext context = server.getContext("/");
			ServletHandler handler = new ServletHandler();
			
			// register the admin and monkey interface servlets
			handler
					.addServlet("monkey", conf.getProperty("monkey.mapping"),
							"org.archive.monkeys.controller.interfaces.ControllerMonkeyServlet");
			handler
					.addServlet("admin", conf.getProperty("admin.mapping"),
							"org.archive.monkeys.controller.interfaces.ControllerAdminServlet");
			context.addHandler(handler);
			server.start();
			
//			// send initialization requests to the servlets to let them know
//			// which instance of Controller we're using 
//			HttpURLConnection c = (HttpURLConnection) (new URL(
//					assembleUrlStart(conf, "monkey")
//							+ "?method=initController&cid="
//							+ controller.getNanoId())).openConnection();
//			c.connect();
//			while (c.getResponseCode() != 200) {
//				c.connect();
//			}
//			c = (HttpURLConnection) (new URL(assembleUrlStart(conf, "admin")
//					+ "?method=initController&cid=" + controller.getNanoId()))
//					.openConnection();
//			c.connect();
//			while (c.getResponseCode() != 200) {
//				c.connect();
//			}
			server.join();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
