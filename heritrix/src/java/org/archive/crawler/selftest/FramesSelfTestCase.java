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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.archive.io.arc.ARCRecordMetaData;

/**
 * Test crawler can parse pages w/ frames in them.
 * 
 * @author stack
 * @version $Id$
 */
public class FramesSelfTestCase extends SelfTestCase
{
    /**
     * Files we expect to find in the archive.
     */
    private static final String [] FILES = { "topframe.html",
            "leftframe.html", "rightframe.html",
            "noframe.html", "index.html"};
    
    
    public void testFramesSelfTest()
    {
        String frameDirURL = getSelftestURLWithTrailingSlash() + getTestName();
        File frameDir = new File(getHtdocs(), getTestName());
        Map files = new HashMap(FILES.length);
        // If file exits on disk, set it into our map w/ a FALSE flag.
        for (int i = 0; i < FILES.length; i++)
        {
            File fileOnDisk = new File(frameDir, FILES[i]);
            if (fileOnDisk.exists())
            {
                files.put(FILES[i], Boolean.FALSE);
            }
        }
        assertTrue("Files are present on disk", files.size() == FILES.length);

        // Now iterate through the ARCRecord meta data and every time I find
        // a map entry, set the flag for that entry to TRUE.
        List metaDatas = getReadReader().getMetaDatas();
        ARCRecordMetaData metaData = null;
        for (Iterator i = metaDatas.iterator(); i.hasNext();)
        {
            metaData = (ARCRecordMetaData)i.next();
            String url = metaData.getUrl();
            if (url.startsWith(frameDirURL))
            {
                String name = url.substring(url.lastIndexOf("/") + 1);
                if (files.containsKey(name)
                    && metaData.getMimetype().equalsIgnoreCase("text/html"))
                {
                    files.put(name, Boolean.TRUE);
                }
            }
        }
        
        // Now make sure all files have been found in the arc.
        String notInARC = null;
        for (Iterator i = files.keySet().iterator(); i.hasNext();)
        {   
            String key = (String)i.next();
            if (!((Boolean)files.get(key)).booleanValue())
            {
                notInARC = key;
                break;
            }
        }
        assertNull("File was not found in arc: " + notInARC, notInARC);
    }
}
