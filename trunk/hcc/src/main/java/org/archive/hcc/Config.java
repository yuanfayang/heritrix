package org.archive.hcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class Config {
	private static Config instance = null;
	private List<Container> containers = null;
	private String defaultSettingsDirectory = null;
	private File configFile = null;
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
	}
	
	private Config(String configXml) throws IOException {
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
		if(this.configFile == null){
			throw new RuntimeException("The hcc-config file was never initialized.");
		}
		ConfigXmlConverter c = new ConfigXmlConverter(this.configFile);
		this.defaultSettingsDirectory = c.getDefaultSettingsDirectory();
		this.containers = c.getContainers();
	}

	public String getDefaultSettingsDirectory() {
		return defaultSettingsDirectory;
	}
	
	public void addContainer(String hostname, int jmxPort, int maxInstances){
		this.containers.add(new Container(new InetSocketAddress(hostname, jmxPort), maxInstances));
	}

}
