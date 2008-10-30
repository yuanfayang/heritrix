package org.archive.crawler.extractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.net.UURIFactory;
import org.archive.util.HttpRecorder;
import org.archive.util.TmpDirTestCase;

/* Note: all of the tests in here grab swf files from the web. But we want
 * heritrix to build without relying on any external services, so the tests are
 * named such that they won't run. To run the tests, rename the methods from
 * xest* to test*.
 */
public class ExtractorSWFTest extends TmpDirTestCase implements
		CoreAttributeConstants {

	private static Logger logger = Logger.getLogger(ExtractorSWFTest.class
			.getName());

	private LegacyExtractorSWF legacyExtractor;
	private ExtractorSWF extractor;

	protected void initializeExtractors()
			throws InvalidAttributeValueException, AttributeNotFoundException,
			MBeanException, ReflectionException {
		// Hack in a settings handler. Do this by adding this extractor
		// to the order file (I'm adding it to a random MapType; seemingly
		// can only add to MapTypes post-construction). This takes care
		// of setting a valid SettingsHandler into the ExtractorHTML (This
		// shouldn't be so difficult). Of note, the order file below is
		// not written to disk.
		SettingsHandler handler = new XMLSettingsHandler(new File(getTmpDir(),
				this.getClass().getName() + ".order.xml"));
		handler.initialize();

		this.legacyExtractor = (LegacyExtractorSWF) ((MapType) handler
				.getOrder().getAttribute(CrawlOrder.ATTR_RULES)).addElement(
				handler.getSettingsObject(null), new LegacyExtractorSWF(
						"ExtractorSWFTest/LegacyExtractorSWF"));

		this.extractor = (ExtractorSWF) ((MapType) handler.getOrder()
				.getAttribute(CrawlOrder.ATTR_RULES)).addElement(handler
				.getSettingsObject(null), new ExtractorSWF(
				"ExtractorSWFTest/ExtractorSWF"));
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.initializeExtractors();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testHer1509() throws IOException {
		// url -> link to find
		HashMap<String, String> testUrls = new HashMap<String, String>();
		testUrls.put("http://wayback.archive-it.org/779/20080709003013/http://www.dreamingmethods.com/uploads/lastdream/loader.swf", "project.swf");
		testUrls.put("http://wayback.archive-it.org/1094/20080923035716/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/Dim%20O%20Gauble/loader.swf", "map_3d.swf");
		testUrls.put("http://wayback.archive-it.org/1094/20080923040243/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/clearance/loader.swf", "clearance_intro.swf");

		for (String url : testUrls.keySet()) {
			HttpRecorder recorder = HttpRecorder.wrapInputStreamWithHttpRecord(
					getTmpDir(), this.getClass().getName(), new URL(url)
							.openStream(), null);
			CrawlURI curi = setupCrawlURI(recorder, url);

			long startTime = System.currentTimeMillis();
			this.extractor.innerProcess(curi);
			long elapsed = System.currentTimeMillis() - startTime;
			logger.info(this.extractor.getClass().getSimpleName() + " took "
					+ elapsed + "ms to process " + url);

			boolean foundIt = false;
			for (Link link : curi.getOutLinks()) {
				logger.info("found link: " + link);
				foundIt = foundIt || link.getDestination().toString().endsWith(testUrls.get(url));
			}

			assertTrue("failed to extract link \"" + testUrls.get(url)
					+ "\" from " + url, foundIt);
		}
	}

	/*
	 * Tests for correct encoding of non-ascii url's. 
	 *
	 * The old javaswf extractor mishandles these. For example:
	 *
	 * "http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/futbol/madrid_vs_barca/previa/barca/barcaOK.swf",
	 *
	 * This one has a link that the new extractor handles correctly but the
	 * legacy one handles wrong. The link string is 'barca/delapeña.swf'.
	 * The legacy extractor incorrectly produces
	 * "barca/delape%EF%BF%BDa.swf" while the new one correctly produces
	 * "barca/delape%C3%B1a.swf". 
	 */
	public void testNonAsciiLink() throws MalformedURLException, IOException {
		// url -> link to find
		HashMap<String,String> testUrls = new HashMap<String, String>();
		testUrls.put("http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/futbol/madrid_vs_barca/previa/barca/barcaOK.swf", "barca/delape%C3%B1a.swf");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.contraloriagen.gov.co/html/publicaciones/imagenes/analisis-proyec-ley.swf", "http://www.contraloriagen.gov.co:8081/internet/html/publicaciones/por_dependencia_y_clase.jsp?clases=3&titulo_pagina=An%C3%A1lisis%20a%20Proyectos%20de%20Ley%20y%20Actos%20Legislativos");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.ine.gov.ve/secciones/modulos/Apure/sApure.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.ine.gov.ve/secciones/modulos/Lara/sLara.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.minsa.gob.pe/hnhipolitounanue/text13.swf", "archivos%20cuerpo/APOYO%20A%20LA%20DOCENCIA%20E%20INVESTIG/Registro%20de%20Estudios%20Cl%C3%ADnicos.pdf");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.nacobre.com.mx/flash/Flash_mercados.swf", "NSMcdoAccesoriosBa%C3%B1o.asp");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.sagarpa.gob.mx/dlg/nuevoleon/ddr's/Montemorelos/text4.swf", "campa%C3%B1a_abeja.htm");
		testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.sagarpa.gob.mx/dlg/tabasco/text2.swf", "delegacion/comunicacion/cartel%20reuni%C3%B3n%20forestal%20xviii%20media2.pdf");
		testUrls.put("http://wayback.archive-it.org/317/20061129141640/http://www.ine.gov.ve/secciones/modulos/Miranda/sMiranda.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
		testUrls.put("http://wayback.archive-it.org/317/20061129141640/http://www.ine.gov.ve/secciones/modulos/Tachira/sTachira.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");

		for (String url : testUrls.keySet()) {
			HttpRecorder recorder = HttpRecorder.wrapInputStreamWithHttpRecord(
					getTmpDir(), this.getClass().getName(), new URL(url)
							.openStream(), null);
			CrawlURI curi = setupCrawlURI(recorder, url);

			long startTime = System.currentTimeMillis();
			this.extractor.innerProcess(curi);
			long elapsed = System.currentTimeMillis() - startTime;
			logger.info(this.extractor.getClass().getSimpleName() + " took "
					+ elapsed + "ms to process " + url);

			boolean foundIt = false;
			for (Link link : curi.getOutLinks()) {
				logger.info("found link: " + link);
				foundIt = foundIt || link.getDestination().toString().endsWith(testUrls.get(url));
			}

			if (!foundIt)
				logger.severe("failed to extract link \"" + testUrls.get(url)
						+ "\" from " + url);
			assertTrue("failed to extract link \"" + testUrls.get(url)
					+ "\" from " + url, foundIt);
		}
	}


	private static final String[] REGRESSION_TEST_URLS = new String[] {
		/*
		 * Each of these urls is known to exercise a different part of the
		 * extractor.
		 */
		"http://webteam.archive.org/jira/secure/attachment/10157/home5.swf",
		"http://loc-wm.archive.org/e08/20080829025445/http://www.brouhahatimes.com/_borders/blomayor2008.swf",
		"http://loc-wm.archive.org/e08/20080717072213/http://www.johnmccain.com/images/states/v2/usamap.swf",
		"http://wayback.archive-it.org/1100/20080721212134/http://www.spiegel.de/media/0,4906,13245,00.swf",
		"http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/futbol/madrid_vs_barca/previa/madrid/madridOK.swf",
		"http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/componentes/flash/newPlayer_marcatv.swf",
		"http://wayback.archive-it.org/928/20080715005115/http://www.thirteen.org/closetohome/overboard/toc/5characters_9.swf",

		/* So far, haven't found a swf with a link inside DoInitAction, so
		 * that piece hasn't actually been regression tested. */

		/*
		 * The legacy extractor finds links in these that the new extractor
		 * doesn't, but they all seem to be caused by malformed tags and such.
		 * See http://webteam.archive.org/jira/browse/HER-1509 for some more
		 * details.
		 *
		 * "http://wayback.archive-it.org/1030/20080405220502/http://i.walmart.com/i/c/00/pov/00014503-15844.swf",
		 * "http://wayback.archive-it.org/928/20080715005115/http://www-tc.pbskids.org/dragontales/swfs/storybook.swf",
		 * "http://wayback.archive-it.org/928/20080715005115/http://pbskids.org/readingrainbow/games/flash/build_a_snowman.swf",
		 * "http://wayback.archive-it.org/461/20070203222149/http://static.filmloop.com/looplets/flash/v2/looplet.swf",
		 * "http://wayback.archive-it.org/1042/20080408200548/http://testiflashs.googlepages.com/dragonball_clock_2.swf",
		 */
	};

	public void testForRegression() throws IOException {
		for (String url : REGRESSION_TEST_URLS) {
			logger.info("testing " + url);

			HttpRecorder recorder = null;
			try {
				URLConnection conn = new URL(url).openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(30000);
				InputStream in = conn.getInputStream();
				 
				recorder = HttpRecorder.wrapInputStreamWithHttpRecord(
						getTmpDir(), this.getClass().getName(), in, null);
				logger.info("got recorder for " + url);
			} catch (IOException e) {
				logger.severe("unable to open url, skipping: " + e);
				continue;
			}

			CrawlURI curi1 = setupCrawlURI(recorder, url);
			CrawlURI curi2 = setupCrawlURI(recorder, url);

			long startTime = System.currentTimeMillis();
			this.legacyExtractor.innerProcess(curi1);
			long elapsed = System.currentTimeMillis() - startTime;
			logger.info(this.legacyExtractor.getClass().getSimpleName()
					+ " took " + elapsed + "ms to process " + url);

			startTime = System.currentTimeMillis();
			this.extractor.innerProcess(curi2);
			elapsed = System.currentTimeMillis() - startTime;
			logger.info(this.extractor.getClass().getSimpleName() + " took "
					+ elapsed + "ms to process " + url);

			Collection<Link> legacyLinks = curi1.getOutLinks();
			Collection<Link> links = curi2.getOutLinks();

			int legacyMissedLinks = 0;
			for (Link link : links) {
				if (!legacyLinks.contains(link)) {
					logger.info(url + " - link not found by legacy extractor: "
							+ link);
					legacyMissedLinks++;
				}
			}

			int missedLinks = 0;
			for (Link link : legacyLinks) {
				if (!links.contains(link)) {
					logger.severe(url + " - link not found by new extractor: "
							+ link);
					missedLinks++;
				}
			}

			assertTrue("new extractor missed " + missedLinks + " links in "
					+ url, missedLinks == 0);
		}
	}

	private CrawlURI setupCrawlURI(HttpRecorder rec, String url)
			throws URIException {
		CrawlURI curi = new CrawlURI(UURIFactory.getInstance(url));
		curi.setContentSize(rec.getRecordedInput().getSize());
		curi.setContentType("application/x-shockwave-flash");
		curi.setFetchStatus(200);
		curi.setHttpRecorder(rec);
		// Fake out the extractor that this is a HTTP transaction.
		curi.putObject(CoreAttributeConstants.A_HTTP_TRANSACTION, new Object());
		return curi;
	}

}
