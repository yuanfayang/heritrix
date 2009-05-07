package org.archive.hcc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import javax.management.DynamicMBean;
import junit.framework.TestCase;

public class ContainerLoadBalancingStrategyTest extends TestCase{
	public void testResolveAvailableContainers(){
		List<Container> containers = new LinkedList<Container>();
		Container c1 = createContainer("server1", 9090,1,0);
		Container c2 = createContainer("server1", 9091,5,5);
		Container c3 = createContainer("server2", 9090,1,0);
		Container c4 = createContainer("server2", 9091,5,2);
		Container c5 = createContainer("server3", 9090,1,0);
		Container c6 = createContainer("server3", 9091,1,1);
		containers.add(c1);
		containers.add(c2);
		containers.add(c3);
		containers.add(c4);
		containers.add(c5);
		containers.add(c6);
		
		ContainerLoadBalancingStrategy strategy = new ContainerLoadBalancingStrategy();
		List<Container> orderedContainers = strategy.prioritize(containers);
		assertEquals(orderedContainers.size(), 4);
		assertEquals(orderedContainers.get(0), c5);
		assertEquals(orderedContainers.get(1), c3);
		assertEquals(orderedContainers.get(2), c4);
		assertEquals(orderedContainers.get(3), c1);
	}
	
	private Container createContainer(String host, int port, int maxInstances, int crawlers){
		Container c =  new Container(new InetSocketAddress(host, port), maxInstances);
		for(int i = 0; i < crawlers; i++){
            DynamicMBean proxy = (DynamicMBean) Proxy.newProxyInstance(
                    DynamicMBean.class.getClassLoader(),
                    new Class[] { DynamicMBean.class },new InvocationHandler(){
                    	public Object invoke(Object proxy, Method method,
                    			Object[] args) throws Throwable {
                    		return null;
                    	}
                    });			
			c.addCrawler(new Crawler(proxy, null, c));
		}
		return c;
	}
}
