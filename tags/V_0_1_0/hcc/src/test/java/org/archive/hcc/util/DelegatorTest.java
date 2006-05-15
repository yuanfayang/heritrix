package org.archive.hcc.util;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.archive.hcc.util.Delegator.DelegatorPolicy;

public class DelegatorTest
        extends
            TestCase {

    public DelegatorTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAcceptFirst() {
        Delegator d = new Delegator();
        final AtomicInteger i = new AtomicInteger(0);
        d.addDelegatable(new Delegatable() {
            public boolean delegate(Object object) {
                i.incrementAndGet();
                return true;
            }
        });

        d.addDelegatable(new Delegatable() {
            public boolean delegate(Object object) {
                i.incrementAndGet();
                return true;
            }
        });

        d.delegate(new Object());

        assertEquals(1, i.get());
    }

    public void testAcceptAll() {
        Delegator d = new Delegator(DelegatorPolicy.ACCEPT_ALL);
        final AtomicInteger i = new AtomicInteger(0);
        d.addDelegatable(new Delegatable() {
            public boolean delegate(Object object) {
                i.incrementAndGet();
                return true;
            }
        });

        d.addDelegatable(new Delegatable() {
            public boolean delegate(Object object) {
                i.incrementAndGet();
                return true;
            }
        });

        d.delegate(new Object());

        assertEquals(2, i.get());
    }

}
