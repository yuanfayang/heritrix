package org.archive.hcc.client;

import junit.framework.TestCase;

import org.archive.hcc.ClusterControllerBean;
import org.archive.hcc.client.ClusterControllerClientImpl;

public class ClusterControllerClientSelfTest
        extends
            TestCase {
    protected ClusterControllerClientImpl cc;

    protected void setUp() throws Exception {
        super.setUp();
        new ClusterControllerBean().init();

        cc = new ClusterControllerClientImpl();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        cc.destroy();

    }

}