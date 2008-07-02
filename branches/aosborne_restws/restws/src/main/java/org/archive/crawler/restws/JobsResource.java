package org.archive.crawler.restws;

import org.archive.crawler.client.Job;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class JobsResource extends CrawlerResource {

	public JobsResource() {
		// TODO Auto-generated constructor stub
	}

	public JobsResource(Context context, Request request, Response response) {
		super(context, request, response);
		// TODO Auto-generated constructor stub
	}
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		StringBuilder s = new StringBuilder();
		for (Job job: getCrawler().getJobs()) {
			s.append(job.getName());
			s.append("\n");
		}
		return new StringRepresentation(s);
	}
}
