package org.archive.crawler.restws;

import java.io.IOException;

import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ReaderRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public class SeedsResource extends JobResource {
	@Override
	protected void setup() {
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
	}
	
	/**
	 * Handle GET: read the seed list.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		Representation rep = new ReaderRepresentation(getJob().getSeedsReader(),
				MediaType.TEXT_PLAIN, getJob().getSeedsSize());
		rep.setCharacterSet(CharacterSet.UTF_8);
		return rep;
	}

	/**
	 * Handle POST: append some seeds.
	 */
	@Override
	public void acceptRepresentation(Representation entity)
			throws ResourceException {
		/* FIXME: wish we could do a real append, but Engine only exposes
		 *        read and writeLines().
		 */
		char[] buf = new char[(int) getJob().getSeedsSize()];
		try {
			StringBuilder sb = new StringBuilder();
			getJob().getSeedsReader().read(buf, 0, buf.length);
			sb.append(buf);
			if (buf.length > 0 && buf[buf.length-1] != '\n') {
				sb.append('\n');
			}
			sb.append(entity.getText());
			getJob().setSeeds(sb.toString());
		} catch (IOException e) {
			throw new ResourceException(500, e);		
		}
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}

	/**
	 * Handle PUT: replace the seeds list.
	 */
	@Override
	public void storeRepresentation(Representation entity)
			throws ResourceException {
		try {
			getJob().setSeeds(entity.getText());
		} catch (IOException e) {
			throw new ResourceException(500, e);
		}
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}
	
	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public boolean allowPut() {
		return true;
	}


	public SeedsResource() {
	}

	public SeedsResource(Context context, Request request, Response response) {
		super(context, request, response);
	}

}
