/* BackgroundImageExtractionSelfTest
 * 
 * Created on Jan 29, 2004
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
import java.util.Iterator;
import java.util.List;

import org.archive.io.arc.ARCRecordMetaData;

/**
 * Test the crawler can find background images in pages.
 * 
 * @author stack
 * @version $Id$
 */
public class BackgroundImageExtractionSelfTestCase
    extends SelfTestCase
{   
    /**
     * The name of the background image the crawler is supposed to find.
     */
    private static final String IMAGE_NAME = "example-background-image.jpeg";
    private static final String JPEG = "image/jpeg";

    /**
     * Read ARC file for the background image the file that contained it.
     * 
     * Look that there is only one instance of the background image in the 
     * ARC and that it is of the same size as the image in the webapp dir.
     */
    public void testBackgroundImageExtraction()
    {
        String relativePath = getTestName() + '/' + IMAGE_NAME;
        String url = getSelftestURLWithTrailingSlash() + relativePath;
        File image = new File(getHtdocs(), relativePath);
        assertTrue("Image exists", image.exists());
        List metaDatas = getReadReader().getMetaDatas();
        boolean found = false;
        ARCRecordMetaData metaData = null;
        for (Iterator i = metaDatas.iterator(); i.hasNext();)
        {
            metaData = (ARCRecordMetaData)i.next();
            String localURL = metaData.getUrl();
            if (metaData.getUrl().equals(url) &&
                metaData.getMimetype().equalsIgnoreCase(JPEG))
            {          
                if (!found)
                {
                    found = true;
                }
                else
                {
                    fail("Found a 2nd instance of " + url);
                }
            }
        }
        assertTrue("No " + url + " in " + getArcFile().getAbsolutePath(), 
            found);
    }
}
