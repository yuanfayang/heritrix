package org.archive.crawler.restws;

import java.io.IOException;

import org.archive.crawler.client.InvalidSheetsException;
import org.archive.crawler.framework.JobStage;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class JobStatusResource extends JobResource {
	public static final Status INVALID_TRANSITION = new Status(409, null, "Invalid status transition", getApiUrl(JobResource.class));

	public JobStatusResource() {
		// TODO Auto-generated constructor stub
	}

	public JobStatusResource(Context context, Request request, Response response) {
		super(context, request, response);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void setup() {
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
	}
	
	/**
	 * Handle GET: read the job status.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return new StringRepresentation(getJob().getStage().toString());
	}
	
	/**
	 * Change the status of the job. Only some status transitions are valid, others will return an error.
	 *
     * To launch a NASCENT job, set the status to PREPARING.
	 * To pause a RUNNING or CHECKPOINTING job, set the status to PAUSING.
     * To resume a PAUSING, PAUSED or CHECKPOINTING job, set the status to RUNNING.
     * To checkpoint a RUNNING, PAUSING or PAUSED job, set the status to CHECKPOINTING.
     * To stop a job with any status, set the status to STOPPING.
	 */
	@Override
	public void storeRepresentation(Representation entity)
			throws ResourceException {
		String newStatus;
		try {
			newStatus = entity.getText().toUpperCase();
		} catch (IOException e1) {
			throw new ResourceException(500, e1);
		}
		
		if (newStatus.equals("PREPARING")) {
			if (getJob().getStage().equals(JobStage.READY)) {
				try {
					getJob().launch();
				} catch (IllegalStateException e) {
					throw new ResourceException(409, e);				
				} catch (InvalidSheetsException e) {
					e.printStackTrace();
					
					throw new ResourceException(409, e);
				} catch (Exception e) {
					e.printStackTrace();
					throw new ResourceException(500, e);
				}
			} else {
				throw new ResourceException(INVALID_TRANSITION);
			}
			
		} else {
			throw new ResourceException(400, null, "Unknown status: " + newStatus, getApiUrl(JobResource.class));
		}
		
		
		try {
			getJob().setSeeds(entity.getText());
		} catch (IOException e) {
			throw new ResourceException(500, e);
		}
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}

}
