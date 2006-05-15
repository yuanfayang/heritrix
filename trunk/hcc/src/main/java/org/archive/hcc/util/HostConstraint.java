package org.archive.hcc.util;

import java.io.File;

public class HostConstraint {
	
	private String host;
	protected String[] hostArray;
	private String regex = null;
	private Long documentLimit = null;
	private Boolean block = null;
	private Boolean ignoreRobots = null;
	
	
	public HostConstraint(String host){
		this.host = host;
		this.hostArray = host.split("[.]");
	}
	
	
	public String getSettingsFileDirectory(){
		StringBuffer b = new StringBuffer();
		b.append("settings");
		for (int i = hostArray.length-1; i > -1; i--) {
			b.append(File.separator);
			b.append(hostArray[i]);
		}
		
		return b.toString();
	}
	
	

	public String getSettingsFilePath(){
		return getSettingsFileDirectory() + File.separator + "settings.xml";
	}


	public Boolean getBlock() {
		return block;
	}


	public void setBlock(Boolean block) {
		this.block = block;
	}


	public Long getDocumentLimit() {
		return documentLimit;
	}


	public void setDocumentLimit(Long documentLimit) {
		this.documentLimit = documentLimit;
	}


	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}


	public Boolean getIgnoreRobots() {
		return ignoreRobots;
	}


	public void setIgnoreRobots(Boolean ignoreRobots) {
		this.ignoreRobots = ignoreRobots;
	}


	public String getRegex() {
		return regex;
	}


	public void setRegex(String regex) {
		this.regex = regex;
	}
	
}
