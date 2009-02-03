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
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.io.FileUtils;
import org.restlet.Directory;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.noelios.restlet.local.DirectoryResource;

/**
 * Enhanced version of Restlet DirectoryResource, adding ability to 
 * edit some files. 
 * 
 * @contributor gojomo
 */
public class EnhDirectoryResource extends DirectoryResource {

    public EnhDirectoryResource(Directory directory, Request request, Response response) throws IOException {
        super(directory, request, response);
    }

    /** 
     * Add EditRepresentation as a variant when appropriate. 
     * 
     * @see com.noelios.restlet.local.DirectoryResource#getVariants()
     */
    @Override
    public List<Variant> getVariants() {
        List<Variant> variants = super.getVariants();
        Form f = getRequest().getResourceRef().getQueryAsForm();
        if("textarea".equals(f.getFirstValue("edit"))) {
            // wrap FileRepresentations in EditRepresentations
            // TODO: limit to appropriate/configured file types
            ListIterator<Variant> iter = variants.listIterator(); 
            while(iter.hasNext()) {
                Variant v = iter.next(); 
                if(v instanceof FileRepresentation) {
                    iter.remove();
                    iter.add(new EditRepresentation((FileRepresentation)v));
                }
            }
        }
        
        return variants; 
    }
    
    /** 
     * Accept a POST used to edit or create a file.
     * 
     * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
     */
    public void acceptRepresentation(Representation entity)
    throws ResourceException {
        // TODO: only allowPost on valid targets
        Form form = getRequest().getEntityAsForm();
        String newContents = form.getFirstValue("contents");
        // TODO: defensive
        EditRepresentation er = (EditRepresentation) getVariants().get(0);
        File file = er.getFileRepresentation().getFile(); 
        try {
            FileUtils.writeStringToFile(file, newContents);
        } catch (IOException e) {
            // TODO report error somehow
            e.printStackTrace();
        }
        // redirect to view version
        Reference ref = getRequest().getOriginalRef().clone(); 
        ref.setQuery(null);
        getResponse().redirectSeeOther(ref);
        
    }
}
