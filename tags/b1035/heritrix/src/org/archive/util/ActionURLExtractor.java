/*
 * Created on Jul 9, 2003
 *
 */
package org.archive.util;



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
