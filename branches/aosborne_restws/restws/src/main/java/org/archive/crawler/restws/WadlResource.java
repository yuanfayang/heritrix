package org.archive.crawler.restws;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public class WadlResource extends BaseResource {
	public WadlResource() {
	}

	public WadlResource(Context context, Request request, Response response) {
		super(context, request, response);
	}
	
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return new InputRepresentation(RestWS.getWadlStream(), variant.getMediaType());
	}

	@Override
	protected void setup() {
		getVariants().add(new Variant(MediaType.APPLICATION_WADL_XML));
		getVariants().add(new Variant(MediaType.APPLICATION_XML));
	}

}
