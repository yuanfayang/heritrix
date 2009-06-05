/* 
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * ExtractorSWFTest.java
 *
 * Created on Jan 31, 2007
 *
 * $Id$
 */
package org.archive.modules.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.archive.modules.DefaultProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;
import org.archive.util.Recorder;

/**
 * Unit test for {@link ExtractorSWF}.
 *
 * @author pjack
 * @author nlevitt
 */
public class ExtractorSWFTest extends ContentExtractorTestBase {

    private static Logger logger = Logger.getLogger(ExtractorSWFTest.class.getName());

    @Override
    protected Class<ExtractorSWF> getModuleClass() {
        return ExtractorSWF.class;
    }

    @Override
    protected Object makeModule() {
        return new ExtractorSWF();
    }

    @Override
    protected ExtractorSWF makeExtractor() {
        ExtractorSWF extractor = new ExtractorSWF();
        initExtractor(extractor);
        return extractor;
    }

    protected void initExtractor(Extractor extractor) {
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();
        ExampleStateProvider dsp = new ExampleStateProvider();
        dsp.set(extractor, Extractor.URI_ERROR_LOGGER_MODULE, ulm);
        extractor.initialTasks(dsp);
    }


    private DefaultProcessorURI setupURI(String url) throws MalformedURLException, IOException {
        UURI uuri = UURIFactory.getInstance(url);
        DefaultProcessorURI curi = new DefaultProcessorURI(uuri, uuri, LinkContext.NAVLINK_MISC);

        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        InputStream in = conn.getInputStream();

        Recorder recorder = Recorder.wrapInputStreamWithHttpRecord(
                this.getTmpDir(), this.getClass().getName(), in, null);
        logger.info("got recorder for " + url);

        curi.setContentSize(recorder.getRecordedInput().getSize());
        curi.setContentType("application/x-shockwave-flash");
        curi.setFetchStatus(200);
        curi.setRecorder(recorder);

        return curi;
    }

    public void xestHer1509() throws IOException {
        // url -> link to find
        HashMap<String, String> testUrls = new HashMap<String, String>();
        testUrls.put("http://wayback.archive-it.org/779/20080709003013/http://www.dreamingmethods.com/uploads/lastdream/loader.swf", "project.swf");
        testUrls.put("http://wayback.archive-it.org/1094/20080923035716/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/Dim%20O%20Gauble/loader.swf", "map_3d.swf");
        testUrls.put("http://wayback.archive-it.org/1094/20080923040243/http://www.dreamingmethods.com/uploads/dm_archive/mainsite/downloads/flash/clearance/loader.swf", "clearance_intro.swf");

        for (String url : testUrls.keySet()) {
            logger.info("testing " + url);

            DefaultProcessorURI curi;
            try {
                curi = setupURI(url);
            } catch (IOException e) {
                logger.severe("unable to open url, skipping: " + e);
                continue;
            }

            long startTime = System.currentTimeMillis();
            this.extractor.extract(curi);
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
    public void xestNonAsciiLink() throws MalformedURLException, IOException {
        // url -> link to find
        HashMap<String,String> testUrls = new HashMap<String, String>();
        testUrls.put("http://wayback.archive-it.org/1100/20080721212134/http://www.marca.com/futbol/madrid_vs_barca/previa/barca/barcaOK.swf", "barca/delape%C3%B1a.swf");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.contraloriagen.gov.co/html/publicaciones/imagenes/analisis-proyec-ley.swf", "http://www.contraloriagen.gov.co:8081/internet/html/publicaciones/por_dependencia_y_clase.jsp?clases=3&titulo_pagina=An%C3%A1lisis%20a%20Proyectos%20de%20Ley%20y%20Actos%20Legislativos");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.ine.gov.ve/secciones/modulos/Apure/sApure.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.ine.gov.ve/secciones/modulos/Lara/sLara.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.minsa.gob.pe/hnhipolitounanue/text13.swf", "archivos%20cuerpo/APOYO%20A%20LA%20DOCENCIA%20E%20INVESTIG/Registro%20de%20Estudios%20Cl%C3%ADnicos.pdf");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.nacobre.com.mx/flash/Flash_mercados.swf", "NSMcdoAccesoriosBa%C3%B1o.asp");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.sagarpa.gob.mx/dlg/nuevoleon/ddr's/Montemorelos/text4.swf", "campa%C3%B1a_abeja.htm");
        // testUrls.put("http://wayback.archive-it.org/176/20080610233230/http://www.sagarpa.gob.mx/dlg/tabasco/text2.swf", "delegacion/comunicacion/cartel%20reuni%C3%B3n%20forestal%20xviii%20media2.pdf");
        // testUrls.put("http://wayback.archive-it.org/317/20061129141640/http://www.ine.gov.ve/secciones/modulos/Miranda/sMiranda.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");
        // testUrls.put("http://wayback.archive-it.org/317/20061129141640/http://www.ine.gov.ve/secciones/modulos/Tachira/sTachira.swf", "aspectosfisicos.asp?Codigo=Nacimientos&titulo=Nacimientos%20vivos%20registrados%20por%20a%C3%B1o,%20seg%C3%BAn%20municipio%20de%20residencia%20habitual%20de%20la%20madre,%201999-2002&Fuente=Fuente:%20Prefecturas%20y%20Jefaturas%20Civiles&cod_ent=13&nvalor=2_2&seccion=2");

        for (String url : testUrls.keySet()) {
            logger.info("testing " + url);
            DefaultProcessorURI curi;
            try {
                curi = setupURI(url);
            } catch (IOException e) {
                logger.severe("unable to open url, skipping: " + e);
                continue;
            }

            long startTime = System.currentTimeMillis();
            this.extractor.extract(curi);
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
}