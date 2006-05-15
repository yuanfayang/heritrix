package org.archive.hcc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

public class OrderJarFactoryTest extends TestCase{

	public void testWriteOrderFile(){
		try {
			HostConstraint hc = new HostConstraint("www.test.com");
			File file = OrderJarFactory.writeSettingsFile(hc);
			assertNotNull(file);
			assertTrue(file.exists());
			assertTrue(file.delete());
		} catch (IOException e) {
			e.printStackTrace();
			assertFalse(true);
		}
	}
	
	
	public void testWriteHostConstraints(){
		try {
			HostConstraint hc = new HostConstraint("www.test.com");
			List<HostConstraint> list = new LinkedList<HostConstraint>();
			list.add(hc);
			Map<String,InputStream> files = OrderJarFactory.writeHostConstraints(list);
			
			assertNotNull(files);
			assertEquals(1,files.size());
			String file = files.keySet().iterator().next();
			assertEquals(file, "settings/com/test/www/settings.xml");
			files.get(file).close();
		} catch (IOException e) {
			e.printStackTrace();
			assertFalse(true);
		}
	}
	
	
	public void testCreateOrderJar(){
		try {
			Map p = new HashMap(); 
			p.put(OrderJarFactory.NAME_KEY, "12345");
			p.put(OrderJarFactory.USER_AGENT_KEY, "user agent info");
			p.put(OrderJarFactory.FROM_EMAIL_KEY, "test@archiveit.org");

			
			List<String> seeds = new LinkedList<String>();
			seeds.add("http://www.test.com");
			seeds.add("http://www.test.com/hidden");
			
			p.put(OrderJarFactory.SEEDS_KEY, seeds);
			

			HostConstraint hc = new HostConstraint("www.test.com");
			List<HostConstraint> hostConstraints = new LinkedList<HostConstraint>();
			hostConstraints.add(hc);

			p.put(OrderJarFactory.HOST_CONSTRAINTS_KEY, hostConstraints);
			
			File jar = OrderJarFactory.createOrderJar(p);
			
			JarInputStream jis = new JarInputStream(new FileInputStream(jar));
			
			JarEntry je = null;
			while((je = jis.getNextJarEntry()) != null){
				System.out.println(je.getName());
				jis.closeEntry();
			}
			
			jis.close();
			
			jar.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			assertFalse(true);

		} catch (IOException e) {
			e.printStackTrace();
			assertFalse(true);

		}
		
	}
}
