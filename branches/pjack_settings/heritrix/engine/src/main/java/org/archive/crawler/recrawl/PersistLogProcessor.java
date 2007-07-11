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
package org.archive.crawler.recrawl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationUtils;
import org.archive.crawler.framework.CrawlerLoggerModule;
import org.archive.crawler.io.CrawlerJournal;
import org.archive.modules.ProcessorURI;
import org.archive.settings.RecoverAction;
import org.archive.settings.file.Checkpointable;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.FileUtils;



/**
 * Log CrawlURI attributes from latest fetch for consultation by a later 
 * recrawl. Log must be imported into alternate data structure in order
 * to be consulted. 
 * 
 * @author gojomo
 * @version $Date: 2006-09-25 20:19:54 +0000 (Mon, 25 Sep 2006) $, $Revision: 4654 $
 */
public class PersistLogProcessor extends PersistProcessor implements Checkpointable {
    private static final long serialVersionUID = 1678691994065439346L;
    
    protected CrawlerJournal log;

    @Immutable
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE = 
        Key.make(CrawlerLoggerModule.class, null);

    @Immutable
    final public static Key<String> LOG_FILENAME = Key.make("persistlog.txtser.gz");
    // description: "Filename to which to log URI persistence information. " +
    // "Interpreted relative to job logs directory. " +
    // "Default is 'persistlog.txtser.gz'. "
    
//    class description: "PersistLogProcessor. Logs CrawlURI attributes " +
//    "from latest fetch for consultation by a later recrawl."
    
    public PersistLogProcessor() {;
    }

    @Override
    public void initialTasks(StateProvider provider) {
        try {
            File logFile = FileUtils.maybeRelative(
            		provider.get(this, LOGGER_MODULE).getLogsDir(),
                    provider.get(this,LOG_FILENAME));
            log = new CrawlerJournal(logFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void finalTasks(StateProvider provider) {
        log.close();
    }

    @Override
    protected void innerProcess(ProcessorURI curi) {
        log.writeLine(persistKeyFor(curi), " ", 
        new String(Base64.encodeBase64(
                SerializationUtils.serialize((Serializable)curi.getData()))));      
    }

	public void checkpoint(File dir, List<RecoverAction> actions) throws IOException {
        // rotate log
        log.checkpoint(dir,null);
    }

	@Override
	protected boolean shouldProcess(ProcessorURI uri) {
		return shouldStore(uri);
	}
}