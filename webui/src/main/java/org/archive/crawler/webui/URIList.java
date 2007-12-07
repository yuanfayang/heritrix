/**
 * 
 */
package org.archive.crawler.webui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.management.openmbean.CompositeData;
import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.framework.Frontier;

/**
 * @author pjack
 *
 */
public class URIList {

    
    private boolean verbose = false;
    private int num = 1000;
    private String regex = "^.*?$";
    private String marker = null;
    private Collection<String> uriList;


    public URIList() {
        uriList = Collections.emptySet();
    }
    
    
    public void toAttributes(HttpServletRequest request) {
        request.setAttribute("verbose", verbose);
        request.setAttribute("num", num);
        request.setAttribute("regex", regex);
        request.setAttribute("marker", marker);
        request.setAttribute("uriList", uriList);
    }
    
    
    public void fromForm(HttpServletRequest request) {
        this.verbose = request.getParameter("verbose") != null;
        this.num = Integer.parseInt(request.getParameter("num"));
        this.regex = request.getParameter("regex");
        if (request.getParameter("action").equals("Start Over")) {
            this.marker = "null";
        } else {
            this.marker = request.getParameter("marker");
        }
    }
    
    
    public void update(Frontier frontier) {
        CompositeData cd = frontier.getURIsList(marker, num, regex, verbose);
        this.marker = (String)cd.get("marker");
        String[] arr = (String[])cd.get("list");
        this.uriList = Arrays.asList(arr);
    }


}
