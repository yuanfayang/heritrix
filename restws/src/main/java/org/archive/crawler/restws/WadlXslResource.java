package org.archive.crawler.restws;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public class WadlXslResource extends BaseResource {
	public WadlXslResource() {
	}

	public WadlXslResource(Context context, Request request, Response response) {
		super(context, request, response);
	}
	
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return new InputRepresentation(RestWS.class.getResourceAsStream("wadl_documentation.xsl"), variant.getMediaType());
	}

	@Override
	protected void setup() {
		getVariants().add(new Variant(MediaType.APPLICATION_W3C_XSLT));
		getVariants().add(new Variant(MediaType.APPLICATION_XML));
	}

}
