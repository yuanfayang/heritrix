package org.archive.hcc.client;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.archive.hcc.ClusterControllerBean;
import org.archive.hcc.util.OrderJarFactory;

public class ClusterControllerClientSelfTestBase
        extends
            TestCase {
    protected ClusterControllerClient cc;

    protected void setUp() throws Exception {
        super.setUp();
        new ClusterControllerBean().init();
        cc = ClusterControllerClientManager.getDefaultClient();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        cc.destroy();
    }
    
    public File getTestJar() {
        Map map = new HashMap();
        map.put("name", "test");
        List<String> seeds = new LinkedList<String>();
        seeds.add("http://crawler.archive.org");
        map.put("seeds", seeds);
        return OrderJarFactory.createOrderJar(map);
    }

}
