/* EnhancedCharSequence
 *
 * $Id$
 *
 * Created on Mar 17, 2004
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
package org.archive.io;


/**
 * Marker interface for CharSequences which may depend on unstable backing
 * (eg a large buffer that could go away or be reused). 
 * 
 * Creators of UnstableCharSequences should clearly document their valid
 * lifetime, and users of such CharSequences during their valid lifetime
 * should be careful to only pass stabilized copies (or subsequences) to
 * code that may run outside the documented lifetime. 
 * 
 * It is not intended that the backing go away at any arbitrary time,
 * nor that isUnstable() be checked before each use. Rather, you would
 * only check a sequence's stability while you know (by design contract)
 * it is stable, when considering using (a form of) it in future operation
 * when the backing's existence can no longer be assured. 
 *
 * @author gojomo
 * @see java.lang.CharSequence
 */
public interface UnstableCharSequence extends CharSequence {

    /**
     * @return whether or not charsequence is actually unstable
     */
    public boolean isUnstable();
    
    /**
     * @return CharSequence that is definitely stable
     */
    public CharSequence stabilize();
}
