/* Copyright (C) 2003 Internet Archive.
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
 *
 * Created on Jul 9, 2003
 *
 */
package org.archive.crawler.extractor;



import java.io.IOException;
import java.util.ArrayList;


import com.anotherbigidea.flash.writers.ActionWriter;

/**
 * Extend the ActionWriter class (javaswf) for the purpose
 * of hijacking URL processing, so we can store them back
 * to CrawlURIs.
 * 
 * @author Parker Thompson
 */
public class ActionURLExtractor extends ActionWriter {

	protected ArrayList links = new ArrayList();
	protected IATagParser tagWriter;

	/** After an swf has been processed this function can be
	 *  used to get a list of URIs found in the swf.
	 * 
	 * @return linksArrayList
	 */
	public ArrayList getLinks(){
		return links;
	}
	
	/** See superclass definition.
	 * @param tagWriter
	 * @param flashVersion
	 */
	public ActionURLExtractor(IATagParser tagWriter, int flashVersion) {
		super(tagWriter, flashVersion);
		
		this.tagWriter = tagWriter;
	}
	
	/**
	 * Extract URIs from the swf and store them.
	 */
	public void getURL( String url, String target ) throws IOException
	{
		// report uri back to tag parser
		tagWriter.putLink(url);
		links.add(url);
			
	}
	
	// override to prevent test from actually being written
	protected void writeBytes( byte[] bytes ) throws IOException
	{
		// do a whole lotta nothin'                       
	}

}
