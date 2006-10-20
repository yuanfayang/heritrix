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
 * Runtime annotation for exposing a notification via JMX.  Use this annotation
 * on an <i>emitter method</i>, which should be a method of the form:
 * 
 * <pre>
 *    void emit(X event)
 * </pre>
 * 
 * where X is javax.management.Notification or one of its subclasses.  Note 
 * that emitter methods can be public, protected, package-protected or even
 * private; this annotation will still be visible to the introspector.
 * 
 * <p>The notification's name will be automatically set to X by the 
 * introspector.  This annotation provides additional description and type
 * fields.
 * 
 * @author pjack
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Emitter {


    /** The description of the notification. */
    String desc();


    /** The notification types (in dot notation) that the MBean may emit. */
    String[] types();
}
