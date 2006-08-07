/* BeanShellProcessor
 *
 * Created on Aug 4, 2006
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package org.archive.crawler.processor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.TextField;
import org.archive.crawler.settings.Type;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * A processor which runs a BeanShell script on the CrawlURI.
 *
 * Script source may be provided directly as a setting, or via a file
 * local to the crawler, or both. (If both, the setting source will be
 * executed first, then the file script.) Script source should define
 * a method with one argument, 'run(curi)'. Each processed CrawlURI is
 * passed to this script method. 
 * 
 * Other variables available to the script include 'self' (this 
 * BeanShellProcessor instance) and 'controller' (the crawl's 
 * CrawlController instance). 
 * 
 * @author gojomo
 * @version $Date$, $Revision$
 */
public class BeanShellProcessor extends Processor implements FetchStatusCodes {
    private static final Logger logger =
        Logger.getLogger(BeanShellProcessor.class.getName());
    
    /** setting for script source code */
    public final static String ATTR_SCRIPT_SOURCE = "script-source";

    /** setting for script file */
    public final static String ATTR_SCRIPT_FILE = "script-file"; 

    /** whether each thread should have its own script runner (true), or
     * they should share a single script runner with synchronized access */
    public final static String ATTR_ISOLATE_THREADS = "isolate-threads";

    protected ThreadLocal<Interpreter> threadInterpreter;
    protected Interpreter sharedInterpreter;
    
    /**
     * Constructor.
     * @param name Name of this processor.
     */
    public BeanShellProcessor(String name) {
        super(name, "GroovyProcessor. Runs the groovy script source " +
                "(supplied directly or via a file path) against the " +
                "current URI. The script may access the current " +
                "CrawlURI via the 'curi' variable, and the " +
                "CrawlController via the 'controller' variable.");
        Type t = addElementToDefinition(new SimpleType(ATTR_SCRIPT_SOURCE,
                "Groovy script source to run", new TextField("")));
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_SCRIPT_FILE,
                "Groovy script file to run", ""));
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_ISOLATE_THREADS,
                "Whether each ToeThread should get its own independent " +
                "script context, or they should share synchronized access " +
                "to one context. Default is true, meaning each threads " +
                "gets its own isolated context.", true));
        t.setOverrideable(false);

    }

    protected synchronized void innerProcess(CrawlURI curi) {
        Interpreter interpreter = getInterpreter(); 
        synchronized(interpreter) {
            try {
                interpreter.set("curi",curi);
                interpreter.eval("run(curi)");
            } catch (EvalError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }

    protected Interpreter getInterpreter() {
        if(sharedInterpreter!=null) {
            return sharedInterpreter;
        }
        Interpreter interpreter = threadInterpreter.get(); 
        if(interpreter==null) {
            interpreter = newInterpreter(); 
            threadInterpreter.set(interpreter);
        }
        return interpreter; 
    }

    protected Interpreter newInterpreter() {
        Interpreter interpreter = new Interpreter(); 
        try {
            interpreter.set("self", this);
            interpreter.set("controller", getController());
            // TODO: add binding other useful items, shared store?
            
            String source = 
                ((TextField) getUncheckedAttribute(null, ATTR_SCRIPT_SOURCE)).toString();
            interpreter.eval(source); 
            
            String filePath = (String) getUncheckedAttribute(null, ATTR_SCRIPT_FILE);
            if(filePath.length()>0) {
                try {
                    File file = getSettingsHandler().getPathRelativeToWorkingDirectory(filePath);
                    interpreter.source(file.getPath());
                } catch (IOException e) {
                    logger.log(Level.SEVERE,"unable to read script file",e);
                }
            }
        } catch (EvalError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return interpreter; 
    }

    protected void initialTasks() {
        super.initialTasks();
        kickUpdate();
    }

    public void kickUpdate() {
        // TODO make it so running state (tallies, etc.) isn't lost on changes
        // unless unavoidable
        if((Boolean)getUncheckedAttribute(null,ATTR_ISOLATE_THREADS)) {
            sharedInterpreter = null; 
            threadInterpreter = new ThreadLocal<Interpreter>(); 
        } else {
            sharedInterpreter = newInterpreter(); 
            threadInterpreter = null;
        }
    }
}
