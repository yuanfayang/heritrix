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
        * @param link
        */
       public void putLink(String link){
           links.add(link);
       }


    /**
     * Overrides super classe's method to force
     * the use of IATagParser's ButtonActionWriter
     * class.
     * @param id
     * @param trackAsMenu
     * @param buttonRecord2s
     * @return An SWFActions.
     * @throws IOException
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
