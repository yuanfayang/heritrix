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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.modules.Processor;
import org.archive.modules.ProcessorURI;
import org.archive.settings.KeyChangeEvent;
import org.archive.settings.KeyChangeListener;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Path;
import org.archive.state.StateProvider;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * A processor which runs a BeanShell script on the CrawlURI.
 *
 * Script source may be provided via a file
 * local to the crawler. 
 * Script source should define
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
public class BeanShellProcessor extends Processor 
implements Initializable, KeyChangeListener {

    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(BeanShellProcessor.class.getName());


    /**
     *  BeanShell script file.
     */
    @Immutable
    final public static Key<Path> SCRIPT_FILE = 
        Key.make(Path.EMPTY);


    /**
     * Whether each ToeThread should get its own independent script context, or
     * they should share synchronized access to one context. Default is true,
     * meaning each threads gets its own isolated context.
     * 
     */
    @Immutable
    final public static Key<Boolean> ISOLATE_THREADS = Key.make(true);
    

    @Immutable
    final public static Key<SheetManager> MANAGER = 
        Key.makeAuto(SheetManager.class);


    protected ThreadLocal<Interpreter> threadInterpreter;
    protected Interpreter sharedInterpreter;
    public Map<Object,Object> sharedMap = Collections.synchronizedMap(
            new HashMap<Object,Object>());

    private SheetManager manager;
    
    /**
     * Constructor.
     */
    public BeanShellProcessor() {
        super();
    }

    protected boolean shouldProcess(ProcessorURI curi) {
        return true;
    }
    
    @Override
    protected synchronized void innerProcess(ProcessorURI curi) {
        // depending on previous configuration, interpreter may 
        // be local to this thread or shared
        Interpreter interpreter = getInterpreter(); 
        synchronized(interpreter) {
            // synchronization is harmless for local thread interpreter,
            // necessary for shared interpreter
            try {
                interpreter.set("curi",curi);
                interpreter.eval("process(curi)");
            } catch (EvalError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }

    /**
     * Get the proper Interpreter instance -- either shared or local 
     * to this thread. 
     * @return Interpreter to use
     */
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

    /**
     * Create a new Interpreter instance, preloaded with any supplied
     * source code or source file and the variables 'self' (this 
     * BeanShellProcessor) and 'controller' (the CrawlController). 
     * 
     * @return  the new Interpreter instance
     */
    protected Interpreter newInterpreter() {
        Interpreter interpreter = new Interpreter(); 
        try {
            interpreter.set("self", this);
            interpreter.set("manager", manager);

            File file = get(SCRIPT_FILE).toFile();
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

    public void initialTasks(StateProvider context) {
        this.manager = context.get(this, MANAGER);
        isolate(context.get(this, ISOLATE_THREADS));
    }

    /**
     * Setup (or reset) Intepreter variables, as appropraite based on 
     * thread-isolation setting. 
     */
    public void keyChanged(KeyChangeEvent event) {
        if (event.getKey() == ISOLATE_THREADS) {
            // TODO make it so running state (tallies, etc.) isn't lost on changes
            // unless unavoidable
            isolate((Boolean)event.getNewValue());
        }
    }
    
    
    private void isolate(boolean isolate) {
        if (isolate) {
            sharedInterpreter = null; 
            threadInterpreter = new ThreadLocal<Interpreter>(); 
        } else {
            sharedInterpreter = newInterpreter(); 
            threadInterpreter = null;
        }        
    }
    
    
    private <T> T get(Key<T> key) {
        Sheet def = manager.getGlobalSheet();
        return def.get(this, key);
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(BeanShellProcessor.class);
    }
}
