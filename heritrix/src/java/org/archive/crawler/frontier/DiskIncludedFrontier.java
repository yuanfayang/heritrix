/* Frontier
 *
 * $Id$
 *
 * Created on 24.5.2004
 *
 * Copyright (C) 2004 National and University Library of Iceland.
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
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.FPUURISet;
import org.archive.util.CachingDiskLongFPSet;

/**
 * Overrides org.archive.crawler.frontier.Frontier to use disk based
 * already included structures.
 *
 * The underlying CachingDiskLongFPSet class has received little
 * testing and may be very slow. Each million slots (of the cache
 * or disk-based set) will use about 72MB of space.
 *
 * @author Kristinn Sigurdsson
 */
public class DiskIncludedFrontier extends Frontier {

    /** The size of the already included's in memory cache. The cache
     *  capacity will be 2 to the power of this factor.
     */
    public final static String ATTR_INCLUDED_URIS_CACHE_EXPONENT =
        "included-uris-cache-exponent";

    /** The inital size of the already included's disk structure. It's initial
     *  capacity will be 2 to the power of this factor.
     */
    public final static String ATTR_INCLUDED_URIS_INITFILE_EXPONENT =
        "included-uris-initfile-exponent";

    public final static String ATTR_INCLUDED_URIS_CACHE_LOADFACTOR =
        "included-uris-cache-loadfactor";

    public final static String ATTR_INCLUDED_URIS_INITFILE_LOADFACTOR =
        "included-uris-initfile-loadfactor";

    protected final static Integer DEFAULT_INCLUDED_URIS_CACHE_EXPONENT =
        new Integer(23); // 8 million slots
    protected final static Integer DEFAULT_INCLUDED_URIS_INITFILE_EXPONENT =
        new Integer(25); // 32 million slots
    protected final static Float DEFAULT_INCLUDED_URIS_CACHE_LOADFACTOR =
        new Float(0.75f);
    protected final static Float DEFAULT_INCLUDED_URIS_INITFILE_LOADFACTOR =
        new Float(0.75f);

    public DiskIncludedFrontier(String name) {
        super(name,"Frontier. \nMaintains the internal" +
                " state of the crawl. It dictates the order in which URIs" +
                " will be scheduled. \nThis frontier is mostly a breadth-first"+
                " frontier, which refrains from emitting more than one" +
                " CrawlURI of the same \'key\' (host) at once, and respects" +
                " minimum-delay and delay-factor specifications for" +
                " politeness.\nThis Frontier also uses a data structure that" +
                " relies on writing data to disk for maintaining its list of" +
                " already encountered URLs. This reduces memory use (that" +
                " would otherwise be unlimited over time in large crawls)" +
                " at the expense of performance.");

        Type t;
        t = addElementToDefinition(
                new SimpleType(ATTR_INCLUDED_URIS_CACHE_EXPONENT,
                        "The size of the already included URIs list" +
                        "in-memory cache. " +
                        "The cache capacity will be 2 to the power of this " +
                        "factor. So a value of 23 equals just over 8 million " +
                        "'slots'. Each million slots require about 9MB of RAM.",
                        DEFAULT_INCLUDED_URIS_CACHE_EXPONENT));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(
                new SimpleType(ATTR_INCLUDED_URIS_CACHE_LOADFACTOR,
                        "Load factor of the already included URIs list's in " +
                        "memory cache.",
                        DEFAULT_INCLUDED_URIS_CACHE_LOADFACTOR));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(
                new SimpleType(ATTR_INCLUDED_URIS_INITFILE_EXPONENT,
                        "The inital size of the already included URIs list " +
                        "backing disk structure." +
                        "Its initial capacity will be 2 to the " +
                        "power of this factor. So a value of 25 equals " +
                        "about 32 million 'slots', taking up about 288MB on" +
                        "disk.",
                        DEFAULT_INCLUDED_URIS_INITFILE_EXPONENT));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(
                new SimpleType(ATTR_INCLUDED_URIS_INITFILE_LOADFACTOR,
                        "Load factor of the already included URIs list's disk " +
                        "structure.",
                        DEFAULT_INCLUDED_URIS_INITFILE_LOADFACTOR));
        t.setExpertSetting(true);
        t.setOverrideable(false);
    }


    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.Frontier#createAlreadyIncluded(java.io.File, java.lang.String)
     */
    protected UURISet createAlreadyIncluded(File dir, String filePrefix)
            throws IOException, FatalConfigurationException {
        try {
			return new FPUURISet(
			                 new CachingDiskLongFPSet(
			                         dir,
			                         filePrefix,
			                         ((Integer)getAttribute(
			                                ATTR_INCLUDED_URIS_INITFILE_EXPONENT))
			                                .intValue(),
                                     ((Float)getAttribute(
                                            ATTR_INCLUDED_URIS_INITFILE_LOADFACTOR))
                                            .floatValue(),
                                     ((Integer)getAttribute(
                                            ATTR_INCLUDED_URIS_CACHE_EXPONENT))
                                            .intValue(),
                                     ((Float)getAttribute(
                                            ATTR_INCLUDED_URIS_CACHE_LOADFACTOR ))
                                            .floatValue()));
		} catch (AttributeNotFoundException e) {
            throw new FatalConfigurationException("AttributeNotFoundException " +
                    "encountered on reading creating CachingDiskLongFPSet. " +
                    "Message:\n" + e.getMessage());
		} catch (MBeanException e) {
            throw new FatalConfigurationException("AttributeNotFoundException " +
                    "encountered on reading creating CachingDiskLongFPSet. " +
                    "Message:\n" + e.getMessage());
		} catch (ReflectionException e) {
            throw new FatalConfigurationException("AttributeNotFoundException " +
                    "encountered on reading creating CachingDiskLongFPSet. " +
                    "Message:\n" + e.getMessage());
		}
    }
}
