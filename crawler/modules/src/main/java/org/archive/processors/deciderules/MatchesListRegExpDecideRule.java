/* MatchesListRegExpDecideRule
 * 
 * $Id$
 * 
 * Created on 30.5.2005
 *
 * Copyright (C) 2005 Kristinn Sigurdsson
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
package org.archive.processors.deciderules;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.archive.processors.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;


/**
 * Rule applies configured decision to any CrawlURIs whose String URI
 * matches the supplied regexps.
 * <p>
 * The list of regular expressions can be considered logically AND or OR.
 *
 * @author Kristinn Sigurdsson
 * 
 * @see MatchesRegExpDecideRule
 */
public class MatchesListRegExpDecideRule extends PredicatedAcceptDecideRule {

    
    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(MatchesListRegExpDecideRule.class.getName());

    
    /**
     * The list of regular expressions to evalute against the URI.
     */
    final public static Key<List<Pattern>> REGEXP_LIST = Key.makeList(Pattern.class);


    /**
     * True if the list of regular expression should be considered as logically
     * AND when matching. False if the list of regular expressions should be
     * considered as logically OR when matching.
     */
    final public static Key<Boolean> LIST_LOGIC = Key.make(true);
    
    static {
        KeyManager.addKeys(MatchesListRegExpDecideRule.class);
    }

    /**
     * Usual constructor. 
     */
    public MatchesListRegExpDecideRule() {
    }

    /**
     * Evaluate whether given object's string version
     * matches configured regexps
     */
    @Override
    protected boolean evaluate(ProcessorURI uri) {
        List<Pattern> regexps = uri.get(this, REGEXP_LIST);
        if(regexps.size()==0){
            return false;
        }

        String str = uri.toString();
        boolean listLogicOR = uri.get(this, LIST_LOGIC);

        for (Pattern p: regexps) {
            boolean matches = p.matcher(str).matches();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Tested '" + str + "' match with regex '" +
                    p.pattern() + " and result was " + matches);
            }
            
            if(matches){
                if(listLogicOR){
                    // OR based and we just got a match, done!
                    logger.fine("Matched: " + str);
                    return true;
                }
            } else {
                if(listLogicOR == false){
                    // AND based and we just found a non-match, done!
                    return false;
                }
            }
        }
        
        if (listLogicOR) {
            return false;
        } else {
            return true;
        }
    }
    
}