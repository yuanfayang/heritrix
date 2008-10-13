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
	private File configFile = null;
	private int defaultMaxInstancesPerContainer = 1;
	private static final String MAX_INSTANCES_ATTRIBUTE = "maxInstances";
	private static final String DEFAULT_SETTINGS_DIRECTORY_ATTRIBUTE= "defaultSettingsDirectory";
	private static final String HOST_ELEMENT = "host";
	private static final String HOST_NAME_ATTRIBUTE = "name";

	private static final String CONTAINER_ELEMENT = "container";
	private static final String JMX_PORT_ATTRIBUTE = "jmxPort";
	
	private Integer defaultMaxInstances = 1;
	
	private Element getRoot() throws DocumentException{
		SAXReader reader = new SAXReader();
	        Document document = reader.read(configFile);
	        return document.getRootElement();
	}
	
	public String getDefaultSettingsDirectory(){
	    try {
			return getElementStringAttribute(getRoot(), DEFAULT_SETTINGS_DIRECTORY_ATTRIBUTE);
		} catch (DocumentException e) {
	        throw new RuntimeException("Error reading file " +
	  	          configFile.getAbsolutePath() + ".", e);
		}
	}
	
	
	
	public List<Container> getContainers(){
	    try {
	        Element root = getRoot();
		    Integer mi = getElementIntAttribute(root, MAX_INSTANCES_ATTRIBUTE);
		    
		    if(mi != null){
		    	this.defaultMaxInstances = mi;
		    }
		    
			List<Container> containers = new LinkedList<Container>();

		    
		    for (Object elementObj : root.elements(HOST_ELEMENT)) {
		      Element host = (Element) elementObj;
		      String hostName = getNonNullAttributeForElement(host, HOST_NAME_ATTRIBUTE).getValue();
		    
		      for(Object containerObj : host.elements(CONTAINER_ELEMENT)){
		    	  Element container = (Element)containerObj;
			      Integer jmxPort = getElementIntNonNullAttribute(container, JMX_PORT_ATTRIBUTE);
			      Integer maxInstances = getElementIntAttribute(container, MAX_INSTANCES_ATTRIBUTE);
					Container c = new Container(new InetSocketAddress(hostName, jmxPort), maxInstances);
					containers.add(c);

		      }
		    }
		    
		    return containers;
	      } catch (DocumentException e) {
	        throw new RuntimeException("Error reading file " +
	          configFile.getAbsolutePath() + ".", e);
	      }
	}

	  
	  private String getElementStringAttribute(Element element, String attributeName){
	  		Attribute attribute = element.attribute(attributeName);
	  		if(attribute != null){
	  			return attribute.toString();
	  		}
	  		return null;
	  }

	  private Integer getElementIntAttribute(Element element, String attributeName) {
  		Attribute attribute = element.attribute(attributeName);
  		if(attribute != null){
  			return new Integer(attribute.getValue());
  		}
  		return null;
	  }
	
	  private Integer getElementIntNonNullAttribute(
	    Element element, String attributeName) {

	    Attribute attribute = getNonNullAttributeForElement(element, attributeName);
	    return new Integer(attribute.getValue());
	  }

	  private Attribute getNonNullAttributeForElement(
	    Element element, String attributeName) {

	    Attribute attribute = element.attribute(attributeName);
	    if (attribute == null) {
	      throw new RuntimeException("Element " + element.getName() +
	        " missing attribute named " + attributeName + ".");
	    }
	    return attribute;
	  }		
	
	public static Config instance(){
		if(instance == null){
			try{
				String configXml = System.getProperty("hcc.config");
				instance = new Config(configXml);
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		
		return instance;
	}
	
	public Config(String configXml) throws IOException {
		File configXmlFile = new File(configXml);
		configXmlFile.getAbsolutePath();
		if(!configXmlFile.exists()){
			throw new FileNotFoundException(
					"Unable to initialize hcc configuration - " +
					"file not found: " + configXml);
		}
		
		this.configFile = configXmlFile;
		
		checkXml();
	}
	
	private void checkXml(){
		getDefaultSettingsDirectory();
		getContainers();
	}
	

}
