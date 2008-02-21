/* Parameter
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
 * Runtime annotation for exposing a method's parameters via JMX.  This
 * annotation is optional, but omitted will cause the introspector to give
 * your methods' parameters generic names like "arg1", "arg2" and so on.
 * 
 * @author pjack
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {

    /** The name of the parameter. */
    String name();
    
    /** The description of the parameter. */
    String desc();
    
    /** The default value of the parameter. */
    String def() default "";
    
    /** The minimum value of the parameter. */
    String min() default "";
    
    /** The maximum value of the parameter. */
    String max() default "";
    
    /** The list of legal values for the parameter. */
    String[] legal() default {};

    /**
     * The open type of the parameter.  
     * 
     * <p>The value of this field should not be a Java class name.  Instead, 
     * the value should reference a public static field defined by the same 
     * class that defined the parameter being annotated.  That static field 
     * should contain an {@link javax.management.openmbeans.OpenType} instance; 
     * that OpenType will be used as the parameter type.
     * 
     * <p>A blank string indicates that the Java class of the parameter
     * should be automagically converted into an Open Type.  Eg, a method
     * parameter of java.lang.String would have an open type of 
     * SimpleType.STRING.
     */
    String type() default "";
}
