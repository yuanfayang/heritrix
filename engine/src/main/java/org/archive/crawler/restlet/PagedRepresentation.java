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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.LongRange;
import org.archive.util.FileUtils;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.resource.CharacterRepresentation;
import org.restlet.resource.FileRepresentation;

/**
 * Representation wrapping a FileRepresentation, displaying its contents
 * in batches of lines at a time, with forward and backward navigation. 
 * 
 * @contributor gojomo
 */
public class PagedRepresentation extends CharacterRepresentation {
    FileRepresentation fileRepresentation; 
    EnhDirectoryResource dirResource;
    long position;
    int lineCount;
    boolean reversedOrder; 
    
    public PagedRepresentation(FileRepresentation representation, EnhDirectoryResource resource, String pos, String lines, String reverse) {
        super(MediaType.TEXT_HTML);
        fileRepresentation = representation;
        dirResource = resource; 
        
        position = StringUtils.isBlank(pos) ? 0 : Long.parseLong(pos);
        lineCount = StringUtils.isBlank(lines) ? 64 : Integer.parseInt(lines);
        reversedOrder = "y".equals(reverse);
        
        // TODO: remove if not necessary in future?
        setCharacterSet(CharacterSet.UTF_8);
    }

    @Override
    public Reader getReader() throws IOException {
        StringWriter writer = new StringWriter((int)fileRepresentation.getSize()+100);
        write(writer); 
        return new StringReader(writer.toString());
    }

    @Override
    public void write(Writer writer) throws IOException {
        File file = fileRepresentation.getFile();
        List<String> lines = new LinkedList<String>();
        
        LongRange range = FileUtils.pagedLines(file, position, lineCount, lines, 128);
        if(reversedOrder) {
            Collections.reverse(lines);
        }
        
        
        
        PrintWriter pw = new PrintWriter(writer); 
        pw.println("<div id='controls'>");
        pw.println(file);
        pw.println("<br/>");
        
        pw.println("<a href='"+ getControlUri(position,lineCount*2,reversedOrder) + "'>&uArr;</a>"); 
        pw.println(lines.size() + " lines ");
        pw.println("<a href='"+getControlUri(position,lineCount/2,reversedOrder)+"'>&dArr;</a>"); 
        
        if(reversedOrder) {
            pw.println("<a href='"+getControlUri(position,lineCount,false)+"'>normal</a>"); 
            pw.println("| <b><u>reversed</u></b>"); 
        } else {
            pw.println("<b><u>normal</u></b>"); 
            pw.println("| <a href='"+getControlUri(position,lineCount,true)+"'>reversed</a>"); 
        }
        
        pw.println("<a href='"+ getControlUri(Math.max(0, range.getMinimumLong()-1),0-lineCount,reversedOrder) + "'>&lArr;earlier</a>");
        pw.println(range.getMinimumLong()
                +"-"+range.getMaximumLong()
                +"/"+file.length());
        pw.println("<a href='"+ getControlUri(Math.min(file.length()-1, range.getMaximumLong()+1),lineCount,reversedOrder) + "'>later&rArr;</a>");
        // Reference viewRef = dirResource.getRequest().getOriginalRef().clone(); 
        // viewRef.setQuery(null);
        // pw.println("<a href='"+viewRef+"'>view</a>");
        // pw.println("<br/>");
        
        pw.println("</div>");
        pw.println("<pre style='overflow:auto;'>");
        for(String line : lines) {
            StringEscapeUtils.escapeHtml(pw,line); 
            pw.println();
        }
        pw.println("</pre>");
        pw.close();
    }

    protected String getControlUri(long pos, int lines, boolean reverse) {
        Form query = new Form(); 
        query.add("format","paged");
        if(pos!=0) {
            query.add("pos", Long.toString(pos));
        }
        if(lines!=64) {
            if(Math.abs(lines)<1) {
                lines = 1;
            }
            query.add("lines",Integer.toString(lines));
        }
        if(reverse) {
            query.add("reverse","y");
        }
        Reference viewRef = dirResource.getRequest().getOriginalRef().clone(); 
        viewRef.setQuery(query.getQueryString());
        
        return viewRef.toString(); 
    }

    public FileRepresentation getFileRepresentation() {
        return fileRepresentation;
    }
}
