/* GroovyDecideRule
*
* $Id$
*
* Created on Aug 3, 2006
*
* Copyright (C) 2006 Internet Archive.
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
*/
package org.archive.crawler.deciderules;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.processor.GroovyProcessor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.TextField;
import org.archive.crawler.settings.Type;
import org.archive.util.FileUtils;


/**
 * Rule which runs a groovy script to make its decision. 
 * 
 * Script source may be provided directly as a setting, or via a file
 * local to the crawler, or both. (If both, the setting source will be
 * executed first, then the file script.)
 * 
 * Variables available to the script include 'object' (the object to be
 * evaluated, typically a CandidateURI or CrawlURI), 'self' 
 * (this GroovyDecideRule instance), and 'controller' (the crawl's 
 * CrawlController instance). 
 *
 * @author gojomo
 */
public class GroovyDecideRule extends DecideRule {
    private static final Logger logger =
        Logger.getLogger(GroovyDecideRule.class.getName());
    
    /** setting for script source code */
    public final static String ATTR_SCRIPT_SOURCE = "script-source";

    /** setting for script file */
    public final static String ATTR_SCRIPT_FILE = "script-file"; 

    protected Binding binding;
    protected GroovyShell shell;
    protected Script sourceScript;
    protected Script fileScript;
    boolean initialized = false; 
    
    
    public GroovyDecideRule(String name) {
        super(name);
        setDescription("GoovyDecideRule. Runs the groovy script source " +
                "(supplied directly or via a file path) against the " +
                "current URI. The script may access the test  " +
                "object via the 'object' variable, and the " +
                "CrawlController via the 'controller' variable. The" +
                "script should return its decision in the 'decision' " +
                "variable.");
        Type t = addElementToDefinition(new SimpleType(ATTR_SCRIPT_SOURCE,
                "Groovy script source to run", new TextField("")));
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_SCRIPT_FILE,
                "Groovy script file to run", ""));
        t.setOverrideable(false);
    }

    public synchronized Object decisionFor(Object object) {
        if(!initialized) {
            initialize();
            initialized=true; 
        }
        this.binding.setVariable("object",object);
        this.binding.setVariable("decision",PASS);
        if(this.sourceScript!=null) {
            this.sourceScript.run();
        }
        if(this.fileScript!=null) {
            this.fileScript.run();
        }
        return this.binding.getVariable("decision");
    }
    
    protected void initialize() {
        this.binding = new Binding();
        this.binding.setVariable("self", this);
        this.binding.setVariable("controller", getController());
        // TODO: add binding other useful items?
        this.shell = new GroovyShell(binding);
        updateScripts();
    }

    public void kickUpdate() {
        updateScripts();
    }
    
    /**
     * Update the parsed scripts (for example after a configuration change). 
     */
    protected void updateScripts() {
        String source = 
            ((TextField) getUncheckedAttribute(null, ATTR_SCRIPT_SOURCE)).toString();
        if(source.length()>0) {
            this.sourceScript = this.shell.parse(source);
        }
        String filePath = (String) getUncheckedAttribute(null, ATTR_SCRIPT_FILE);
        if(filePath.length()>0) {
            try {
                File file = getSettingsHandler().getPathRelativeToWorkingDirectory(filePath);
                String fileSource = FileUtils.readFileAsString(file);
                this.fileScript = this.shell.parse(fileSource);
            } catch (IOException e) {
                logger.log(Level.SEVERE,"unable to read script file",e);
            }
        }
    }
}
