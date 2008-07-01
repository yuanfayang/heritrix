package org.archive.hcc;

import java.net.InetSocketAddress;
import java.util.Collection;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.archive.util.JmxUtils;
import org.archive.hcc.client.Crawler;
import org.archive.hcc.util.jmx.MBeanServerConnectionFactory;

public class ClusterControllerBeanSelfTest
        extends
            TestCase {
    private ClusterControllerBean ccBean;

    protected void setMaxCrawlersTo(int max){
    	try{
    		ObjectName[] list = ccBean.listCrawlers();
    		InetSocketAddress a  = org.archive.hcc.util.JmxUtils.extractRemoteAddress(list[0]);
    		ccBean.setMaxInstances(a.getHostName(), a.getPort(), max);
    	}catch(Exception e){
            e.printStackTrace();
            assertFalse(true);
    	}

    }
    
    protected void setUp() throws Exception {
        super.setUp();
        this.ccBean = new ClusterControllerBean();
        this.ccBean.init();
        setMaxCrawlersTo(5);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        this.ccBean.destroy();
    }

    /*
     * Test method for 'org.archive.hcc.ClusterControllerBean.createCrawler()'
     */
    public void testCreateDestroyCrawler() throws Exception {
        int count = ccBean.getTotalCrawlerCount();
        ObjectName name = (ObjectName) ccBean.invoke(
                "createCrawler",
                new Object[] {},
                new String[] {});
        assertNotNull(name);
        ObjectName[] crawlers = (ObjectName[]) ccBean.invoke(
                "listCrawlers",
                new Object[] {},
                new String[] {});
        assertTrue(crawlers.length == (count + 1));

    }

    /*
     * Test method for 'org.archive.hcc.ClusterControllerBean.createCrawler()'
     */
    public void testListCrawlers() throws Exception {
        ObjectName[] crawlers = (ObjectName[]) ccBean.invoke(
                "listCrawlers",
                new Object[] {},
                new String[] {});
        assertNotNull(crawlers);
        assertTrue(crawlers.length > 0);
    }
    



}
