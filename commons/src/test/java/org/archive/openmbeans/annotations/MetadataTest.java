/* MetadataTest
 *
 * Created on October 18, 2006
 *
 * Copyright (C) 2006 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.openmbeans.annotations;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.openmbeans.factory.Info;
import org.archive.openmbeans.factory.Notif;
import org.archive.openmbeans.factory.Op;
import org.archive.openmbeans.factory.Param;

import junit.framework.TestCase;

public class MetadataTest extends TestCase {

    
    public void testExampleBean() {
        ExampleBean example = new ExampleBean();
        Info info = new Info();
        info.name = ExampleBean.class.getName();
        info.desc = "The example bean for unit testing purposes.";
        
        info.ops.add(makeAnd());
        info.ops.add(makeConcatenate());
        info.ops.add(makeSum(SimpleType.BYTE, "byte", new Byte((byte)0)));
        info.ops.add(makeSum(SimpleType.CHARACTER, "char", '0'));
        info.ops.add(makeSum(SimpleType.DOUBLE, "double", 0.0));
        info.ops.add(makeSum(SimpleType.FLOAT, "float", (float)0.0));
        info.ops.add(makeSum(SimpleType.INTEGER, "int", 0));
        info.ops.add(makeSum(SimpleType.SHORT, "short", (short)0));
        info.ops.add(makeSum(SimpleType.LONG, "long", 0L));
        info.ops.add(makeSum(SimpleType.BIGDECIMAL, "BigDecimal", new BigDecimal("0")));
        info.ops.add(makeSum(SimpleType.BIGINTEGER, "BigInteger", new BigInteger("0")));
        
        Notif notif = new Notif();
        notif.name = "javax.management.Notification";
        notif.desc = "Example notification.";
        notif.notif.add("foo");
        notif.notif.add("bar");
        info.notifs.add(notif);
        
        OpenMBeanInfoSupport expected = info.make();
        assertEquals(expected, example.getMBeanInfo());
    }

    
    private static Op makeSum(SimpleType type, String name, Object def) {
        String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Op op = new Op();
        op.name = "sum" + cap;
        op.desc = "Sums two " + name + "s.";
        op.impact = Bean.ACTION;
        op.ret = type;
        Param param = new Param();
        param.name = "a";
        param.desc = "The first " + name + " to sum.";
        param.type = type;
        param.def = def;
        op.sig.add(param);
        param = new Param();
        param.name = "b";
        param.desc = "The second " + name + " to sum.";
        param.type = type;
        param.def = def;
        op.sig.add(param);
        return op;
    }

    
    private static Op makeAnd() {
        Param param = new Param();        
        Op op = new Op();
        op.name = "and";
        op.desc = "Performs a logical AND on two booleans.";
        op.impact = Bean.ACTION;
        op.ret = SimpleType.BOOLEAN;
        param = new Param();
        param.name = "a";
        param.desc = "The first boolean.";
        param.type = SimpleType.BOOLEAN;
        param.def = false;
        op.sig.add(param);
        param = new Param();
        param.name = "b";
        param.desc = "The second boolean.";
        param.type = SimpleType.BOOLEAN;
        param.def = false;
        op.sig.add(param);
        return op;
    }

    
    private static Op makeConcatenate() {
        Param param = new Param();        
        Op op = new Op();
        op.name = "concatenate";
        op.desc = "Concatenates two strings.";
        op.impact = Bean.ACTION;
        op.ret = SimpleType.STRING;
        param = new Param();
        param.name = "a";
        param.desc = "The first string to concatenate.";
        param.type = SimpleType.STRING;
        param.def = "";
        op.sig.add(param);
        param = new Param();
        param.name = "b";
        param.desc = "The second string to concatenate.";
        param.type = SimpleType.STRING;
        param.def = "";
        op.sig.add(param);
        return op;
    }

}
