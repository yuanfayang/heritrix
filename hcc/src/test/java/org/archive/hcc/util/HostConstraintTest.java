package org.archive.hcc.util;

import junit.framework.TestCase;

public class HostConstraintTest extends TestCase{
	private HostConstraint h = new HostConstraint("www.test.com");
	
	protected void setUp() throws Exception {
	}
	
	public void testConstructor(){
		assertEquals(h.hostArray[0],"www");
		assertEquals(h.hostArray[1],"test");
		assertEquals(h.hostArray[2],"com");
	}
	
	public void testGetOrderFileDirectory(){
		assertEquals("settings/com/test/www", h.getSettingsFileDirectory());
	}

	public void testGetOrderFilePath(){
		assertEquals("settings/com/test/www/settings.xml", h.getSettingsFilePath());
	}
	
}
