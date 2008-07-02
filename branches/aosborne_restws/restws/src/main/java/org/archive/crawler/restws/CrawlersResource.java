package org.archive.crawler.restws;

import java.util.Collection;

import org.archive.crawler.client.Crawler;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class CrawlersResource extends BaseResource {

	public CrawlersResource() {
		// TODO Auto-generated constructor stub
	}

	public CrawlersResource(Context context, Request request, Response response) {
		super(context, request, response);
	}
	
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		StringBuilder s = new StringBuilder();
		Collection<Crawler> crawlers = getCluster().getCrawlers();
		if (crawlers.isEmpty()) {
			return new StringRepresentation("No crawlers found.");
		}
		for (Crawler crawler: crawlers) {
			s.append(crawler.getId());
			s.append("\r\n");
		}
		return new StringRepresentation(s);
	}

	@Override
	protected void setup() {
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));				
	}
	

}
