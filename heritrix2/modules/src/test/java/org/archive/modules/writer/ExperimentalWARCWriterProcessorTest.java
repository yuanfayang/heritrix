/* 
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
 *
 * ExperimentalWARCWriterProcessor.java
 *
 * Created on Jan 31, 2007
 *
 * $Id:$
 */
package org.archive.modules.writer;


import java.io.File;

import org.archive.modules.ProcessorTestBase;
import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.modules.writer.ExperimentalWARCWriterProcessor;
import org.archive.modules.writer.MetadataProvider;
import org.archive.modules.writer.WriterPoolProcessor;
import org.archive.state.ExampleStateProvider;
import org.archive.state.Path;
import org.archive.util.TmpDirTestCase;


/**
 * Unit test for {@link ExperimentalWARCWriterProcessor}.
 *
 * @author pjack
 */
public class ExperimentalWARCWriterProcessorTest extends ProcessorTestBase {
    public class DummyMetadataProvider implements MetadataProvider {
        public String getAudience() {
            return null;
        }

        public String getFrom() {
            return null;
        }

        public String getJobDescription() {
            return null;
        }

        public String getJobName() {
            return null;
        }

        public String getJobOperator() {
            return null;
        }

        public String getOrganization() {
            return null;
        }

        public String getRobotsPolicy() {
            return null;
        }

        public String getUserAgent() {
            return null;
        }
    }


    @Override
    protected Class<?> getModuleClass() {
        return ExperimentalWARCWriterProcessor.class;
    }
    
    
    @Override
    protected Object makeModule() throws Exception {
        File tmp = TmpDirTestCase.tmpDir();
        tmp = new File(tmp, "ARCWriterProcessTest");
        tmp.mkdirs();

        ExampleStateProvider sp = new ExampleStateProvider();
        Path dir = new Path(tmp.getAbsolutePath());

        ExperimentalWARCWriterProcessor result = new ExperimentalWARCWriterProcessor();
        sp.set(result, WriterPoolProcessor.DIRECTORY, dir);
        sp.set(result, WriterPoolProcessor.SERVER_CACHE, new DefaultServerCache());
        sp.set(result, WriterPoolProcessor.METADATA_PROVIDER, new DummyMetadataProvider());
        result.initialTasks(sp);
        return result;
    }

    @Override
    protected void verifySerialization(Object first, byte[] firstBytes, 
            Object second, byte[] secondBytes) throws Exception {

    }
    
    // TODO TESTME!
    
}
