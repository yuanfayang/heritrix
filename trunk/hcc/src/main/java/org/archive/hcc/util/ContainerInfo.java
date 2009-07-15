package org.archive.hcc.util;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;

public class ContainerInfo implements Serializable{
	private InetSocketAddress address;
	private int maxInstances;
	private List<CrawlerInfo> crawlerInfoList;

	public ContainerInfo(InetSocketAddress address, int maxInstances,
			List<CrawlerInfo> crawlerInfoList) {
		super();
		this.address = address;
		this.maxInstances = maxInstances;
		this.crawlerInfoList = crawlerInfoList;
	}

	public InetSocketAddress getAddress() {
		return address;
	}
	public int getMaxInstances() {
		return maxInstances;
	}
	public List<CrawlerInfo> getCrawlerInfoList() {
		return crawlerInfoList;
	}
	
	
}
