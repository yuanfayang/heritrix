package org.archive.crawler.restui.microframework;

import junit.framework.TestCase;

public class RestServletTest extends TestCase {
    RestServlet servlet;  
    
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new RestServlet();
        servlet.register(DummyController.class);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRegister() {
        assertTrue(servlet.isRegistered(DummyController.class));
    }
    
    public void testGetMethodForPath() {
        assertNotNull(servlet.getMethodForPath(GET.class, "/foo"));
    }
}
