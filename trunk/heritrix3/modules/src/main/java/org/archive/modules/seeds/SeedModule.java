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

package org.archive.modules.seeds;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.archive.checkpointing.RecoverAction;
import org.archive.modules.ProcessorURI;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SeedModule implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Whether to tag seeds with their own URI as a heritable 'source' String,
     * which will be carried-forward to all URIs discovered on paths originating
     * from that seed. When present, such source tags appear in the
     * second-to-last crawl.log field.
     */
    boolean sourceTagSeeds;
    public boolean getSourceTagSeeds() {
        return sourceTagSeeds;
    }
    public void setSourceTagSeeds(boolean sourceTagSeeds) {
        this.sourceTagSeeds = sourceTagSeeds;
    }
    
    /**
     * Whether to reread the seeds specification, whether it has changed or not,
     * every time any configuration change occurs. If true, seeds are reread
     * even when (for example) new domain overrides are set. Rereading the seeds
     * can take a long time with large seed lists.
     */
    protected boolean rereadSeedsOnConfig = true;
    public boolean getRereadSeedsOnConfig() {
        return rereadSeedsOnConfig;
    }
    public void setRereadSeedsOnConfig(boolean rereadSeedsOnConfig) {
        this.rereadSeedsOnConfig = rereadSeedsOnConfig;
    }

    protected Set<SeedListener> seedListeners = 
        new HashSet<SeedListener>();
    public Set<SeedListener> getSeedListeners() {
        return seedListeners;
    }
    @Autowired
    public void setSeedListeners(Set<SeedListener> seedListeners) {
        this.seedListeners = seedListeners;
    }
    
    protected void publishAddedSeed(ProcessorURI curi) {
        for (SeedListener l: seedListeners) {
            l.addedSeed(curi);
        }
    }
    protected void publishNonSeedLine(String line) {
        for (SeedListener l: seedListeners) {
            l.nonseedLine(line);
        }
    }

    public SeedModule() {
        super();
    }
    
    public abstract void announceSeeds();
    
    public abstract void actOn(File f); 
    
    public abstract boolean addSeed(final ProcessorURI curi);

    public abstract void checkpoint(File dir, List<RecoverAction> actions) throws IOException;

    public void addSeedListener(SeedListener sl) {
        seedListeners.add(sl);
    }
}