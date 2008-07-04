package org.archive.crawler.client;

import java.io.IOException;
import java.io.Reader;

/**
 * JobInputStream wraps Engine#readFile() method to allow
 * reading of a job file over JMX.  It's probably a good idea to wrap this
 * in a BufferedReader.
 * @author ato
 *
 */
public class JobFileReader extends Reader {
	private Job job;
	private String settingsPath;
	private long pos = 0;
	
	public JobFileReader(Job job, String settingsPath) {
		this.job = job;
		this.settingsPath = settingsPath;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int read(char[] b, int off, int len) throws IOException {
		String data = job.getCrawler().getEngine().readFile(job.getJobId(),
		settingsPath, null, pos, len);
		if (data.length() == 0) return -1;
		
		data.getChars(0, data.length(), b, off);
		pos += data.length();
		return data.length();
	}
}
