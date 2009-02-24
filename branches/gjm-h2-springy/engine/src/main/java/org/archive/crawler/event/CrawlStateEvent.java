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
package org.archive.crawler.event;

import org.archive.crawler.framework.CrawlControllerImpl.State;
import org.springframework.context.ApplicationEvent;

public class CrawlStateEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    protected State state;
    protected String message;

    public CrawlStateEvent(Object source, State state, String message) {
        super(source);
        this.state = state;
        this.message = message; 
    }

    public String getMessage() {
        return message;
    }

    public State getState() {
        return state;
    }
    
    public static void translate(CrawlStatusListener listener, CrawlStateEvent event) {
        switch(event.getState()) {
            case PAUSED:
                listener.crawlPaused(event.getMessage());
                break;
            case RUNNING:
                listener.crawlResuming(event.getMessage());
                break;
            case PAUSING:
                listener.crawlPausing(event.getMessage());
                break;
            case STARTED:
                listener.crawlStarted(event.getMessage());
                break;
            case STOPPING:
                listener.crawlEnding(event.getMessage());
                break;
            case FINISHED:
                listener.crawlEnded(event.getMessage());
                break;
            case PREPARING:
                listener.crawlResuming(event.getMessage());
                break;
            default:
                throw new RuntimeException("Unknown state: " + event.getState());
        }
    }
    
}
