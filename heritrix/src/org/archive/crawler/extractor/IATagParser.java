/*
 * Created on Jul 9, 2003
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import com.anotherbigidea.flash.interfaces.SWFActions;
import com.anotherbigidea.flash.interfaces.SWFTags;
import com.anotherbigidea.flash.writers.TagWriter;

/**
 * Overrides the javaswf TagWriter class so that URIs
 * can more easily be extracted during swf parsing.
 * 
 * @author Parker Thompson
 */
public class IATagParser extends TagWriter {

	ArrayList links = new ArrayList();
	
	/**
	 * @param tags
	 */
	public IATagParser(SWFTags tags) {
		super(tags);
	}
	
	/**
	 * Overrides super method so ActionURLExtractors are
	 * created instead of ActionWriter instances
	 */
	protected SWFActions factorySWFActions()
	{
		return new ActionURLExtractor( this, version );
	}

	/** Return a list of all links (ArrayList of strings) that action parser's
	 *  this object has created has told us about (requires they report correctly)
	 * @return links
	 */
   	public ArrayList getLinks(){
   		return links;
   	}
   	
   	/** Store links to other documents that are enountered in 
   	 *  the course of parsing.
   	 */
   	public void putLink(String link){
   		links.add(link);
   	}
	
	
	/**
	 * Overrides super classe's method to force 
	 * the use of IATagParser's ButtonActionWriter 
	 * class.
	 */
	public SWFActions tagDefineButton2( int id, 
										boolean trackAsMenu, 
										Vector buttonRecord2s )
		throws IOException
	{
		startTag( TAG_DEFINEBUTTON2, id, true ); 
		out.writeUI8( trackAsMenu ? 1 : 0 );

		return new ButtonActionWriter( this, version, buttonRecord2s );
	}
	
	// override inner class so the created super() is an ActionURLExtractor
	protected static class ButtonActionWriter extends ActionURLExtractor
	{   
		public ButtonActionWriter( TagWriter tagWriter, int flashVersion, Vector buttonRecs )
			throws IOException 
		{
			super( (IATagParser)tagWriter, flashVersion );
		}
	}
}
