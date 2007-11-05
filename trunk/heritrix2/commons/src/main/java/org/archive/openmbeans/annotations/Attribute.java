/* Attribute
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
 * Runtime annotation for exposing an attribute via JMX.  Use this annotation
 * on a public accessor method (eg, <code>getFoo()</code> or 
 * <code>isBaz()</code>, etc).  The resulting attribute will be read/write if 
 * your class defines a corresponding setter method following the JavaBeans 
 * conventions (eg, if the accessor is <code>String getFoo()</code>, then the 
 * setter must be <code>void setFoo(String)</code>).  Otherwise the 
 * attribute will be read-only.
 * 
 * <p>The attribute's name will be automatically determined based on the 
 * accessor method's name.
 * 
 * @author pjack
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attribute {


    /** The description of the attribute. */
    String desc();


    /** 
     * The default value of the attribute, as a string.  
     * 
     * <p>The value must be parseable into the return type of the attribute's 
     * accessor.  Eg, if you annotate <code>int getFoo()</code> then the 
     * <code>def</code> value must be a valid <code>int</code>.
     */
    String def();


    /** 
     * The minimum value of the attribute.  An empty string means that there
     * is no minimum value.
     * 
     * <p>The value must be parseable into the return type of the attribute's 
     * accessor.  Eg, if you annotate <code>int getFoo()</code> then the 
     * <code>min</code> value must be a valid <code>int</code> (or empty).
     */
    String min() default "";


    /** 
     * The maximum value of the attribute.  An empty string means that there
     * is no maximum value.
     * 
     * <p>The value must be parseable into the return type of the attribute's 
     * accessor.  Eg, if you annotate <code>int getFoo()</code> then the 
     * <code>max</code> value must be a valid <code>int</code> (or empty).
     */
    String max() default "";


    /** 
     * The legal values of the attribute, as strings.  An empty array
     * (the default) means that any value is allowed. 
     */
    String[] legal() default {};

}
