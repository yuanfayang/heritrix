package org.archive.hcc.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.archive.hcc.Config;

public class TestUtils  {

	public static void setupConfigFile() throws Exception{
		InputStream is = TestUtils.class.getResourceAsStream("/hcc-config.xml");
		File tmp = File.createTempFile("test", ".xml");
		tmp.deleteOnExit();
		FileOutputStream os = new FileOutputStream(tmp);
		byte[] b = new byte[1024];
		int i = -1;
		while((i = is.read(b)) > -1){
			os.write(b,0, i);
		}
		os.close();
		System.setProperty("hcc.config", tmp.getAbsolutePath());
	}
}
