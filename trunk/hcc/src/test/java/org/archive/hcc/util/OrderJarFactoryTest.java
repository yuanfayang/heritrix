package org.archive.hcc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

import org.archive.hcc.ConfigTest;

public class OrderJarFactoryTest extends TestCase{
	public OrderJarFactoryTest() throws Exception{
		ConfigTest.setupConfigFile();
	}
	
	public void testWriteOrderFile(){
		try {
			HostConstraint hc = new HostConstraint("www.test.com");
			 File tempCrawlSettingsDirectoryRoot = 
	            	new File(System.getProperty("java.io.tmpdir") + 
	            			File.separator + new Date().getTime());
	           
			File file = OrderJarFactory.writeSettingsFile(hc, tempCrawlSettingsDirectoryRoot);
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

			 File tempCrawlSettingsDirectoryRoot = 
	            	new File(System.getProperty("java.io.tmpdir") + 
	            			File.separator + new Date().getTime());
	           
			Map<String,InputStream> files = 
				OrderJarFactory.writeHostConstraints(list,tempCrawlSettingsDirectoryRoot);
			
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
	
	
	public void testAddFilesFromSettingsDirectory() throws IOException{
		File directoryRoot = null;
		try{
			directoryRoot = new File(System.getProperty("java.io.tmpdir"), "order-jar-factor-test");
			directoryRoot.mkdir();
			File file1 = new File(directoryRoot,"test.xml");

			File subdir =  new File(directoryRoot, "subdir1");
			subdir.mkdir();
			File file2 = new File(subdir,"test.xml");

			PrintWriter writer = new PrintWriter(new FileOutputStream(file1));
			writer.write("This is a test");
			writer.close();

			writer = new PrintWriter(new FileOutputStream(file2));
			writer.write("This is a test");
			writer.close();

			Map<String,InputStream> files = new HashMap<String,InputStream>();
			OrderJarFactory.addFilesFromSettingsDirectory(files, directoryRoot.getCanonicalPath());
			assertEquals(2,files.size());
			assertNotNull(files.get("test.xml"));
			assertNotNull(files.get("subdir1/test.xml"));
		}catch(Exception ex){
			ex.printStackTrace();
			assertTrue(false);
		}finally{
			directoryRoot.delete();
		}
	}

	public void testCreateOrderJar(){
		try {
			List<String> seeds = new LinkedList<String>();
			seeds.add("http://www.test.com");
			seeds.add("http://www.test.com/hidden");
			OrderJarFactory f = new OrderJarFactory("1234", "userAgentInfo", "test@archive-it.org", "description here", "my org", seeds);
			HostConstraint hc = new HostConstraint("www.test.com");
			List<HostConstraint> hostConstraints = new LinkedList<HostConstraint>();
			hostConstraints.add(hc);
			f.setHostConstraints(hostConstraints);
			
            File jar = f.createOrderJar();
			
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
