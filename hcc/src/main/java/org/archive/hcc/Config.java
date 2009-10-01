package org.archive.hcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Config {
	private static Log log = LogFactory.getLog(Config.class);

	private static Config instance = null;
	private List<Container> containers = null;
	private String defaultSettingsDirectory = null;
	private File configFile = null;
	private String heritrixJmxUsername = "controlRole";
	private String heritrixJmxPassword = "letmein";
	public static void init(){
		init(true);
	}
	
	public static void init(boolean fromFile){
		
		if(instance == null){
			if(!fromFile){
				instance = new Config();
				return;
			}
			try{
				String configXml = System.getProperty("hcc.config");
				if (configXml == null)
					throw new Exception("system property hcc.config is not set, it must point to your hcc-config.xml");
				instance = new Config(configXml);
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
	}

	public static Config instance(){
		if(instance == null){
			try{
				init();
				return instance;
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		
		return instance;
	}
	
	private Config(){
		this.containers = new LinkedList<Container>();
		log.info("hcc initializing without config file");
	}
	
	private Config(String configXml) throws IOException {
		log.info("hcc initializing from config file: " + configXml);
		File configXmlFile = new File(configXml);
		configXmlFile.getAbsolutePath();
		if(!configXmlFile.exists()){
			throw new FileNotFoundException(
					"Unable to initialize hcc configuration - " +
					"file not found: " + configXml);
		}
		
		this.configFile = configXmlFile;
		refreshFromFile();
	}
	
	public List<Container> getContainers(){
		return new LinkedList<Container>(this.containers);
	}
	
	public void refresh(){
		if(this.configFile != null){
			refreshFromFile();
		}
	}
	

	private void refreshFromFile(){
		// log.info("refreshing crawler config from " + configFile);
		if(this.configFile == null){
			throw new RuntimeException("The hcc-config file was never initialized.");
		}
		ConfigXmlConverter c = new ConfigXmlConverter(this.configFile);
		this.defaultSettingsDirectory = c.getDefaultSettingsDirectory();
		this.heritrixJmxUsername = c.getHeritrixJmxUsername();
		this.heritrixJmxPassword = c.getHeritrixJmxPassword();
		this.containers = c.getContainers();
	}

	public String getDefaultSettingsDirectory() {
		return defaultSettingsDirectory;
	}
	
	public void addContainer(String hostname, int jmxPort, int maxInstances){
		this.containers.add(new Container(new InetSocketAddress(hostname, jmxPort), maxInstances));
	}

	public String getHeritrixJmxUsername() {
		return heritrixJmxUsername;
	}

	public String getHeritrixJmxPassword() {
		return heritrixJmxPassword;
	}

}
