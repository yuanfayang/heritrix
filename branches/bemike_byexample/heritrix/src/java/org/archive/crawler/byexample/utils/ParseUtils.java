package org.archive.crawler.byexample.utils;

import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;

public class ParseUtils {
    
    /**
     * Receives a web page and converts it to array of terms
     * @param cs
     * @return
     */
    public static String[] tokenizer(CharSequence cs){
        
        //Extract the text contents out of the html code  
        StringBuffer textContents = new StringBuffer();
        Parser htmlParser;
        StringBean sb = new StringBean();
        
        //Ignore empty pages
        int end = cs.length();
        if(end <= 0)
            return null;
            
       //use HtmlParser to parse the text contents of the page
        htmlParser = Parser.createParser((cs.subSequence(0, end - 1)).toString(),null); 
        sb.setCollapse(true);
        sb.setLinks(false);
        try {
            htmlParser.visitAllNodesWith(sb);
        } catch (ParserException e) {
            throw new RuntimeException("Failed to parse",e);
        }
        textContents.append(sb.getStrings());
        
        // Ignore pages with empty text contents     
        if(textContents == null){
            return null; 
        }
        
        return textContents.toString().toLowerCase().split("\\W");        
    }

} //END OF CLASS
