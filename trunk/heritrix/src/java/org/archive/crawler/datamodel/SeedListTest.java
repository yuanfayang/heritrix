/* SeedListTest
 * 
 * Created on Apr 30, 2004
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
package org.archive.crawler.datamodel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.util.TmpDirTestCase;


/**
 * Test {@link SeedList}.
 * @author stack
 * @version $Revision$, $Date$
 */
public class SeedListTest extends TmpDirTestCase {
    
    private static final Logger logger =
        Logger.getLogger(SeedListTest.class.getName());
   
    private static Set seeds = null;
    
    /**
     * Comparator for treeset of uuris.
     */
    private static final Comparator CMP = new Comparator () {
        public int compare(Object o1, Object o2) {
            int result = -1;
            if (o1 == null && o1 == null){
                result = 0;
            } else if (o1 == null) {
                result = -1;
            } else if (o2 == null) {
                result = 1;
            } else {
                String s1 = ((UURI)o1).toString();
                String s2 = ((UURI)o2).toString();
                result = s1.compareTo(s2);
                result = (result < 0)? result = -1:
                    (result > 0)? result = 1: 0;
            }
            return result;
        }
    };

    
    /**
     * Seed file reference.
     */
    private File seedsfile;
    
    
    /* (non-Javadoc)
     * @see org.archive.util.TmpDirTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        // First create array of seeds and add to treeset.
        SeedListTest.seeds = new TreeSet(SeedListTest.CMP);
        UURI [] tmp = {
            new UURI("http://www.google.com"),
            new UURI("https://www.google.com"),
            new UURI("gopher://www.google.com"),
            new UURI("news://www.google.com"),
            new UURI("rss://www.google.com"),
            new UURI("telnet://www.google.com"),
            new UURI("ftp://myname@example.com/etc/motd"),
            new UURI("ftp://example.com/etc/motd2")
        };
        SeedListTest.seeds.addAll(Arrays.asList(tmp));
            
        // Write a seeds file w/ our list of seeds.
        this.seedsfile = new File(getTmpDir(),
            SeedListTest.class.getName() + ".seedfile");
        PrintWriter writer = new PrintWriter(new FileWriter(this.seedsfile));
        for (Iterator i = seeds.iterator(); i.hasNext();) {
            writer.println(((UURI)i.next()).toString());
        }
        writer.close();
    }
    
    
    /* (non-Javadoc)
     * @see org.archive.util.TmpDirTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        if (this.seedsfile.exists()) {
             this.seedsfile.delete();
        }
    }
    
    public void testNoCaching() throws URIException {
        coreTest(false);
    }
    
    public void testCaching() throws URIException {
        coreTest(true);
    }
    
    public void testNoScheme() throws IOException {
        final String NOSCHEME = "x.y.z";
        FileWriter fw = new FileWriter(this.seedsfile, true);
        // Write to new (last) line the URL.
        fw.write("\n");
        fw.write(NOSCHEME);
        fw.flush();
        fw.close();
        boolean found = false;
        SeedList sl = new SeedList(this.seedsfile, SeedListTest.logger, false);
        for (Iterator i = sl.iterator(); i.hasNext();) {
            UURI uuri = (UURI)i.next();
            if (uuri.getHost().equals(NOSCHEME)) {
                found = true;
                break;
            }
        }
        assertTrue("Did not find " + NOSCHEME, found);
    }

    public void coreTest(boolean caching) throws URIException {
        // First make sure that I can the seed set from seed file.
        SeedList sl = checkContent(SeedListTest.seeds, caching);
        // Now do add and see if get set matches seed file content.
        final UURI uuri = new UURI("http://one.two.three");
        sl.add(uuri);
        Set set = new TreeSet(SeedListTest.CMP);
        set.addAll(SeedListTest.seeds);
        set.add(uuri);
        checkContent(sl, set, caching);
    }
    
    private SeedList checkContent(Set seedSet, boolean caching) {
        return checkContent(null, seedSet, caching);
    }
    
    private SeedList checkContent(SeedList sl, Set seedSet, boolean caching) {
        if (sl == null) {
            sl = new SeedList(this.seedsfile, SeedListTest.logger, caching);
        }
        int count = 0;
        for (Iterator i = sl.iterator(); i.hasNext();) {
            count++;
            UURI uuri = (UURI)i.next();
            assertTrue("Does not contain: " + uuri.toString(),
                seedSet.contains(uuri));
        }
        assertTrue("Different sizes: " + count + ", " + seedSet.size(),
            count == seedSet.size());
        return sl;
    }
}
