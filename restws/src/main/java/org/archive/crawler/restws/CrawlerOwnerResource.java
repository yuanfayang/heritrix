package org.archive.crawler.restws;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.client.Crawler;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class CrawlerOwnerResource extends CrawlerResource {
	
	public CrawlerOwnerResource() {
	}
	

	public CrawlerOwnerResource(Context context, Request request, Response response) {
		super(context, request, response);
	}

	@Override
	public Representation represent(Variant variant) throws ResourceException {
		// TODO
		//return new StringRepresentation("hello from " + getCrawler());
		return null;
	}
	
	@Override
	protected void setup() {
		// TODO
	}
	
}
