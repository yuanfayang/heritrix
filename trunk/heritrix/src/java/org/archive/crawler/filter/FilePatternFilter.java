/* 
 * PathSuffixFilter
 * 
 * $Id$
 * 
 * Created on Mar 11, 2004
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

package org.archive.crawler.filter;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.settings.SimpleType;

/**
 * Compares suffix of a passed CrawlURI, UURI, or String against a regular 
 * expression pattern accepting matches. 
 * * 
 * @author Igor Ranitovic
 */
public class FilePatternFilter extends URIRegExpFilter {
    public static final String ATTR_USE_DEFAULT = "use-default-patterns";
    public static final String IMAGES_PATTERNS = ".*(?i)(\\.(bmp|gif|jpe?g" +
        "|png|tiff?))$";
    public static final String AUDIO_PATTERNS = ".*(?i)(\\.(mid|mp2|mp3|mp4" +
        "|wav))$";
    public static final String VIDEO_PATTERNS = ".*(?i)(\\.(avi|mov|mpeg|ram" +
        "|rm|smil|wmv))$";
    public static final String MISC_PATTERNS = ".*(?i)(\\.(doc|pdf|ppt|swf))$";
    public static final String ALL_DEFAULT_PATTERNS = ".*(?i)(\\.(bmp|gif" +
        "|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|rm|smil|wmv" +
        "|doc|pdf|ppt|swf))$";

    public static final String ALL = "All";
    public static final String IMAGES = "Images";
    public static final String AUDIO = "Audio";
    public static final String VIDEO = "Video";
    public static final String MISC = "Miscellaneous";
    public static final String CUSTOM = "Custom";
    
    /**
     * @param name
     */
    public FilePatternFilter(String name) {
        super(name);
        setDescription("By default this filter is URI path suffix filter. " +
            "All URLs that end with the specified pattern(s) will be added " +
            "to the scope's focus. Default file patterns are:\n.avi, .bmp, " +
            ".doc, .gif, .jp(e)g, .mid, .mov, .mp2, .mp3, .mp4, .mpeg, " +
            ".pdf, .png, .ppt, .ram, .rm,.smil, .swf, .tif(f), .wav, .wmv");

        String[] options = new String[] {ALL, IMAGES, AUDIO, VIDEO, MISC, 
                                            CUSTOM};
                                
        addElementToDefinition(
            new SimpleType(ATTR_USE_DEFAULT, "URLs that match selected file " +
                "patterns will be crawled. Default file patterns are:\n" +
                "Images: .bmp, .gif, .jp(e)g, .png, .tif(f)\nAudio: .mid, " +
                ".mp2, .mp3, .mp4, .wav\nVideo: .avi, .mov, .mpeg, .ram, " +
                ".rm, .smil, .wmv\nMiscellaneous: .doc, .pdf, .ppt, .swf\n" +
                "All: All above patterns\nChoose 'Custom' to specify your own" +
                " pattern. These default patterns are case insensitive.",
                "All", options));
                
        addElementToDefinition(
            new SimpleType(ATTR_REGEXP, "Java regular expression.", ""));


    }

    /** 
     * @see org.archive.crawler.filter.URIRegExpFilter#getRegexp(java.lang.Object)
     */
    protected String getRegexp(Object o) {
        try {
            String patternType = (String)getAttribute(getSettingsFromObject(o),
                ATTR_USE_DEFAULT);
            
            if (patternType.equals(ALL)){
                return ALL_DEFAULT_PATTERNS;   
            } else if (patternType.equals(IMAGES)){
                return IMAGES_PATTERNS;
            }else if (patternType.equals(AUDIO)){
                return AUDIO_PATTERNS;
            }else if(patternType.equals(VIDEO)){
                return VIDEO_PATTERNS;
            }else if(patternType.equals(MISC)){
                return MISC_PATTERNS;
            }else if(patternType.equals(CUSTOM)){                            
                return (String) getAttribute(
                        getSettingsFromObject(o), ATTR_REGEXP);
                        
            }else {
                assert false : "Unrecognized pattern type " + patternType +
                               ". Should never happened!";
            }
                
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        
        return null;  // Basically the filter is inactive if this occurs.
    }
}
