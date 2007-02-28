/* PersistLogProcessor.java
 * 
 * Created on Feb 18, 2005
 *
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
 */
package org.archive.crawler.processor.recrawl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.codec.binary.Base64;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;



/**
 * Log CrawlURI attributes from latest fetch for consultation by a later 
 * recrawl. Log must be imported into alternate data structure in order
 * to be consulted. 
 * 
 * @author gojomo
 * @version $Date: 2006-09-25 20:19:54 +0000 (Mon, 25 Sep 2006) $, $Revision: 4654 $
 */
public class PersistLogProcessor extends PersistProcessor {
    private static final long serialVersionUID = 1678691994065439346L;
    
    protected PrintStream log;

    /** setting for log filename */
    public static final String ATTR_LOG_FILENAME = "log-filename";
    /** default log filename */ 
    public static final String DEFAULT_LOG_FILENAME = "persistlog.txtser";
    /**
     * Usual constructor
     * 
     * @param name
     */
    public PersistLogProcessor(String name) {
        super(name, "PersistLogProcessor. Logs CrawlURI attributes " +
                "from latest fetch for consultation by a later recrawl.");
        
        addElementToDefinition(new SimpleType(ATTR_LOG_FILENAME,
                "Filename to which to log URI persistence information. " +
                "Interpreted relative to job directory. " +
                "Default is 'persistlog.txtser'. ", 
                DEFAULT_LOG_FILENAME));
    }

    @Override
    protected void initialTasks() {
        try {
            // TODO make configurable filename
            File logFile = FileUtils.maybeRelative(getController().getDisk(),
                    (String) getUncheckedAttribute(null, ATTR_LOG_FILENAME));
            log = new PrintStream(logFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void finalTasks() {
        log.close();
    }

    @Override
    protected void innerProcess(CrawlURI curi) {
        if(shouldStore(curi)) {
            synchronized (log) {
                log.print(persistKeyFor(curi));
                log.append(' ');
                try {
                    log.write(Base64.encodeBase64(IoUtils
                            .serializeToByteArray(curi.getPersistentAList())));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
                log.print('\n');
            }            
        }
    }
}