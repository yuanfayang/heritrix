/* Operation
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


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Runtime annotation for exposing an operation via JMX.  Use this on a 
 * public method.  You may also want to consider using the {@link Parameter}
 * annotation on each of the method's parameters; otherwise the parameter
 * metadata exposed via JMX will be uninformative (eg, a parameter with a 
 * name of "arg1" and a description of "arg1").
 * 
 * @author pjack
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {

    /**
     * The description of the operation.
     */
    String desc();
    
    
    /**
     * The impact of the operation.
     */
    int impact() default Bean.ACTION;
    
    
    /**
     * The open type of the operation.  This is the return type, but the value
     * of this field should not be a Java class name.  Instead, the value 
     * should reference a public static field defined by the same class that
     * defined the operation being annotated.  That static field should 
     * contain an {@link javax.management.openmbeans.OpenType} instance; 
     * that OpenType will be used as the return type for the operation.
     * 
     * <p>A blank string indicates that the Java class of the return type
     * should be automagically converted into an Open Type.  Eg, a method
     * that returns java.lang.String would have an open type of 
     * SimpleType.STRING.
     */
    String type() default "";

}
