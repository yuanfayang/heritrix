/* Controller
 * 
 * $Id$
 * 
 * Created on Dec 19, 2003
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
package org.archive.crawler.datamodel.settings;

/**
 * 
 * @author John Erik Halse
 *
 */
public class Controller extends CrawlerModule {

	/**
	 * @param parent
	 * @param name
	 * @param description
	 */
	public Controller() {
		super("controller", "Heritrix crawl controller");
        
        addElementToDefinition(new SimpleType("settings-directory", "Directory where per host settings are kept", "settings"));
        addElementToDefinition(new SimpleType("foo", "Just for testing", "bar"));
        addElementToDefinition(new SimpleType("int1", "Just for testing", new Integer(1)));
        addElementToDefinition(new IntegerList("list", "List Test", new int[] {10, 20}));
        
        MapType procMap = (MapType) addElementToDefinition(new MapType("Processors", "URI processors"));
        procMap.addElementToDefinition(new CrawlerModule("Processor1", "URI processors"));
        procMap.addElementToDefinition(new SimpleType("firstProcessor", "First processor to run URI trough", "Processor1"));
        
        addElementToDefinition(new CrawlerModule("Frontier", "Frontier description"));
	}
}
