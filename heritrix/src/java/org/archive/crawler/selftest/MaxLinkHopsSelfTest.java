/* MaxLinkHopsSelfTest
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

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.basic.Scope;


/**
 * Test the max-link-hops setting.
 * 
 * @author stack
 * @version $Id$
 */
public class MaxLinkHopsSelfTest
    extends SelfTestCase
{
    /**
     * Assumption is that the setting for max-link-hops is less than this
     * number.
     */
    private static final int MAXLINKHOPS = 5;


    /**
     * Files to find as an array.
     */
    private static final String [] FILES_TO_FIND_AS_ARRAY =
        {"1.html", "2.html", "3.html", "4.html", "5.html"};

    /**
     * Files to find as a list.
     */
    private static final List FILES_TO_FIND =
        Arrays.asList(FILES_TO_FIND_AS_ARRAY);

    /**
     * File not to find.
     */
    private static final String FILE_NOT_TO_FIND = "6.html";


    /**
     * Test the max-link-hops setting is being respected.
     */
    public void testMaxLinkHops()
        throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        Scope scope =
           (Scope)getCrawlJob().getSettingsHandler().getModule(Scope.ATTR_NAME);
        int maxLinkHops =
            ((Integer)scope.getAttribute(Scope.ATTR_MAX_LINK_HOPS)).intValue();
        assertTrue("max-link-hops incorrect", MAXLINKHOPS == maxLinkHops);

        // Make sure file we're NOT supposed to find is actually on disk.
        assertTrue("File present on disk", fileExists(FILE_NOT_TO_FIND));

        // Ok.  The file not to find exists.  Lets see if it made it into arc.
        testFilesInArc(FILES_TO_FIND);
    }
}

