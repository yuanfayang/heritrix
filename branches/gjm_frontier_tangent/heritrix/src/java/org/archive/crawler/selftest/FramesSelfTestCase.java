/* FramesSelfTest
 *
 * Created on Feb 6, 2004
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
 * Test crawler can parse pages w/ frames in them.
 *
 * @author stack
 * @version $Id$
 */
public class FramesSelfTestCase extends SelfTestCase
{
    /**
     * Files we expect to find in the archive as an array.
     */
    private static final String [] FILES_AS_ARRAY = { "topframe.html",
            "leftframe.html", "rightframe.html",
            "noframe.html", "index.html"};

    /**
     * Files we expect to find in the archive as a list.
     */
    private static final List FILES = Arrays.asList(FILES_AS_ARRAY);


    /**
     * Verify that all frames and their contents are found by the crawler.
     *
     */
    public void testFrames()
    {
        testFilesInArc(FILES);
    }
}