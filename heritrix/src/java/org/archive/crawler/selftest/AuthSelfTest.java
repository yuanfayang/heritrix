/* AuthSelfTest
 *
 * Created on Feb 17, 2004
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
package org.archive.crawler.selftest;

import java.util.Arrays;
import java.util.List;


/**
 * Test authentications, both basic/digest auth and html form logins.
 *
 * @author stack
 * @version $Id$
 */
public class AuthSelfTest
    extends SelfTestCase
{
    /**
     * Files to find as a list.
     */
    private static final List FILES_TO_FIND =
        Arrays.asList(new String[] {"basic-loggedin.html"});


    /**
     * Test the max-link-hops setting is being respected.
     */
    public void testAuth() {
        // Ok.  The file not to find exists.  Lets see if it made it into arc.
         List foundFiles = filesFoundInArc();
         assertTrue("All found", foundFiles.containsAll(FILES_TO_FIND));
         // TODO: Make this like other selftests that look also on disk. Means
         // cneed to change the filesFoundInArc method without breaking other
         // selftests.
    }
}

