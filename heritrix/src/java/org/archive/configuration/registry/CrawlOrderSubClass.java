/* $Id$
 *
 * Created Oct 12, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.configuration.registry;

import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;

/**
 * @author stack
 * @version $Date$ $Version$
 */
public class CrawlOrderSubClass extends CrawlOrder {

	public CrawlOrderSubClass(String n) {
		super(n);
		// TODO Auto-generated constructor stub
	}

    public synchronized Configuration getConfiguration()
    throws ConfigurationException {
        return new CrawlOrderSubClassConfiguration("Heritrix crawl order. " +
            "This forms the root of the settings framework.");
    }
    
    @SuppressWarnings("serial")
    protected static class CrawlOrderSubClassConfiguration
    extends CrawlOrderConfiguration {
        public CrawlOrderSubClassConfiguration(String description)
        throws ConfigurationException {
            super(description);
        }
        
        protected void initialize()
        throws AttributeNotFoundException, InvalidAttributeValueException,
                MBeanException, ReflectionException {
            
            setAttribute(new Attribute("XXX",
                new String[] {ATTR_SETTINGS_DIRECTORY}));
        }
        
        @Override
        protected List<OpenMBeanAttributeInfo> addAttributeInfos(
                List<OpenMBeanAttributeInfo> infos)
        throws OpenDataException {
            infos = super.addAttributeInfos(infos);
            infos.add(new OpenMBeanAttributeInfoSupport(
                "XXX",
                "Directory where override settings are kept. The settings " +
                "for many modules can be overridden based on the domain or " +
                "subdomain of the URI being processed. This setting specifies" +
                " a file level directory to store those settings. The path" +
                " is relative to 'disk-path' unless" +
                " an absolute path is provided.",
                SimpleType.STRING, true, true, false, "settings"));
            return infos;
        }
    }
}