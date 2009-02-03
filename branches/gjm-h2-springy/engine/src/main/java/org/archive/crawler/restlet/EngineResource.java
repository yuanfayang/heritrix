/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.crawler.restlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;

import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.EngineImpl;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.resource.WriterRepresentation;

/**
 * Restlet Resource representing an Engine that may be used
 * to assemble, launch, monitor, and manage crawls. 
 * 
 * @contributor gojomo
 */
public class EngineResource extends Resource {

    public EngineResource(Context ctx, Request req, Response res) {
        super(ctx, req, res);

        getVariants().add(new Variant(MediaType.TEXT_HTML));
    }

    public Representation represent(Variant variant) throws ResourceException {
        Representation representation = new WriterRepresentation(
                MediaType.TEXT_HTML) {
            public void write(Writer writer) throws IOException {
                EngineResource.this.writeHtml(writer);
            }
        };
        // TODO: remove if not necessary in future?
        representation.setCharacterSet(CharacterSet.UTF_8);
        return representation;
    }

    protected void writeHtml(Writer writer) {
        EngineImpl engine = getEngine();
        String engineTitle = "Heritrix Engine "+engine.getHeritrixVersion();
        String baseRef = getRequest().getResourceRef().getBaseRef().toString();
        if(!baseRef.endsWith("/")) {
            baseRef += "/";
        }
        PrintWriter pw = new PrintWriter(writer); 
        pw.println("<head><title>"+engineTitle+"</title>");
        pw.println("<base href='"+baseRef+"'/>");
        pw.println("</head><body>");
        pw.println("<h1>"+engineTitle+"</h1>"); 
        pw.println("<h2>Browse <a href='jobsdir'>Jobs Directory</a></h2>");
        pw.println("<h2>Job Configs</h2>");
        ArrayList<CrawlJob> jobs = new ArrayList<CrawlJob>();
        jobs.addAll(engine.getJobConfigs().values());
        Collections.sort(jobs);
        for(CrawlJob cj: jobs) {
            pw.println("<li>");
            cj.writeHtmlTo(pw,"job/");
            pw.println("</li>");
        }
        pw.println("</body>");
        pw.flush();
    }

    protected EngineImpl getEngine() {
        return ((EngineApplication)getApplication()).getEngine();
    }
}
