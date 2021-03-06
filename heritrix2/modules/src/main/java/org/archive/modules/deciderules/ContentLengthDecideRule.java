/* $Id$
 * 
 * Created on 28.8.2006
 *
 * Copyright (C) 2006 Olaf Freyer
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

import org.archive.modules.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;

public class ContentLengthDecideRule extends DecideRule {

    private static final long serialVersionUID = 3L;


    /**
     * Content-length threshold.  The rule returns ACCEPT if the content-length
     * is less than this threshold, or REJECT otherwise.  The default is
     * 2^63, meaning any document will be accepted.
     */
    final public static Key<Long> CONTENT_LENGTH_THRESHOLD = 
        Key.make(Long.MAX_VALUE);

    
    static {
        KeyManager.addKeys(ContentLengthDecideRule.class);
    }

    /**
     * Usual constructor. 
     */
    public ContentLengthDecideRule() {
    }
    
    
    protected DecideResult innerDecide(ProcessorURI uri) {
        if (uri.getContentLength() < uri.get(this, CONTENT_LENGTH_THRESHOLD)) {
            return DecideResult.ACCEPT;
        }
        return DecideResult.REJECT;
    }

}