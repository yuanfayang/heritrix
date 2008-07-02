package org.archive.crawler.restws;

import org.archive.crawler.client.CrawlerCluster;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public abstract class BaseResource extends Resource {
	public BaseResource() {
		super();
	}

	public BaseResource(Context context, Request request, Response response) {
		super(context, request, response);
		setup();
	}

	/**
	 * This method should be used by extending classes to add the
	 * accepted variants, call setModifiable() etc so that subclasses
	 * can override it.
	 */
	protected abstract void setup();
	
	public CrawlerCluster getCluster() {
		return ((RestWS)getApplication()).getCluster();
	}
	
    public static String getApiUrl(Class<?> cls) {    	
		return "/wadl.xml##" + cls.getName();
    }
}
