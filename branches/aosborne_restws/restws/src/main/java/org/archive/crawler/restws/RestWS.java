package org.archive.crawler.restws;

import java.io.InputStream;

import org.archive.crawler.client.CrawlerCluster;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.resource.InputRepresentation;

public class RestWS extends WadlApplication {
	private CrawlerCluster cluster = new CrawlerCluster();
	
    public RestWS(Context context) {
		super(context, new InputRepresentation(getWadlStream(),
				MediaType.APPLICATION_WADL_XML));
		
		//getRouter().attach("/doc", WadlResource.class);
	}

    
    public static InputStream getWadlStream() {
		InputStream wadl = RestWS.class.getResourceAsStream("wadl.xml");
		if (wadl == null) {
			throw new RuntimeException("org/archive/crawler/restws/wadl.xml resource was not loadable!");
		}
    	return wadl;
    }
    
    public static void main(String[] args) {
    	// TODO: add some command-line options to set port, password etc.
        try {
            Component component = new Component();
            component.getServers().add(Protocol.HTTP, 8182);

            component.getDefaultHost().attach(
                    new RestWS(component.getContext()));
            component.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public CrawlerCluster getCluster() {
		return cluster;
	}


}
