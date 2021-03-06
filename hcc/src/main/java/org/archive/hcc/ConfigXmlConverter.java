package org.archive.hcc;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ConfigXmlConverter {
	
	private File configFile = null;
	private static final String MAX_INSTANCES_ATTRIBUTE = "maxInstances";
	private static final String DEFAULT_SETTINGS_DIRECTORY_ATTRIBUTE= "defaultSettingsDirectory";
	private static final String HOST_ELEMENT = "host";
	private static final String HOST_NAME_ATTRIBUTE = "name";
	private static final String CONTAINER_ELEMENT = "container";
	private static final String JMX_PORT_ATTRIBUTE = "jmxPort";
	private static final String HERITRIX_JMX_USERNAME = "heritrixJmxUsername";
	private static final String HERITRIX_JMX_PASSWORD = "heritrixJmxPassword";
	
	
	public ConfigXmlConverter(File file){
		this.configFile = file;
	}
	
	private Element getRoot() throws DocumentException{
		SAXReader reader = new SAXReader();
	        Document document = reader.read(configFile);
	        return document.getRootElement();
	}
	
	public String getDefaultSettingsDirectory(){
		return getRootAttribute(DEFAULT_SETTINGS_DIRECTORY_ATTRIBUTE);
	}
	
	
	public String getHeritrixJmxUsername(){
		return getRootAttribute(HERITRIX_JMX_USERNAME);
	}
	
	public String getHeritrixJmxPassword(){
		return getRootAttribute(HERITRIX_JMX_PASSWORD);
	}

	private String getRootAttribute(String attributeName){
	    try {
			return getElementStringAttribute(getRoot(), attributeName);
		} catch (DocumentException e) {
	        throw new RuntimeException("Error reading file " +
	  	          configFile.getAbsolutePath() + ". trying to get attribute[" +attributeName+"]", e);
		}
	}



	
	public List<Container> getContainers(){
	    try {
	        Element root = getRoot();
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
	  			return attribute.getValue();
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
	

	

}
