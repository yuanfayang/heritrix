package org.archive.crawler.restws;

import java.io.IOException;

import org.archive.crawler.client.Job;
import org.archive.crawler.framework.JobStage;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class JobResource extends CrawlerResource {
	public static final Status NOT_FOUND = new Status(404, null, "Job not found", getApiUrl(CrawlerResource.class));
	
	private String jobName;

	public JobResource() {
		setup();
	}

	public JobResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.jobName = (String) request.getAttributes().get("job");
	}

	/**
	 * Handle GET: Return job info.
	 * 
	 * TODO: for now this just returns the job's stage.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return new StringRepresentation(getJob().getStage().getLabel());
	}
	
	/**
	 * Handle POST: Copy the job.
	 */
	@Override
	public void acceptRepresentation(Representation entity)
			throws ResourceException {

		Form form = new Form(entity);
		String name = validateJobName(form);		
		JobStage stage = validateJobStage(form);
		
		// perform copy
		Job target;
		try {
			target = getJob().copy(name, stage);
		} catch (IOException e) {
			throw new ResourceException(500, e);
		}

		copyWasSucessful(target);
	}

	private void copyWasSucessful(Job target) throws ResourceException {
		getResponse().setStatus(Status.SUCCESS_CREATED);
		Representation rep = new StringRepresentation("Job created",
				MediaType.TEXT_PLAIN);
		Reference url = new Reference(getRequest().getRootRef().toString()
				+ "/" + getCrawler().getId()).addSegment("jobs").addSegment(
				target.getName());
		rep.setIdentifier(url);
		getResponse().setEntity(rep);
		getResponse().setLocationRef(url);
	}

	private JobStage validateJobStage(Form form) throws ResourceException {
		JobStage stage;
		try {
			stage = JobStage.getJobStage(form.getFirstValue("stage", "ready") + "-");
		} catch (IllegalArgumentException e) {
			stage = null;
		}
		if (stage == null || (!stage.equals(JobStage.READY) && !stage.equals(JobStage.PROFILE))) {
			throw new ResourceException(400, null,
					"Argument 'stage' must be 'ready' or 'profile'",
					getApiUrl(JobResource.class));
		}
		return stage;
	}

	private String validateJobName(Form form) throws ResourceException {
		String name = form.getFirstValue("name");
		if (name == null || name.isEmpty()) {
			throw new ResourceException(400, null,
					"Argument 'name' must be specified",
					getApiUrl(JobResource.class));
		}

		if (name.contains("/")) {
			throw new ResourceException(400, null,
					"Argument 'name' cannot contain '/'",
					getApiUrl(JobResource.class));
		}

		if (getCrawler().getJob(name) != null) {
			throw new ResourceException(409, null,
					"Target already exists",
					getApiUrl(JobResource.class));
		}
		return name;
	}
	
	/**
	 * Handle DELETE: remove the job
	 */
	@Override
	public void removeRepresentations() throws ResourceException {
		getJob().delete();
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}
	
	public Job getJob() throws ResourceException {
		Job job = getCrawler().getJob(jobName);
		if (job == null) {
			throw new ResourceException(NOT_FOUND);
		}
		return job;
	}

	@Override
	protected void setup() {
		setModifiable(true);
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
	}

}
