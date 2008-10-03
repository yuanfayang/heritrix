package org.archive.crawler.restws;

import java.io.InputStream;

import org.archive.crawler.client.CrawlerCluster;
import org.archive.crawler.framework.EngineConfig;
import org.archive.crawler.framework.EngineImpl;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Route;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.resource.InputRepresentation;

public class RestWS extends WadlApplication {
	private CrawlerCluster cluster;
	private EngineImpl manager;
	
    public RestWS(Context context) {
		super(context, new InputRepresentation(getWadlStream(),
				MediaType.APPLICATION_WADL_XML));
		
		// XXX: debug code
		System.out.println("Defined routes:");
		for (Route r: getRouter().getRoutes()) {
			System.out.println(r.getTemplate().getPattern());	
		}
		
		//EngineConfig config = new EngineConfig();
		//config.setJobsDirectory("/tmp/tmp_heritrix_unit_test_/jobs/");
		//this.manager = new EngineImpl(config);
		
		cluster = new CrawlerCluster();		
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
