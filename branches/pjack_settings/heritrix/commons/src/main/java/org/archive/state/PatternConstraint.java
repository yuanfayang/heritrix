/* Copyright (C) 2006 Internet Archive.
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
 *
 * ClassicScope.java
 * Created on Nov 29, 2006
 *
 * $Header$
 */
package org.archive.state;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;


/**
 * Only allows strings that match a regular expression.
 * 
 * @author pjack
 */
public class PatternConstraint implements Constraint<String> {

    /**
     * For serialization. 
     */
    private static final long serialVersionUID = 1L;

    
    /**
     * The pattern that string values must match to pass this constraint.
     */
    final private Pattern pattern;

    
    /**
     * Constructor.
     * 
     * @param p   the pattern that string values must match to pass this
     *     constraint.
     */
    public PatternConstraint(Pattern p) {
        if (p == null) {
            throw new IllegalArgumentException("Pattern may not be null.");
        }
        this.pattern = p;
    }
    

    /**
     * Returns true if the given string matches this constraint's pattern.
     * 
     * @return  true if the given string matches this constraint's pattern
     */
    public boolean allowed(String s) {
        return pattern.matcher(StringUtils.defaultString(s)).matches();
    }
}
