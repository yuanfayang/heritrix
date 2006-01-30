package org.archive.hcc;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.archive.util.JmxUtils;
import org.archive.hcc.util.jmx.MBeanServerConnectionFactory;

public class ClusterControllerBeanSelfTest
        extends
            TestCase {
    private ClusterControllerBean ccBean;

    protected void setUp() throws Exception {
        super.setUp();
        this.ccBean = new ClusterControllerBean();
        this.ccBean.init();
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
    


//    public void testGetAttributesFromCrawler() throws Exception {
//        ObjectName[] crawlers = (ObjectName[]) ccBean.invoke(
//                "listCrawlers",
//                new Object[] {},
//                new String[] {});
//        MBeanServerConnection c = MBeanServerConnectionFactory
//                .createConnection(JmxUtils.extractAddress(crawlers[0]));
//        AttributeList list = (AttributeList) c.getAttributes(
//                crawlers[0],
//                new String[] { "Status" });
//        assertNotNull(list);
//        System.out.println(((Attribute) list.get(0)).getValue());
//    }
//
//    public void testInvokeCrawler() throws Exception {
//        ObjectName[] crawlers = (ObjectName[]) ccBean.invoke(
//                "listCrawlers",
//                new Object[] {},
//                new String[] {});
//        MBeanServerConnection c = MBeanServerConnectionFactory
//                .createConnection(JmxUtils.extractAddress(crawlers[0]));
//        c.invoke(crawlers[0], "startCrawling", new Object[0], new String[0]);
//
//    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.getAttribute(String)'
     */
    public void testGetAttribute() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.getAttributes(String[])'
     */
    public void testGetAttributes() {

    }

    /*
     * Test method for 'org.archive.hcc.ClusterControllerBean.getMBeanInfo()'
     */
    public void testGetMBeanInfo() {

    }

    /*
     * Test method for 'org.archive.hcc.ClusterControllerBean.invoke(String,
     * Object[], String[])'
     */
    public void testInvoke() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.setAttribute(Attribute)'
     */
    public void testSetAttribute() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.setAttributes(AttributeList)'
     */
    public void testSetAttributes() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.addNotificationListener(NotificationListener,
     * NotificationFilter, Object)'
     */
    public void testAddNotificationListener() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.getNotificationInfo()'
     */
    public void testGetNotificationInfo() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.removeNotificationListener(NotificationListener,
     * NotificationFilter, Object)'
     */
    public void testRemoveNotificationListenerNotificationListenerNotificationFilterObject() {

    }

    /*
     * Test method for
     * 'org.archive.hcc.ClusterControllerBean.removeNotificationListener(NotificationListener)'
     */
    public void testRemoveNotificationListenerNotificationListener() {

    }

}
