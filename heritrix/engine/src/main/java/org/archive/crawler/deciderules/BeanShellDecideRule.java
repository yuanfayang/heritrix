/* BeanShellDecideRule
*
* $Id$
*
* Created on Aug 7, 2006
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlController;
import org.archive.modules.ProcessorURI;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRule;
import org.archive.state.FileModule;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

import bsh.EvalError;
import bsh.Interpreter;


/**
 * Rule which runs a groovy script to make its decision. 
 * 
 * Script source may be provided via a file local to the crawler.
 * 
 * Variables available to the script include 'object' (the object to be
 * evaluated, typically a CandidateURI or CrawlURI), 'self' 
 * (this GroovyDecideRule instance), and 'controller' (the crawl's 
 * CrawlController instance). 
 *
 * TODO: reduce copy & paste with GroovyProcessor
 * 
 * @author gojomo
 */
public class BeanShellDecideRule extends DecideRule implements Initializable {

    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(BeanShellDecideRule.class.getName());
    
    /** BeanShell script file. */
    @Immutable
    final public static Key<FileModule> SCRIPT_FILE = 
        Key.make(FileModule.class, null);

    final public static Key<CrawlController> CONTROLLER =
        Key.make(CrawlController.class, null);

    /**
     * Whether each ToeThread should get its own independent script context, or
     * they should share synchronized access to one context. Default is true,
     * meaning each threads gets its own isolated context.
     */
    @Immutable
    final public static Key<Boolean> ISOLATE_THREADS = Key.make(true);


    protected ThreadLocal<Interpreter> threadInterpreter = 
        new ThreadLocal<Interpreter>();;
    protected Interpreter sharedInterpreter;
    public Map<Object,Object> sharedMap = 
        Collections.synchronizedMap(new HashMap<Object,Object>());
    protected boolean initialized = false; 

    private FileModule scriptFile;
    private CrawlController controller;

    static {
        KeyManager.addKeys(BeanShellDecideRule.class);
    }
    
    public BeanShellDecideRule() {
    }
    
    
    public void initialTasks(StateProvider context) {
        this.scriptFile = context.get(this, SCRIPT_FILE);
        this.controller = context.get(this, CONTROLLER);
    }

    
    @Override
    public synchronized DecideResult innerDecide(ProcessorURI uri) {
        // depending on previous configuration, interpreter may 
        // be local to this thread or shared
        Interpreter interpreter = getInterpreter(uri); 
        synchronized(interpreter) {
            // synchronization is harmless for local thread interpreter,
            // necessary for shared interpreter
            try {
                interpreter.set("object",uri);
                return (DecideResult)interpreter.eval("decisionFor(object)");
            } catch (EvalError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return DecideResult.PASS;
            } 
        }
    }

    /**
     * Get the proper Interpreter instance -- either shared or local 
     * to this thread. 
     * @return Interpreter to use
     */
    protected Interpreter getInterpreter(StateProvider context) {
        if(sharedInterpreter==null 
           && context.get(this, ISOLATE_THREADS)) {
            // initialize
            sharedInterpreter = newInterpreter(context);
        }
        if(sharedInterpreter!=null) {
            return sharedInterpreter;
        }
        Interpreter interpreter = threadInterpreter.get(); 
        if(interpreter==null) {
            interpreter = newInterpreter(context); 
            threadInterpreter.set(interpreter);
        }
        return interpreter; 
    }

    /**
     * Create a new Interpreter instance, preloaded with any supplied
     * source file and the variables 'self' (this 
     * BeanShellProcessor) and 'controller' (the CrawlController). 
     * 
     * @return  the new Interpreter instance
     */
    protected Interpreter newInterpreter(StateProvider context) {
        Interpreter interpreter = new Interpreter(); 
        try {
            interpreter.set("self", this);
            interpreter.set("controller", controller);
            
            File file = scriptFile.getFile();
            try {
                interpreter.source(file.getPath());
            } catch (IOException e) {
                logger.log(Level.SEVERE,"unable to read script file",e);
            }
        } catch (EvalError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return interpreter; 
    }
    
    
    /**
     * Setup (or reset) Intepreter variables, as appropraite based on 
     * thread-isolation setting. 
     */
    public void kickUpdate(StateProvider context) {
        // TODO make it so running state (tallies, etc.) isn't lost on changes
        // unless unavoidable
        if (context.get(this, ISOLATE_THREADS)) {
            sharedInterpreter = null; 
            threadInterpreter = new ThreadLocal<Interpreter>(); 
        } else {
            sharedInterpreter = newInterpreter(context); 
            threadInterpreter = null;
        }
    }
}
