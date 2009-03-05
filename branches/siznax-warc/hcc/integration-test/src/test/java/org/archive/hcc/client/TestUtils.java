package org.archive.hcc.client;

import java.net.InetAddress;

import org.archive.hcc.Config;

public class TestUtils {

	public static void setupConfigFile() throws Exception {
		Config.init(false);
		Config c = Config.instance();
		c.addContainer(InetAddress.getLocalHost().getHostName(), 8849, 1);

	}
}
