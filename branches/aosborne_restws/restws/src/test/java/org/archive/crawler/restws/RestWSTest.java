package org.archive.crawler.restws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import junit.framework.TestCase;

import org.archive.crawler.client.Crawler;
import org.archive.crawler.client.CrawlerCluster;
import org.archive.crawler.framework.EngineConfig;
import org.archive.crawler.framework.EngineImpl;
import org.archive.util.FileUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;

public class RestWSTest extends TestCase {
	EngineImpl manager;
	CrawlerCluster cluster;
	RestWS restws;
	String crawlerId;
	File tmpDir;

	public void setUp() throws Exception {
		EngineConfig config = new EngineConfig();
		config.setJobsDirectory(createJobsDir().getAbsolutePath());
		this.manager = new EngineImpl(config);

		cluster = new CrawlerCluster();

		restws = new RestWS(new Context());
		restws.start();

		crawlerId = cluster.getCrawlers().iterator().next().getId();
		// FIXME: workaround for "Channel closed, may be due to thread interrupt"
		try {Thread.sleep(1);} catch (InterruptedException e) {}
	}

	public void tearDown() {
		manager.close();

		/*
		 * This is to work around a weird BDB threading exception.
		 * 
		 * It may be we're confusing it by recreating too rapidly. This seems to
		 * give it a chance to clean up.
		 */
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
		}

		// FileUtils.deleteDir(tmpDir);

	}

	public File createJobsDir() throws IOException {
		tmpDir = new File(System.getProperty("java.io.tmpdir", "/tmp"),
				"tmp_heritrix_unit_test_" /* + System.identityHashCode(this) */);
		tmpDir.mkdirs();

		// clear out the jobs directory
		File jobsDir = new File(tmpDir, "jobs");
		if (jobsDir.exists()) {
			FileUtils.deleteDir(jobsDir);
		}
		jobsDir.mkdirs();

		// create an initial empty profile
		File profileDir = new File(jobsDir, "profile-empty_profile");
		profileDir.mkdirs();

		new File(profileDir, "config.txt").createNewFile();
		new File(profileDir, "seeds.txt").createNewFile();

		File sheetsDir = new File(profileDir, "sheets");
		sheetsDir.mkdirs();
		FileWriter writer = new FileWriter(new File(sheetsDir, "global.sheet"));
		writer.append("root=map, java.lang.Object\n");
		writer
				.append("root:seeds=primary, org.archive.modules.seeds.SeedModuleImpl\n");
		writer.close();

		return jobsDir;
	}

	public void testLocalDiscovery() {
		Collection<Crawler> crawlers = cluster.getCrawlers();
		assertFalse(crawlers.isEmpty());
		System.out.println(crawlers);
	}

	public void testCrawlerList() throws ResourceException, IOException {
		Response response = restws.get("/crawlers/");
		assertEquals(200, response.getStatus().getCode());
		assertEquals(crawlerId + "\r\n", response.getEntity().getText());
	}

	public void testCrawlerStatus() {
		assertEquals(404, restws.get("/crawlers/nonExistantCrawler:1.17").getStatus()
				.getCode());
		assertEquals(400, restws.get("/crawlers/bogus").getStatus().getCode());
		assertEquals(400, restws.get("/crawlers/bogus!!!::::::...").getStatus()
				.getCode());
		assertEquals(400, restws.get("/crawlers/bogus:foo.bar").getStatus().getCode());
		assertEquals(200, restws.get("/crawlers/" + crawlerId).getStatus().getCode());
	}

	public void testJobsList() throws Exception {
		Response response = restws.get("/crawlers/" + crawlerId + "/jobs");
		assertEquals(200, response.getStatus().getCode());

		assertEquals("check bogus job not found", 404, restws.get(
				"/crawlers/" + crawlerId + "/jobs/bogus_job").getStatus().getCode());
		assertEquals("check real job found", 200, restws.get(
				"/crawlers/" + crawlerId + "/jobs/empty_profile").getStatus().getCode());

	}

	public void testCopyAndDeleteJob() throws Exception {
		Form form = new Form();
		form.add("name", "copied_profile");
		form.add("stage", "profile");
		Request req = new Request(Method.POST, "/crawlers/" + crawlerId
				+ "/jobs/empty_profile");
		req.setEntity(form.getWebRepresentation());
		req.setRootRef(new Reference(""));
		Response resp = restws.handle(req);

		assertEquals("check correct response code", 201, resp.getStatus()
				.getCode());
		assertNotNull("check has location field", resp.getLocationRef());
		assertEquals("ensure job was actually created", 200, restws.get(
				resp.getLocationRef()).getStatus().getCode());

		// try overwriting our copy
		req = new Request(Method.POST, "/crawlers/" + crawlerId + "/jobs/empty_profile");
		req.setEntity(form.getWebRepresentation());
		req.setRootRef(new Reference(""));
		assertEquals("make sure we can't overwrite jobs", 409, restws.handle(
				req).getStatus().getCode());

		// now lets delete it
		assertEquals("delete bogus job", 404, restws.delete(
				"/crawlers/" + crawlerId + "/jobs/bogus_job").getStatus().getCode());
		assertEquals("delete real job", 204, restws.delete(
				"/crawlers/" + crawlerId + "/jobs/empty_profile").getStatus().getCode());
		assertEquals("make sure it is gone", 404, restws.get(
				"/crawlers/" + crawlerId + "/jobs/empty_profile").getStatus().getCode());

	}

	private String slurp(Response resp) throws IOException {
		StringBuilder sb = new StringBuilder((int) resp.getEntity().getSize());
		InputStream in = resp.getEntity().getStream();
		byte[] b = new byte[4096];
		int n;
		while ((n = in.read(b)) != -1) {
			sb.append(new String(b, 0, n));
		}
		return sb.toString();
	}

	public void testSeeds() throws Exception {
		assertEquals("should start empty", "", restws.get(
				"/crawlers/" + crawlerId + "/jobs/empty_profile/seeds").getEntity()
				.getText());
		assertEquals("now add some seeds", 204, restws.post(
				"/crawlers/" + crawlerId + "/jobs/empty_profile/seeds",
				new StringRepresentation("http://seed1/\nhttp://seed2/"))
				.getStatus().getCode());
		assertEquals("check added ok", "http://seed1/\nhttp://seed2/\n", slurp(restws
				.get("/crawlers/" + crawlerId + "/jobs/empty_profile/seeds")));
		assertEquals("add some more seeds", 204, restws.post("/crawlers/" + crawlerId
				+ "/jobs/empty_profile/seeds", new StringRepresentation(
				"http://seed3/\n")).getStatus().getCode());
		assertEquals("check added more ok",
				"http://seed1/\nhttp://seed2/\nhttp://seed3/\n", slurp(restws.get(
						"/crawlers/" + crawlerId + "/jobs/empty_profile/seeds")));
		assertEquals("replace the lot", 204, restws.put(
				"/crawlers/" + crawlerId + "/jobs/empty_profile/seeds",
				new StringRepresentation("http://prime.master.seed/\n"))
				.getStatus().getCode());
		assertEquals("check replaced ok", "http://prime.master.seed/\n", slurp(restws
				.get("/crawlers/" + crawlerId + "/jobs/empty_profile/seeds")));
	}

}
