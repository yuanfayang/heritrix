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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringEscapeUtils;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.EngineImpl;
import org.archive.spring.ConfigPath;
import org.archive.util.FileUtils;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.resource.WriterRepresentation;

/**
 * Restlet Resource representing a single local CrawlJob inside an
 * Engine.
 * 
 * @contributor gojomo
 */
public class JobResource extends Resource {
    public static final IOFileFilter EDIT_FILTER = 
        FileUtils.getRegexFileFilter(".*\\.((c?xml)|(txt))$");

    CrawlJob cj; 
    
    public JobResource(Context ctx, Request req, Response res) throws ResourceException {
        super(ctx, req, res);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        cj = getEngine().getJob((String)req.getAttributes().get("job"));
        if(cj==null) {
            throw new ResourceException(404);
        }
    }

    public Representation represent(Variant variant) throws ResourceException {
        Representation representation = new WriterRepresentation(
                MediaType.TEXT_HTML) {
            public void write(Writer writer) throws IOException {
                JobResource.this.writeHtml(writer);
            }
        };
        // TODO: remove if not necessary in future?
        representation.setCharacterSet(CharacterSet.UTF_8);
        return representation;
    }

    protected void writeHtml(Writer writer) {
        PrintWriter pw = new PrintWriter(writer); 
        String jobTitle = "Job "+cj.getShortName();
        String baseRef = getRequest().getResourceRef().getBaseRef().toString();
        if(!baseRef.endsWith("/")) {
            baseRef += "/";
        }
        // TODO: replace with use a templating system (FreeMarker?)
        pw.println("<head><title>"+jobTitle+"</title>");
        pw.println("<base href='"+baseRef+"'/>");
        pw.println("</head><body>");
        pw.println("<h1>Job: <i>"+cj.getShortName()+"</i></h1>");
        
        pw.println(cj.getLaunchCount() + " launches ");
        if(cj.getLastLaunch()!=null) {
            pw.println("(last at "+cj.getLastLaunch()+")");
        }
        pw.println("<br/>");
        
        // configuration & launch 
        pw.println("configuration: <a href='jobdir/" 
                + cj.getPrimaryConfig().getName() + "'>" 
                + cj.getPrimaryConfig() +"</a>");
        pw.println("[<a href='jobdir/" 
                + cj.getPrimaryConfig().getName() 
                +  "?format=textedit'>edit</a>]");        
        pw.println("<form method='POST'>");
        pw.print("<input type='submit' name='action' value='checkXML' ");
        pw.print(cj.isXmlOk()?"disabled='disabled' title='XML OK'":"");
        pw.println("/>");
        pw.print("<input type='submit' name='action' value='instantiate' ");
        pw.print(cj.isContainerOk()?"disabled='disabled' title='instantiated'":"");
        pw.println("/>");
        pw.print("<input type='submit' name='action' value='validate' ");
        pw.print(cj.isContainerValidated()?"disabled='disabled' alt='validated'":"");
        pw.println("/>");
        
        pw.print("<input type='submit' name='action' value='launch'");
        if(cj.isProfile()) {
            pw.print("disabled='disabled' title='profiles cannot be launched'");
        }
        if(cj.isRunning()) {
            pw.print("disabled='disabled' title='launched OK'");
        }
        pw.println("/>");
        
        pw.print("<input type='submit' name='action' value='discard' ");
        pw.print(cj.isContainerOk()?"":"disabled='disabled' title='no instance'");
        pw.println("/>");

        pw.println("</form>");
        
        pw.println("<hr/>");
        pw.println("<h2>Job Log</h2>");
        pw.println("<div style='font-family:monospace; white-space:pre-wrap; white-space:normal; text-indent:-10px; padding-left:10px;'>");
        if(cj.getJobLog().exists()) {
            try {
                List<String> logLines = new LinkedList<String>();
                FileUtils.pagedLines(cj.getJobLog(), -1, -3, logLines);
                Collections.reverse(logLines);
                for(String line : logLines) {
                    pw.print("<p style='margin:0px'>");
                    StringEscapeUtils.escapeHtml(pw,line);
                    pw.print("</p>");
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe); 
            }
        }
        pw.println("</div>");
        pw.println("<a href='jobdir/"
                +cj.getJobLog().getName()
                +"?format=paged&pos=-1&lines=-128&reverse=y'>more job log...</a>");
        pw.println("<hr/>");
        pw.println("<h2>Active Job</h2>");

        pw.println("<br style='clear:both'/>");
        if(cj.isRunning()) {
            pw.println("<h3>"+cj.getCrawlController().getState()+"</h3>");
            pw.println("<form method='POST'>");
            pw.println("<input ");
            if(!cj.isPausable()) {
                pw.println(" disabled ");
            }
            pw.println(" type='submit' name='action' value='pause'/>");
            pw.println("<input ");
            if(!cj.isUnpausable()) {
                pw.println(" disabled ");
            }
            pw.println(" type='submit' name='action' value='unpause'/>");
            pw.println("<input type='submit' name='action' value='terminate'/>");
            pw.println("</form>");
            
            pw.println("<h3>Crawl Log</h3>");
            pw.println("<pre style=\'overflow:auto\'>");
            try {
                List<String> logLines = new LinkedList<String>();
                FileUtils.pagedLines(
                        cj.getCrawlController().getLoggerModule().getCrawlLogPath().getFile(),
                        -1, 
                        -10, 
                        logLines);
                Collections.reverse(logLines);
                for(String line : logLines) {
                    StringEscapeUtils.escapeHtml(pw,line);
                    pw.println();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe); 
            }
            pw.println("</pre>");
            pw.println("<a href='jobdir"
                    +cj.jobDirRelativePath(
                            cj.getCrawlController().getLoggerModule().getCrawlLogPath().getFile())
                    +"?format=paged&pos=-1&lines=-128&reverse=y'>more crawl log...</a>");
        }
        pw.println("<hr/>");
        pw.println("<h2>Files</h2>");
        pw.println("<h3>Browse <a href='jobdir'>Job Directory</a></h3>");
        // specific paths from wired context
        pw.println("<h3>Configuration-referenced Paths</h3>");
        pw.println("<dl>");
        for(ConfigPath cp : cj.getConfigPaths().values()) {
            pw.println("<dt>"+cp.getName()+"</dt>");
            File f = cp.getFile();
            String jobDirRelative = cj.jobDirRelativePath(f);
            if(jobDirRelative==null) {
                pw.println("<dd>"+f+"</dd>");
            } else {
                pw.println("<dd><a href='jobdir"+jobDirRelative+"'>");
                pw.println(f);
                pw.println("</a>");
                if(EDIT_FILTER.accept(f)) {
                    pw.print("[<a href='jobdir"+jobDirRelative+"?format=textedit'>");
                    pw.println("edit</a>]");
                }
                pw.println("</dd>");
            }
        }
        pw.println("</dl>");
        pw.println("<hr/>");
        pw.println("<form method='POST'>Copy job to <input name='copyTo'/><input type='submit'/><input type='checkbox' name='asProfile'/>as profile</form>");
        pw.println("<hr/>");
        pw.close();
    }

    protected EngineImpl getEngine() {
        return ((EngineApplication)getApplication()).getEngine();
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        // copy op?
        Form form = getRequest().getEntityAsForm();
        String copyTo = form.getFirstValue("copyTo");
        if(copyTo!=null) {
            copyJob(copyTo,"on".equals(form.getFirstValue("asProfile")));
            return;
        }
        String action = form.getFirstValue("action");
        if("launch".equals(action)) {
            cj.launch(); 
        } else if("checkXML".equals(action)) {
            cj.checkXML();
        } else if("instantiate".equals(action)) {
            cj.instantiateContainer();
        } else if("validate".equals(action)) {
            cj.validateConfiguration();
        } else if("discard".equals(action)) {
            cj.reset(); 
        } else if("pause".equals(action)) {
            cj.getCrawlController().requestCrawlPause();
        } else if("unpause".equals(action)) {
            cj.getCrawlController().requestCrawlResume();
        } else if("terminate".equals(action)) {
            cj.getCrawlController().requestCrawlStop();
        }
        // default: redirect to GET self
        getResponse().redirectSeeOther(getRequest().getOriginalRef());
    }

    protected void copyJob(String copyTo, boolean asProfile) throws ResourceException {
        try {
            getEngine().copy(cj, copyTo, asProfile);
        } catch (IOException e) {
            throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,e);
        }
        // redirect to destination job page
        getResponse().redirectSeeOther(copyTo);
    }
    
    
}
