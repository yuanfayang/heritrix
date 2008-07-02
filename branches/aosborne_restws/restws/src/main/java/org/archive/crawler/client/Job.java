package org.archive.crawler.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.management.ObjectName;

import org.archive.crawler.framework.Engine;
import org.archive.crawler.framework.JobStage;
import org.archive.settings.jmx.JMXSheetManager;

public class Job {
	final private static String SEEDS_FILE = "root:seeds:seedsfile";

	/**
	 * The Crawler which owns this job.
	 */
	private Crawler crawler;
	
	/**
	 * The name of this job.
	 */
	private String name;
	
	private JobStage stage;
	
	
	public Job() {
	}
	
	public Job(Crawler crawler, String jobId) {
		setCrawler(crawler);
		setJobId(jobId);
	}
	
	/**
	 * Copy the job to a new profile or a new ready job.
	 * @param targetName
	 * @param targetStage
	 * @return the newly created job.
	 * @throws IOException 
	 */
	public Job copy(String targetName, JobStage targetStage) throws IOException {
		String targetId = JobStage.encode(targetStage, targetName);
		getCrawler().getEngine().copy(getJobId(), targetId);
		return new Job(crawler, targetId);
	}
	
	/**
	 * Delete the job.
	 */
	public void delete() {
		getCrawler().getEngine().deleteJob(getJobId());
	}
	
	/**
	 * Return true if this job is a profile.
	 * @return
	 */
	public boolean isProfile() {
		return JobStage.PROFILE.equals(stage);
	}
	
	/**
	 * Return a list of sheets associated with this job.
	 * @param jobId
	 */
	public Collection<Sheet> getSheets() {
		LinkedList<Sheet> sheets = new LinkedList<Sheet>();
		JMXSheetManager sheetManager = getJMXSheetManager();
		for (String name: sheetManager.getSheets()) {
			sheets.add(new Sheet(this, name));
		}			
		return sheets;
	}
	
	/**
	 * Get the Sheet Manager associated with this job.
	 * @throws IOException 
	 */
	public JMXSheetManager getJMXSheetManager() {
		ObjectName oname;
		try {
			// FIXME: this may not work for ACTIVE jobs?
			oname = getCrawler().getEngine().getSheetManagerStub(getJobId());
		} catch (IOException e) {
			/* FIXME: handle this better */
			throw new RuntimeException("Error getting sheet manager", e);
		}
		return getCrawler().getObject(oname, JMXSheetManager.class);
	}
	
	/**
	 * Return a reader for the seeds list.
	 */
	public BufferedReader getSeedsReader() {
		return new BufferedReader(new JobFileReader(this, SEEDS_FILE));
	}
	
	/**
	 * Set the contents of the seeds file.
	 * @throws IOException 
	 */
	public void setSeeds(String seeds) throws IOException {
		getCrawler().getEngine().writeLines(getJobId(), SEEDS_FILE, seeds);
	}
	
	/**
	 * Return the size in characters of the seeds list.
	 */
	public long getSeedsSize() {
		Engine engine = getCrawler().getEngine();
		return engine.getFileSize(getJobId(), SEEDS_FILE);
	}
	
	private void setJobId(String jobId) {
		setStage(JobStage.getJobStage(jobId));
		setName(JobStage.getJobName(jobId));
	}
	
	public String getJobId() {
		return JobStage.encode(stage, name);
	}

	public JobStage getStage() {
		return stage;
	}

	public void setStage(JobStage stage) {
		this.stage = stage;
	}

	public Crawler getCrawler() {
		return crawler;
	}

	public void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return "<Job: " + getJobId() + ">";
	}
}
