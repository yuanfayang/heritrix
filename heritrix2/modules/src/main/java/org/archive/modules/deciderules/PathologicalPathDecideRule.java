/* PathologicalPathDecideRule
*
* $Id$
*
* Created on Apr 1, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.modules.deciderules;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.archive.modules.ProcessorURI;
import org.archive.settings.KeyChangeEvent;
import org.archive.settings.KeyChangeListener;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyManager;



/**
 * Rule REJECTs any URI which contains an excessive number of identical, 
 * consecutive path-segments (eg http://example.com/a/a/a/boo.html == 3 '/a' 
 * segments)
 *
 * @author gojomo
 */
public class PathologicalPathDecideRule extends DecideRule 
implements KeyChangeListener {

    private static final long serialVersionUID = 3L;


    /**
     * Number of times the pattern should be allowed to occur. This rule returns
     * its decision (usually REJECT) if a path-segment is repeated more than
     * number of times.
     */
    @Global
    final public static Key<Integer> MAX_REPETITIONS = Key.make(2);


    private AtomicReference<Pattern> pattern = new AtomicReference<Pattern>();

    static {
        KeyManager.addKeys(PathologicalPathDecideRule.class);
    }
    
    /** Constructs a new PathologicalPathFilter.
     *
     * @param name the name of the filter.
     */
    public PathologicalPathDecideRule() {
    }


    @Override
    protected DecideResult innerDecide(ProcessorURI uri) {
        int maxRep = uri.get(this, MAX_REPETITIONS);
        Pattern p = getPattern(maxRep);
        if (p.matcher(uri.getUURI().toString()).matches()) {
            return DecideResult.REJECT;
        } else {
            return DecideResult.PASS;
        }
    }

    /** 
     * Construct the regexp string to be matched against the URI.
     * @param o an object to extract a URI from.
     * @return the regexp pattern.
     */
    private Pattern getPattern(int maxRep) {
        // race no concern: assignment is atomic, happy with any last value
        Pattern p = pattern.get();
        if (p != null) {
            return p;
        }
        String regex = constructRegexp(maxRep);
        p = Pattern.compile(regex);
        pattern.set(p);
        return p;
    }
    
    protected String constructRegexp(int rep) {
        return (rep == 0) ? null : ".*?/(.*?/)\\1{" + rep + ",}.*";
    }
    
    
    /**
     * Repetitions may have changed; refresh constructedRegexp
     */
    public void keyChanged(KeyChangeEvent event) {
        pattern.set(null);
    }
}
