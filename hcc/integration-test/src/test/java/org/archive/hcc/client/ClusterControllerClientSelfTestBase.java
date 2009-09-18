package org.archive.hcc.client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import junit.framework.TestCase;

import org.archive.hcc.ClusterControllerBean;
import org.archive.hcc.util.JmxUtils;
import org.archive.hcc.util.OrderJarFactory;

public class ClusterControllerClientSelfTestBase
        extends
            TestCase {
    protected ClusterControllerClient cc;
    
    protected void setMaxCrawlersTo(int max){
    	try{
    		Collection<Crawler> list = cc.listCrawlers();
    		Crawler c;
    		if(list.size() == 0){
        		c  = cc.createCrawler();
    		}else{
    			c = list.iterator().next();
    		}
    		InetSocketAddress a  = JmxUtils.extractRemoteAddress(c.getName());
    		cc.setMaxInstances(a.getHostName(), a.getPort(), max);
    		c.destroy();
    	}catch(Exception e){
            e.printStackTrace();
            assertFalse(true);
    	}

    }

    
    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.setupConfigFile();
        new ClusterControllerBean().init();
        ClusterControllerClientManager.resetDefaultClient();
        cc = ClusterControllerClientManager.getDefaultClient();
        setMaxCrawlersTo(10);
        Thread.sleep(3*1000);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        //cc.destroyAllCrawlers();
        cc.destroy();
        Thread.sleep(6*1000);

    }
    
    public File getTestJar() {
        List<String> seeds = new LinkedList<String>();
        seeds.add("http://crawler.archive.org/hcc");
 
    	OrderJarFactory f = new OrderJarFactory(
        		"test"
        		,"Mozilla/5.0 (compatible;archive.org_bot/1.7.0; Heritrix Cluster Controller Test; +http://hcc.archive.org)"
        		,"hccTest@archive.org"
        		,"This is a <b>TEST</b> description."
        		,"My Org"
        		,seeds
        );
        
    	try {
    		return f.createOrderJarFile();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }

}
