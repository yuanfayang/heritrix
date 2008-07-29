/* $Id:  $
 *
 * Copyright (C) 2007 Olaf Freyer
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

import java.util.logging.Logger;

import org.archive.modules.ProcessorURI;
import org.archive.net.PublicSuffixes;
import org.archive.net.UURI;

/**
 * Applies its decision if the current URI differs in that portion of
 * its hostname/domain that is assigned/sold by registrars (AKA its
 * 'topmost assigned SURT' or 'public suffix'.)
 * 
 * @author Olaf Freyer
 */
public class IsCrossTopmostAssignedSurtHopDecideRule extends PredicatedDecideRule {
    private static final long serialVersionUID = 1L;
    
    private static final Logger LOGGER = Logger
            .getLogger(IsCrossTopmostAssignedSurtHopDecideRule.class.getName());

    public IsCrossTopmostAssignedSurtHopDecideRule() {
    }

    protected boolean evaluate(ProcessorURI uri) {
        UURI via = uri.getVia();
        if (via == null) {
            return false;
        }
        try {
            // determine if this hop crosses domain borders
            String myTopmostAssignedSurt = getTopmostAssignedSurt(uri.getUURI());
            String viaTopmostAssignetSurt = getTopmostAssignedSurt(via);
            if (myTopmostAssignedSurt != null && viaTopmostAssignetSurt != null
                    && !myTopmostAssignedSurt.equals(viaTopmostAssignetSurt)) {
                LOGGER.fine("rule matched for \"" + myTopmostAssignedSurt+"\" vs. \""+viaTopmostAssignetSurt+"\"");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Return false since we could not get hostname or something else
            // went wrong
        }
        return false;
    }
    
    private String getTopmostAssignedSurt(UURI uuri){
        String surt = uuri.getSurtForm().replaceFirst(".*://\\((.*?)\\).*", "$1");
        return PublicSuffixes.reduceSurtToTopmostAssigned(surt);
        
    }

    @Override
    protected DecideResult getDefaultDecision(ProcessorURI uri) {
        return DecideResult.REJECT;
    }

}
