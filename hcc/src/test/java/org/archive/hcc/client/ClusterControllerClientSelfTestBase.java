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
        ClusterControllerClientManager.resetDefaultClient();

        cc = ClusterControllerClientManager.getDefaultClient();
        
        cc.setMaxInstances("linux", 8849, 20);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        //cc.destroyAllCrawlers();
        cc.destroy();
    }
    
    public File getTestJar() {
        Map map = new HashMap();
        map.put(OrderJarFactory.NAME_KEY, "test");
        map.put(OrderJarFactory.OPERATOR_KEY, "test operator");
        map.put(OrderJarFactory.DESCRIPTION, "This is a <b>TEST</b> description.");
        map.put(OrderJarFactory.ORGANIZATION, "The Test Organization");

        map.put(OrderJarFactory.USER_AGENT_KEY, "Mozilla/5.0 (compatible;archive.org_bot/1.7.0; Heritrix Cluster Controller Test; +http://hcc.archive.org)");
        map.put(OrderJarFactory.FROM_EMAIL_KEY, "hccTest@archive.org");
        List<String> seeds = new LinkedList<String>();
        seeds.add("http://crawler.archive.org");
        
        map.put(OrderJarFactory.SEEDS_KEY, seeds);
        return OrderJarFactory.createOrderJar(map);
    }

}
