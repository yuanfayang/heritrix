 /** 
 * 
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * Heritrix is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.hcc.util;

import java.io.File;

/**
 * A set of restrictions associated with a host that limit the scope of a crawl.
 * @author Daniel Bernstein (dbernstein@archive.org)
 *
 */
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
