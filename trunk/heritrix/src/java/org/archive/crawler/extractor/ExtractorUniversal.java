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
 * Created on Jan 15, 2004
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.TextUtils;

/**
 * A last ditch extractor that will look at the raw byte code and try to extract
 * anything that <i>looks</i> like a link. If used, it should always be specified
 * as the last link extractor in the order file.
 * <p>
 * To accomplish this we will scan through the bytecode and try and build up 
 * strings of consecutive bytes that all represent characters that are valid
 * in a URL. Once we hit the end of such a string (i.e. find a character that
 * should not be in a URL) we will try and determine if we have a URL using
 * a regular expression (assuming the string is long enough).  If the regular
 * expression matches we will consider the string a URL.
 * 
 * @author Kristinn Sigurdsson
 */
public class ExtractorUniversal extends Processor implements CoreAttributeConstants{

    private static String XP_MAX_DEPTH_BYTES = "@max-depth-bytes";
    /** Default value for how far into an unknown document we should scan - 10k*/
    private static long DEFAULT_MAX_DEPTH_BYTES = 10240;

    /**
     * A pattern to determine if a string might be a URL.
     * 
     * Has an internal dot or some slash, begins and ends with either '/' or a word-char
     * Finds plenty of false postitives.
     */
    static final Pattern LIKELY_URL_EXTRACTOR = Pattern.compile(
        "(\\w|/)[\\S&&[^<>]]*(\\.|/)[\\S&&[^<>]]*(\\w|/)"); //TODO: IMPROVE THIS    

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted= 0;

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {
        
        if(curi.hasBeenLinkExtracted()){
            //Some other extractor already handled this one. We'll pass on it.
            return;
        }
        
        if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
            // We only handle HTTP at the moment.
            return;
        }
        numberOfCURIsHandled++;
        
        GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);

        try{
            InputStream instream = get.getHttpRecorder().getRecordedInput().getContentReplayInputStream();
            int ch = instream.read();
            StringBuffer lookat = new StringBuffer();
            long counter = 0;
            long maxdepth = getLongAt(XP_MAX_DEPTH_BYTES, DEFAULT_MAX_DEPTH_BYTES);
            while(ch != -1 && counter++ <= maxdepth){   
                if(isURLableChar(ch))
                {
                    //Add to buffer.
                    lookat.append((char)ch);
                } else if(lookat.length() > 3) {
                    // It takes a bare mininum of 4 characters to form a URL
                    // Since we have at least that many let's try link extraction.
                    Matcher uri = TextUtils.getMatcher(LIKELY_URL_EXTRACTOR, lookat.toString());
                    if(uri.matches())
                    {
                        // Looks like we found something.
                        numberOfLinksExtracted++;
                        curi.addSpeculativeEmbed(lookat.toString());
                    }
                    // Reset lookat for next string.
                    lookat = new StringBuffer();
                } else if(lookat.length()>0) {
                    // Didn't get enough chars. Reset lookat for next string.
                    lookat = new StringBuffer();
                }
                ch = instream.read();
            }
        } catch(IOException e){
            //TODO: Handle this exception.
            e.printStackTrace();
        }

    }

    /**
     * Determines if a char (as represented by an int in the range of 0-255) is
     * a character (in the Ansi character set) that can be present in a URL. 
     * This method takes a STRICT approach to what characters can be in a URL.
     * <p>
     * The following are considered to be 'URLable'<br>
     * <ul>
     *  <li> # $ % & + , - . / values 35-38,43-47
     *  <li> [0-9] values 48-57
     *  <li> : ; = ? @ value 58-59,61,63-64
     *  <li> [A-Z] : values 65-90 
     *  <li> [a-z] : values 97-122
     *  <li> ~ : value 126
     * </ul>
     * <p>
     * To summerize, the following ranges are considered URLable:<br>
     * 35-38,43-59,61,63-90,97-122,126
     * 
     * @param ch The character (represented by an int) to test.
     * @return True if it is a printable character, false otherwise.
     */
    private boolean isURLableChar(int ch) {
        return (ch>=35 && ch<=38) 
            || (ch>=43 && ch<=59) 
            || (ch==61) 
            || (ch>=63 && ch<=90)
            || (ch>=97 && ch<=122)
            || (ch==126);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorUniversal\n");
        ret.append("  Function:          Link extraction on unknown file types.\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        
        return ret.toString();
    }
}
