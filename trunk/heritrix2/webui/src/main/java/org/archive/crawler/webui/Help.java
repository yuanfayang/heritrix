package org.archive.crawler.webui;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Help {

    public static void showHelp(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_help.jsp");
    }
    
    public static void showAbout(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_about_ui.jsp");
    }
    
    public static void showCodes(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_codes.jsp");
    }
    
    public static void showRegExpr(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_regexpr.jsp");
    }
    
    public static void showWebUIPrefs(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Misc.forward(request, response, "page_webui-prefs.jsp");
    }
    
}
