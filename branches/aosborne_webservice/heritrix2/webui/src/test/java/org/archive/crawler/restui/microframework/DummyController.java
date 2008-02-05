package org.archive.crawler.restui.microframework;

import org.archive.crawler.restui.microframework.GET;
import org.archive.crawler.restui.microframework.RestController;

public class DummyController extends RestController {
    @GET("/foo")
    public void getSimple() {
        
    }
}
