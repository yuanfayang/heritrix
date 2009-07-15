package org.archive.hcc.util;

import java.io.Serializable;

import javax.management.ObjectName;

public class CrawlerInfo implements Serializable{
	public ObjectName getName() {
		return name;
	}
	public String getStatus() {
		return status;
	}
	public CrawlerInfo(ObjectName name, String status) {
		super();
		this.name = name;
		this.status = status;
	}
	private ObjectName name; 
	private String status;
}
