/* ValueConstraint
 * 
 * $Id$
 * 
 * Created on Mar 29, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel.settings;

import java.util.logging.Level;

import javax.management.Attribute;


/**
 * Superclass for constraints that can be set on attribute definitions.
 * <p>
 * Constraints will be checked against attribute values. If a constraint check
 * fails, an object of type FailedCheck is returned containing information that
 * can be used to build meaningful information to the user.
 * <p>
 * A constraint has one of three levels:
 * <ul>
 * <li>{@link java.util.logging.Level#SEVERE}The attribute could not be set
 * whatsoever.
 * <li>{@link java.util.logging.Level#WARNING}The attribute is illegal i
 * CrawlJobs, but could be set in profiles. Mostly used as holder value for
 * settings that should be changed for every entity running a crawl.
 * <li>{@link java.util.logging.Level#INFO}The attribute has a legal value,
 * but is outside the bounds of what are considered a reasonable value. The user
 * could be warned that she should investigate if the value actally is what she
 * wants it be.
 * </ul>
 * 
 * @author John Erik Halse
 */
public abstract class Constraint implements Comparable {
    private final Level severity;
    private final String msg;

    /** Constructs a new Constraint.
     * 
     * @param level the level for this constraint.
     * @param msg default message to return if the check fails.
     */
    public Constraint(Level level, String msg) {
        if (level != Level.SEVERE && level != Level.WARNING
                && level != Level.INFO) {
            throw new IllegalArgumentException("Illegal level: "
                    + level.getName());
        }
        this.severity = level;
        this.msg = msg;
    }
    
    /**
     * Run the check.
     * 
     * @param owner the ComplexType owning the attribute to check.
     * @param definition the definition to check the attribute against.
     * @param attribute the attribute to check.
     * @return null if ok, or an instance of {@link FailedCheck}if the check
     *         failed.
     */
    public final FailedCheck check(ComplexType owner, Type definition,
            Attribute attribute) {
        return innerCheck(owner, definition, attribute);
    }
    
    /** The method all subclasses should implement to do the actual checking.
     * 
     * @param owner the ComplexType owning the attribute to check.
     * @param definition the definition to check the attribute against.
     * @param attribute the attribute to check.
     * @return null if ok, or an instance of {@link FailedCheck}if the check
     *         failed.
     */
    public abstract FailedCheck innerCheck(ComplexType owner, Type definition,
            Attribute attribute);
    
    /** Get the default message to return if a check fails.
     * 
     * @return the default message to return if a check fails.
     */
    protected String getDefaultMessage() {
        return msg;
    }
    
    /** Objects of this class represents failed constraint checks.
     * 
     * @author John Erik Halse
     */
    public class FailedCheck {
        private final String msg;
        private final ComplexType owner;
        private final Type definition;
        private final Object value;
        
        /**
         * Construct a new FailedCheck object.
         * 
         * @param owner the ComplexType owning the attribute to check.
         * @param definition the definition to check the attribute against.
         * @param attribute the attribute to check.
         * @param msg a message describing what went wrong and possibly hints to
         *            the user on how to fix it.
         */
        public FailedCheck(ComplexType owner, Type definition, Attribute attr, String msg) {
            this.msg = msg;
            this.owner = owner;
            this.definition = definition;
            this.value = attr.getValue();
        }
        
        /**
         * Construct a new FailedCheck object using the constraints default
         * message.
         * 
         * @param owner the ComplexType owning the attribute to check.
         * @param definition the definition to check the attribute against.
         * @param attribute the attribute to check.
         */
        public FailedCheck(ComplexType owner, Type definition, Attribute attr) {
            this(owner, definition, attr, getDefaultMessage());
        }

        /** Get the error message.
         * 
         * @return the error message.
         */
        public String getMessage() {
            return msg;
        }
        
        /** Get the severity level.
         * 
         * @return the severity level.
         */
        public Level getLevel() {
            return severity;
        }
        
        /** Get the definition for the checked attribute.
         * 
         * @return the definition for the checked attribute.
         */
        public Type getDefinition() {
            return definition;
        }
        
        /** Get the value of the checked attribute.
         * 
         * @return the value of the checked attribute.
         */
        public Object getValue() {
            return value;
        }
        
        /** Get the {@link ComplexType} owning the checked attribute.
         * 
         * @return the {@link ComplexType} owning the checked attribute.
         */
        public ComplexType getOwner() {
            return owner;
        }

        /** Returns a human readeable string for the failed check.
         * Returns the same as {@link #getMessage()}
         * 
         * @param String  a human readeable string for the failed check.
         */
        public String toString() {
            return getMessage();
        }
    }
    
    /** Compare this constraints level to another constraint.
     * This method is implemented to let constraints be sorted with the highest
     * level first.
     * 
     * @param o a Constraint to compare to.
     */
    public int compareTo(Object o) {
        Constraint c = (Constraint) o;
        return c.severity.intValue() - severity.intValue();
    }
    
}
