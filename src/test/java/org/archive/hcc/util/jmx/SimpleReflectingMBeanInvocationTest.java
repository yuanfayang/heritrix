package org.archive.hcc.util.jmx;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.hcc.util.jmx.SimpleReflectingMbeanInvocation;

public class SimpleReflectingMBeanInvocationTest
        extends
            MBeanTestCase {

    public void test() {
        class TestClass {
            private boolean toggled = false;

            public String test() {
                return "test";
            }

            public Integer test(Integer i) {
                return i;
            }

            public void toggleTest() {
                toggled = true;
            }

            public boolean isToggled() {
                return this.toggled;
            }
            
            public String invoke(){
                return SimpleReflectingMBeanInvocationTest.this.invoke();
            }
        }

        TestClass testClass = new TestClass();
        SimpleReflectingMbeanInvocation i = new SimpleReflectingMbeanInvocation(
                testClass,
                createInfo(),
                new Object[0]);

        try {
            String s = (String) i.invoke();
            assertEquals("test", s);

        } catch (MBeanException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (ReflectionException e) {
            e.printStackTrace();
            assertTrue(false);

        }

        i = new SimpleReflectingMbeanInvocation(
                testClass,
                new OpenMBeanOperationInfoSupport(
                        "test",
                        "test operation",
                        new OpenMBeanParameterInfo[] { new OpenMBeanParameterInfoSupport(
                                "i",
                                "an integer",
                                SimpleType.INTEGER) },
                        SimpleType.INTEGER,
                        0),
                new Object[] { new Integer(5) });

        try {
            Integer num = (Integer) i.invoke();
            assertEquals(new Integer(5), num);
        } catch (MBeanException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (ReflectionException e) {
            e.printStackTrace();
            assertTrue(false);

        }

        i = new SimpleReflectingMbeanInvocation(
                testClass,
                new OpenMBeanOperationInfoSupport(
                        "toggleTest",
                        "toggle test operation",
                        null,
                        SimpleType.VOID,
                        0),
                null);

        try {
            i.invoke();
            assertTrue(testClass.isToggled());
        } catch (MBeanException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (ReflectionException e) {
            e.printStackTrace();
            assertTrue(false);

        }

    }
}
