package org.archive.hcc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class ConfigTest extends TestCase {
	public static void setupConfigFile() throws Exception{
		InputStream is = Config.class.getResourceAsStream("/hcc-config.xml");
		File tmp = File.createTempFile("test", ".xml");
		FileOutputStream os = new FileOutputStream(tmp);
		byte[] b = new byte[1024];
		int i = 0;
		while((i = is.read(b)) > 0){
			os.write(b,0, i);
		}
		os.close();
		System.setProperty("hcc.config", tmp.getAbsolutePath());
	}
	public void testConfig() throws Exception{
		
		setupConfigFile();
		
		Config c = Config.instance();
		assertNotNull(c);
		List<Container> containers = c.getContainers();
		assertNotNull(containers);
		assertTrue(containers.size() > 0);
		assertNotNull(c.getDefaultSettingsDirectory());
		
		assertEquals("localhost", containers.get(0).getAddress().getHostName());
		assertEquals(9090, containers.get(0).getAddress().getPort());

	}
}
